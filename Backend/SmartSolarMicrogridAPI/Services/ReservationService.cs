using System.Globalization;
using System.Security.Authentication;
using Microsoft.Extensions.Options;
using MongoDB.Bson;
using MongoDB.Driver;
using SmartSolarMicrogridAPI.Models;

namespace SmartSolarMicrogridAPI.Services;

/// <summary>
/// Member 3 — Energy Booking &amp; Reservation module.
/// Owns EnergyBookingSlots + EnergyReservation, and reads/updates
/// SolarStationInfo.AvailableCapacity directly (same pattern FieldOperationService
/// already uses to reach across into Member 2's collection).
/// </summary>
public class ReservationService
{
    private readonly IMongoCollection<EnergyBookingSlot> _slots;
    private readonly IMongoCollection<EnergyReservation> _reservations;
    private readonly IMongoCollection<SolarStationInfo> _stations;
    private readonly ILogger<ReservationService> _logger;
    private readonly string _databaseName;
    private Exception? _initError;

    public ReservationService(IOptions<MongoDBSettings> settings, ILogger<ReservationService> logger)
    {
        _logger = logger;
        _databaseName = settings.Value.DatabaseName;

        var url = new MongoUrl(settings.Value.ConnectionString);
        var clientSettings = MongoClientSettings.FromUrl(url);
        clientSettings.SslSettings = new SslSettings
        {
            EnabledSslProtocols = SslProtocols.Tls12 | SslProtocols.Tls13,
            CheckCertificateRevocation = false
        };
        clientSettings.ConnectTimeout = TimeSpan.FromSeconds(10);
        clientSettings.ServerSelectionTimeout = TimeSpan.FromSeconds(10);

        var client = new MongoClient(clientSettings);
        var database = client.GetDatabase(_databaseName);
        _slots = database.GetCollection<EnergyBookingSlot>("EnergyBookingSlots");
        _reservations = database.GetCollection<EnergyReservation>("EnergyReservation");
        _stations = database.GetCollection<SolarStationInfo>("SolarStationInfo");

        try
        {
            var slotIndex = Builders<EnergyBookingSlot>.IndexKeys.Ascending(s => s.SlotId);
            _slots.Indexes.CreateOne(new CreateIndexModel<EnergyBookingSlot>(slotIndex, new CreateIndexOptions { Unique = true }));

            var resIndex = Builders<EnergyReservation>.IndexKeys.Ascending(r => r.ReservationId);
            _reservations.Indexes.CreateOne(new CreateIndexModel<EnergyReservation>(resIndex, new CreateIndexOptions { Unique = true }));

            SeedIfEmpty();
            _logger.LogInformation("ReservationService ready: database '{Db}', collections 'EnergyBookingSlots' + 'EnergyReservation'.", _databaseName);
        }
        catch (Exception ex)
        {
            _initError = ex;
            _logger.LogError(ex, "MongoDB initialization failed for Reservation module.");
        }
    }

    private void EnsureReady()
    {
        if (_initError is not null)
            throw new InvalidOperationException(
                $"Database unavailable for Reservations ({_initError.GetType().Name}: {_initError.Message}).",
                _initError);
    }

    // ---------------------------------------------------------------
    // Booking slots
    // ---------------------------------------------------------------

    public async Task<List<EnergyBookingSlot>> GetSlotsAsync(string? stationId, DateTime? date)
    {
        EnsureReady();
        var filter = Builders<EnergyBookingSlot>.Filter.Empty;
        if (!string.IsNullOrWhiteSpace(stationId))
            filter &= Builders<EnergyBookingSlot>.Filter.Eq(s => s.StationId, stationId);
        if (date is not null)
            filter &= Builders<EnergyBookingSlot>.Filter.Eq(s => s.Date, date.Value.Date);

        return await _slots.Find(filter).SortBy(s => s.Date).ThenBy(s => s.SlotId).ToListAsync();
    }

    public async Task<EnergyBookingSlot?> GetSlotByIdAsync(string slotId)
    {
        EnsureReady();
        return await _slots.Find(s => s.SlotId == slotId).FirstOrDefaultAsync();
    }

    // ---------------------------------------------------------------
    // Reservations
    // ---------------------------------------------------------------

    public async Task<List<EnergyReservation>> GetAllReservationsAsync()
    {
        EnsureReady();
        return await _reservations.Find(_ => true).SortByDescending(r => r.CreatedAt).ToListAsync();
    }

    public async Task<EnergyReservation?> GetReservationByIdAsync(string reservationId)
    {
        EnsureReady();
        return await _reservations.Find(r => r.ReservationId == reservationId).FirstOrDefaultAsync();
    }

    public async Task<List<EnergyReservation>> GetUserReservationsAsync(string userId)
    {
        EnsureReady();
        return await _reservations.Find(r => r.UserId == userId)
            .SortByDescending(r => r.CreatedAt)
            .ToListAsync();
    }

    /// <summary>
    /// Runs the full reservation-creation flow described in the module spec:
    /// station exists -> station active -> booking date within 7 days ->
    /// slot exists/available -> requested energy fits -> create -> reduce capacity.
    /// </summary>
    public async Task<(bool Success, int StatusCode, string Message, EnergyReservation? Reservation)> CreateReservationAsync(CreateReservationDto dto)
    {
        EnsureReady();

        var station = await _stations.Find(s => s.StationId == dto.StationId).FirstOrDefaultAsync();
        if (station is null)
            return (false, 404, $"Station {dto.StationId} not found.", null);

        if (!string.Equals(station.Status, "Active", StringComparison.OrdinalIgnoreCase))
            return (false, 400, $"Station {dto.StationId} is not active (status: {station.Status}). Reservation rejected.", null);

        var slot = await _slots.Find(s => s.SlotId == dto.SlotId).FirstOrDefaultAsync();
        if (slot is null)
            return (false, 404, $"Booking slot {dto.SlotId} not found.", null);

        if (slot.StationId != dto.StationId)
            return (false, 400, $"Slot {dto.SlotId} does not belong to station {dto.StationId}.", null);

        // Rule 1: 7-day booking window (relative to today, inclusive).
        var today = DateTime.UtcNow.Date;
        if (slot.Date.Date < today || slot.Date.Date > today.AddDays(7))
            return (false, 400, "Booking date must be within the next 7 days.", null);

        if (!string.Equals(slot.Status, "Available", StringComparison.OrdinalIgnoreCase))
            return (false, 400, $"Slot {dto.SlotId} is not available (status: {slot.Status}).", null);

        // Rule 3: requested energy must fit within the slot's remaining energy.
        if (dto.EnergyAmount > slot.AvailableEnergy)
            return (false, 400, $"Requested {dto.EnergyAmount} kWh exceeds available {slot.AvailableEnergy} kWh for this slot.", null);

        var reservationId = await NextReservationIdAsync();
        var verificationCode = $"SG-{reservationId}";

        var reservation = new EnergyReservation
        {
            ReservationId = reservationId,
            UserId = dto.UserId,
            StationId = dto.StationId,
            SlotId = dto.SlotId,
            Date = slot.Date,
            StartTime = slot.StartTime,
            EndTime = slot.EndTime,
            EnergyAmount = dto.EnergyAmount,
            Status = "Confirmed",
            CreatedAt = DateTime.UtcNow,
            VerificationCode = verificationCode
        };

        await _reservations.InsertOneAsync(reservation);

        // Rule 4: reduce available energy on the slot, and mirror the change
        // onto the station's AvailableCapacity so Member 2/4 see it too.
        var newSlotAvailable = Math.Round(slot.AvailableEnergy - dto.EnergyAmount, 2);
        var slotUpdate = Builders<EnergyBookingSlot>.Update
            .Set(s => s.AvailableEnergy, newSlotAvailable)
            .Set(s => s.Status, newSlotAvailable <= 0 ? "Full" : "Available");
        await _slots.UpdateOneAsync(s => s.SlotId == dto.SlotId, slotUpdate);

        var newStationAvailable = Math.Max(0, Math.Round(station.AvailableCapacity - dto.EnergyAmount, 2));
        await _stations.UpdateOneAsync(
            s => s.StationId == dto.StationId,
            Builders<SolarStationInfo>.Update
                .Set(s => s.AvailableCapacity, newStationAvailable)
                .Set(s => s.LastUpdated, DateTime.UtcNow));

        return (true, 201, "Reservation confirmed.", reservation);
    }

    /// <summary>
    /// 12-hour cancellation rule: only allowed if the booked start time is
    /// still at least 12 hours away.
    /// </summary>
    public async Task<(bool Success, int StatusCode, string Message, EnergyReservation? Reservation)> CancelReservationAsync(string reservationId)
    {
        EnsureReady();

        var reservation = await _reservations.Find(r => r.ReservationId == reservationId).FirstOrDefaultAsync();
        if (reservation is null)
            return (false, 404, $"Reservation {reservationId} not found.", null);

        if (string.Equals(reservation.Status, "Cancelled", StringComparison.OrdinalIgnoreCase))
            return (false, 400, "Reservation is already cancelled.", reservation);

        if (string.Equals(reservation.Status, "Completed", StringComparison.OrdinalIgnoreCase))
            return (false, 400, "Completed reservations cannot be cancelled.", reservation);

        var bookingStart = CombineDateAndTime(reservation.Date, reservation.StartTime);
        var hoursRemaining = (bookingStart - DateTime.UtcNow).TotalHours;

        if (hoursRemaining < 12)
            return (false, 400, $"Cancellation is only allowed until 12 hours before the booking. Only {Math.Max(0, hoursRemaining):F1} hour(s) remain.", reservation);

        var now = DateTime.UtcNow;
        var update = Builders<EnergyReservation>.Update
            .Set(r => r.Status, "Cancelled")
            .Set(r => r.CancelledAt, now);
        await _reservations.UpdateOneAsync(r => r.ReservationId == reservationId, update);
        reservation.Status = "Cancelled";
        reservation.CancelledAt = now;

        // Restore energy back onto the slot and the station.
        var slot = await _slots.Find(s => s.SlotId == reservation.SlotId).FirstOrDefaultAsync();
        if (slot is not null)
        {
            var restoredSlot = slot.AvailableEnergy + reservation.EnergyAmount;
            await _slots.UpdateOneAsync(
                s => s.SlotId == reservation.SlotId,
                Builders<EnergyBookingSlot>.Update
                    .Set(s => s.AvailableEnergy, restoredSlot)
                    .Set(s => s.Status, "Available"));
        }

        var station = await _stations.Find(s => s.StationId == reservation.StationId).FirstOrDefaultAsync();
        if (station is not null)
        {
            var restoredStation = Math.Min(station.TotalCapacity, station.AvailableCapacity + reservation.EnergyAmount);
            await _stations.UpdateOneAsync(
                s => s.StationId == reservation.StationId,
                Builders<SolarStationInfo>.Update
                    .Set(s => s.AvailableCapacity, restoredStation)
                    .Set(s => s.LastUpdated, DateTime.UtcNow));
        }

        return (true, 200, "Reservation cancelled.", reservation);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static DateTime CombineDateAndTime(DateTime date, string time)
    {
        // Slot times are stored like "10:00 AM". Fall back to midnight if parsing fails
        // rather than throwing, so a malformed sample row can't block cancellation forever.
        if (DateTime.TryParseExact(time, "h:mm tt", CultureInfo.InvariantCulture, DateTimeStyles.None, out var parsedTime))
            return date.Date.Add(parsedTime.TimeOfDay);

        return DateTime.TryParse(time, CultureInfo.InvariantCulture, DateTimeStyles.None, out var fallback)
            ? date.Date.Add(fallback.TimeOfDay)
            : date.Date;
    }

    private async Task<string> NextReservationIdAsync()
    {
        var count = await _reservations.CountDocumentsAsync(FilterDefinition<EnergyReservation>.Empty);
        var next = count + 1;
        var candidate = $"RES{next:000}";

        // Guard against gaps left by cancelled/deleted test data reusing an id.
        while (await _reservations.Find(r => r.ReservationId == candidate).AnyAsync())
        {
            next++;
            candidate = $"RES{next:000}";
        }

        return candidate;
    }

    private void SeedIfEmpty()
    {
        if (_slots.CountDocuments(_ => true) > 0) return;

        var today = DateTime.UtcNow.Date;
        var seed = new List<EnergyBookingSlot>
        {
            new() { SlotId = "SLOT001", StationId = "ST001", Date = today.AddDays(2), StartTime = "10:00 AM", EndTime = "12:00 PM", AvailableEnergy = 50, Status = "Available" },
            new() { SlotId = "SLOT002", StationId = "ST001", Date = today.AddDays(2), StartTime = "1:00 PM",  EndTime = "3:00 PM",  AvailableEnergy = 40, Status = "Available" },
            new() { SlotId = "SLOT003", StationId = "ST002", Date = today.AddDays(3), StartTime = "9:00 AM",  EndTime = "11:00 AM", AvailableEnergy = 30, Status = "Available" },
            new() { SlotId = "SLOT004", StationId = "ST004", Date = today.AddDays(4), StartTime = "2:00 PM",  EndTime = "4:00 PM",  AvailableEnergy = 60, Status = "Available" },
        };
        _slots.InsertMany(seed);
    }
}

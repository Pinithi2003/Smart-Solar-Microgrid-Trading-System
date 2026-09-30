// ------------------------------------------------------------
// Project     : Smart Solar Microgrid Trading System
// Module      : Energy Booking and Reservation (Member 3)
// Course      : SE4040 Enterprise Application Development
// File        : ReservationService.cs
// Description : Business rules for booking slots and energy reservations.
// ------------------------------------------------------------

using System.Globalization;
using System.Security.Authentication;
using System.Text.RegularExpressions;
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
    private readonly IMongoCollection<User> _users;
    private readonly ILogger<ReservationService> _logger;
    private readonly string _databaseName;
    private Exception? _initError;

    public ReservationService(IOptions<MongoDBSettings> settings, ILogger<ReservationService> logger)
    {
        // Opens MongoDB and prepares the booking slot and reservation collections.
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
        _users = database.GetCollection<User>("Users");

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
        // Stops the request when MongoDB failed to start.
        if (_initError is not null)
            throw new InvalidOperationException(
                $"Database unavailable for Reservations ({_initError.GetType().Name}: {_initError.Message}).",
                _initError);
    }

    // ---------------------------------------------------------------
    // Booking slots
    // ---------------------------------------------------------------

    public async Task<List<EnergyBookingSlot>> GetAvailableSlots() {
        // Returns every booking slot.
        return await GetSlotsAsync(null, null);
    }

    public async Task<List<EnergyBookingSlot>> GetSlotsAsync(string? stationId, DateTime? date)
    {
        // Returns booking slots, optionally filtered by station and calendar date.
        EnsureReady();
        var filter = Builders<EnergyBookingSlot>.Filter.Empty;
        if (!string.IsNullOrWhiteSpace(stationId))
            filter &= Builders<EnergyBookingSlot>.Filter.Eq(s => s.StationId, stationId);

        var slots = await _slots.Find(filter).SortBy(s => s.Date).ThenBy(s => s.SlotId).ToListAsync();
        if (date is null)
            return slots;

        var target = CalendarUtc(date.Value);
        return slots.Where(s => CalendarUtc(s.Date) == target).ToList();
    }

    public async Task<EnergyBookingSlot?> GetSlotByIdAsync(string slotId)
    {
        // Returns one booking slot by its slot id.
        EnsureReady();
        return await _slots.Find(s => s.SlotId == slotId).FirstOrDefaultAsync();
    }

    public async Task<(bool Success, int StatusCode, string Message, EnergyBookingSlot? Slot)> CreateSlotAsync(CreateBookingSlotDto dto)
    {
        // Creates a slot on an active station inside the 7-day window.
        EnsureReady();

        var station = await _stations.Find(s => s.StationId == dto.StationId).FirstOrDefaultAsync();
        if (station is null)
            return (false, 404, $"Station {dto.StationId} not found.", null);

        if (!string.Equals(station.Status, "Active", StringComparison.OrdinalIgnoreCase))
            return (false, 400, $"Station {dto.StationId} is not active (status: {station.Status}).", null);

        var today = CalendarUtc(DateTime.Now);
        var requestedDay = CalendarUtc(dto.Date);
        if (requestedDay < today || requestedDay > today.AddDays(7))
            return (false, 400, "Booking is only allowed within the 7-day booking window.", null);

        if (dto.EnergyCapacity > station.AvailableCapacity)
            return (false, 400, $"Slot capacity {dto.EnergyCapacity} kWh exceeds station available capacity of {station.AvailableCapacity} kWh.", null);

        var slotDate = CalendarUtc(dto.Date);
        var sameDay = await _slots.Find(s => s.StationId == dto.StationId).ToListAsync();
        if (sameDay.Any(s => CalendarUtc(s.Date) == slotDate && s.StartTime == dto.StartTime.Trim()))
            return (false, 409, "A booking slot already exists for this station, date and start time.", null);

        var slot = new EnergyBookingSlot
        {
            SlotId = await NextSlotIdAsync(),
            StationId = dto.StationId,
            Date = slotDate,
            StartTime = dto.StartTime.Trim(),
            EndTime = dto.EndTime.Trim(),
            EnergyCapacity = dto.EnergyCapacity,
            AvailableEnergy = dto.EnergyCapacity,
            Status = "Available"
        };

        await _slots.InsertOneAsync(slot);
        return (true, 201, "Booking slot created.", slot);
    }

    public async Task<(bool Success, int StatusCode, string Message, EnergyBookingSlot? Slot)> UpdateSlotAsync(string slotId, UpdateBookingSlotDto dto)
    {
        // Updates a slot date, time, capacity or status.
        EnsureReady();

        var slot = await _slots.Find(s => s.SlotId == slotId).FirstOrDefaultAsync();
        if (slot is null)
            return (false, 404, $"Slot {slotId} not found.", null);

        if (dto.Date is not null)
        {
            var today = CalendarUtc(DateTime.Now);
            var next = CalendarUtc(dto.Date.Value);
            if (next < today || next > today.AddDays(7))
                return (false, 400, "Booking is only allowed within the 7-day booking window.", slot);
            slot.Date = next;
        }

        if (!string.IsNullOrWhiteSpace(dto.StartTime))
            slot.StartTime = dto.StartTime.Trim();
        if (!string.IsNullOrWhiteSpace(dto.EndTime))
            slot.EndTime = dto.EndTime.Trim();

        if (dto.EnergyCapacity is not null)
        {
            var reserved = Math.Max(0, slot.EnergyCapacity - slot.AvailableEnergy);
            if (dto.EnergyCapacity.Value < reserved)
                return (false, 400, $"Capacity cannot drop below the {reserved} kWh already reserved.", slot);
            slot.EnergyCapacity = dto.EnergyCapacity.Value;
            slot.AvailableEnergy = Math.Round(dto.EnergyCapacity.Value - reserved, 2);
        }

        if (!string.IsNullOrWhiteSpace(dto.Status))
        {
            if (!EnergyBookingSlot.AllowedStatuses.Contains(dto.Status))
                return (false, 400, $"Status must be one of: {string.Join(", ", EnergyBookingSlot.AllowedStatuses)}.", slot);
            slot.Status = dto.Status;
        }
        else if (slot.AvailableEnergy <= 0)
            slot.Status = "Full";
        else if (slot.Status == "Full")
            slot.Status = "Available";

        await _slots.ReplaceOneAsync(s => s.SlotId == slotId, slot);
        return (true, 200, "Booking slot updated.", slot);
    }

    public async Task<(bool Success, int StatusCode, string Message)> DeleteSlotAsync(string slotId)
    {
        // Deletes a slot that has no confirmed reservation.
        EnsureReady();

        var slot = await _slots.Find(s => s.SlotId == slotId).FirstOrDefaultAsync();
        if (slot is null)
            return (false, 404, $"Slot {slotId} not found.");

        var inUse = await _reservations.Find(r => r.SlotId == slotId && r.Status == "Confirmed").AnyAsync();
        if (inUse)
            return (false, 409, "This slot has a confirmed reservation. Cancel that reservation before deleting the slot.");

        await _slots.DeleteOneAsync(s => s.SlotId == slotId);
        return (true, 200, $"Slot {slotId} deleted.");
    }

    public async Task<(bool Success, int StatusCode, string Message)> DeleteReservationAsync(
        string reservationId, string? callerUserId = null, string? callerRole = null)
    {
        // Deletes a reservation, cancelling a confirmed booking first.
        EnsureReady();

        var reservation = await _reservations.Find(r => r.ReservationId == reservationId).FirstOrDefaultAsync();
        if (reservation is null)
            return (false, 404, "Reservation not found.");

        if (!CanManage(callerUserId, callerRole, reservation.UserId))
            return (false, 403, "You can only manage your own reservations.");

        if (string.Equals(reservation.Status, "Confirmed", StringComparison.OrdinalIgnoreCase))
        {
            var cancelled = await CancelReservationAsync(reservationId, callerUserId, callerRole);
            if (!cancelled.Success)
                return (false, cancelled.StatusCode, cancelled.Message);
        }
        else if (string.Equals(reservation.Status, "Verified", StringComparison.OrdinalIgnoreCase) ||
                 string.Equals(reservation.Status, "Completed", StringComparison.OrdinalIgnoreCase))
        {
            return (false, 400, $"{reservation.Status} reservations cannot be deleted.");
        }

        await _reservations.DeleteOneAsync(r => r.ReservationId == reservationId);
        return (true, 200, $"Reservation {reservationId} deleted.");
    }

    // ---------------------------------------------------------------
    // Reservations
    // ---------------------------------------------------------------

    public async Task<List<EnergyReservation>> GetAllReservationsAsync()
    {
        // Returns every reservation, newest first.
        EnsureReady();
        return await _reservations.Find(_ => true).SortByDescending(r => r.CreatedAt).ToListAsync();
    }

    public async Task<EnergyReservation?> GetReservationByIdAsync(string reservationId)
    {
        // Returns one reservation by its reservation id.
        EnsureReady();
        return await _reservations.Find(r => r.ReservationId == reservationId).FirstOrDefaultAsync();
    }

    public async Task<List<EnergyReservation>> GetUserReservationsAsync(string userId)
    {
        // Returns the reservations that belong to one user.
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
        // Validates the booking, stores the reservation and consumes energy.
        EnsureReady();

        var (ok, statusCode, message, station, slot) = await ValidateBooking(dto);
        if (!ok || station is null || slot is null)
            return (false, statusCode, message, null);

        var reservationId = await NextReservationIdAsync();
        var verificationCode = $"SG-{reservationId}";

        var now = DateTime.Now;
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
            CreatedAt = now,
            UpdatedAt = now,
            VerificationCode = verificationCode
        };

        // Consume capacity first so two requests cannot book the same kWh.
        if (!await TryConsumeEnergyAsync(dto.SlotId, dto.StationId, dto.EnergyAmount))
            return (false, 409, "This booking slot is no longer available.", null);

        try
        {
            await _reservations.InsertOneAsync(reservation);
        }
        catch
        {
            await RestoreEnergyAsync(dto.SlotId, dto.StationId, dto.EnergyAmount);
            throw;
        }

        return (true, 201, "Reservation confirmed.", reservation);
    }

    /// <summary>
    /// 12-hour cancellation rule: only allowed if the booked start time is
    /// still at least 12 hours away.
    /// </summary>
    public async Task<(bool Success, int StatusCode, string Message, EnergyReservation? Reservation)> CancelReservationAsync(
        string reservationId, string? callerUserId = null, string? callerRole = null)
    {
        // Cancels a confirmed reservation when at least 12 hours remain.
        EnsureReady();

        var reservation = await _reservations.Find(r => r.ReservationId == reservationId).FirstOrDefaultAsync();
        if (reservation is null)
            return (false, 404, "Reservation not found.", null);

        if (!CanManage(callerUserId, callerRole, reservation.UserId))
            return (false, 403, "You can only manage your own reservations.", reservation);

        if (string.Equals(reservation.Status, "Cancelled", StringComparison.OrdinalIgnoreCase))
            return (false, 400, "This reservation has already been cancelled.", reservation);

        if (!string.Equals(reservation.Status, "Confirmed", StringComparison.OrdinalIgnoreCase))
            return (false, 400, $"{reservation.Status} reservations cannot be cancelled.", reservation);

        if (!HasTwelveHoursRemaining(reservation))
            return (false, 400, "Cancellation is not allowed within 12 hours of the booking time.", reservation);

        var now = DateTime.Now;
        var update = Builders<EnergyReservation>.Update
            .Set(r => r.Status, "Cancelled")
            .Set(r => r.CancelledAt, now)
            .Set(r => r.UpdatedAt, now);
        await _reservations.UpdateOneAsync(r => r.ReservationId == reservationId, update);
        reservation.Status = "Cancelled";
        reservation.CancelledAt = now;
        reservation.UpdatedAt = now;

        await RestoreEnergyAsync(reservation.SlotId, reservation.StationId, reservation.EnergyAmount);
        return (true, 200, "Reservation cancelled.", reservation);
    }

    public async Task<(bool Success, int StatusCode, string Message, EnergyReservation? Reservation)> UpdateReservationAsync(
        string reservationId, UpdateReservationDto dto, string? callerUserId = null, string? callerRole = null)
    {
        // Updates energy or slot when at least 12 hours remain.
        EnsureReady();

        var reservation = await _reservations.Find(r => r.ReservationId == reservationId).FirstOrDefaultAsync();
        if (reservation is null)
            return (false, 404, "Reservation not found.", null);

        if (!CanManage(callerUserId, callerRole, reservation.UserId))
            return (false, 403, "You can only manage your own reservations.", reservation);

        if (!string.Equals(reservation.Status, "Confirmed", StringComparison.OrdinalIgnoreCase))
            return (false, 400, "Only confirmed reservations can be updated.", reservation);

        if (!HasTwelveHoursRemaining(reservation))
            return (false, 400, "Updates are not allowed within 12 hours of the booking time.", reservation);

        var newSlotId = string.IsNullOrWhiteSpace(dto.SlotId) ? reservation.SlotId : dto.SlotId.Trim();
        var newAmount = dto.EnergyAmount ?? reservation.EnergyAmount;
        if (newSlotId == reservation.SlotId && Math.Abs(newAmount - reservation.EnergyAmount) < 0.001)
            return (true, 200, "Reservation unchanged.", reservation);

        if (newSlotId == reservation.SlotId)
        {
            var delta = Math.Round(newAmount - reservation.EnergyAmount, 2);
            if (delta > 0 && !await TryConsumeEnergyAsync(reservation.SlotId, reservation.StationId, delta))
                return (false, 400, "Requested energy exceeds the available energy.", reservation);
            if (delta < 0)
                await RestoreEnergyAsync(reservation.SlotId, reservation.StationId, -delta);
        }
        else
        {
            var previousSlotId = reservation.SlotId;
            var previousAmount = reservation.EnergyAmount;
            await RestoreEnergyAsync(previousSlotId, reservation.StationId, previousAmount);

            var check = await ValidateBooking(new CreateReservationDto
            {
                UserId = reservation.UserId,
                StationId = reservation.StationId,
                SlotId = newSlotId,
                EnergyAmount = newAmount
            }, ignoreReservationId: reservation.ReservationId);

            if (!check.Ok || check.Slot is null || !await TryConsumeEnergyAsync(newSlotId, reservation.StationId, newAmount))
            {
                await TryConsumeEnergyAsync(previousSlotId, reservation.StationId, previousAmount);
                return (false, check.Ok ? 409 : check.StatusCode, check.Ok ? "This booking slot is no longer available." : check.Message, reservation);
            }

            reservation.SlotId = check.Slot.SlotId;
            reservation.Date = check.Slot.Date;
            reservation.StartTime = check.Slot.StartTime;
            reservation.EndTime = check.Slot.EndTime;
        }

        var now = DateTime.Now;
        reservation.EnergyAmount = newAmount;
        reservation.UpdatedAt = now;
        await _reservations.ReplaceOneAsync(r => r.ReservationId == reservationId, reservation);
        return (true, 200, "Reservation updated.", reservation);
    }

    /// <summary>
    /// Allowed transitions: Confirmed → Verified, Verified → Completed.
    /// Confirmed → Cancelled is accepted here only when the 12-hour rule passes,
    /// and it restores energy the same way as the cancel endpoint.
    /// </summary>
    public async Task<(bool Success, int StatusCode, string Message, EnergyReservation? Reservation)> UpdateStatusAsync(
        string reservationId, string status, string? callerUserId = null, string? callerRole = null)
    {
        // Moves a reservation to Verified, Completed or Cancelled.
        EnsureReady();

        var reservation = await _reservations.Find(r => r.ReservationId == reservationId).FirstOrDefaultAsync();
        if (reservation is null)
            return (false, 404, "Reservation not found.", null);

        if (!CanManage(callerUserId, callerRole, reservation.UserId))
            return (false, 403, "You can only manage your own reservations.", reservation);

        var next = status.Trim();
        var current = reservation.Status;
        var allowed =
            (current.Equals("Confirmed", StringComparison.OrdinalIgnoreCase) && next.Equals("Verified", StringComparison.OrdinalIgnoreCase)) ||
            (current.Equals("Verified", StringComparison.OrdinalIgnoreCase) && next.Equals("Completed", StringComparison.OrdinalIgnoreCase)) ||
            (current.Equals("Confirmed", StringComparison.OrdinalIgnoreCase) && next.Equals("Cancelled", StringComparison.OrdinalIgnoreCase));

        if (!allowed)
            return (false, 400, $"Cannot change status from {current} to {next}.", reservation);

        if (next.Equals("Cancelled", StringComparison.OrdinalIgnoreCase))
            return await CancelReservationAsync(reservationId, callerUserId, callerRole);

        var now = DateTime.Now;
        await _reservations.UpdateOneAsync(
            r => r.ReservationId == reservationId,
            Builders<EnergyReservation>.Update
                .Set(r => r.Status, next.Equals("Verified", StringComparison.OrdinalIgnoreCase) ? "Verified" : "Completed")
                .Set(r => r.UpdatedAt, now));
        reservation.Status = next.Equals("Verified", StringComparison.OrdinalIgnoreCase) ? "Verified" : "Completed";
        reservation.UpdatedAt = now;
        return (true, 200, $"Reservation marked {reservation.Status}.", reservation);
    }

    /// <summary>
    /// Shared booking checks used by CreateReservation: station exists and is Active,
    /// slot exists, date is inside the 7-day window, slot is Available, and requested
    /// energy fits both the slot and the station remaining capacity.
    /// </summary>
    public async Task<(bool Ok, int StatusCode, string Message, SolarStationInfo? Station, EnergyBookingSlot? Slot)>
        ValidateBooking(CreateReservationDto dto, string? ignoreReservationId = null)
    {
        // Checks station, slot, 7-day window, capacity and duplicate bookings.
        EnsureReady();

        if (dto.EnergyAmount <= 0)
            return (false, 400, "Energy amount must be greater than 0 kWh.", null, null);

        if (!await UserExistsAsync(dto.UserId))
            return (false, 404, "User not found.", null, null);

        var station = await _stations.Find(s => s.StationId == dto.StationId).FirstOrDefaultAsync();
        if (station is null)
            return (false, 404, $"Station {dto.StationId} not found.", null, null);

        if (!string.Equals(station.Status, "Active", StringComparison.OrdinalIgnoreCase))
            return (false, 400, $"Station {dto.StationId} is not active (status: {station.Status}). Reservation rejected.", station, null);

        var slot = await _slots.Find(s => s.SlotId == dto.SlotId).FirstOrDefaultAsync();
        if (slot is null)
            return (false, 404, $"Booking slot {dto.SlotId} not found.", station, null);

        if (slot.StationId != dto.StationId)
            return (false, 400, $"Slot {dto.SlotId} does not belong to station {dto.StationId}.", station, slot);

        var today = CalendarUtc(DateTime.Now);
        var slotDay = CalendarUtc(slot.Date);
        if (slotDay < today || slotDay > today.AddDays(7))
            return (false, 400, "Booking is only allowed within the 7-day booking window.", station, slot);

        if (!string.Equals(slot.Status, "Available", StringComparison.OrdinalIgnoreCase))
            return (false, 409, "This booking slot is no longer available.", station, slot);

        if (dto.EnergyAmount > slot.AvailableEnergy || dto.EnergyAmount > station.AvailableCapacity)
            return (false, 400, "Requested energy exceeds the available energy.", station, slot);

        var duplicateFilter = Builders<EnergyReservation>.Filter.Eq(r => r.UserId, dto.UserId) &
                              Builders<EnergyReservation>.Filter.Eq(r => r.SlotId, dto.SlotId) &
                              Builders<EnergyReservation>.Filter.Eq(r => r.Status, "Confirmed");
        if (!string.IsNullOrWhiteSpace(ignoreReservationId))
            duplicateFilter &= Builders<EnergyReservation>.Filter.Ne(r => r.ReservationId, ignoreReservationId);

        if (await _reservations.Find(duplicateFilter).AnyAsync())
            return (false, 409, "You already have a confirmed reservation for this booking slot.", station, slot);

        return (true, 200, "Booking is valid.", station, slot);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static bool CanManage(string? callerUserId, string? callerRole, string ownerUserId)
    {
        // Allows the owner, or Backoffice and Grid Operator, to change a reservation.
        if (string.IsNullOrWhiteSpace(callerUserId))
            return true;

        if (callerRole is "Backoffice" or "Grid Operator")
            return true;

        return string.Equals(callerUserId, ownerUserId, StringComparison.OrdinalIgnoreCase);
    }

    private static bool HasTwelveHoursRemaining(EnergyReservation reservation)
    {
        // Returns true when the slot start is at least 12 hours away.
        var bookingStart = CombineDateAndTime(reservation.Date, reservation.StartTime);
        return (bookingStart - DateTime.Now).TotalHours >= 12;
    }

    private async Task<bool> UserExistsAsync(string userId)
    {
        // Accepts a Mongo id, email, demo user id or Sri Lankan NIC.
        if (string.IsNullOrWhiteSpace(userId))
            return false;

        var id = userId.Trim();
        // User.Id is a Mongo ObjectId. Comparing USR001 (or any other code) throws a 500.
        if (ObjectId.TryParse(id, out _))
        {
            if (await _users.Find(u => u.Id == id).AnyAsync())
                return true;
        }

        if (id.Contains('@'))
        {
            var email = id.ToLowerInvariant();
            if (await _users.Find(u => u.Email == email).AnyAsync())
                return true;
        }

        // Demo ids (USR001) and Sri Lankan NIC values used as the prosumer key.
        return Regex.IsMatch(id, "^USR\\d{3,}$", RegexOptions.IgnoreCase)
            || Regex.IsMatch(id, "^\\d{9}[VvXx]$")
            || Regex.IsMatch(id, "^\\d{12}$");
    }

    private async Task<bool> TryConsumeEnergyAsync(string slotId, string stationId, double amount)
    {
        // Atomically reduces slot and station energy for a booking.
        var slotFilter =
            Builders<EnergyBookingSlot>.Filter.Eq(s => s.SlotId, slotId) &
            Builders<EnergyBookingSlot>.Filter.Eq(s => s.Status, "Available") &
            Builders<EnergyBookingSlot>.Filter.Gte(s => s.AvailableEnergy, amount);

        var consumedSlot = await _slots.FindOneAndUpdateAsync(
            slotFilter,
            Builders<EnergyBookingSlot>.Update.Inc(s => s.AvailableEnergy, -amount),
            new FindOneAndUpdateOptions<EnergyBookingSlot> { ReturnDocument = ReturnDocument.After });

        if (consumedSlot is null)
            return false;

        var slotStatus = consumedSlot.AvailableEnergy <= 0.001 ? "Full" : "Available";
        var normalized = Math.Max(0, Math.Round(consumedSlot.AvailableEnergy, 2));
        await _slots.UpdateOneAsync(
            s => s.SlotId == slotId,
            Builders<EnergyBookingSlot>.Update
                .Set(s => s.AvailableEnergy, normalized)
                .Set(s => s.Status, slotStatus));

        var stationFilter =
            Builders<SolarStationInfo>.Filter.Eq(s => s.StationId, stationId) &
            Builders<SolarStationInfo>.Filter.Gte(s => s.AvailableCapacity, amount);

        var consumedStation = await _stations.FindOneAndUpdateAsync(
            stationFilter,
            Builders<SolarStationInfo>.Update
                .Inc(s => s.AvailableCapacity, -amount)
                .Set(s => s.LastUpdated, DateTime.UtcNow),
            new FindOneAndUpdateOptions<SolarStationInfo> { ReturnDocument = ReturnDocument.After });

        if (consumedStation is null)
        {
            await RestoreSlotOnlyAsync(slotId, amount);
            return false;
        }

        await _stations.UpdateOneAsync(
            s => s.StationId == stationId,
            Builders<SolarStationInfo>.Update.Set(s => s.AvailableCapacity, Math.Max(0, Math.Round(consumedStation.AvailableCapacity, 2))));

        return true;
    }

    private async Task RestoreEnergyAsync(string slotId, string stationId, double amount)
    {
        // Returns energy to the slot and the station.
        await RestoreSlotOnlyAsync(slotId, amount);

        var station = await _stations.Find(s => s.StationId == stationId).FirstOrDefaultAsync();
        if (station is null)
            return;

        var restored = Math.Min(station.TotalCapacity, Math.Round(station.AvailableCapacity + amount, 2));
        await _stations.UpdateOneAsync(
            s => s.StationId == stationId,
            Builders<SolarStationInfo>.Update
                .Set(s => s.AvailableCapacity, restored)
                .Set(s => s.LastUpdated, DateTime.UtcNow));
    }

    private async Task RestoreSlotOnlyAsync(string slotId, double amount)
    {
        // Returns energy to the slot only.
        var slot = await _slots.Find(s => s.SlotId == slotId).FirstOrDefaultAsync();
        if (slot is null)
            return;

        var restored = Math.Round(slot.AvailableEnergy + amount, 2);
        if (slot.EnergyCapacity > 0)
            restored = Math.Min(slot.EnergyCapacity, restored);

        await _slots.UpdateOneAsync(
            s => s.SlotId == slotId,
            Builders<EnergyBookingSlot>.Update
                .Set(s => s.AvailableEnergy, restored)
                .Set(s => s.Status, restored <= 0 ? "Full" : "Available"));
    }

    /// <summary>
    /// Calendar day as UTC midnight. MongoDB was storing local midnight as the previous
    /// evening in UTC (18:30Z in Sri Lanka), so a date filter for "today" missed the slot.
    /// </summary>
    private static DateTime CalendarUtc(DateTime value)
    {
        // Stores the calendar day as UTC midnight so date filters match.
        if (value.Kind == DateTimeKind.Utc && value.TimeOfDay != TimeSpan.Zero)
            return DateTime.SpecifyKind(value.ToLocalTime().Date, DateTimeKind.Utc);

        return DateTime.SpecifyKind(value.Date, DateTimeKind.Utc);
    }

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
        // Builds the next unused reservation id such as RES001.
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

    private async Task<string> NextSlotIdAsync()
    {
        // Builds the next unused slot id such as SLOT001.
        var count = await _slots.CountDocumentsAsync(FilterDefinition<EnergyBookingSlot>.Empty);
        var next = count + 1;
        var candidate = $"SLOT{next:000}";
        while (await _slots.Find(s => s.SlotId == candidate).AnyAsync())
        {
            next++;
            candidate = $"SLOT{next:000}";
        }
        return candidate;
    }

    private void SeedIfEmpty()
    {
        // Inserts sample slots when the collection is missing demo data.
        var today = DateTime.Now.Date;
        var seedPlan = new List<EnergyBookingSlot>
        {
            // Inserts sample slots when the collection is missing demo data.
            new() { SlotId = "SLOT001", StationId = "ST001", Date = today.AddDays(2), StartTime = "10:00 AM", EndTime = "12:00 PM", EnergyCapacity = 50, AvailableEnergy = 50, Status = "Available" },
            new() { SlotId = "SLOT002", StationId = "ST001", Date = today.AddDays(2), StartTime = "1:00 PM",  EndTime = "3:00 PM",  EnergyCapacity = 40, AvailableEnergy = 40, Status = "Available" },
            new() { SlotId = "SLOT003", StationId = "ST002", Date = today.AddDays(3), StartTime = "9:00 AM",  EndTime = "11:00 AM", EnergyCapacity = 30, AvailableEnergy = 30, Status = "Available" },
            new() { SlotId = "SLOT004", StationId = "ST003", Date = today.AddDays(4), StartTime = "2:00 PM",  EndTime = "4:00 PM",  EnergyCapacity = 60, AvailableEnergy = 60, Status = "Available" },
            new() { SlotId = "SLOT005", StationId = "ST004", Date = today.AddDays(1), StartTime = "8:00 AM",  EndTime = "10:00 AM", EnergyCapacity = 45, AvailableEnergy = 45, Status = "Available" },
            new() { SlotId = "SLOT006", StationId = "ST001", Date = today, StartTime = "10:00 AM", EndTime = "12:00 PM", EnergyCapacity = 25, AvailableEnergy = 25, Status = "Available" },
        };

        foreach (var planned in seedPlan)
        {
            planned.Date = DateTime.SpecifyKind(planned.Date.Date, DateTimeKind.Utc);
            var existing = _slots.Find(s => s.SlotId == planned.SlotId).FirstOrDefault();
            if (existing is null)
            {
                _slots.InsertOne(planned);
                continue;
            }

            // Keep demo slots on a real calendar day inside the 7-day window.
            var updates = new List<UpdateDefinition<EnergyBookingSlot>>();
            if (existing.EnergyCapacity <= 0)
                updates.Add(Builders<EnergyBookingSlot>.Update.Set(s => s.EnergyCapacity, Math.Max(existing.AvailableEnergy, planned.EnergyCapacity)));

            if (CalendarUtc(existing.Date) != planned.Date)
            {
                updates.Add(Builders<EnergyBookingSlot>.Update
                    .Set(s => s.Date, planned.Date)
                    .Set(s => s.StationId, planned.StationId)
                    .Set(s => s.StartTime, planned.StartTime)
                    .Set(s => s.EndTime, planned.EndTime)
                    .Set(s => s.Status, existing.AvailableEnergy <= 0 ? "Full" : "Available"));
            }

            if (updates.Count > 0)
                _slots.UpdateOne(s => s.SlotId == planned.SlotId, Builders<EnergyBookingSlot>.Update.Combine(updates.ToArray()));
        }
    }
}

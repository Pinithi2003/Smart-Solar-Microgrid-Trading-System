using System.Security.Authentication;
using Microsoft.Extensions.Options;
using MongoDB.Bson;
using MongoDB.Driver;
using SmartSolarMicrogridAPI.Models;

namespace SmartSolarMicrogridAPI.Services;

public class FieldOperationService
{
    private readonly IMongoCollection<FieldOperation> _operations;
    private readonly IMongoCollection<SolarStationInfo> _stations;
    private readonly ILogger<FieldOperationService> _logger;
    private readonly string _databaseName;
    private readonly string _collectionName = "FieldOperations";
    private Exception? _initError;

    public FieldOperationService(IOptions<MongoDBSettings> settings, ILogger<FieldOperationService> logger)
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
        _operations = database.GetCollection<FieldOperation>(_collectionName);
        _stations = database.GetCollection<SolarStationInfo>("SolarStationInfo");

        try
        {
            // Unique business indexes: reservationId and verificationCode
            var indexReservation = Builders<FieldOperation>.IndexKeys.Ascending(o => o.ReservationId);
            _operations.Indexes.CreateOne(new CreateIndexModel<FieldOperation>(indexReservation, new CreateIndexOptions { Unique = true }));

            var indexCode = Builders<FieldOperation>.IndexKeys.Ascending(o => o.VerificationCode);
            _operations.Indexes.CreateOne(new CreateIndexModel<FieldOperation>(indexCode, new CreateIndexOptions { Unique = true }));

            SeedIfEmpty();
            _logger.LogInformation("FieldOperationService ready: database '{Db}', collection '{Coll}'.",
                _databaseName, _collectionName);
        }
        catch (Exception ex)
        {
            _initError = ex;
            _logger.LogError(ex, "MongoDB initialization failed for FieldOperations collection.");
        }
    }

    private void EnsureReady()
    {
        if (_initError is not null)
            throw new InvalidOperationException(
                $"Database unavailable for Field Operations ({_initError.GetType().Name}: {_initError.Message}).",
                _initError);
    }

    private async Task SyncStationsAsync()
    {
        try
        {
            // Only consider stations that are not Inactive (Active or Maintenance)
            var activeStations = await _stations.Find(s => s.Status != "Inactive").ToListAsync();
            var activeStationIds = activeStations
                .Select(s => s.StationId)
                .ToHashSet(StringComparer.OrdinalIgnoreCase);

            // Clean up ST099 test artifact and any unverified operations for stations that were deactivated/removed
            await _operations.DeleteManyAsync(o => 
                o.StationId == "ST099" || 
                (o.Status == "Confirmed" && !activeStationIds.Contains(o.StationId))
            );
            await _stations.DeleteManyAsync(s => s.StationId == "ST099");

            var existingOperations = await _operations.Find(_ => true).ToListAsync();
            var existingStationIds = existingOperations
                .Select(o => o.StationId)
                .ToHashSet(StringComparer.OrdinalIgnoreCase);

            var newOps = new List<FieldOperation>();
            var now = DateTime.UtcNow;

            foreach (var st in activeStations)
            {
                if (string.IsNullOrWhiteSpace(st.StationId))
                    continue;

                // Sync stationName in existing operations if Member 2 renamed it
                await _operations.UpdateManyAsync(
                    o => o.StationId == st.StationId && o.StationName != st.StationName,
                    Builders<FieldOperation>.Update.Set(o => o.StationName, st.StationName)
                );

                // Auto-create a pending confirmed reservation if this station has no bookings yet
                if (!existingStationIds.Contains(st.StationId))
                {
                    var numMatch = System.Text.RegularExpressions.Regex.Match(st.StationId, @"\d+");
                    var numStr = numMatch.Success ? numMatch.Value : "999";
                    var resId = $"RES{numStr.PadLeft(3, '0')}";
                    var code = $"SG-RES{numStr.PadLeft(3, '0')}";

                    int counter = 1;
                    while (await _operations.Find(o => o.ReservationId == resId || o.VerificationCode == code).AnyAsync())
                    {
                        resId = $"RES{numStr.PadLeft(3, '0')}_{counter}";
                        code = $"SG-RES{numStr.PadLeft(3, '0')}_{counter}";
                        counter++;
                    }

                    var newOp = new FieldOperation
                    {
                        OperationId = $"OP{numStr.PadLeft(3, '0')}",
                        ReservationId = resId,
                        StationId = st.StationId,
                        StationName = st.StationName,
                        UserId = $"USR{numStr.PadLeft(3, '0')}",
                        CustomerName = $"Registered User ({st.Location ?? st.StationName})",
                        VerificationCode = code,
                        EnergyAmount = Math.Round(Math.Min(st.TotalCapacity * 0.25, 25.0), 1),
                        Status = "Confirmed",
                        TimeSlot = "10:00 AM - 12:00 PM",
                        BookingDate = now.Date.AddHours(10),
                        OperatorId = "OPR-FIELD-01",
                        LastUpdated = now
                    };

                    newOps.Add(newOp);
                    existingStationIds.Add(st.StationId);
                }
            }

            if (newOps.Count > 0)
            {
                await _operations.InsertManyAsync(newOps);
                _logger.LogInformation("Auto-synced {Count} new station operations from Member 2.", newOps.Count);
            }
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Station sync failed; continuing with existing operations.");
        }
    }

    public async Task<List<FieldOperation>> GetAllAsync()
    {
        EnsureReady();
        await SyncStationsAsync();
        return await _operations.Find(_ => true)
            .SortByDescending(o => o.LastUpdated)
            .ToListAsync();
    }

    public async Task<List<FieldOperation>> GetPendingAsync()
    {
        EnsureReady();
        await SyncStationsAsync();
        var filter = Builders<FieldOperation>.Filter.In(o => o.Status, ["Confirmed", "Verified"]);
        return await _operations.Find(filter)
            .SortBy(o => o.Status)
            .ToListAsync();
    }

    public async Task<List<FieldOperation>> GetCompletedAsync()
    {
        EnsureReady();
        var filter = Builders<FieldOperation>.Filter.Eq(o => o.Status, "Completed");
        return await _operations.Find(filter)
            .SortByDescending(o => o.CompletedAt)
            .ToListAsync();
    }

    public async Task<FieldOperation?> GetByIdOrCodeAsync(string idOrCode)
    {
        EnsureReady();
        await SyncStationsAsync();
        var filter = Builders<FieldOperation>.Filter.Or(
            Builders<FieldOperation>.Filter.Regex(o => o.ReservationId, new BsonRegularExpression($"^{idOrCode}$", "i")),
            Builders<FieldOperation>.Filter.Regex(o => o.VerificationCode, new BsonRegularExpression($"^{idOrCode}$", "i")),
            Builders<FieldOperation>.Filter.Regex(o => o.OperationId, new BsonRegularExpression($"^{idOrCode}$", "i"))
        );
        return await _operations.Find(filter).FirstOrDefaultAsync();
    }

    /// <summary>
    /// Executes the 5 QR / Reservation verification rules.
    /// </summary>
    public async Task<(bool Success, int StatusCode, string Message, FieldOperation? Operation)> VerifyAsync(string verificationCode, string? operatorId)
    {
        EnsureReady();

        if (string.IsNullOrWhiteSpace(verificationCode))
            return (false, 400, "Verification code cannot be blank.", null);

        var trimmed = verificationCode.Trim();

        // Step 1: Find reservation by code or reservationId
        var op = await GetByIdOrCodeAsync(trimmed);
        if (op is null)
        {
            return (false, 404, "Invalid verification code. No matching reservation found.", null);
        }

        // Step 2: Status validations
        if (string.Equals(op.Status, "Cancelled", StringComparison.OrdinalIgnoreCase))
        {
            return (false, 400, "Cannot verify cancelled reservation.", op);
        }

        if (string.Equals(op.Status, "Completed", StringComparison.OrdinalIgnoreCase))
        {
            return (false, 400, $"Reservation is already completed (completed at {op.CompletedAt:yyyy-MM-dd HH:mm}).", op);
        }

        // Step 3: Prevent duplicate verification
        if (string.Equals(op.Status, "Verified", StringComparison.OrdinalIgnoreCase))
        {
            return (false, 409, $"Reservation already verified (verified at {op.VerifiedAt:yyyy-MM-dd HH:mm}).", op);
        }

        if (!string.Equals(op.Status, "Confirmed", StringComparison.OrdinalIgnoreCase))
        {
            return (false, 400, $"Cannot verify reservation with current status: '{op.Status}'. Expected 'Confirmed'.", op);
        }

        // Step 4: Successful verification transition
        var now = DateTime.UtcNow;
        var update = Builders<FieldOperation>.Update
            .Set(o => o.Status, "Verified")
            .Set(o => o.VerifiedAt, now)
            .Set(o => o.OperatorId, string.IsNullOrWhiteSpace(operatorId) ? op.OperatorId : operatorId)
            .Set(o => o.LastUpdated, now);

        await _operations.UpdateOneAsync(o => o.ReservationId == op.ReservationId, update);

        var updated = await _operations.Find(o => o.ReservationId == op.ReservationId).FirstOrDefaultAsync();
        return (true, 200, "Verification successful. Status changed to Verified.", updated);
    }

    /// <summary>
    /// Completes the field energy transfer operation (Verified -> Completed).
    /// </summary>
    public async Task<(bool Success, int StatusCode, string Message, FieldOperation? Operation)> CompleteAsync(string reservationId, string? notes, string? operatorId)
    {
        EnsureReady();

        var op = await GetByIdOrCodeAsync(reservationId);
        if (op is null)
        {
            return (false, 404, $"Reservation {reservationId} not found.", null);
        }

        if (string.Equals(op.Status, "Completed", StringComparison.OrdinalIgnoreCase))
        {
            return (false, 400, $"Operation for {reservationId} is already completed.", op);
        }

        if (!string.Equals(op.Status, "Verified", StringComparison.OrdinalIgnoreCase))
        {
            return (false, 400, $"Cannot complete operation: status is '{op.Status}' (reservation must be 'Verified' first).", op);
        }

        var now = DateTime.UtcNow;
        var updateBuilder = Builders<FieldOperation>.Update
            .Set(o => o.Status, "Completed")
            .Set(o => o.CompletedAt, now)
            .Set(o => o.LastUpdated, now);

        if (!string.IsNullOrWhiteSpace(notes))
            updateBuilder = updateBuilder.Set(o => o.Notes, notes.Trim());

        if (!string.IsNullOrWhiteSpace(operatorId))
            updateBuilder = updateBuilder.Set(o => o.OperatorId, operatorId.Trim());

        await _operations.UpdateOneAsync(o => o.ReservationId == op.ReservationId, updateBuilder);

        var completed = await _operations.Find(o => o.ReservationId == op.ReservationId).FirstOrDefaultAsync();
        return (true, 200, "Operation completed successfully.", completed);
    }

    public async Task ResetDataAsync()
    {
        EnsureReady();
        await _operations.DeleteManyAsync(_ => true);
        SeedIfEmpty();
    }

    private void SeedIfEmpty()
    {
        if (_operations.CountDocuments(_ => true) > 0) return;

        var now = DateTime.UtcNow;
        var seed = new List<FieldOperation>
        {
            new()
            {
                OperationId = "OP001",
                ReservationId = "RES001",
                StationId = "ST001",
                StationName = "Colombo Solar Station",
                UserId = "USR001",
                CustomerName = "Kasun Silva",
                VerificationCode = "SG-RES001",
                EnergyAmount = 20.0,
                Status = "Confirmed",
                TimeSlot = "10:00 AM - 12:00 PM",
                BookingDate = now.Date.AddHours(10),
                OperatorId = "OPR-FIELD-01",
                LastUpdated = now
            },
            new()
            {
                OperationId = "OP002",
                ReservationId = "RES002",
                StationId = "ST002",
                StationName = "Kalutara Solar Station",
                UserId = "USR002",
                CustomerName = "Nimal Fernando",
                VerificationCode = "SG-RES002",
                EnergyAmount = 15.0,
                Status = "Verified",
                TimeSlot = "01:00 PM - 03:00 PM",
                BookingDate = now.Date.AddHours(13),
                VerifiedAt = now.AddMinutes(-30),
                OperatorId = "OPR-FIELD-02",
                LastUpdated = now.AddMinutes(-30)
            },
            new()
            {
                OperationId = "OP003",
                ReservationId = "RES003",
                StationId = "ST003",
                StationName = "Kalmunai Solar Station",
                UserId = "USR003",
                CustomerName = "Priyani Perera",
                VerificationCode = "SG-RES003",
                EnergyAmount = 30.0,
                Status = "Cancelled",
                TimeSlot = "08:00 AM - 10:00 AM",
                BookingDate = now.Date.AddHours(8),
                OperatorId = "OPR-FIELD-01",
                Notes = "Cancelled by user 12 hours prior to slot.",
                LastUpdated = now.AddHours(-2)
            },
            new()
            {
                OperationId = "OP004",
                ReservationId = "RES004",
                StationId = "ST004",
                StationName = "Kandy Solar Station",
                UserId = "USR004",
                CustomerName = "Anura Bandara",
                VerificationCode = "SG-RES004",
                EnergyAmount = 25.0,
                Status = "Confirmed",
                TimeSlot = "02:00 PM - 04:00 PM",
                BookingDate = now.Date.AddHours(14),
                OperatorId = "OPR-FIELD-01",
                LastUpdated = now
            },
            new()
            {
                OperationId = "OP005",
                ReservationId = "RES005",
                StationId = "ST005",
                StationName = "Negombo Solar Station",
                UserId = "USR005",
                CustomerName = "Suresh Rodrigo",
                VerificationCode = "SG-RES005",
                EnergyAmount = 18.0,
                Status = "Completed",
                TimeSlot = "09:00 AM - 11:00 AM",
                BookingDate = now.Date.AddHours(9),
                VerifiedAt = now.AddHours(-3),
                CompletedAt = now.AddHours(-1),
                OperatorId = "OPR-FIELD-01",
                Notes = "Energy transferred via fast charger. Telemetry verified.",
                LastUpdated = now.AddHours(-1)
            },
            new()
            {
                OperationId = "OP006",
                ReservationId = "RES006",
                StationId = "ST006",
                StationName = "Matara Solar Station",
                UserId = "USR006",
                CustomerName = "Dilshan Madushanka",
                VerificationCode = "SG-RES006",
                EnergyAmount = 22.0,
                Status = "Confirmed",
                TimeSlot = "11:00 AM - 01:00 PM",
                BookingDate = now.Date.AddHours(11),
                OperatorId = "OPR-FIELD-01",
                LastUpdated = now
            },
            new()
            {
                OperationId = "OP007",
                ReservationId = "RES007",
                StationId = "ST007",
                StationName = "Malabe Solar Station",
                UserId = "USR007",
                CustomerName = "Kavindi Jayawardena",
                VerificationCode = "SG-RES007",
                EnergyAmount = 16.5,
                Status = "Confirmed",
                TimeSlot = "03:00 PM - 05:00 PM",
                BookingDate = now.Date.AddHours(15),
                OperatorId = "OPR-FIELD-02",
                LastUpdated = now
            }
        };

        _operations.InsertMany(seed);
        _logger.LogInformation("Seeded {Count} initial field operations.", seed.Count);
    }
}

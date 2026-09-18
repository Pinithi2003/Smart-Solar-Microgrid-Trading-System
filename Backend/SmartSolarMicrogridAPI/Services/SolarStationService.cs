using System.Security.Authentication;
using Microsoft.Extensions.Options;
using MongoDB.Bson;
using MongoDB.Driver;
using SmartSolarMicrogridAPI.Models;

namespace SmartSolarMicrogridAPI.Services;

public class SolarStationService
{
    private readonly IMongoCollection<SolarStationInfo> _stations;
    private readonly ILogger<SolarStationService> _logger;
    private readonly string _databaseName;
    private readonly string _collectionName;
    private Exception? _initError;

    public SolarStationService(IOptions<MongoDBSettings> settings, ILogger<SolarStationService> logger)
    {
        _logger = logger;
        _databaseName = settings.Value.DatabaseName;
        _collectionName = settings.Value.CollectionName;

        var url = new MongoUrl(settings.Value.ConnectionString);
        var clientSettings = MongoClientSettings.FromUrl(url);
        clientSettings.SslSettings = new SslSettings
        {
            // Allow OS negotiation across TLS 1.2/1.3.
            EnabledSslProtocols = SslProtocols.Tls12 | SslProtocols.Tls13,
            // Dev-environment workaround: restricted networks/VPNs often block the
            // CRL/OCSP revocation checks, which makes Windows SChannel abort the
            // Atlas handshake (0x80090304). Re-enable for locked-down production.
            CheckCertificateRevocation = false
        };
        clientSettings.ConnectTimeout = TimeSpan.FromSeconds(10);
        clientSettings.ServerSelectionTimeout = TimeSpan.FromSeconds(10);

        var client = new MongoClient(clientSettings);
        var database = client.GetDatabase(_databaseName);
        _stations = database.GetCollection<SolarStationInfo>(_collectionName);

        try
        {
            // Unique business key: stationId (ST001...)
            var indexKeys = Builders<SolarStationInfo>.IndexKeys.Ascending(s => s.StationId);
            var indexOptions = new CreateIndexOptions { Unique = true };
            _stations.Indexes.CreateOne(new CreateIndexModel<SolarStationInfo>(indexKeys, indexOptions));

            SeedIfEmpty();
            _logger.LogInformation("MongoDB ready: database '{Db}', collection '{Coll}'.",
                _databaseName, _collectionName);
        }
        catch (Exception ex)
        {
            // Don't crash the app: fail fast per request with a clear message instead.
            _initError = ex;
            _logger.LogError(ex, "MongoDB initialization failed. Check connection string, database user, IP whitelist, and network/VPN.");
        }
    }

    public string DatabaseName => _databaseName;
    public string CollectionName => _collectionName;

    private void EnsureReady()
    {
        if (_initError is not null)
            throw new InvalidOperationException(
                $"Database unavailable ({_initError.GetType().Name}: {FirstLine(_initError.Message)}). " +
                "Check the Atlas connection string, database user, IP whitelist, and network/VPN.",
                _initError);
    }

    /// <summary>Fast DB reachability probe for GET /api/health. Never throws.</summary>
    public async Task<(bool Healthy, string? Error)> CheckHealthAsync()
    {
        if (_initError is not null)
            return (false, $"Init failed: {FirstLine(_initError.Message)}");

        try
        {
            await _stations.Database.RunCommandAsync<BsonDocument>(new BsonDocument("ping", 1));
            return (true, null);
        }
        catch (Exception ex)
        {
            return (false, $"{ex.GetType().Name}: {FirstLine(ex.Message)}");
        }
    }

    private static string FirstLine(string message) =>
        message.Split('\n')[0].Trim();

    public async Task<List<SolarStationInfo>> GetAllAsync()
    {
        EnsureReady();
        return await _stations.Find(_ => true).SortBy(s => s.StationId).ToListAsync();
    }

    public async Task<SolarStationInfo?> GetByStationIdAsync(string stationId)
    {
        EnsureReady();
        return await _stations.Find(s => s.StationId == stationId).FirstOrDefaultAsync();
    }

    public async Task CreateAsync(SolarStationInfo station)
    {
        EnsureReady();
        station.LastUpdated = DateTime.UtcNow;
        await _stations.InsertOneAsync(station);
    }

    public async Task<bool> UpdateAsync(string stationId, SolarStationInfo updated)
    {
        EnsureReady();
        // Preserve the immutable Mongo _id (clients never send it: it's JsonIgnore'd).
        var existing = await _stations.Find(s => s.StationId == stationId).FirstOrDefaultAsync();
        if (existing is null) return false;
        updated.Id = existing.Id;
        updated.StationId = stationId; // route wins over body
        updated.LastUpdated = DateTime.UtcNow;
        var result = await _stations.ReplaceOneAsync(s => s.StationId == stationId, updated);
        return result.MatchedCount > 0;
    }

    /// <summary>Soft delete: keep the document, set Status = Inactive.</summary>
    public async Task<bool> SoftDeleteAsync(string stationId)
    {
        EnsureReady();
        var update = Builders<SolarStationInfo>.Update
            .Set(s => s.Status, "Inactive")
            .Set(s => s.LastUpdated, DateTime.UtcNow);
        var result = await _stations.UpdateOneAsync(s => s.StationId == stationId, update);
        return result.MatchedCount > 0;
    }

    public async Task<bool> UpdateStatusAsync(string stationId, string status)
    {
        EnsureReady();
        var update = Builders<SolarStationInfo>.Update
            .Set(s => s.Status, status)
            .Set(s => s.LastUpdated, DateTime.UtcNow);
        var result = await _stations.UpdateOneAsync(s => s.StationId == stationId, update);
        return result.MatchedCount > 0;
    }

    private void SeedIfEmpty()
    {
        if (_stations.CountDocuments(_ => true) > 0) return;

        var now = DateTime.UtcNow;
        var seed = new List<SolarStationInfo>
        {
            new() { StationId = "ST001", StationName = "Colombo Solar Station", Location = "Colombo", Latitude = 6.9271, Longitude = 79.8612, TotalCapacity = 100, AvailableCapacity = 65, Status = "Active", Operator = "Grid Operator", LastUpdated = now },
            new() { StationId = "ST002", StationName = "Kalutara Solar Station", Location = "Kalutara", Latitude = 6.5854, Longitude = 79.9607, TotalCapacity = 120, AvailableCapacity = 48, Status = "Active", Operator = "Grid Operator", LastUpdated = now },
            new() { StationId = "ST003", StationName = "Galle Solar Station", Location = "Galle", Latitude = 6.0535, Longitude = 80.2210, TotalCapacity = 90, AvailableCapacity = 32, Status = "Maintenance", Operator = "Grid Operator", LastUpdated = now },
            new() { StationId = "ST004", StationName = "Kandy Solar Station", Location = "Kandy", Latitude = 7.2906, Longitude = 80.6337, TotalCapacity = 110, AvailableCapacity = 74, Status = "Active", Operator = "Grid Operator", LastUpdated = now },
            new() { StationId = "ST005", StationName = "Negombo Solar Station", Location = "Negombo", Latitude = 7.2083, Longitude = 79.8358, TotalCapacity = 95, AvailableCapacity = 16, Status = "Inactive", Operator = "Grid Operator", LastUpdated = now },
        };
        _stations.InsertMany(seed);
    }
}

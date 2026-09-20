using System.Security.Authentication;
using Microsoft.Extensions.Options;
using MongoDB.Bson;
using MongoDB.Driver;
using SmartSolarMicrogridAPI.Models;

namespace SmartSolarMicrogridAPI.Services;

public class UserService
{
    private readonly IMongoCollection<User> _users;
    private readonly ILogger<UserService> _logger;
    private Exception? _initError;

    public UserService(IOptions<MongoDBSettings> settings, ILogger<UserService> logger)
    {
        _logger = logger;

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
        var database = client.GetDatabase(settings.Value.DatabaseName);
        _users = database.GetCollection<User>("Users");

        try
        {
            // Unique business key: email (case-insensitive via normalized lowercase storage).
            var indexKeys = Builders<User>.IndexKeys.Ascending(u => u.Email);
            var indexOptions = new CreateIndexOptions { Unique = true };
            _users.Indexes.CreateOne(new CreateIndexModel<User>(indexKeys, indexOptions));

            _logger.LogInformation("Users collection ready.");
        }
        catch (Exception ex)
        {
            _initError = ex;
            _logger.LogError(ex, "Users MongoDB initialization failed. Check connection string, database user, IP whitelist, and network/VPN.");
        }
    }

    private void EnsureReady()
    {
        if (_initError is not null)
            throw new InvalidOperationException(
                $"Database unavailable ({_initError.GetType().Name}: {_initError.Message.Split('\n')[0].Trim()}). " +
                "Check the Atlas connection string, database user, IP whitelist, and network/VPN.",
                _initError);
    }

    private static string NormalizeEmail(string email) =>
        email.Trim().ToLowerInvariant();

    public async Task<List<User>> GetUsersAsync()
    {
        EnsureReady();
        return await _users.Find(_ => true).SortBy(u => u.Email).ToListAsync();
    }

    public async Task<User?> GetByIdAsync(string id)
    {
        EnsureReady();
        if (!ObjectId.TryParse(id, out _))
            return null;
        return await _users.Find(u => u.Id == id).FirstOrDefaultAsync();
    }

    public async Task<User?> GetUserByEmailAsync(string email)
    {
        EnsureReady();
        var normalized = NormalizeEmail(email);
        return await _users.Find(u => u.Email == normalized).FirstOrDefaultAsync();
    }

    public async Task CreateUserAsync(User user)
    {
        EnsureReady();
        user.Id = null; // Mongo generates ObjectId; ignores "USR001"/"string"
        user.Email = NormalizeEmail(user.Email);
        user.FullName = user.FullName.Trim();
        user.CreatedAt = DateTime.UtcNow;
        await _users.InsertOneAsync(user);
    }

    public async Task<bool> UpdateAsync(string id, User updated)
    {
        EnsureReady();
        if (!ObjectId.TryParse(id, out _))
            return false;

        var existing = await _users.Find(u => u.Id == id).FirstOrDefaultAsync();
        if (existing is null) return false;

        updated.Id = existing.Id;
        updated.Email = NormalizeEmail(updated.Email);
        updated.FullName = updated.FullName.Trim();
        updated.CreatedAt = existing.CreatedAt; // preserve original creation time
        var result = await _users.ReplaceOneAsync(u => u.Id == id, updated);
        return result.MatchedCount > 0;
    }

    public async Task<bool> DeleteAsync(string id)
    {
        EnsureReady();
        if (!ObjectId.TryParse(id, out _))
            return false;
        var result = await _users.DeleteOneAsync(u => u.Id == id);
        return result.DeletedCount > 0;
    }
}

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

    public UserService(
        IOptions<MongoDBSettings> settings,
        ILogger<UserService> logger)
    {
        _logger = logger;

        var url = new MongoUrl(settings.Value.ConnectionString);

        var clientSettings = MongoClientSettings.FromUrl(url);

        clientSettings.SslSettings = new SslSettings
        {
            EnabledSslProtocols =
                SslProtocols.Tls12 | SslProtocols.Tls13,

            CheckCertificateRevocation = false
        };

        clientSettings.ConnectTimeout =
            TimeSpan.FromSeconds(10);

        clientSettings.ServerSelectionTimeout =
            TimeSpan.FromSeconds(10);

        var client = new MongoClient(clientSettings);

        var database =
            client.GetDatabase(settings.Value.DatabaseName);

        _users = database.GetCollection<User>("Users");

        try
        {
            // Email must be unique
            var indexKeys =
                Builders<User>.IndexKeys.Ascending(u => u.Email);

            var indexOptions =
                new CreateIndexOptions
                {
                    Unique = true
                };

            _users.Indexes.CreateOne(
                new CreateIndexModel<User>(
                    indexKeys,
                    indexOptions));

            _logger.LogInformation(
                "Users collection ready.");
        }
        catch (Exception ex)
        {
            _initError = ex;

            _logger.LogError(
                ex,
                "Users MongoDB initialization failed. " +
                "Check connection string, database user, " +
                "IP whitelist, and network/VPN.");
        }
    }


    // =========================================================
    // Check MongoDB connection
    // =========================================================
    private void EnsureReady()
    {
        if (_initError is not null)
        {
            throw new InvalidOperationException(
                $"Database unavailable " +
                $"({_initError.GetType().Name}: " +
                $"{_initError.Message.Split('\n')[0].Trim()}). " +
                "Check the Atlas connection string, database user, " +
                "IP whitelist, and network/VPN.",
                _initError);
        }
    }


    // =========================================================
    // Normalize email
    // =========================================================
    private static string NormalizeEmail(string email)
    {
        return email.Trim().ToLowerInvariant();
    }


    // =========================================================
    // GET ALL USERS
    // =========================================================
    public async Task<List<User>> GetUsersAsync()
    {
        EnsureReady();

        return await _users
            .Find(_ => true)
            .SortBy(u => u.Email)
            .ToListAsync();
    }


    // =========================================================
    // GET USER BY ID
    // =========================================================
    public async Task<User?> GetByIdAsync(string id)
    {
        EnsureReady();

        if (!ObjectId.TryParse(id, out _))
        {
            return null;
        }

        return await _users
            .Find(u => u.Id == id)
            .FirstOrDefaultAsync();
    }


    // =========================================================
    // GET USER BY EMAIL
    // =========================================================
    public async Task<User?> GetUserByEmailAsync(string email)
    {
        EnsureReady();

        var normalized =
            NormalizeEmail(email);

        return await _users
            .Find(u => u.Email == normalized)
            .FirstOrDefaultAsync();
    }


    // =========================================================
    // CREATE USER
    // =========================================================
    public async Task CreateUserAsync(User user)
    {
        EnsureReady();

        // MongoDB generates ObjectId
        user.Id = null;

        // Normalize email
        user.Email =
            NormalizeEmail(user.Email);

        // Clean full name
        user.FullName =
            user.FullName.Trim();

        // Set creation time
        user.CreatedAt =
            DateTime.UtcNow;

        // -----------------------------------------------------
        // Password hashing
        // -----------------------------------------------------
        // Public registration sends the plain password here.
        // Hash it before saving to MongoDB.
        //
        // If the password already looks like a BCrypt hash,
        // do not hash it again.
        // -----------------------------------------------------

        if (!string.IsNullOrWhiteSpace(user.PasswordHash) &&
            !user.PasswordHash.StartsWith("$2"))
        {
            user.PasswordHash =
                BCrypt.Net.BCrypt.HashPassword(
                    user.PasswordHash);
        }

        await _users.InsertOneAsync(user);
    }


    // =========================================================
    // UPDATE USER
    // =========================================================
    public async Task<bool> UpdateAsync(
        string id,
        User updated)
    {
        EnsureReady();

        if (!ObjectId.TryParse(id, out _))
        {
            return false;
        }

        // Get existing user
        var existing =
            await _users
                .Find(u => u.Id == id)
                .FirstOrDefaultAsync();

        if (existing is null)
        {
            return false;
        }

        // -----------------------------------------------------
        // Preserve important existing values
        // -----------------------------------------------------

        updated.Id =
            existing.Id;

        updated.Email =
            NormalizeEmail(updated.Email);

        updated.FullName =
            updated.FullName.Trim();

        // Keep original creation date
        updated.CreatedAt =
            existing.CreatedAt;

        // -----------------------------------------------------
        // Password protection
        // -----------------------------------------------------
        //
        // If updated.PasswordHash is empty,
        // keep the existing password hash.
        //
        // If it is already a BCrypt hash,
        // keep it as it is.
        //
        // If it is a new plain password,
        // hash it before saving.
        // -----------------------------------------------------

        if (string.IsNullOrWhiteSpace(
            updated.PasswordHash))
        {
            updated.PasswordHash =
                existing.PasswordHash;
        }
        else if (!updated.PasswordHash.StartsWith("$2"))
        {
            updated.PasswordHash =
                BCrypt.Net.BCrypt.HashPassword(
                    updated.PasswordHash);
        }

        var result =
            await _users.ReplaceOneAsync(
                u => u.Id == id,
                updated);

        return result.MatchedCount > 0;
    }


    // =========================================================
    // DELETE USER
    // =========================================================
    public async Task<bool> DeleteAsync(string id)
    {
        EnsureReady();

        if (!ObjectId.TryParse(id, out _))
        {
            return false;
        }

        var result =
            await _users.DeleteOneAsync(
                u => u.Id == id);

        return result.DeletedCount > 0;
    }
}
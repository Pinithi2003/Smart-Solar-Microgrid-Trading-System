using System.Security.Authentication;
using Microsoft.Extensions.Options;
using MongoDB.Driver;

namespace SmartSolarMicrogridAPI.Models;

public class MongoDbService
{
    private readonly IMongoDatabase _database;

    public MongoDbService(IOptions<MongoDBSettings> settings)
    {
        var connectionString = settings.Value.ConnectionString;
        var databaseName = settings.Value.DatabaseName;

        if (string.IsNullOrWhiteSpace(connectionString))
            throw new InvalidOperationException("MongoDB connection string is NOT configured. Set MONGODB_CONNECTION_STRING / MongoDBSettings:ConnectionString.");
        if (string.IsNullOrWhiteSpace(databaseName))
            throw new InvalidOperationException("MongoDB database name is NOT configured.");

        var url = new MongoUrl(connectionString);
        var clientSettings = MongoClientSettings.FromUrl(url);
        clientSettings.SslSettings = new SslSettings
        {
            EnabledSslProtocols = SslProtocols.Tls12 | SslProtocols.Tls13,
            CheckCertificateRevocation = false
        };
        clientSettings.ConnectTimeout = TimeSpan.FromSeconds(10);
        clientSettings.ServerSelectionTimeout = TimeSpan.FromSeconds(10);

        var client = new MongoClient(clientSettings);
        _database = client.GetDatabase(databaseName);
    }

    public IMongoDatabase Database => _database;
}

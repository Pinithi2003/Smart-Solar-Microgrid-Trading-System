using MongoDB.Driver;

namespace SmartSolarMicrogridAPI.Models;

public class MongoDbService
{
    private readonly IMongoDatabase _database;

    public MongoDbService(IConfiguration configuration)
    {
        var connectionString =
            configuration["MongoDbSettings:ConnectionString"];

        var databaseName =
            configuration["MongoDbSettings:DatabaseName"];

        var client = new MongoClient(connectionString);

        _database = client.GetDatabase(databaseName);
    }

    public IMongoDatabase Database => _database;
}
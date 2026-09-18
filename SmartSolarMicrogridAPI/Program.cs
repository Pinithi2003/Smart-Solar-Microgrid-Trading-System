using MongoDB.Driver;
using SmartSolarMicrogridAPI.Models;
using SmartSolarMicrogridAPI.Services;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.AddOpenApi();
builder.Services.AddControllers();

// Register MongoDB Service
builder.Services.AddSingleton<MongoDbService>();

// Register User Service
builder.Services.AddSingleton<UserService>();

// MongoDB Configuration
var mongoConnectionString =
    builder.Configuration["MongoDbSettings:ConnectionString"];

var mongoDatabaseName =
    builder.Configuration["MongoDbSettings:DatabaseName"];

if (string.IsNullOrEmpty(mongoConnectionString))
{
    throw new InvalidOperationException(
        "MongoDB connection string is not configured.");
}

if (string.IsNullOrEmpty(mongoDatabaseName))
{
    throw new InvalidOperationException(
        "MongoDB database name is not configured.");
}

var app = builder.Build();

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
}

// app.UseHttpsRedirection();

app.MapControllers();

// Home endpoint
app.MapGet("/", () => "Smart Solar Microgrid API is running!");

// MongoDB connection test endpoint
app.MapGet("/api/test-mongodb", async (MongoDbService mongoDb) =>
{
    var database = mongoDb.Database;

    var collections = await database
        .ListCollectionNames()
        .ToListAsync();

    return Results.Ok(new
    {
        message = "MongoDB connection successful!",
        database = database.DatabaseNamespace.DatabaseName,
        collections = collections
    });
});

app.Run();
using MongoDB.Driver;
using SmartSolarMicrogridAPI.Models;
using SmartSolarMicrogridAPI.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using System.Text;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.AddOpenApi();
builder.Services.AddControllers();

// Register MongoDB Service
builder.Services.AddSingleton<MongoDbService>();

// Register User Service
builder.Services.AddSingleton<UserService>();

// Register Authentication Service
builder.Services.AddSingleton<AuthService>();

// JWT Authentication
var jwtKey = builder.Configuration["Jwt:Key"];

if (string.IsNullOrEmpty(jwtKey))
{
    throw new InvalidOperationException(
        "JWT key is not configured.");
}

builder.Services
    .AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuerSigningKey = true,

            IssuerSigningKey = new SymmetricSecurityKey(
                Encoding.UTF8.GetBytes(jwtKey)),

            ValidateIssuer = false,
            ValidateAudience = false,

            ValidateLifetime = true,

            ClockSkew = TimeSpan.Zero
        };
    });

// Authorization
builder.Services.AddAuthorization();

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

// Authentication must come before Authorization
app.UseAuthentication();
app.UseAuthorization();

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
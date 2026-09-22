using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using SmartSolarMicrogridAPI.Models;
using SmartSolarMicrogridAPI.Services;
using System.Text;

// Team convention: load SmartSolarMicrogridAPI/.env into environment variables.
// No-op when the file is absent.
DotNetEnv.Env.Load();

var builder = WebApplication.CreateBuilder(args);

// Optional .env overrides for MongoDB configuration.
foreach (var (envKey, configKey) in new[]
{
    ("MONGODB_CONNECTION_STRING", "MongoDBSettings:ConnectionString"),
    ("MONGODB_DATABASE", "MongoDBSettings:DatabaseName"),
    ("MONGODB_COLLECTION", "MongoDBSettings:CollectionName")
})
{
    var value = Environment.GetEnvironmentVariable(envKey);

    if (!string.IsNullOrWhiteSpace(value))
    {
        builder.Configuration[configKey] = value;
    }
}

// MongoDB
builder.Services.Configure<MongoDBSettings>(
    builder.Configuration.GetSection("MongoDBSettings"));

builder.Services.AddSingleton<SolarStationService>();
builder.Services.AddSingleton<FieldOperationService>();
builder.Services.AddSingleton<MongoDbService>();
builder.Services.AddSingleton<UserService>();
builder.Services.AddSingleton<AuthService>();

// JWT Authentication
// First check configuration, then fallback to JWT_KEY from .env.
var jwtKey =
    builder.Configuration["Jwt:Key"]
    ?? Environment.GetEnvironmentVariable("JWT_KEY");

if (string.IsNullOrWhiteSpace(jwtKey))
{
    throw new InvalidOperationException(
        "JWT key is not configured. Set JWT_KEY in .env, user-secrets, or environment variables.");
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

builder.Services.AddAuthorization();

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// Allow demo frontend / Postman / mobile clients during development.
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowAll", policy =>
        policy.AllowAnyOrigin()
              .AllowAnyMethod()
              .AllowAnyHeader());
});

var app = builder.Build();

// MongoDB startup check
var configuredConn =
    app.Configuration["MongoDBSettings:ConnectionString"] ?? "";

if (string.IsNullOrWhiteSpace(configuredConn) ||
    configuredConn == "YOUR_MONGODB_CONNECTION_STRING")
{
    app.Logger.LogWarning(
        "MongoDB connection string is NOT configured. " +
        "Set MONGODB_CONNECTION_STRING in .env, user-secrets, " +
        "or environment variables.");
}
else
{
    var fromEnv =
        !string.IsNullOrWhiteSpace(
            Environment.GetEnvironmentVariable(
                "MONGODB_CONNECTION_STRING"));

    app.Logger.LogInformation(
        "MongoDB configured from {Source}: database '{Db}', collection '{Coll}'.",
        fromEnv
            ? "environment (.env or system env)"
            : "user-secrets/appsettings",
        app.Configuration["MongoDBSettings:DatabaseName"],
        app.Configuration["MongoDBSettings:CollectionName"]);
}

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();

app.UseCors("AllowAll");

// Authentication MUST come before Authorization.
app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

// ============================================================
// SEED DEFAULT BACKOFFICE ACCOUNT
// ============================================================

using (var scope = app.Services.CreateScope())
{
    try
    {
        var userService =
            scope.ServiceProvider.GetRequiredService<UserService>();

        var existingBackoffice =
            await userService.GetUserByEmailAsync(
                "backoffice@smartsolar.com");

        if (existingBackoffice == null)
        {
            var backofficeUser = new User
            {
                FullName = "System Backoffice",
                Email = "backoffice@smartsolar.com",
                PasswordHash = "Backoffice@123",
                Role = "Backoffice",
                Status = "Approved",
                IsActive = true
            };

            await userService.CreateUserAsync(
                backofficeUser);

            app.Logger.LogInformation(
                "Default Backoffice account created successfully.");
        }
        else
        {
            app.Logger.LogInformation(
                "Backoffice account already exists.");
        }
    }
    catch (Exception ex)
    {
        app.Logger.LogError(
            ex,
            "Failed to seed default Backoffice account.");
    }
}

app.Run();
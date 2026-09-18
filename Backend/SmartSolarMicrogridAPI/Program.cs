using SmartSolarMicrogridAPI.Models;
using SmartSolarMicrogridAPI.Services;

// Team convention: load SmartSolarMicrogridAPI/.env into environment variables.
// No-op when the file is absent (e.g. production). Never overwrites real env vars.
DotNetEnv.Env.Load();

var builder = WebApplication.CreateBuilder(args);

// Optional .env overrides (MONGODB_*), applied only when set.
// Precedence: real env var > .env file > user-secrets > appsettings.json.
foreach (var (envKey, configKey) in new[]
    {
        ("MONGODB_CONNECTION_STRING", "MongoDBSettings:ConnectionString"),
        ("MONGODB_DATABASE", "MongoDBSettings:DatabaseName"),
        ("MONGODB_COLLECTION", "MongoDBSettings:CollectionName")
    })
{
    var value = Environment.GetEnvironmentVariable(envKey);
    if (!string.IsNullOrWhiteSpace(value))
        builder.Configuration[configKey] = value;
}

// MongoDB (secret comes from .env / user-secrets / env in dev, appsettings placeholder in repo)
builder.Services.Configure<MongoDBSettings>(
    builder.Configuration.GetSection("MongoDBSettings"));
builder.Services.AddSingleton<SolarStationService>();

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();
builder.Services.AddOpenApi();

// Allow demo frontend / Postman / mobile clients during development
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowAll", policy =>
        policy.AllowAnyOrigin().AllowAnyMethod().AllowAnyHeader());
});

var app = builder.Build();

// Secret-free startup check: warn early if no real connection string is configured.
var configuredConn = app.Configuration["MongoDBSettings:ConnectionString"] ?? "";
if (string.IsNullOrWhiteSpace(configuredConn) || configuredConn == "YOUR_MONGODB_CONNECTION_STRING")
{
    app.Logger.LogWarning("MongoDB connection string is NOT configured. Set it via .env (MONGODB_CONNECTION_STRING), user-secrets, or the MongoDBSettings__ConnectionString env var.");
}
else
{
    var fromEnv = !string.IsNullOrWhiteSpace(Environment.GetEnvironmentVariable("MONGODB_CONNECTION_STRING"));
    app.Logger.LogInformation("MongoDB configured from {Source}: database '{Db}', collection '{Coll}'.",
        fromEnv ? "environment (.env or system env)" : "user-secrets/appsettings",
        app.Configuration["MongoDBSettings:DatabaseName"],
        app.Configuration["MongoDBSettings:CollectionName"]);
}

if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();
app.UseCors("AllowAll");
app.UseAuthorization();
app.MapControllers();

app.Run();

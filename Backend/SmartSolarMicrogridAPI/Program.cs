using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;
using SmartSolarMicrogridAPI.Models;
using SmartSolarMicrogridAPI.Services;
using System.Text;

// Load .env file
DotNetEnv.Env.Load();

var builder = WebApplication.CreateBuilder(args);

// ============================================================
// LOCAL NETWORK / ANDROID MOBILE ACCESS
// ============================================================

builder.WebHost.UseUrls("http://0.0.0.0:5205");

// ============================================================
// MONGODB CONFIGURATION
// ============================================================

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

// ============================================================
// SERVICES
// ============================================================

builder.Services.Configure<MongoDBSettings>(
    builder.Configuration.GetSection("MongoDBSettings"));

builder.Services.AddSingleton<SolarStationService>();
builder.Services.AddSingleton<FieldOperationService>();
builder.Services.AddSingleton<ReservationService>();
builder.Services.AddSingleton<MongoDbService>();
builder.Services.AddSingleton<UserService>();
builder.Services.AddSingleton<AuthService>();

// ============================================================
// JWT AUTHENTICATION
// ============================================================

var jwtKey =
    builder.Configuration["Jwt:Key"]
    ?? Environment.GetEnvironmentVariable("JWT_KEY");

if (string.IsNullOrWhiteSpace(jwtKey))
{
    throw new InvalidOperationException(
        "JWT key is not configured. Set JWT_KEY in .env, user-secrets, or environment variables.");
}

builder.Services
    .AddAuthentication(options =>
    {
        options.DefaultAuthenticateScheme =
            JwtBearerDefaults.AuthenticationScheme;

        options.DefaultChallengeScheme =
            JwtBearerDefaults.AuthenticationScheme;
    })
    .AddJwtBearer(options =>
    {
        options.RequireHttpsMetadata = false;
        options.SaveToken = false;

        options.TokenValidationParameters =
            new TokenValidationParameters
            {
                ValidateIssuerSigningKey = true,

                IssuerSigningKey =
                    new SymmetricSecurityKey(
                        Encoding.UTF8.GetBytes(jwtKey)),

                ValidateIssuer = false,
                ValidateAudience = false,

                ValidateLifetime = true,

                ClockSkew = TimeSpan.Zero
            };

        // Useful while debugging mobile authentication.
        options.Events = new JwtBearerEvents
        {
            OnAuthenticationFailed = context =>
            {
                Console.WriteLine(
                    $"JWT Authentication Failed: {context.Exception.Message}");

                return Task.CompletedTask;
            },

            OnTokenValidated = context =>
            {
                Console.WriteLine(
                    "JWT Authentication Successful.");

                return Task.CompletedTask;
            }
        };
    });

builder.Services.AddAuthorization();

// ============================================================
// CONTROLLERS + SWAGGER
// ============================================================

builder.Services.AddControllers();

builder.Services.AddEndpointsApiExplorer();

// ============================================================
// SWAGGER + JWT BEARER AUTHORIZATION
// ============================================================

builder.Services.AddSwaggerGen(options =>
{
    // Add JWT Bearer authentication definition
    options.AddSecurityDefinition(
        "Bearer",
        new OpenApiSecurityScheme
        {
            Name = "Authorization",
            Type = SecuritySchemeType.Http,
            Scheme = "bearer",
            BearerFormat = "JWT",
            In = ParameterLocation.Header,

            Description =
                "Enter your JWT token. Example: Bearer eyJhbGciOiJIUzI1NiIs..."
        });

    // Tell Swagger that API endpoints can use Bearer authentication
    options.AddSecurityRequirement(
        new OpenApiSecurityRequirement
        {
            {
                new OpenApiSecurityScheme
                {
                    Reference =
                        new OpenApiReference
                        {
                            Type = ReferenceType.SecurityScheme,
                            Id = "Bearer"
                        }
                },

                Array.Empty<string>()
            }
        });
});

// ============================================================
// CORS
// ============================================================

builder.Services.AddCors(options =>
{
    options.AddPolicy(
        "AllowAll",
        policy =>
            policy
                .AllowAnyOrigin()
                .AllowAnyMethod()
                .AllowAnyHeader());
});

// ============================================================
// BUILD
// ============================================================

var app = builder.Build();

// ============================================================
// MONGODB STARTUP CHECK
// ============================================================

var configuredConn =
    app.Configuration["MongoDBSettings:ConnectionString"] ?? "";

if (
    string.IsNullOrWhiteSpace(configuredConn) ||
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

// ============================================================
// SWAGGER
// ============================================================

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();

    app.UseSwaggerUI();
}

// ============================================================
// HTTP LOCAL DEVELOPMENT
// ============================================================

// Do NOT use HTTPS redirection for Android local network testing.

// app.UseHttpsRedirection();

// ============================================================
// CORS
// ============================================================

app.UseCors("AllowAll");

// ============================================================
// AUTHENTICATION & AUTHORIZATION
// ============================================================

app.UseAuthentication();

app.UseAuthorization();

// ============================================================
// CONTROLLERS
// ============================================================

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

                // Initial password.
                // UserService automatically hashes it with BCrypt.
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

// ============================================================
// RUN
// ============================================================

app.Run();
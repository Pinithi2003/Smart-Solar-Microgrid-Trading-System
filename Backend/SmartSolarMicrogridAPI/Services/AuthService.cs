// =========================================================
// Smart Solar Microgrid Trading System
// SE4040 - Enterprise Application Development
//
// Member: Pinithi Ransiluni
// Student ID: IT23143654
// Contribution: Member 1 - Identity & Access
//
// File: AuthService.cs
// Description: Provides user registration, login, password
//              verification, and JWT token generation
//              for the Smart Solar Microgrid system.
// =========================================================

using Microsoft.IdentityModel.Tokens;
using MongoDB.Driver;
using SmartSolarMicrogridAPI.Models;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;

namespace SmartSolarMicrogridAPI.Services
{
    public class AuthService
    {
        private readonly MongoDbService _mongoDbService;
        private readonly IConfiguration _configuration;

        public AuthService(
            MongoDbService mongoDbService,
            IConfiguration configuration)
        {
            _mongoDbService = mongoDbService;
            _configuration = configuration;
        }

        // =========================================================
        // Get user by email
        // =========================================================
        public async Task<User?> GetUserByEmailAsync(string email)
        {
            var collection = _mongoDbService.Database
                .GetCollection<User>("Users");

            var normalizedEmail = email
                .Trim()
                .ToLowerInvariant();

            return await collection
                .Find(u => u.Email == normalizedEmail)
                .FirstOrDefaultAsync();
        }

        // =========================================================
        // Register new Prosumer
        // =========================================================
        public async Task<(bool Success, string Message, User? User)>
            RegisterProsumerAsync(RegisterRequest request)
        {
            var collection = _mongoDbService.Database
                .GetCollection<User>("Users");

            // -----------------------------------------------------
            // Normalize input
            // -----------------------------------------------------
            var normalizedNIC = request.NIC.Trim();

            var normalizedEmail = request.Email
                .Trim()
                .ToLowerInvariant();

            // -----------------------------------------------------
            // Check existing email
            // -----------------------------------------------------
            var existingEmail = await collection
                .Find(u => u.Email == normalizedEmail)
                .FirstOrDefaultAsync();

            if (existingEmail != null)
            {
                return (
                    false,
                    "An account with this email already exists.",
                    null
                );
            }

            // -----------------------------------------------------
            // Check existing NIC
            // -----------------------------------------------------
            var existingNIC = await collection
                .Find(u => u.NIC == normalizedNIC)
                .FirstOrDefaultAsync();

            if (existingNIC != null)
            {
                return (
                    false,
                    "An account with this NIC already exists.",
                    null
                );
            }

            // -----------------------------------------------------
            // Hash password
            // -----------------------------------------------------
            var passwordHash =
                BCrypt.Net.BCrypt.HashPassword(request.Password);

            // -----------------------------------------------------
            // Create user
            // -----------------------------------------------------
            var user = new User
            {
                NIC = normalizedNIC,

                FullName = request.FullName.Trim(),

                Email = normalizedEmail,

                Phone = request.Phone.Trim(),

                PasswordHash = passwordHash,

                // Mobile registration creates Prosumer accounts only.
                Role = "Prosumer",

                // Backoffice approval is required.
                Status = "Pending",

                // Pending accounts are not active.
                IsActive = false,

                CreatedAt = DateTime.UtcNow
            };

            // -----------------------------------------------------
            // Save user to MongoDB
            // -----------------------------------------------------
            await collection.InsertOneAsync(user);

            return (
                true,
                "Registration successful. Your account is pending Backoffice approval.",
                user
            );
        }

        // =========================================================
        // Generate JWT token
        // =========================================================
        public string GenerateJwtToken(User user)
        {
            var jwtKey =
                _configuration["Jwt:Key"]
                ?? Environment.GetEnvironmentVariable("JWT_KEY");

            if (string.IsNullOrWhiteSpace(jwtKey))
            {
                throw new InvalidOperationException(
                    "JWT key is not configured. Set JWT_KEY in .env, user-secrets, or environment variables.");
            }

            var claims = new[]
            {
                new Claim(
                    ClaimTypes.NameIdentifier,
                    user.Id ?? string.Empty),

                new Claim(
                    ClaimTypes.Name,
                    user.FullName),

                new Claim(
                    ClaimTypes.Email,
                    user.Email),

                new Claim(
                    ClaimTypes.Role,
                    user.Role)
            };

            var key = new SymmetricSecurityKey(
                Encoding.UTF8.GetBytes(jwtKey));

            var credentials = new SigningCredentials(
                key,
                SecurityAlgorithms.HmacSha256);

            var token = new JwtSecurityToken(
                claims: claims,
                expires: DateTime.UtcNow.AddHours(2),
                signingCredentials: credentials);

            return new JwtSecurityTokenHandler()
                .WriteToken(token);
        }
    }
}
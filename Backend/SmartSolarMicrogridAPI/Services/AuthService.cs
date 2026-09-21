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

        public string GenerateJwtToken(User user)
        {
            // First check configuration, then fallback to JWT_KEY
            // from the .env file / environment variables.
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

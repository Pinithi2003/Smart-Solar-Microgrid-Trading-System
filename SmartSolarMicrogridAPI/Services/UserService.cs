using MongoDB.Driver;
using SmartSolarMicrogridAPI.Models;

namespace SmartSolarMicrogridAPI.Services;

public class UserService
{
    private readonly IMongoCollection<User> _users;

    public UserService(MongoDbService mongoDbService)
    {
        _users = mongoDbService.Database.GetCollection<User>("Users");
    }

    public async Task<List<User>> GetUsersAsync()
    {
        return await _users.Find(_ => true).ToListAsync();
    }

    public async Task<User?> GetUserByEmailAsync(string email)
    {
        return await _users
            .Find(user => user.Email == email)
            .FirstOrDefaultAsync();
    }

    public async Task CreateUserAsync(User user)
    {
        // Hash the password before saving it to MongoDB
        user.PasswordHash = BCrypt.Net.BCrypt.HashPassword(
            user.PasswordHash);

        await _users.InsertOneAsync(user);
    }
}
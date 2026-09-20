using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogridAPI.Models;
using SmartSolarMicrogridAPI.Services;

namespace SmartSolarMicrogridAPI.Controllers;

[ApiController]
[Route("api/[controller]")]
public class UsersController : ControllerBase
{
    private readonly UserService _userService;

    public UsersController(UserService userService)
    {
        _userService = userService;
    }

    // GET: api/users
    // Only Backoffice users can view all users
    [HttpGet]
    [Authorize(Roles = "Backoffice")]
    public async Task<IActionResult> GetUsers()
    {
        var users = await _userService.GetUsersAsync();

        var result = users.Select(user => new
        {
            id = user.Id,
            fullName = user.FullName,
            email = user.Email,
            role = user.Role,
            isActive = user.IsActive,
            createdAt = user.CreatedAt
        });

        return Ok(result);
    }

    // GET: api/users/email/{email}
    // Only authenticated users can search a user by email
    [HttpGet("email/{email}")]
    [Authorize]
    public async Task<IActionResult> GetUserByEmail(string email)
    {
        var user = await _userService.GetUserByEmailAsync(email);

        if (user == null)
        {
            return NotFound(new
            {
                message = "User not found."
            });
        }

        // Do NOT return PasswordHash
        return Ok(new
        {
            id = user.Id,
            fullName = user.FullName,
            email = user.Email,
            role = user.Role,
            isActive = user.IsActive,
            createdAt = user.CreatedAt
        });
    }

    // POST: api/users
    // Only Backoffice users can create users manually
    [HttpPost]
    [Authorize(Roles = "Backoffice")]
    public async Task<IActionResult> CreateUser(User user)
    {
        var existingUser =
            await _userService.GetUserByEmailAsync(user.Email);

        if (existingUser != null)
        {
            return Conflict(new
            {
                message = "Email already exists."
            });
        }

        await _userService.CreateUserAsync(user);

        return Ok(new
        {
            message = "User created successfully."
        });
    }

    // POST: api/users/register
    // Public registration
    // New public users are always Prosumer
    [HttpPost("register")]
    [AllowAnonymous]
    public async Task<IActionResult> Register(
        [FromBody] RegisterRequest request)
    {
        // Check whether the email already exists
        var existingUser =
            await _userService.GetUserByEmailAsync(request.Email);

        if (existingUser != null)
        {
            return Conflict(new
            {
                message = "Email already exists."
            });
        }

        // Public registration can only create Prosumer accounts
        var user = new User
        {
            FullName = request.FullName,
            Email = request.Email,
            PasswordHash = request.Password,
            Role = "Prosumer",
            IsActive = true,
            CreatedAt = DateTime.UtcNow
        };

        // UserService hashes the password before saving
        await _userService.CreateUserAsync(user);

        return Ok(new
        {
            message = "Registration successful."
        });
    }
}
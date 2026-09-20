using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MongoDB.Bson;
using MongoDB.Driver;
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

    // GET: api/users/{id}
    // Only authenticated users can view a user by ID
    [HttpGet("{id}")]
    [Authorize]
    public async Task<IActionResult> GetUserById(string id)
    {
        if (!ObjectId.TryParse(id, out _))
        {
            return BadRequest(new
            {
                message = "Invalid user id. Must be a 24-digit hex string."
            });
        }

        var user = await _userService.GetByIdAsync(id);

        if (user == null)
        {
            return NotFound(new
            {
                message = "User not found."
            });
        }

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

    // GET: api/users/email/{email}
    // Only authenticated users can search a user by email
    [HttpGet("email/{email}")]
    [Authorize]
    public async Task<IActionResult> GetUserByEmail(string email)
    {
        if (string.IsNullOrWhiteSpace(email))
        {
            return BadRequest(new
            {
                message = "Email is required."
            });
        }

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
    public async Task<IActionResult> CreateUser([FromBody] User user)
    {
        if (!ModelState.IsValid)
        {
            return ValidationProblem(ModelState);
        }

        user.Id = null;
        user.CreatedAt = DateTime.UtcNow;

        var existingUser =
            await _userService.GetUserByEmailAsync(user.Email);

        if (existingUser != null)
        {
            return Conflict(new
            {
                message = "Email already exists."
            });
        }

        try
        {
            await _userService.CreateUserAsync(user);
        }
        catch (MongoWriteException ex)
            when (ex.WriteError.Category == ServerErrorCategory.DuplicateKey)
        {
            return Conflict(new
            {
                message = "Email already exists."
            });
        }

        var created =
            await _userService.GetUserByEmailAsync(user.Email);

        return CreatedAtAction(
            nameof(GetUserById),
            new { id = created!.Id },
            new
            {
                id = created.Id,
                fullName = created.FullName,
                email = created.Email,
                role = created.Role,
                isActive = created.IsActive,
                createdAt = created.CreatedAt
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

    // PUT: api/users/{id}
    // Only Backoffice users can update users
    [HttpPut("{id}")]
    [Authorize(Roles = "Backoffice")]
    public async Task<IActionResult> Update(
        string id,
        [FromBody] User updated)
    {
        if (!ModelState.IsValid)
        {
            return ValidationProblem(ModelState);
        }

        if (!ObjectId.TryParse(id, out _))
        {
            return BadRequest(new
            {
                message = "Invalid user id. Must be a 24-digit hex string."
            });
        }

        var existing = await _userService.GetByIdAsync(id);

        if (existing is null)
        {
            return NotFound(new
            {
                message = "User not found."
            });
        }

        // If email is changing, ensure no other user owns it
        var owner =
            await _userService.GetUserByEmailAsync(updated.Email);

        if (owner is not null && owner.Id != id)
        {
            return Conflict(new
            {
                message = "Email already exists."
            });
        }

        try
        {
            await _userService.UpdateAsync(id, updated);
        }
        catch (MongoWriteException ex)
            when (ex.WriteError.Category == ServerErrorCategory.DuplicateKey)
        {
            return Conflict(new
            {
                message = "Email already exists."
            });
        }

        var result = await _userService.GetByIdAsync(id);

        if (result == null)
        {
            return NotFound(new
            {
                message = "User not found."
            });
        }

        return Ok(new
        {
            id = result.Id,
            fullName = result.FullName,
            email = result.Email,
            role = result.Role,
            isActive = result.IsActive,
            createdAt = result.CreatedAt
        });
    }

    // DELETE: api/users/{id}
    // Only Backoffice users can delete users
    [HttpDelete("{id}")]
    [Authorize(Roles = "Backoffice")]
    public async Task<IActionResult> Delete(string id)
    {
        if (!ObjectId.TryParse(id, out _))
        {
            return BadRequest(new
            {
                message = "Invalid user id. Must be a 24-digit hex string."
            });
        }

        var existing = await _userService.GetByIdAsync(id);

        if (existing is null)
        {
            return NotFound(new
            {
                message = "User not found."
            });
        }

        await _userService.DeleteAsync(id);

        return Ok(new
        {
            message = "User deleted successfully."
        });
    }
}
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

    // =========================================================
    // GET: api/users
    // Only Backoffice users can view all users
    // =========================================================
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
            status = user.Status,
            isActive = user.IsActive,
            createdAt = user.CreatedAt
        });

        return Ok(result);
    }


    // =========================================================
    // GET: api/users/{id}
    // Authenticated users can view a user by ID
    // =========================================================
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
            status = user.Status,
            isActive = user.IsActive,
            createdAt = user.CreatedAt
        });
    }


    // =========================================================
    // GET: api/users/email/{email}
    // Authenticated users can search a user by email
    // =========================================================
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

        var user = await _userService.GetUserByEmailAsync(email.Trim());

        if (user == null)
        {
            return NotFound(new
            {
                message = "User not found."
            });
        }

        // Never return PasswordHash
        return Ok(new
        {
            id = user.Id,
            fullName = user.FullName,
            email = user.Email,
            role = user.Role,
            status = user.Status,
            isActive = user.IsActive,
            createdAt = user.CreatedAt
        });
    }


    // =========================================================
    // POST: api/users
    // Only Backoffice users can create users manually
    // =========================================================
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

        // Backoffice-created users are already approved
        user.Status = "Approved";
        user.IsActive = true;

        var existingUser =
            await _userService.GetUserByEmailAsync(user.Email.Trim());

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
            await _userService.GetUserByEmailAsync(user.Email.Trim());

        if (created == null)
        {
            return StatusCode(500, new
            {
                message = "User was created but could not be retrieved."
            });
        }

        return CreatedAtAction(
            nameof(GetUserById),
            new { id = created.Id },
            new
            {
                id = created.Id,
                fullName = created.FullName,
                email = created.Email,
                role = created.Role,
                status = created.Status,
                isActive = created.IsActive,
                createdAt = created.CreatedAt
            });
    }


    // =========================================================
    // POST: api/users/register
    // Public registration
    //
    // New public users:
    // Role   = Prosumer
    // Status = Pending
    // Active = false
    //
    // Backoffice must approve the account before login.
    // =========================================================
    [HttpPost("register")]
    [AllowAnonymous]
    public async Task<IActionResult> Register(
        [FromBody] RegisterRequest request)
    {
        if (!ModelState.IsValid)
        {
            return ValidationProblem(ModelState);
        }

        if (string.IsNullOrWhiteSpace(request.FullName))
        {
            return BadRequest(new
            {
                message = "Full name is required."
            });
        }

        if (string.IsNullOrWhiteSpace(request.Email))
        {
            return BadRequest(new
            {
                message = "Email is required."
            });
        }

        if (string.IsNullOrWhiteSpace(request.Password))
        {
            return BadRequest(new
            {
                message = "Password is required."
            });
        }

        var email = request.Email.Trim();

        var existingUser =
            await _userService.GetUserByEmailAsync(email);

        if (existingUser != null)
        {
            return Conflict(new
            {
                message = "Email already exists."
            });
        }

        // Public registration creates a Prosumer account.
        // Account is pending until Backoffice approval.
        var user = new User
        {
            FullName = request.FullName.Trim(),
            Email = email,
            PasswordHash = request.Password,
            Role = "Prosumer",
            IsActive = false,
            Status = "Pending",
            CreatedAt = DateTime.UtcNow
        };

        // UserService should hash the password
        // before saving it to MongoDB.
        await _userService.CreateUserAsync(user);

        return Ok(new
        {
            message =
                "Registration submitted successfully. Your account is pending approval.",
            status = user.Status
        });
    }


    // =========================================================
    // PUT: api/users/{id}/approve
    // Only Backoffice users can approve pending users
    // =========================================================
    [HttpPut("{id}/approve")]
    [Authorize(Roles = "Backoffice")]
    public async Task<IActionResult> ApproveUser(string id)
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

        // Already approved
        if (string.Equals(
            user.Status,
            "Approved",
            StringComparison.OrdinalIgnoreCase))
        {
            return BadRequest(new
            {
                message = "User account is already approved."
            });
        }

        // Rejected users should not be approved directly
        if (string.Equals(
            user.Status,
            "Rejected",
            StringComparison.OrdinalIgnoreCase))
        {
            return BadRequest(new
            {
                message = "Rejected users cannot be approved directly."
            });
        }

        // Approve account
        user.Status = "Approved";
        user.IsActive = true;

        await _userService.UpdateAsync(id, user);

        return Ok(new
        {
            message = "User approved successfully.",
            id = user.Id,
            fullName = user.FullName,
            email = user.Email,
            role = user.Role,
            status = user.Status,
            isActive = user.IsActive
        });
    }


    // =========================================================
    // PUT: api/users/{id}/reject
    // Only Backoffice users can reject pending users
    // =========================================================
    [HttpPut("{id}/reject")]
    [Authorize(Roles = "Backoffice")]
    public async Task<IActionResult> RejectUser(string id)
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

        if (string.Equals(
            user.Status,
            "Approved",
            StringComparison.OrdinalIgnoreCase))
        {
            return BadRequest(new
            {
                message = "Approved users cannot be rejected."
            });
        }

        if (string.Equals(
            user.Status,
            "Rejected",
            StringComparison.OrdinalIgnoreCase))
        {
            return BadRequest(new
            {
                message = "User account is already rejected."
            });
        }

        // Reject account
        user.Status = "Rejected";
        user.IsActive = false;

        await _userService.UpdateAsync(id, user);

        return Ok(new
        {
            message = "User rejected successfully.",
            id = user.Id,
            fullName = user.FullName,
            email = user.Email,
            role = user.Role,
            status = user.Status,
            isActive = user.IsActive
        });
    }


    // =========================================================
    // PUT: api/users/{id}
    // Only Backoffice users can update users
    // =========================================================
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

        // Check whether another user already owns the email
        var owner =
            await _userService.GetUserByEmailAsync(updated.Email.Trim());

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
            status = result.Status,
            isActive = result.IsActive,
            createdAt = result.CreatedAt
        });
    }


    // =========================================================
    // DELETE: api/users/{id}
    // Only Backoffice users can delete users
    // =========================================================
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
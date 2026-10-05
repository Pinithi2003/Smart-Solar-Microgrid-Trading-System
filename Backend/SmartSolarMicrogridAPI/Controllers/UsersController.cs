using System.Security.Claims;
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
    [Authorize(Roles = "Backoffice,Grid Operator")]
    public async Task<IActionResult> GetUsers()
    {
        var users = await _userService.GetUsersAsync();

        var result = users.Select(user => new
        {
            id = user.Id,
            nic = user.NIC,
            fullName = user.FullName,
            email = user.Email,
            phone = user.Phone,
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
            nic = user.NIC,
            fullName = user.FullName,
            email = user.Email,
            phone = user.Phone,
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

        var normalizedEmail = email.Trim().ToLowerInvariant();

        var user =
            await _userService.GetUserByEmailAsync(normalizedEmail);

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
            nic = user.NIC,
            fullName = user.FullName,
            email = user.Email,
            phone = user.Phone,
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
    public async Task<IActionResult> CreateUser(
        [FromBody] CreateUserRequest request)
    {
        // Model validation
        if (!ModelState.IsValid)
        {
            var errors = ModelState
                .Where(x =>
                    x.Value != null &&
                    x.Value.Errors.Count > 0)
                .ToDictionary(
                    x => x.Key,
                    x => x.Value!.Errors
                        .Select(e => e.ErrorMessage)
                        .ToArray()
                );

            return BadRequest(new
            {
                message = "Validation failed.",
                errors = errors
            });
        }

        // =====================================================
        // Validate required fields
        // =====================================================

        if (string.IsNullOrWhiteSpace(request.NIC))
        {
            return BadRequest(new
            {
                message = "NIC is required."
            });
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

        if (string.IsNullOrWhiteSpace(request.Phone))
        {
            return BadRequest(new
            {
                message = "Phone number is required."
            });
        }

        if (string.IsNullOrWhiteSpace(request.Password))
        {
            return BadRequest(new
            {
                message = "Password is required."
            });
        }

        if (string.IsNullOrWhiteSpace(request.Role))
        {
            return BadRequest(new
            {
                message = "Role is required."
            });
        }

        // =====================================================
        // Normalize input
        // =====================================================

        var nic = request.NIC.Trim();

        var fullName = request.FullName.Trim();

        var email = request.Email
            .Trim()
            .ToLowerInvariant();

        var phone = request.Phone.Trim();

        var role = request.Role.Trim();

        // =====================================================
        // Validate role
        // =====================================================

       if (!SmartSolarMicrogridAPI.Models.User.AllowedRoles.Contains(role))
        {
           return BadRequest(new
{
    message =
        $"Role must be one of: " +
        $"{string.Join(
            ", ",
            SmartSolarMicrogridAPI.Models.User.AllowedRoles)}."
});
        }

        // =====================================================
        // Check duplicate email
        // =====================================================

        var existingUser =
            await _userService.GetUserByEmailAsync(email);

        if (existingUser != null)
        {
            return Conflict(new
            {
                message = "Email already exists."
            });
        }

        // =====================================================
        // Check duplicate NIC
        // =====================================================

        var allUsers =
            await _userService.GetUsersAsync();

        var existingNIC =
            allUsers.FirstOrDefault(
                x => string.Equals(
                    x.NIC?.Trim(),
                    nic,
                    StringComparison.OrdinalIgnoreCase));

        if (existingNIC != null)
        {
            return Conflict(new
            {
                message = "NIC already exists."
            });
        }

        // =====================================================
        // Create User model
        // =====================================================

        var user = new User
        {
            NIC = nic,

            FullName = fullName,

            Email = email,

            Phone = phone,

            // IMPORTANT:
            // UserService will BCrypt-hash this password
            // before saving it to MongoDB.
            PasswordHash = request.Password,

            // Backoffice selects the role.
            Role = role,

            // Backoffice-created users are already approved.
            Status = "Approved",

            IsActive = true,

            CreatedAt = DateTime.UtcNow
        };

        // =====================================================
        // Save user
        // =====================================================

        try
        {
            await _userService.CreateUserAsync(user);
        }
        catch (MongoWriteException ex)
            when (
                ex.WriteError.Category ==
                ServerErrorCategory.DuplicateKey)
        {
            return Conflict(new
            {
                message =
                    "A user with the same unique information already exists."
            });
        }

        // =====================================================
        // Retrieve created user
        // =====================================================

        var created =
            await _userService.GetUserByEmailAsync(user.Email);

        if (created == null)
        {
            return StatusCode(500, new
            {
                message =
                    "User was created but could not be retrieved."
            });
        }

        // =====================================================
        // Return created user
        // Never return PasswordHash
        // =====================================================

        return CreatedAtAction(
            nameof(GetUserById),
            new { id = created.Id },
            new
            {
                id = created.Id,
                nic = created.NIC,
                fullName = created.FullName,
                email = created.Email,
                phone = created.Phone,
                role = created.Role,
                status = created.Status,
                isActive = created.IsActive,
                createdAt = created.CreatedAt
            });
    }


    // =========================================================
    // POST: api/users/register
    // Public Prosumer registration
    //
    // New users:
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

        // -------------------------
        // Validate NIC
        // -------------------------
        if (string.IsNullOrWhiteSpace(request.NIC))
        {
            return BadRequest(new
            {
                message = "NIC is required."
            });
        }

        // -------------------------
        // Validate Full Name
        // -------------------------
        if (string.IsNullOrWhiteSpace(request.FullName))
        {
            return BadRequest(new
            {
                message = "Full name is required."
            });
        }

        // -------------------------
        // Validate Email
        // -------------------------
        if (string.IsNullOrWhiteSpace(request.Email))
        {
            return BadRequest(new
            {
                message = "Email is required."
            });
        }

        // -------------------------
        // Validate Phone
        // -------------------------
        if (string.IsNullOrWhiteSpace(request.Phone))
        {
            return BadRequest(new
            {
                message = "Phone number is required."
            });
        }

        // -------------------------
        // Validate Password
        // -------------------------
        if (string.IsNullOrWhiteSpace(request.Password))
        {
            return BadRequest(new
            {
                message = "Password is required."
            });
        }

        // =====================================================
        // Normalize input
        // =====================================================

        var nic = request.NIC.Trim();

        // Store email in lowercase because AuthService
        // also searches using lowercase email.
        var email = request.Email
            .Trim()
            .ToLowerInvariant();

        var phone = request.Phone.Trim();

        // -------------------------
        // Check duplicate email
        // -------------------------

        var existingEmail =
            await _userService.GetUserByEmailAsync(email);

        if (existingEmail != null)
        {
            return Conflict(new
            {
                message = "Email already exists."
            });
        }

        // -------------------------
        // Check duplicate NIC
        // -------------------------

        var allUsers =
            await _userService.GetUsersAsync();

        var existingNIC =
            allUsers.FirstOrDefault(
                x => string.Equals(
                    x.NIC?.Trim(),
                    nic,
                    StringComparison.OrdinalIgnoreCase));

        if (existingNIC != null)
        {
            return Conflict(new
            {
                message = "NIC already exists."
            });
        }

        // -------------------------
        // Public registration
        // ALWAYS create Prosumer
        // -------------------------

        var user = new User
        {
            NIC = nic,

            FullName = request.FullName.Trim(),

            Email = email,

            Phone = phone,

            // UserService should hash this password
            PasswordHash = request.Password,

            // Never trust role sent by mobile app
            Role = "Prosumer",

            // Requires Backoffice approval
            IsActive = false,

            Status = "Pending",

            CreatedAt = DateTime.UtcNow
        };

        try
        {
            await _userService.CreateUserAsync(user);
        }
        catch (MongoWriteException ex)
            when (
                ex.WriteError.Category ==
                ServerErrorCategory.DuplicateKey)
        {
            return Conflict(new
            {
                message =
                    "A user with the same unique information already exists."
            });
        }

        return Ok(new
        {
            message =
                "Registration submitted successfully. Your account is pending approval.",

            status = user.Status,

            nic = user.NIC,

            fullName = user.FullName,

            email = user.Email,

            phone = user.Phone,

            role = user.Role
        });
    }


    // =========================================================
    // GET: api/users/me
    // Get currently logged-in user's profile
    // =========================================================
    [HttpGet("me")]
    [Authorize]
    public async Task<IActionResult> GetMyProfile()
    {
        var userId =
            User.FindFirstValue(
                ClaimTypes.NameIdentifier);

        if (string.IsNullOrWhiteSpace(userId))
        {
            return Unauthorized(new
            {
                message =
                    "User identity could not be determined from token."
            });
        }

        var user =
            await _userService.GetByIdAsync(userId);

        if (user == null)
        {
            return NotFound(new
            {
                message = "User profile not found."
            });
        }

        return Ok(new
        {
            id = user.Id,
            nic = user.NIC,
            fullName = user.FullName,
            email = user.Email,
            phone = user.Phone,
            role = user.Role,
            status = user.Status,
            isActive = user.IsActive,
            createdAt = user.CreatedAt
        });
    }


    // =========================================================
    // PUT: api/users/me
    // Logged-in user can modify own profile
    //
    // Password is NOT changed here.
    // NIC is NOT changed here.
    // =========================================================
    [HttpPut("me")]
    [Authorize]
    public async Task<IActionResult> UpdateMyProfile(
        [FromBody] UpdateProfileRequest request)
    {
        if (!ModelState.IsValid)
        {
            return ValidationProblem(ModelState);
        }

        var userId =
            User.FindFirstValue(
                ClaimTypes.NameIdentifier);

        if (string.IsNullOrWhiteSpace(userId))
        {
            return Unauthorized(new
            {
                message =
                    "User identity could not be determined from token."
            });
        }

        var existing =
            await _userService.GetByIdAsync(userId);

        if (existing == null)
        {
            return NotFound(new
            {
                message = "User profile not found."
            });
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

        if (string.IsNullOrWhiteSpace(request.Phone))
        {
            return BadRequest(new
            {
                message = "Phone number is required."
            });
        }

        // Normalize email
        var email =
            request.Email.Trim().ToLowerInvariant();

        // Check whether another user already owns this email
        var owner =
            await _userService.GetUserByEmailAsync(email);

        if (owner != null &&
            owner.Id != existing.Id)
        {
            return Conflict(new
            {
                message = "Email already exists."
            });
        }

        // Update only allowed profile fields
        existing.FullName =
            request.FullName.Trim();

        existing.Email =
            email;

        existing.Phone =
            request.Phone.Trim();

        // Do NOT change:
        // NIC
        // PasswordHash
        // Role
        // Status
        // IsActive
        // CreatedAt

        try
        {
            await _userService.UpdateAsync(
                userId,
                existing);
        }
        catch (MongoWriteException ex)
            when (
                ex.WriteError.Category ==
                ServerErrorCategory.DuplicateKey)
        {
            return Conflict(new
            {
                message = "Email already exists."
            });
        }

        var result =
            await _userService.GetByIdAsync(userId);

        if (result == null)
        {
            return NotFound(new
            {
                message = "User profile not found."
            });
        }

        return Ok(new
        {
            message = "Profile updated successfully.",

            user = new
            {
                id = result.Id,
                nic = result.NIC,
                fullName = result.FullName,
                email = result.Email,
                phone = result.Phone,
                role = result.Role,
                status = result.Status,
                isActive = result.IsActive,
                createdAt = result.CreatedAt
            }
        });
    }


    // =========================================================
    // POST: api/users/me/deactivation-request
    // Logged-in user can request account deactivation
    // =========================================================
    [HttpPost("me/deactivation-request")]
    [Authorize]
    public async Task<IActionResult> RequestDeactivation()
    {
        var userId =
            User.FindFirstValue(
                ClaimTypes.NameIdentifier);

        if (string.IsNullOrWhiteSpace(userId))
        {
            return Unauthorized(new
            {
                message =
                    "User identity could not be determined from token."
            });
        }

        var user =
            await _userService.GetByIdAsync(userId);

        if (user == null)
        {
            return NotFound(new
            {
                message = "User profile not found."
            });
        }

        if (!user.IsActive)
        {
            return BadRequest(new
            {
                message = "This account is already inactive."
            });
        }

        // Deactivate the account.
        // Keep Status = Approved so Backoffice can still
        // identify the original approval state.
        user.IsActive = false;

        await _userService.UpdateAsync(
            userId,
            user);

        return Ok(new
        {
            message =
                "Account deactivation request submitted successfully.",

            id = user.Id,

            nic = user.NIC,

            status = user.Status,

            isActive = user.IsActive
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
                message =
                    "Invalid user id. Must be a 24-digit hex string."
            });
        }

        var user =
            await _userService.GetByIdAsync(id);

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
                message =
                    "User account is already approved."
            });
        }

        if (string.Equals(
            user.Status,
            "Rejected",
            StringComparison.OrdinalIgnoreCase))
        {
            return BadRequest(new
            {
                message =
                    "Rejected users cannot be approved directly."
            });
        }

        user.Status = "Approved";
        user.IsActive = true;

        await _userService.UpdateAsync(
            id,
            user);

        return Ok(new
        {
            message = "User approved successfully.",

            id = user.Id,

            nic = user.NIC,

            fullName = user.FullName,

            email = user.Email,

            phone = user.Phone,

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
                message =
                    "Invalid user id. Must be a 24-digit hex string."
            });
        }

        var user =
            await _userService.GetByIdAsync(id);

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
                message =
                    "Approved users cannot be rejected."
            });
        }

        if (string.Equals(
            user.Status,
            "Rejected",
            StringComparison.OrdinalIgnoreCase))
        {
            return BadRequest(new
            {
                message =
                    "User account is already rejected."
            });
        }

        user.Status = "Rejected";
        user.IsActive = false;

        await _userService.UpdateAsync(
            id,
            user);

        return Ok(new
        {
            message = "User rejected successfully.",

            id = user.Id,

            nic = user.NIC,

            fullName = user.FullName,

            email = user.Email,

            phone = user.Phone,

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
                message =
                    "Invalid user id. Must be a 24-digit hex string."
            });
        }

        var existing =
            await _userService.GetByIdAsync(id);

        if (existing is null)
        {
            return NotFound(new
            {
                message = "User not found."
            });
        }

        if (string.IsNullOrWhiteSpace(updated.NIC))
        {
            return BadRequest(new
            {
                message = "NIC is required."
            });
        }

        if (string.IsNullOrWhiteSpace(updated.Email))
        {
            return BadRequest(new
            {
                message = "Email is required."
            });
        }

        updated.NIC =
            updated.NIC.Trim();

        updated.Email =
            updated.Email.Trim().ToLowerInvariant();

        updated.Phone =
            updated.Phone?.Trim() ?? string.Empty;

        // Check email ownership
        var owner =
            await _userService.GetUserByEmailAsync(
                updated.Email);

        if (owner is not null &&
            owner.Id != id)
        {
            return Conflict(new
            {
                message = "Email already exists."
            });
        }

        // Check NIC ownership
        var allUsers =
            await _userService.GetUsersAsync();

        var nicOwner =
            allUsers.FirstOrDefault(
                x =>
                    x.Id != id &&
                    string.Equals(
                        x.NIC?.Trim(),
                        updated.NIC,
                        StringComparison.OrdinalIgnoreCase));

        if (nicOwner != null)
        {
            return Conflict(new
            {
                message = "NIC already exists."
            });
        }

        // =====================================================
        // Preserve protected fields if they are not intentionally
        // changed through Backoffice.
        // =====================================================

        existing.NIC =
            updated.NIC;

        existing.FullName =
            updated.FullName.Trim();

        existing.Email =
            updated.Email;

        existing.Phone =
            updated.Phone;

        existing.Role =
            updated.Role;

        existing.Status =
            updated.Status;

        existing.IsActive =
            updated.IsActive;

        // Keep existing password unless a separate password
        // update feature is implemented.
        existing.PasswordHash =
            existing.PasswordHash;

        // Keep original creation date
        existing.CreatedAt =
            existing.CreatedAt;

        try
        {
            await _userService.UpdateAsync(
                id,
                existing);
        }
        catch (MongoWriteException ex)
            when (
                ex.WriteError.Category ==
                ServerErrorCategory.DuplicateKey)
        {
            return Conflict(new
            {
                message =
                    "A user with the same unique information already exists."
            });
        }

        var result =
            await _userService.GetByIdAsync(id);

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

            nic = result.NIC,

            fullName = result.FullName,

            email = result.Email,

            phone = result.Phone,

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
                message =
                    "Invalid user id. Must be a 24-digit hex string."
            });
        }

        var existing =
            await _userService.GetByIdAsync(id);

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


// =========================================================
// Backoffice Create User Request
// =========================================================
public class CreateUserRequest
{
    public string FullName { get; set; } = string.Empty;

    public string NIC { get; set; } = string.Empty;

    public string Email { get; set; } = string.Empty;

    public string Phone { get; set; } = string.Empty;

    public string Role { get; set; } = string.Empty;

    public string Password { get; set; } = string.Empty;
}


// =========================================================
// Self Profile Update Request
// =========================================================
public class UpdateProfileRequest
{
    public string FullName { get; set; } = string.Empty;

    public string Email { get; set; } = string.Empty;

    public string Phone { get; set; } = string.Empty;
}

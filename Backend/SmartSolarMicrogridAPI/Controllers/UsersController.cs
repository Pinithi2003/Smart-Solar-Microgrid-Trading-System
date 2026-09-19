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
    [HttpGet]
    public async Task<ActionResult<List<User>>> GetUsers()
    {
        var users = await _userService.GetUsersAsync();
        return Ok(users);
    }

    // GET: api/users/{id} (Mongo ObjectId)
    [HttpGet("{id}")]
    public async Task<ActionResult<User>> GetUserById(string id)
    {
        if (!ObjectId.TryParse(id, out _))
            return BadRequest(new { message = "Invalid user id. Must be a 24-digit hex string." });

        var user = await _userService.GetByIdAsync(id);
        if (user == null)
            return NotFound(new { message = "User not found." });

        return Ok(user);
    }

    // GET: api/users/email/{email}
    [HttpGet("email/{email}")]
    public async Task<ActionResult<User>> GetUserByEmail(string email)
    {
        if (string.IsNullOrWhiteSpace(email))
            return BadRequest(new { message = "Email is required." });

        var user = await _userService.GetUserByEmailAsync(email);

        if (user == null)
        {
            return NotFound(new
            {
                message = "User not found."
            });
        }

        return Ok(user);
    }

    // POST: api/users
    [HttpPost]
    public async Task<IActionResult> CreateUser([FromBody] User user)
    {
        if (!ModelState.IsValid) return ValidationProblem(ModelState);

        // Mongo _id is an ObjectId. Ignore any client-sent Id like "USR001"/"string".
        user.Id = null;
        user.CreatedAt = DateTime.UtcNow;

        var existingUser = await _userService.GetUserByEmailAsync(user.Email);
        if (existingUser != null)
        {
            return Conflict(new { message = "Email already exists." });
        }

        try
        {
            await _userService.CreateUserAsync(user);
        }
        catch (MongoWriteException ex) when (ex.WriteError.Category == ServerErrorCategory.DuplicateKey)
        {
            return Conflict(new { message = "Email already exists." });
        }

        var created = await _userService.GetUserByEmailAsync(user.Email);
        return CreatedAtAction(nameof(GetUserById), new { id = created!.Id }, created);
    }

    // PUT: api/users/{id}
    [HttpPut("{id}")]
    public async Task<ActionResult<User>> Update(string id, [FromBody] User updated)
    {
        if (!ModelState.IsValid) return ValidationProblem(ModelState);
        if (!ObjectId.TryParse(id, out _))
            return BadRequest(new { message = "Invalid user id. Must be a 24-digit hex string." });

        var existing = await _userService.GetByIdAsync(id);
        if (existing is null)
            return NotFound(new { message = "User not found." });

        // If email is changing, ensure no other user owns it.
        var owner = await _userService.GetUserByEmailAsync(updated.Email);
        if (owner is not null && owner.Id != id)
            return Conflict(new { message = "Email already exists." });

        try
        {
            await _userService.UpdateAsync(id, updated);
        }
        catch (MongoWriteException ex) when (ex.WriteError.Category == ServerErrorCategory.DuplicateKey)
        {
            return Conflict(new { message = "Email already exists." });
        }

        return Ok(await _userService.GetByIdAsync(id));
    }

    // DELETE: api/users/{id}
    [HttpDelete("{id}")]
    public async Task<IActionResult> Delete(string id)
    {
        if (!ObjectId.TryParse(id, out _))
            return BadRequest(new { message = "Invalid user id. Must be a 24-digit hex string." });

        var existing = await _userService.GetByIdAsync(id);
        if (existing is null)
            return NotFound(new { message = "User not found." });

        await _userService.DeleteAsync(id);
        return Ok(new { message = "User deleted successfully." });
    }
}

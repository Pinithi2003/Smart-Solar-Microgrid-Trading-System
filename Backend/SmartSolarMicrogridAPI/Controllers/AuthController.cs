using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogridAPI.Models;
using SmartSolarMicrogridAPI.Services;

namespace SmartSolarMicrogridAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class AuthController : ControllerBase
    {
        private readonly AuthService _authService;

        public AuthController(AuthService authService)
        {
            _authService = authService;
        }

        // =========================================================
        // POST: api/auth/register
        // Register new Prosumer
        // =========================================================
        [HttpPost("register")]
        [AllowAnonymous]
        public async Task<IActionResult> Register(
            [FromBody] RegisterRequest request)
        {
            // -----------------------------------------------------
            // Validate request model
            // -----------------------------------------------------
            if (!ModelState.IsValid)
            {
                return ValidationProblem(ModelState);
            }

            // -----------------------------------------------------
            // Validate NIC
            // -----------------------------------------------------
            if (string.IsNullOrWhiteSpace(request.NIC))
            {
                return BadRequest(new
                {
                    message = "NIC is required."
                });
            }

            // -----------------------------------------------------
            // Validate full name
            // -----------------------------------------------------
            if (string.IsNullOrWhiteSpace(request.FullName))
            {
                return BadRequest(new
                {
                    message = "Full name is required."
                });
            }

            // -----------------------------------------------------
            // Validate email
            // -----------------------------------------------------
            if (string.IsNullOrWhiteSpace(request.Email))
            {
                return BadRequest(new
                {
                    message = "Email is required."
                });
            }

            // -----------------------------------------------------
            // Validate phone
            // -----------------------------------------------------
            if (string.IsNullOrWhiteSpace(request.Phone))
            {
                return BadRequest(new
                {
                    message = "Phone number is required."
                });
            }

            // -----------------------------------------------------
            // Validate password
            // -----------------------------------------------------
            if (string.IsNullOrWhiteSpace(request.Password))
            {
                return BadRequest(new
                {
                    message = "Password is required."
                });
            }

            // -----------------------------------------------------
            // Register Prosumer
            // -----------------------------------------------------
            var result =
                await _authService.RegisterProsumerAsync(request);

            // -----------------------------------------------------
            // Duplicate email/NIC
            // -----------------------------------------------------
            if (!result.Success)
            {
                return Conflict(new
                {
                    message = result.Message
                });
            }

            // -----------------------------------------------------
            // Registration successful
            // -----------------------------------------------------
            return StatusCode(
                StatusCodes.Status201Created,
                new
                {
                    message = result.Message,

                    user = new
                    {
                        id = result.User!.Id,
                        nic = result.User.NIC,
                        fullName = result.User.FullName,
                        email = result.User.Email,
                        phone = result.User.Phone,
                        role = result.User.Role,
                        status = result.User.Status,
                        isActive = result.User.IsActive
                    }
                });
        }

        // =========================================================
        // POST: api/auth/login
        // User login
        // =========================================================
        [HttpPost("login")]
        [AllowAnonymous]
        public async Task<IActionResult> Login(
            [FromBody] LoginRequest request)
        {
            // -----------------------------------------------------
            // Validate model
            // -----------------------------------------------------
            if (!ModelState.IsValid)
            {
                return ValidationProblem(ModelState);
            }

            // -----------------------------------------------------
            // Validate email
            // -----------------------------------------------------
            if (string.IsNullOrWhiteSpace(request.Email))
            {
                return BadRequest(new
                {
                    message = "Email is required."
                });
            }

            // -----------------------------------------------------
            // Validate password
            // -----------------------------------------------------
            if (string.IsNullOrWhiteSpace(request.Password))
            {
                return BadRequest(new
                {
                    message = "Password is required."
                });
            }

            // -----------------------------------------------------
            // Normalize email
            // -----------------------------------------------------
            var email =
                request.Email.Trim().ToLowerInvariant();

            // -----------------------------------------------------
            // Find user
            // -----------------------------------------------------
            var user =
                await _authService.GetUserByEmailAsync(email);

            if (user == null)
            {
                return Unauthorized(new
                {
                    message = "Invalid email or password."
                });
            }

            // -----------------------------------------------------
            // Verify password
            // -----------------------------------------------------
            if (!BCrypt.Net.BCrypt.Verify(
                request.Password,
                user.PasswordHash))
            {
                return Unauthorized(new
                {
                    message = "Invalid email or password."
                });
            }

            // -----------------------------------------------------
            // Pending account
            // -----------------------------------------------------
            if (string.Equals(
                user.Status,
                "Pending",
                StringComparison.OrdinalIgnoreCase))
            {
                return Unauthorized(new
                {
                    message =
                        "Your account is pending Backoffice approval."
                });
            }

            // -----------------------------------------------------
            // Rejected account
            // -----------------------------------------------------
            if (string.Equals(
                user.Status,
                "Rejected",
                StringComparison.OrdinalIgnoreCase))
            {
                return Unauthorized(new
                {
                    message =
                        "Your account registration was rejected."
                });
            }

            // -----------------------------------------------------
            // Only Approved users can login
            // -----------------------------------------------------
            if (!string.Equals(
                user.Status,
                "Approved",
                StringComparison.OrdinalIgnoreCase))
            {
                return Unauthorized(new
                {
                    message =
                        "Your account is not approved for login."
                });
            }

            // -----------------------------------------------------
            // Check active status
            // -----------------------------------------------------
            if (!user.IsActive)
            {
                return Unauthorized(new
                {
                    message =
                        "User account is inactive."
                });
            }

            // -----------------------------------------------------
            // Generate JWT
            // -----------------------------------------------------
            var token =
                _authService.GenerateJwtToken(user);

            // -----------------------------------------------------
            // Login successful
            // -----------------------------------------------------
            return Ok(new
            {
                message = "Login successful.",

                token = token,

                user = new
                {
                    id = user.Id,
                    nic = user.NIC,
                    fullName = user.FullName,
                    email = user.Email,
                    phone = user.Phone,
                    role = user.Role,
                    status = user.Status,
                    isActive = user.IsActive
                }
            });
        }
    }
}
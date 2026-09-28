using System.Security.Claims;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogridAPI.Models;
using SmartSolarMicrogridAPI.Services;

namespace SmartSolarMicrogridAPI.Controllers;

[ApiController]
[Route("api/reservations")]
public class EnergyReservationsController : ControllerBase
{
    private readonly ReservationService _service;

    public EnergyReservationsController(ReservationService service) => _service = service;

    // GET /api/reservations  (all reservations - handy for an admin/demo view)
    [HttpGet]
    public async Task<ActionResult<List<EnergyReservation>>> GetAll()
    {
        var reservations = await _service.GetAllReservationsAsync();
        return Ok(reservations);
    }

    // GET /api/reservations/my?userId=USR001
    // When a JWT is present, the token user is used. Otherwise userId is accepted for Postman and the web UI.
    [HttpGet("my")]
    public async Task<ActionResult<List<EnergyReservation>>> GetMine([FromQuery] string? userId)
    {
        var id = User.FindFirstValue(ClaimTypes.NameIdentifier);
        if (string.IsNullOrWhiteSpace(id))
            id = userId;

        if (string.IsNullOrWhiteSpace(id))
            return Unauthorized(new { message = "Sign in, or pass userId, to view your reservations." });

        return Ok(await _service.GetUserReservationsAsync(id));
    }

    // GET /api/reservations/RES001
    [HttpGet("{reservationId}")]
    public async Task<ActionResult<EnergyReservation>> GetOne(string reservationId)
    {
        var reservation = await _service.GetReservationByIdAsync(reservationId);
        return reservation is null
            ? NotFound(new { message = $"Reservation {reservationId} not found." })
            : Ok(reservation);
    }

    // GET /api/reservations/user/USR001
    [HttpGet("user/{userId}")]
    public async Task<ActionResult<List<EnergyReservation>>> GetForUser(string userId)
    {
        var reservations = await _service.GetUserReservationsAsync(userId);
        return Ok(reservations);
    }

    // POST /api/reservations
    [HttpPost]
    public async Task<ActionResult<EnergyReservation>> Create([FromBody] CreateReservationDto dto)
    {
        if (!ModelState.IsValid) return ValidationProblem(ModelState);

        var (success, statusCode, message, reservation) = await _service.CreateReservationAsync(dto);

        if (!success)
            return StatusCode(statusCode, new { message });

        return CreatedAtAction(nameof(GetOne), new { reservationId = reservation!.ReservationId },
            new { message, reservation });
    }

    // PATCH /api/reservations/RES001/cancel
    [HttpPatch("{reservationId}/cancel")]
    public async Task<ActionResult<EnergyReservation>> Cancel(string reservationId)
    {
        var (callerId, callerRole) = Caller();
        var (success, statusCode, message, reservation) = await _service.CancelReservationAsync(reservationId, callerId, callerRole);

        if (!success)
            return StatusCode(statusCode, new { message, reservation });

        return Ok(new { message, reservation });
    }

    // PUT /api/reservations/RES001
    [HttpPut("{reservationId}")]
    public async Task<ActionResult<EnergyReservation>> Update(string reservationId, [FromBody] UpdateReservationDto dto)
    {
        if (!ModelState.IsValid) return ValidationProblem(ModelState);

        var (callerId, callerRole) = Caller();
        var (success, statusCode, message, reservation) = await _service.UpdateReservationAsync(reservationId, dto, callerId, callerRole);
        if (!success)
            return StatusCode(statusCode, new { message, reservation });

        return Ok(new { message, reservation });
    }

    // PATCH /api/reservations/RES001/status
    [HttpPatch("{reservationId}/status")]
    public async Task<ActionResult<EnergyReservation>> UpdateStatus(string reservationId, [FromBody] StatusUpdateDto dto)
    {
        if (!ModelState.IsValid) return ValidationProblem(ModelState);

        var (callerId, callerRole) = Caller();
        var (success, statusCode, message, reservation) = await _service.UpdateStatusAsync(reservationId, dto.Status, callerId, callerRole);
        if (!success)
            return StatusCode(statusCode, new { message, reservation });

        return Ok(new { message, reservation });
    }

    // DELETE /api/reservations/RES001
    [HttpDelete("{reservationId}")]
    public async Task<IActionResult> Delete(string reservationId)
    {
        var (callerId, callerRole) = Caller();
        var (success, statusCode, message) = await _service.DeleteReservationAsync(reservationId, callerId, callerRole);
        if (!success)
            return StatusCode(statusCode, new { message });

        return Ok(new { message });
    }

    private (string? UserId, string? Role) Caller() =>
        (User.FindFirstValue(ClaimTypes.NameIdentifier), User.FindFirstValue(ClaimTypes.Role));
}

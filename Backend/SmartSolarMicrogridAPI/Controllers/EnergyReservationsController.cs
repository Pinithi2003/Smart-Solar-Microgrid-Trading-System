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
        var (success, statusCode, message, reservation) = await _service.CancelReservationAsync(reservationId);

        if (!success)
            return StatusCode(statusCode, new { message, reservation });

        return Ok(new { message, reservation });
    }
}

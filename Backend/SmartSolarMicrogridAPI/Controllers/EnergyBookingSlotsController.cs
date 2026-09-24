using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogridAPI.Models;
using SmartSolarMicrogridAPI.Services;

namespace SmartSolarMicrogridAPI.Controllers;

[ApiController]
[Route("api/booking-slots")]
public class EnergyBookingSlotsController : ControllerBase
{
    private readonly ReservationService _service;

    public EnergyBookingSlotsController(ReservationService service) => _service = service;

    // GET /api/booking-slots
    // GET /api/booking-slots?stationId=ST001&date=2026-09-20
    [HttpGet]
    public async Task<ActionResult<List<EnergyBookingSlot>>> GetAll([FromQuery] string? stationId, [FromQuery] DateTime? date)
    {
        var slots = await _service.GetSlotsAsync(stationId, date);
        return Ok(slots);
    }

    // GET /api/booking-slots/SLOT001
    [HttpGet("{slotId}")]
    public async Task<ActionResult<EnergyBookingSlot>> GetOne(string slotId)
    {
        var slot = await _service.GetSlotByIdAsync(slotId);
        return slot is null ? NotFound(new { message = $"Slot {slotId} not found." }) : Ok(slot);
    }
}

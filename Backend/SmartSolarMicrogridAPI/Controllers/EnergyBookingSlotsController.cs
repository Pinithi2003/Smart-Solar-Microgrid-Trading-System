// ------------------------------------------------------------
// Project     : Smart Solar Microgrid Trading System
// Module      : Energy Booking and Reservation (Member 3)
// Course      : SE4040 Enterprise Application Development
// File        : EnergyBookingSlotsController.cs
// Description : REST API for creating, updating, deleting and listing booking slots.
// ------------------------------------------------------------

using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogridAPI.Models;
using SmartSolarMicrogridAPI.Services;

namespace SmartSolarMicrogridAPI.Controllers;

[ApiController]
[Route("api/booking-slots")]
public class EnergyBookingSlotsController : ControllerBase
{
    private readonly ReservationService _service;

    public EnergyBookingSlotsController(ReservationService service)
    {
        // Receives the reservation service used by the slot API.
        _service = service;
    }

    // GET /api/booking-slots
    // GET /api/booking-slots?stationId=ST001&date=2026-09-20
    [HttpGet]
    public async Task<ActionResult<List<EnergyBookingSlot>>> GetAll([FromQuery] string? stationId, [FromQuery] DateTime? date)
    {
        // Lists booking slots, optionally filtered by station and date.
        var slots = await _service.GetSlotsAsync(stationId, date);
        return Ok(slots);
    }

    // GET /api/booking-slots/SLOT001
    [HttpGet("{slotId}")]
    public async Task<ActionResult<EnergyBookingSlot>> GetOne(string slotId)
    {
        // Returns one booking slot by id.
        var slot = await _service.GetSlotByIdAsync(slotId);
        return slot is null ? NotFound(new { message = $"Slot {slotId} not found." }) : Ok(slot);
    }

    // POST /api/booking-slots
    [HttpPost]
    public async Task<ActionResult<EnergyBookingSlot>> Create([FromBody] CreateBookingSlotDto dto)
    {
        // Creates a booking slot.
        if (!ModelState.IsValid) return ValidationProblem(ModelState);

        var (success, statusCode, message, slot) = await _service.CreateSlotAsync(dto);
        if (!success)
            return StatusCode(statusCode, new { message });

        return CreatedAtAction(nameof(GetOne), new { slotId = slot!.SlotId }, new { message, slot });
    }

    // PUT /api/booking-slots/SLOT001
    [HttpPut("{slotId}")]
    public async Task<ActionResult<EnergyBookingSlot>> Update(string slotId, [FromBody] UpdateBookingSlotDto dto)
    {
        // Updates a booking slot.
        if (!ModelState.IsValid) return ValidationProblem(ModelState);

        var (success, statusCode, message, slot) = await _service.UpdateSlotAsync(slotId, dto);
        if (!success)
            return StatusCode(statusCode, new { message, slot });

        return Ok(new { message, slot });
    }

    // DELETE /api/booking-slots/SLOT001
    [HttpDelete("{slotId}")]
    public async Task<IActionResult> Delete(string slotId)
    {
        // Deletes a booking slot.
        var (success, statusCode, message) = await _service.DeleteSlotAsync(slotId);
        if (!success)
            return StatusCode(statusCode, new { message });

        return Ok(new { message });
    }
}

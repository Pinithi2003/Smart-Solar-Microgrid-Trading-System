// ------------------------------------------------------------
// Project     : Smart Solar Microgrid Trading System
// Module      : Energy Booking and Reservation (Member 3)
// Course      : SE4040 Enterprise Application Development
// File        : UpdateBookingSlotDto.cs
// Description : Request body for updating a booking slot.
// ------------------------------------------------------------

using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogridAPI.Models;

/// <summary>Request body for PUT /api/booking-slots/{slotId}.</summary>
public class UpdateBookingSlotDto
{
    public DateTime? Date { get; set; }

    public string? StartTime { get; set; }

    public string? EndTime { get; set; }

    [Range(0.1, double.MaxValue, ErrorMessage = "Energy capacity must be greater than 0.")]
    public double? EnergyCapacity { get; set; }

    public string? Status { get; set; }
}

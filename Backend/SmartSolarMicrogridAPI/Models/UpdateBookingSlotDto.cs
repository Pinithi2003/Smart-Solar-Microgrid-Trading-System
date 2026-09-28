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

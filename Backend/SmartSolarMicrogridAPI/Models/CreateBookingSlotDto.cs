using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogridAPI.Models;

/// <summary>Request body for POST /api/booking-slots.</summary>
public class CreateBookingSlotDto
{
    [Required(ErrorMessage = "StationId is required.")]
    public string StationId { get; set; } = string.Empty;

    [Required(ErrorMessage = "Date is required.")]
    public DateTime Date { get; set; }

    [Required(ErrorMessage = "StartTime is required (e.g. 10:00 AM).")]
    public string StartTime { get; set; } = string.Empty;

    [Required(ErrorMessage = "EndTime is required (e.g. 12:00 PM).")]
    public string EndTime { get; set; } = string.Empty;

    [Range(0.1, double.MaxValue, ErrorMessage = "Energy capacity must be greater than 0.")]
    public double EnergyCapacity { get; set; }
}

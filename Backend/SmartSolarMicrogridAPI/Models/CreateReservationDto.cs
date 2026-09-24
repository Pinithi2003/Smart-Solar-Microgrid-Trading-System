using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogridAPI.Models;

/// <summary>Request body for POST /api/reservations.</summary>
public class CreateReservationDto
{
    [Required(ErrorMessage = "UserId is required.")]
    public string UserId { get; set; } = string.Empty;

    [Required(ErrorMessage = "StationId is required.")]
    public string StationId { get; set; } = string.Empty;

    [Required(ErrorMessage = "SlotId is required.")]
    public string SlotId { get; set; } = string.Empty;

    [Range(0.1, double.MaxValue, ErrorMessage = "Energy amount must be greater than 0.")]
    public double EnergyAmount { get; set; }
}

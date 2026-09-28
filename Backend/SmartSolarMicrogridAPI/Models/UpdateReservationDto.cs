using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogridAPI.Models;

/// <summary>Request body for PUT /api/reservations/{reservationId}.</summary>
public class UpdateReservationDto
{
    public string? SlotId { get; set; }

    [Range(0.1, double.MaxValue, ErrorMessage = "Energy amount must be greater than 0.")]
    public double? EnergyAmount { get; set; }
}

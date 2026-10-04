// ------------------------------------------------------------
// Project     : Smart Solar Microgrid Trading System
// Module      : Energy Booking and Reservation (Member 3)
// Course      : SE4040 Enterprise Application Development
// File        : UpdateReservationDto.cs
// Description : Request body for updating a reservation.
// ------------------------------------------------------------

using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogridAPI.Models;

/// <summary>Request body for PUT /api/reservations/{reservationId}.</summary>
public class UpdateReservationDto
{
    public string? SlotId { get; set; }

    [Range(0.1, double.MaxValue, ErrorMessage = "Energy amount must be greater than 0.")]
    public double? EnergyAmount { get; set; }
}

using System.ComponentModel.DataAnnotations;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartSolarMicrogridAPI.Models;

/// <summary>
/// One document in the EnergyReservation MongoDB collection (Member 3).
/// Business key is ReservationId (RES001...). Mongo _id stays separate.
/// </summary>
public class EnergyReservation
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    [System.Text.Json.Serialization.JsonIgnore]
    public string? Id { get; set; }

    [BsonElement("reservationId")]
    public string ReservationId { get; set; } = string.Empty;

    [BsonElement("userId")]
    [Required(ErrorMessage = "UserId is required.")]
    public string UserId { get; set; } = string.Empty;

    [BsonElement("stationId")]
    [Required(ErrorMessage = "StationId is required.")]
    public string StationId { get; set; } = string.Empty;

    [BsonElement("slotId")]
    [Required(ErrorMessage = "SlotId is required.")]
    public string SlotId { get; set; } = string.Empty;

    [BsonElement("date")]
    public DateTime Date { get; set; }

    [BsonElement("startTime")]
    public string StartTime { get; set; } = string.Empty;

    [BsonElement("endTime")]
    public string EndTime { get; set; } = string.Empty;

    [BsonElement("energyAmount")]
    [Range(0.1, double.MaxValue, ErrorMessage = "Energy amount must be greater than 0.")]
    public double EnergyAmount { get; set; }

    [BsonElement("status")]
    public string Status { get; set; } = "Confirmed";

    [BsonElement("createdAt")]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    [BsonElement("cancelledAt")]
    public DateTime? CancelledAt { get; set; }

    [BsonElement("verificationCode")]
    public string VerificationCode { get; set; } = string.Empty;

    // Confirmed -> Verified -> Completed, or Confirmed -> Cancelled
    public static readonly string[] AllowedStatuses = ["Confirmed", "Verified", "Completed", "Cancelled"];
}

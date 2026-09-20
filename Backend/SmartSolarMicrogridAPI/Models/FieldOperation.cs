using System.ComponentModel.DataAnnotations;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartSolarMicrogridAPI.Models;

/// <summary>
/// Represents an energy field operation associated with a confirmed reservation (Member 4).
/// </summary>
public class FieldOperation
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    [System.Text.Json.Serialization.JsonIgnore]
    public string? Id { get; set; }

    [BsonElement("operationId")]
    [Required]
    public string OperationId { get; set; } = string.Empty;

    [BsonElement("reservationId")]
    [Required]
    public string ReservationId { get; set; } = string.Empty;

    [BsonElement("stationId")]
    [Required]
    public string StationId { get; set; } = string.Empty;

    [BsonElement("stationName")]
    public string StationName { get; set; } = string.Empty;

    [BsonElement("userId")]
    public string UserId { get; set; } = string.Empty;

    [BsonElement("customerName")]
    public string CustomerName { get; set; } = string.Empty;

    [BsonElement("verificationCode")]
    [Required]
    public string VerificationCode { get; set; } = string.Empty;

    [BsonElement("energyAmount")]
    [Range(0.1, double.MaxValue, ErrorMessage = "Energy amount must be greater than 0.")]
    public double EnergyAmount { get; set; }

    [BsonElement("status")]
    public string Status { get; set; } = "Confirmed"; // Confirmed -> Verified -> Completed, or Cancelled

    [BsonElement("timeSlot")]
    public string TimeSlot { get; set; } = "10:00 AM - 12:00 PM";

    [BsonElement("bookingDate")]
    public DateTime BookingDate { get; set; } = DateTime.UtcNow;

    [BsonElement("verifiedAt")]
    public DateTime? VerifiedAt { get; set; }

    [BsonElement("completedAt")]
    public DateTime? CompletedAt { get; set; }

    [BsonElement("operatorId")]
    public string OperatorId { get; set; } = "OPR-FIELD-01";

    [BsonElement("notes")]
    public string? Notes { get; set; }

    [BsonElement("lastUpdated")]
    public DateTime LastUpdated { get; set; } = DateTime.UtcNow;

    public static readonly string[] AllowedStatuses = ["Confirmed", "Verified", "Completed", "Cancelled"];
}

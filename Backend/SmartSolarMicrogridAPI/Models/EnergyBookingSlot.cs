using System.ComponentModel.DataAnnotations;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartSolarMicrogridAPI.Models;

/// <summary>
/// One document in the EnergyBookingSlots MongoDB collection (Member 3).
/// Business key is SlotId (SLOT001...). Mongo _id stays separate.
/// </summary>
public class EnergyBookingSlot : IValidatableObject
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    [System.Text.Json.Serialization.JsonIgnore]
    public string? Id { get; set; }

    [BsonElement("slotId")]
    [Required(ErrorMessage = "SlotId is required (e.g. SLOT001).")]
    public string SlotId { get; set; } = string.Empty;

    [BsonElement("stationId")]
    [Required(ErrorMessage = "StationId is required (e.g. ST001).")]
    public string StationId { get; set; } = string.Empty;

    [BsonElement("date")]
    [Required(ErrorMessage = "Date is required.")]
    public DateTime Date { get; set; }

    [BsonElement("startTime")]
    [Required(ErrorMessage = "StartTime is required (e.g. 10:00 AM).")]
    public string StartTime { get; set; } = string.Empty;

    [BsonElement("endTime")]
    [Required(ErrorMessage = "EndTime is required (e.g. 12:00 PM).")]
    public string EndTime { get; set; } = string.Empty;

    [BsonElement("energyCapacity")]
    [Range(0, double.MaxValue, ErrorMessage = "Energy capacity cannot be negative.")]
    public double EnergyCapacity { get; set; }

    [BsonElement("availableEnergy")]
    [Range(0, double.MaxValue, ErrorMessage = "Available energy cannot be negative.")]
    public double AvailableEnergy { get; set; }

    [BsonElement("status")]
    public string Status { get; set; } = "Available";

    public static readonly string[] AllowedStatuses = ["Available", "Full", "Inactive", "Unavailable"];

    public IEnumerable<ValidationResult> Validate(ValidationContext validationContext)
    {
        if (!AllowedStatuses.Contains(Status))
            yield return new ValidationResult(
                $"Status must be one of: {string.Join(", ", AllowedStatuses)}.",
                [nameof(Status)]);
    }
}

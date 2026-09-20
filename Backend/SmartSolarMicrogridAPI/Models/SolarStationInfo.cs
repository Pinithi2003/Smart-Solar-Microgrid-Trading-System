using System.ComponentModel.DataAnnotations;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartSolarMicrogridAPI.Models;

/// <summary>
/// One document in the SolarStationInfo MongoDB collection.
/// Business key is StationId (ST001...). Mongo _id stays separate.
/// </summary>
public class SolarStationInfo : IValidatableObject
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    [System.Text.Json.Serialization.JsonIgnore]
    public string? Id { get; set; }

    [BsonElement("stationId")]
    [Required(ErrorMessage = "StationId is required (e.g. ST001).")]
    [StringLength(20, ErrorMessage = "StationId must be 20 characters or fewer.")]
    [RegularExpression(@"^ST\d{3,}$", ErrorMessage = "StationId must look like ST001 (ST followed by at least 3 digits).")]
    public string StationId { get; set; } = string.Empty;

    [BsonElement("stationName")]
    [Required(ErrorMessage = "Station name cannot be empty.")]
    [StringLength(120, MinimumLength = 2, ErrorMessage = "Station name must be 2-120 characters.")]
    public string StationName { get; set; } = string.Empty;

    [BsonElement("location")]
    [Required(ErrorMessage = "Location cannot be empty.")]
    [StringLength(120, MinimumLength = 2, ErrorMessage = "Location must be 2-120 characters.")]
    public string Location { get; set; } = string.Empty;

    [BsonElement("latitude")]
    [Range(-90, 90, ErrorMessage = "Latitude must be between -90 and 90.")]
    public double Latitude { get; set; }

    [BsonElement("longitude")]
    [Range(-180, 180, ErrorMessage = "Longitude must be between -180 and 180.")]
    public double Longitude { get; set; }

    [BsonElement("totalCapacity")]
    [Range(0.01, double.MaxValue, ErrorMessage = "Total capacity must be greater than 0.")]
    public double TotalCapacity { get; set; }

    [BsonElement("availableCapacity")]
    [Range(0, double.MaxValue, ErrorMessage = "Available capacity cannot be negative.")]
    public double AvailableCapacity { get; set; }

    [BsonElement("status")]
    public string Status { get; set; } = "Active";

    [BsonElement("operator")]
    [StringLength(120, ErrorMessage = "Operator must be 120 characters or fewer.")]
    public string Operator { get; set; } = "Grid Operator";

    [BsonElement("lastUpdated")]
    public DateTime LastUpdated { get; set; } = DateTime.UtcNow;

    public static readonly string[] AllowedStatuses = ["Active", "Inactive", "Maintenance"];

    public IEnumerable<ValidationResult> Validate(ValidationContext validationContext)
    {
        if (!AllowedStatuses.Contains(Status))
            yield return new ValidationResult(
                $"Status must be one of: {string.Join(", ", AllowedStatuses)}.",
                [nameof(Status)]);

        // [Required] accepts whitespace-only strings — reject them with a clear message.
        if (!string.IsNullOrEmpty(StationId) && string.IsNullOrWhiteSpace(StationId))
            yield return new ValidationResult("StationId cannot be blank.", [nameof(StationId)]);
        if (!string.IsNullOrEmpty(StationName) && string.IsNullOrWhiteSpace(StationName))
            yield return new ValidationResult("Station name cannot be blank.", [nameof(StationName)]);
        if (!string.IsNullOrEmpty(Location) && string.IsNullOrWhiteSpace(Location))
            yield return new ValidationResult("Location cannot be blank.", [nameof(Location)]);
        if (!string.IsNullOrEmpty(Operator) && string.IsNullOrWhiteSpace(Operator))
            yield return new ValidationResult("Operator cannot be blank.", [nameof(Operator)]);

        // (0, 0) is almost always an unpicked map pin, not a real Sri Lankan station.
        if (Latitude == 0 && Longitude == 0)
            yield return new ValidationResult(
                "Coordinates cannot be 0, 0. Pick the station position on the map or use coordinate lookup.",
                [nameof(Latitude), nameof(Longitude)]);

        if (AvailableCapacity > TotalCapacity)
            yield return new ValidationResult(
                "Available capacity must not be greater than total capacity.",
                [nameof(AvailableCapacity)]);
    }
}

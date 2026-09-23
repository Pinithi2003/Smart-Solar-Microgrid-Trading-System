using System.ComponentModel.DataAnnotations;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartSolarMicrogridAPI.Models;

public class User : IValidatableObject
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    [BsonElement("fullName")]
    [Required(ErrorMessage = "Full name cannot be empty.")]
    [StringLength(
        120,
        MinimumLength = 2,
        ErrorMessage = "Full name must be 2-120 characters.")]
    public string FullName { get; set; } = string.Empty;

    [BsonElement("email")]
    [Required(ErrorMessage = "Email is required.")]
    [EmailAddress(ErrorMessage = "Email must be a valid email address.")]
    [StringLength(
        160,
        ErrorMessage = "Email must be 160 characters or fewer.")]
    public string Email { get; set; } = string.Empty;

    [BsonElement("passwordHash")]
    [Required(ErrorMessage = "Password is required.")]
    [MinLength(
        6,
        ErrorMessage = "Password must be at least 6 characters.")]
    public string PasswordHash { get; set; } = string.Empty;

    [BsonElement("role")]
    public string Role { get; set; } = "Prosumer";

    [BsonElement("isActive")]
    public bool IsActive { get; set; } = false;

    [BsonElement("status")]
    public string Status { get; set; } = "Pending";

    [BsonElement("createdAt")]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    // Roles used in the Smart Solar Microgrid system
    public static readonly string[] AllowedRoles =
    [
        "Prosumer",
        "Grid Operator",
        "Backoffice"
    ];

    public IEnumerable<ValidationResult> Validate(
        ValidationContext validationContext)
    {
        if (!string.IsNullOrEmpty(FullName) &&
            string.IsNullOrWhiteSpace(FullName))
        {
            yield return new ValidationResult(
                "Full name cannot be blank.",
                [nameof(FullName)]);
        }

        if (!string.IsNullOrEmpty(Email) &&
            string.IsNullOrWhiteSpace(Email))
        {
            yield return new ValidationResult(
                "Email cannot be blank.",
                [nameof(Email)]);
        }

        if (!AllowedRoles.Contains(Role))
        {
            yield return new ValidationResult(
                $"Role must be one of: " +
                $"{string.Join(", ", AllowedRoles)}.",
                [nameof(Role)]);
        }

        var allowedStatuses = new[]
        {
            "Pending",
            "Approved",
            "Rejected"
        };

        if (!allowedStatuses.Contains(Status))
        {
            yield return new ValidationResult(
                "Status must be Pending, Approved, or Rejected.",
                [nameof(Status)]);
        }
    }
}
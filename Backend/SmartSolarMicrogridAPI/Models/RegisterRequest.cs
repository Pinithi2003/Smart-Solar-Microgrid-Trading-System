using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogridAPI.Models;

public class RegisterRequest
{
    [Required(ErrorMessage = "NIC is required.")]
    [StringLength(
        20,
        MinimumLength = 5,
        ErrorMessage = "NIC must be 5-20 characters.")]
    public string NIC { get; set; } = string.Empty;

    [Required(ErrorMessage = "Full name is required.")]
    [StringLength(
        120,
        MinimumLength = 2,
        ErrorMessage = "Full name must be 2-120 characters.")]
    public string FullName { get; set; } = string.Empty;

    [Required(ErrorMessage = "Email is required.")]
    [EmailAddress(ErrorMessage = "Email must be a valid email address.")]
    [StringLength(
        160,
        ErrorMessage = "Email must be 160 characters or fewer.")]
    public string Email { get; set; } = string.Empty;

    [Required(ErrorMessage = "Phone is required.")]
    [StringLength(
        20,
        ErrorMessage = "Phone must be 20 characters or fewer.")]
    public string Phone { get; set; } = string.Empty;

    [Required(ErrorMessage = "Password is required.")]
    [MinLength(
        6,
        ErrorMessage = "Password must be at least 6 characters.")]
    public string Password { get; set; } = string.Empty;
}
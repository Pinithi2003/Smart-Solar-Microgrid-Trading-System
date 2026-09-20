using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogridAPI.Models;

public class VerifyRequestDto
{
    [Required(ErrorMessage = "Verification code is required.")]
    public string VerificationCode { get; set; } = string.Empty;

    public string OperatorId { get; set; } = "OPR-FIELD-01";
}

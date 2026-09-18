using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogridAPI.Models;

public class StatusUpdateDto
{
    [Required(ErrorMessage = "Status is required.")]
    public string Status { get; set; } = string.Empty;
}

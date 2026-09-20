using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogridAPI.Models;
using SmartSolarMicrogridAPI.Services;

namespace SmartSolarMicrogridAPI.Controllers;

[ApiController]
[Route("api/field-operations")]
public class FieldOperationsController : ControllerBase
{
    private readonly FieldOperationService _service;

    public FieldOperationsController(FieldOperationService service) => _service = service;

    // GET /api/field-operations
    [HttpGet]
    public async Task<ActionResult<List<FieldOperation>>> GetAll([FromQuery] string? status)
    {
        var all = await _service.GetAllAsync();
        if (!string.IsNullOrWhiteSpace(status) && !string.Equals(status, "All", StringComparison.OrdinalIgnoreCase))
        {
            all = all.Where(o => string.Equals(o.Status, status, StringComparison.OrdinalIgnoreCase)).ToList();
        }
        return Ok(all);
    }

    // GET /api/field-operations/pending
    [HttpGet("pending")]
    public async Task<ActionResult<List<FieldOperation>>> GetPending()
    {
        var pending = await _service.GetPendingAsync();
        return Ok(pending);
    }

    // GET /api/field-operations/completed
    [HttpGet("completed")]
    public async Task<ActionResult<List<FieldOperation>>> GetCompleted()
    {
        var completed = await _service.GetCompletedAsync();
        return Ok(completed);
    }

    // GET /api/field-operations/RES001 or /api/field-operations/SG-RES001
    [HttpGet("{id}")]
    public async Task<ActionResult<FieldOperation>> GetById(string id)
    {
        var op = await _service.GetByIdOrCodeAsync(id);
        return op is null ? NotFound(new { message = $"No operation found for identifier '{id}'." }) : Ok(op);
    }

    // POST /api/field-operations/verify
    [HttpPost("verify")]
    public async Task<IActionResult> Verify([FromBody] VerifyRequestDto dto)
    {
        if (!ModelState.IsValid) return ValidationProblem(ModelState);

        var (success, statusCode, message, operation) = await _service.VerifyAsync(dto.VerificationCode, dto.OperatorId);

        var response = new
        {
            success,
            message,
            operation
        };

        return StatusCode(statusCode, response);
    }

    // PATCH /api/field-operations/{reservationId}/complete
    [HttpPatch("{reservationId}/complete")]
    public async Task<IActionResult> Complete(string reservationId, [FromBody] CompleteOperationDto? dto)
    {
        var notes = dto?.Notes;
        var operatorId = dto?.OperatorId;

        var (success, statusCode, message, operation) = await _service.CompleteAsync(reservationId, notes, operatorId);

        var response = new
        {
            success,
            message,
            operation
        };

        return StatusCode(statusCode, response);
    }

    // POST /api/field-operations/reset
    [HttpPost("reset")]
    public async Task<IActionResult> Reset()
    {
        await _service.ResetDataAsync();
        return Ok(new { message = "Field operations reset to default initial demo dataset." });
    }
}

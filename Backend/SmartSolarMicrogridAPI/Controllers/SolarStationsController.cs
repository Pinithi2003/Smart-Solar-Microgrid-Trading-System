using Microsoft.AspNetCore.Mvc;
using MongoDB.Driver;
using SmartSolarMicrogridAPI.Models;
using SmartSolarMicrogridAPI.Services;

namespace SmartSolarMicrogridAPI.Controllers;

[ApiController]
[Route("api/solarstations")]
public class SolarStationsController : ControllerBase
{
    private readonly SolarStationService _service;

    public SolarStationsController(SolarStationService service) => _service = service;

    // GET /api/solarstations
    [HttpGet]
    public async Task<ActionResult<List<SolarStationInfo>>> GetAll()
    {
        var stations = await _service.GetAllAsync();
        return Ok(stations);
    }

    // GET /api/solarstations/ST001
    [HttpGet("{stationId}")]
    public async Task<ActionResult<SolarStationInfo>> GetOne(string stationId)
    {
        var station = await _service.GetByStationIdAsync(stationId);
        return station is null ? NotFound(new { message = $"Station {stationId} not found." }) : Ok(station);
    }

    // POST /api/solarstations
    [HttpPost]
    public async Task<ActionResult<SolarStationInfo>> Create([FromBody] SolarStationInfo station)
    {
        if (!ModelState.IsValid) return ValidationProblem(ModelState);

        if (await _service.GetByStationIdAsync(station.StationId) is not null)
            return Conflict(new { message = $"Station {station.StationId} already exists." });

        try
        {
            await _service.CreateAsync(station);
        }
        catch (MongoWriteException ex) when (ex.WriteError.Category == ServerErrorCategory.DuplicateKey)
        {
            return Conflict(new { message = $"Station {station.StationId} already exists." });
        }

        var created = await _service.GetByStationIdAsync(station.StationId);
        return CreatedAtAction(nameof(GetOne), new { stationId = station.StationId }, created);
    }

    // PUT /api/solarstations/ST006
    [HttpPut("{stationId}")]
    public async Task<ActionResult<SolarStationInfo>> Update(string stationId, [FromBody] SolarStationInfo updated)
    {
        if (!ModelState.IsValid) return ValidationProblem(ModelState);

        if (!string.IsNullOrWhiteSpace(updated.StationId) && updated.StationId != stationId)
            return BadRequest(new { message = "StationId in body must match the route id." });

        if (await _service.GetByStationIdAsync(stationId) is null)
            return NotFound(new { message = $"Station {stationId} not found." });

        await _service.UpdateAsync(stationId, updated);
        return Ok(await _service.GetByStationIdAsync(stationId));
    }

    // DELETE /api/solarstations/ST006 -> soft delete (Status = Inactive)
    [HttpDelete("{stationId}")]
    public async Task<IActionResult> Deactivate(string stationId)
    {
        if (await _service.GetByStationIdAsync(stationId) is null)
            return NotFound(new { message = $"Station {stationId} not found." });

        await _service.SoftDeleteAsync(stationId);
        return Ok(new { message = $"Station {stationId} deactivated.", status = "Inactive" });
    }

    // PATCH /api/solarstations/ST006/status  { "status": "Maintenance" }
    [HttpPatch("{stationId}/status")]
    public async Task<ActionResult<SolarStationInfo>> UpdateStatus(string stationId, [FromBody] StatusUpdateDto dto)
    {
        if (!ModelState.IsValid) return ValidationProblem(ModelState);

        if (!SolarStationInfo.AllowedStatuses.Contains(dto.Status))
            return BadRequest(new { message = $"Status must be one of: {string.Join(", ", SolarStationInfo.AllowedStatuses)}." });

        if (await _service.GetByStationIdAsync(stationId) is null)
            return NotFound(new { message = $"Station {stationId} not found." });

        await _service.UpdateStatusAsync(stationId, dto.Status);
        return Ok(await _service.GetByStationIdAsync(stationId));
    }
}

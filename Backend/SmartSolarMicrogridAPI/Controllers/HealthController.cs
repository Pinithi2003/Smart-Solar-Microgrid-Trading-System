using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogridAPI.Services;

namespace SmartSolarMicrogridAPI.Controllers;

[ApiController]
[Route("api/health")]
public class HealthController : ControllerBase
{
    private readonly SolarStationService _service;

    public HealthController(SolarStationService service) => _service = service;

    // GET /api/health -> 200 Healthy / 503 Unhealthy (never throws)
    [HttpGet]
    public async Task<IActionResult> Get()
    {
        var (healthy, error) = await _service.CheckHealthAsync();
        var payload = new
        {
            status = healthy ? "Healthy" : "Unhealthy",
            database = _service.DatabaseName,
            collection = _service.CollectionName,
            error
        };
        return healthy ? Ok(payload) : StatusCode(503, payload);
    }
}

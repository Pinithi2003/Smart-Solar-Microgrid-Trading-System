namespace SmartSolarMicrogridAPI.Models;

public class MongoDBSettings
{
    public string ConnectionString { get; set; } = string.Empty;
    public string DatabaseName { get; set; } = "SmartSolarMicrogrid";
    public string CollectionName { get; set; } = "SolarStationInfo";
}

package com.smartsolar.stations.services

import android.content.Context
import com.smartsolar.stations.database.SmartSolarDbHelper
import com.smartsolar.stations.models.SolarStation

/**
 * Service managing solar microgrid nodes and stations.
 * Retrieves stations from local SQLite cache or remote ASP.NET Core API (/api/solarstations).
 */
object StationService {

    fun getAllStations(context: Context): List<SolarStation> {
        val db = SmartSolarDbHelper(context)
        val stations = db.getAllStations()
        return stations.ifEmpty {
            // Fallback default stations if database was cleared
            listOf(
                SolarStation("ST001", "Grid Node A (Colombo)", "Colombo Microgrid Station", 6.9271, 79.8612, 100.0, 65.0, "Active", "Grid Operator"),
                SolarStation("ST002", "Grid Node B (Kalutara)", "Kalutara Feeder Hub", 6.5854, 79.9607, 120.0, 48.0, "Active", "Grid Operator"),
                SolarStation("ST003", "Grid Node C (Galle)", "Galle Coastal Microgrid", 6.0535, 80.2210, 90.0, 32.0, "Maintenance", "Grid Operator"),
                SolarStation("ST004", "Grid Node D (Kandy)", "Central Highlands Solar Sub", 7.2906, 80.6337, 110.0, 74.0, "Active", "Grid Operator")
            )
        }
    }

    fun getStationById(context: Context, stationId: String): SolarStation? {
        val db = SmartSolarDbHelper(context)
        return db.getStationById(stationId) ?: getAllStations(context).find { it.stationId.equals(stationId, ignoreCase = true) }
    }
}

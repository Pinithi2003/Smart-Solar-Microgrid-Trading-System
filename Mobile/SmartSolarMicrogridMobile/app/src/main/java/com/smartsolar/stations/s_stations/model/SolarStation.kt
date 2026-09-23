package com.smartsolar.stations.s_stations.model

import com.google.gson.annotations.SerializedName

/**
 * Member 2 — mirrors Backend SolarStationInfo (Mongo SolarStationInfo collection).
 * Business key is stationId (ST001...). Field names match /api/solarstations JSON.
 */
data class SolarStation(
    @SerializedName("stationId") val stationId: String = "",
    @SerializedName("stationName") val stationName: String = "",
    @SerializedName("location") val location: String = "",
    @SerializedName("latitude") val latitude: Double = 0.0,
    @SerializedName("longitude") val longitude: Double = 0.0,
    @SerializedName("totalCapacity") val totalCapacity: Double = 0.0,
    @SerializedName("availableCapacity") val availableCapacity: Double = 0.0,
    @SerializedName("status") val status: String = "Active",
    @SerializedName("operator") val operator: String = "Grid Operator",
    @SerializedName("lastUpdated") val lastUpdated: String? = null
) {
    companion object {
        val ALLOWED_STATUSES = listOf("Active", "Inactive", "Maintenance")
    }
}

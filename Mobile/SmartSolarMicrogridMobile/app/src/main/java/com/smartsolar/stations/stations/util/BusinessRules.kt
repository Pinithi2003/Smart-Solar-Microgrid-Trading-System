package com.smartsolar.stations.stations.util

import com.smartsolar.stations.stations.model.SolarStation

/**
 * Member 2 business rules — mirrors Backend SolarStationInfo.Validate.
 * Booking-window / 12h-cancel / QR rules live with Members 3-4; only helpers here.
 */
object BusinessRules {

    fun validateStation(s: SolarStation): List<String> {
        val errors = mutableListOf<String>()
        if (!Regex("^ST\\d{3,}$").matches(s.stationId)) errors += "StationId must look like ST001."
        if (s.stationName.trim().length < 2) errors += "Station name must be 2-120 characters."
        if (s.location.trim().length < 2) errors += "Location must be 2-120 characters."
        if (s.latitude !in -90.0..90.0) errors += "Latitude must be between -90 and 90."
        if (s.longitude !in -180.0..180.0) errors += "Longitude must be between -180 and 180."
        if (s.latitude == 0.0 && s.longitude == 0.0) errors += "Coordinates cannot be 0, 0. Pick the station position on the map."
        if (s.totalCapacity <= 0) errors += "Total capacity must be greater than 0."
        if (s.availableCapacity < 0) errors += "Available capacity cannot be negative."
        if (s.availableCapacity > s.totalCapacity) errors += "Available capacity must not exceed total capacity."
        if (s.status !in SolarStation.ALLOWED_STATUSES) errors += "Status must be Active, Inactive or Maintenance."
        return errors
    }

    fun isBookable(status: String): Boolean = status == "Active"

    fun statusColor(status: String): String = when (status) {
        "Active" -> "#198754"
        "Maintenance" -> "#E0A800"
        else -> "#DC3545"
    }
}

package com.smartsolar.stations.core

import com.smartsolar.stations.auth.model.LocalUser
import com.smartsolar.stations.stations.model.SolarStation

/**
 * Demo/mock repository. UI works without backend; later swap with API calls.
 * Distances are demo-only (Member 4 computes real distances with Maps).
 */
object MockData {

    val prosumer = LocalUser(
        nic = "200012345678",
        name = "Nimal Perera",
        email = "nimal@example.com",
        role = "Prosumer"
    )

    val operator = LocalUser(
        nic = "199512345678",
        name = "Kamal Silva",
        email = "operator@example.com",
        role = "GridOperator"
    )

    const val DEMO_PASSWORD = "123456"

    /** Mirrors backend seed in SolarStationService.SeedIfEmpty (ST001..ST005). */
    val stations = listOf(
        SolarStation("ST001", "Colombo Solar Station", "Colombo", 6.9271, 79.8612, 100.0, 65.0, "Active", "Grid Operator"),
        SolarStation("ST002", "Kalutara Solar Station", "Kalutara", 6.5854, 79.9607, 120.0, 48.0, "Active", "Grid Operator"),
        SolarStation("ST003", "Galle Solar Station", "Galle", 6.0535, 80.2210, 90.0, 32.0, "Maintenance", "Grid Operator"),
        SolarStation("ST004", "Kandy Solar Station", "Kandy", 7.2906, 80.6337, 110.0, 74.0, "Active", "Grid Operator"),
        SolarStation("ST005", "Negombo Solar Station", "Negombo", 7.2083, 79.8358, 95.0, 16.0, "Inactive", "Grid Operator")
    )

    /** Demo grid-node aliases used in the project brief (A/B/C). */
    val demoDistances = mapOf("ST001" to "1.2 km", "ST002" to "2.8 km", "ST003" to "4.1 km")
}

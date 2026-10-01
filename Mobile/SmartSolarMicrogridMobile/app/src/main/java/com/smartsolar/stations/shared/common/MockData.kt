package com.smartsolar.stations.shared.common

import com.smartsolar.stations.auth.model.LocalUser
import com.smartsolar.stations.stations.model.SolarStation

/**
 * Demo/mock repository.
 * UI works without backend; later swap with API calls.
 * Distances are demo-only.
 */
object MockData {

    val prosumer = LocalUser(
        id = "prosumer-001",
        nic = "200012345678",
        fullName = "Nimal Perera",
        email = "nimal@example.com",
        phone = "0712345678",
        role = "Prosumer",
        status = "Active",
        isActive = true,
        token = "demo-token"
    )

    val operator = LocalUser(
        id = "operator-001",
        nic = "199512345678",
        fullName = "Kamal Silva",
        email = "operator@example.com",
        phone = "0771234567",
        role = "GridOperator",
        status = "Active",
        isActive = true,
        token = "demo-token"
    )

    const val DEMO_PASSWORD = "123456"

    val stations = listOf(

        SolarStation(
            "ST001",
            "Colombo Solar Station",
            "Colombo",
            6.9271,
            79.8612,
            100.0,
            65.0,
            "Active",
            "Grid Operator"
        ),

        SolarStation(
            "ST002",
            "Kalutara Solar Station",
            "Kalutara",
            6.5854,
            79.9607,
            120.0,
            48.0,
            "Active",
            "Grid Operator"
        ),

        SolarStation(
            "ST003",
            "Galle Solar Station",
            "Galle",
            6.0535,
            80.2210,
            90.0,
            32.0,
            "Maintenance",
            "Grid Operator"
        ),

        SolarStation(
            "ST004",
            "Kandy Solar Station",
            "Kandy",
            7.2906,
            80.6337,
            110.0,
            74.0,
            "Active",
            "Grid Operator"
        ),

        SolarStation(
            "ST005",
            "Negombo Solar Station",
            "Negombo",
            7.2083,
            79.8358,
            95.0,
            16.0,
            "Inactive",
            "Grid Operator"
        )
    )

    val demoDistances = mapOf(
        "ST001" to "1.2 km",
        "ST002" to "2.8 km",
        "ST003" to "4.1 km"
    )
}
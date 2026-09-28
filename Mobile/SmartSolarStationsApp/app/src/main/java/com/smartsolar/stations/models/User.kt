package com.smartsolar.stations.models

/**
 * User model representing Solar Prosumer or Grid Operator.
 * Stored locally in SQLite and synchronized with ASP.NET Core API.
 */
data class LocalUser(
    val id: Long = 0,
    val nic: String,
    val name: String,
    val email: String,
    val phone: String = "+94 77 123 4567",
    val role: String = "Prosumer", // "Prosumer" | "GridOperator"
    val password: String = "123456",
    val isActive: Boolean = true,
    val token: String = "demo-token"
)

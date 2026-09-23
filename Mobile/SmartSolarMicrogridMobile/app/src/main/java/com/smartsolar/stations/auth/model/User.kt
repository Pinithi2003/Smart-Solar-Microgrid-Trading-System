package com.smartsolar.stations.auth.model

/** Minimal local user — session only. Full user management belongs to Member 1. */
data class LocalUser(
    val nic: String,
    val name: String,
    val email: String,
    val role: String, // "Prosumer" | "GridOperator"
    val token: String = "demo-token"
)

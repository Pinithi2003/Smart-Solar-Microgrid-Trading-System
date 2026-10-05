package com.smartsolar.stations.auth.model

/**
 * Local authenticated user used for Android session and SQLite persistence.
 *
 * The central Web API remains responsible for authentication,
 * authorization and user management.
 */
data class LocalUser(
    val id: String,
    val nic: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val role: String, 
    val status: String,
    val isActive: Boolean,
    val token: String
)
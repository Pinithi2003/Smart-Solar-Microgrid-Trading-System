package com.smartsolar.stations.auth.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val token: String,
    val user: UserResponse
)

data class UserResponse(
    val id: String,
    val nic: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val role: String,
    val status: String,
    val isActive: Boolean
)
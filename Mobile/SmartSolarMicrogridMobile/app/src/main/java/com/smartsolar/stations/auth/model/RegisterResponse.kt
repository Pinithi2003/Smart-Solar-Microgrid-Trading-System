package com.smartsolar.stations.auth.model

data class RegisterResponse(
    val message: String,
    val user: UserResponse
)
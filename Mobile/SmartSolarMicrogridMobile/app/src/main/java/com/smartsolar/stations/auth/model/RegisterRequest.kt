package com.smartsolar.stations.auth.model

data class RegisterRequest(
    val nic: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val password: String
)
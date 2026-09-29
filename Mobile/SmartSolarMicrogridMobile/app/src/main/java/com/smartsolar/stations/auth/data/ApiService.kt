package com.smartsolar.stations.auth.data

import com.smartsolar.stations.auth.model.LoginRequest
import com.smartsolar.stations.auth.model.LoginResponse
import com.smartsolar.stations.auth.model.RegisterRequest
import com.smartsolar.stations.auth.model.RegisterResponse

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT


data class UpdateProfileRequest(
    val fullName: String,
    val email: String,
    val phone: String
)

data class ProfileResponse(
    val id: String,
    val nic: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val role: String,
    val status: String,
    val isActive: Boolean,
    val createdAt: String? = null
)

data class UpdateProfileResponse(
    val message: String,
    val user: ProfileResponse
)

data class DeactivationResponse(
    val message: String,
    val id: String,
    val nic: String,
    val status: String,
    val isActive: Boolean
)

interface ApiService {

    // ================================
    // Authentication - Login
    // ================================

    @POST("api/Auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse


    // ================================
    // Authentication - Registration
    // ================================

    @POST("api/Auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): RegisterResponse


    // ================================
    // My Profile
    // ================================

    @GET("api/Users/me")
    suspend fun getMyProfile(
        @Header("Authorization") authorization: String
    ): ProfileResponse


    // ================================
    // Update My Profile
    // ================================

    @PUT("api/Users/me")
    suspend fun updateMyProfile(
        @Header("Authorization") authorization: String,
        @Body request: UpdateProfileRequest
    ): UpdateProfileResponse


    // ================================
    // Account Deactivation
    // ================================

    @POST("api/Users/me/deactivation-request")
    suspend fun requestDeactivation(
        @Header("Authorization") authorization: String
    ): DeactivationResponse
}
package com.smartsolar.stations.services.api

import com.smartsolar.stations.models.Booking
import com.smartsolar.stations.models.LocalUser
import com.smartsolar.stations.models.SolarStation
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API definitions matching the team's ASP.NET Core Web API architecture.
 * Ready for live HTTP communication when API backend is hosted.
 */
interface SmartSolarApi {

    // === Member 1: Authentication & User Management ===
    @POST("api/auth/login")
    suspend fun login(@Body body: Map<String, String>): Response<LocalUser>

    @POST("api/auth/register")
    suspend fun register(@Body user: LocalUser): Response<LocalUser>

    @GET("api/users/{id}")
    suspend fun getUserProfile(@Path("id") id: String): Response<LocalUser>

    // === Member 2: Infrastructure & Stations ===
    @GET("api/solarstations")
    suspend fun getStations(): Response<List<SolarStation>>

    @GET("api/solarstations/{stationId}")
    suspend fun getStationById(@Path("stationId") stationId: String): Response<SolarStation>

    // === Member 3: Reservations ===
    @GET("api/reservations")
    suspend fun getReservations(): Response<List<Booking>>

    @POST("api/reservations")
    suspend fun createReservation(@Body booking: Booking): Response<Booking>

    @PUT("api/reservations/{id}")
    suspend fun updateReservation(@Path("id") id: String, @Body booking: Booking): Response<Booking>

    @DELETE("api/reservations/{id}")
    suspend fun cancelReservation(@Path("id") id: String): Response<Map<String, String>>

    // === Member 4: Field Operations & QR ===
    @POST("api/qr/verify")
    suspend fun verifyQr(@Body body: Map<String, String>): Response<Map<String, Any>>

    @POST("api/qr/finalize")
    suspend fun finalizeQr(@Body body: Map<String, String>): Response<Map<String, Any>>
}

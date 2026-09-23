package com.smartsolar.stations.s_stations.data

import com.smartsolar.stations.s_stations.model.SolarStation
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit interface for Member 2 routes. Mirrors
 * Backend Controllers/SolarStationsController (route api/solarstations).
 * Other members add their own interfaces (AuthService, ReservationService,
 * QrService) — never put API calls directly in Activities.
 */
interface StationApi {
    @GET("api/solarstations")
    suspend fun getAll(): List<SolarStation>

    @GET("api/solarstations/{id}")
    suspend fun getOne(@Path("id") id: String): SolarStation

    @POST("api/solarstations")
    suspend fun create(@Body s: SolarStation): SolarStation

    @PUT("api/solarstations/{id}")
    suspend fun update(@Path("id") id: String, @Body s: SolarStation): SolarStation
}

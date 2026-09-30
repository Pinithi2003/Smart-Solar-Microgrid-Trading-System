package com.smartsolar.stations.reservations.data

import com.smartsolar.stations.reservations.model.BookingSlot
import com.smartsolar.stations.reservations.model.CreateReservationBody
import com.smartsolar.stations.reservations.model.EnergyReservation
import com.smartsolar.stations.reservations.model.ReservationEnvelope
import com.smartsolar.stations.reservations.model.UpdateReservationBody
import com.smartsolar.stations.s_stations.model.SolarStation
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** REST calls for Member 3. Business rules stay on the server. */
interface ReservationApi {
    @GET("api/reservations")
    suspend fun all(): List<EnergyReservation>

    @GET("api/reservations/my")
    suspend fun mine(@Query("userId") userId: String): List<EnergyReservation>

    @GET("api/reservations/{id}")
    suspend fun one(@Path("id") id: String): EnergyReservation

    @POST("api/reservations")
    suspend fun create(@Body body: CreateReservationBody): ReservationEnvelope

    @PUT("api/reservations/{id}")
    suspend fun update(@Path("id") id: String, @Body body: UpdateReservationBody): ReservationEnvelope

    @PATCH("api/reservations/{id}/cancel")
    suspend fun cancel(@Path("id") id: String): ReservationEnvelope

    @GET("api/booking-slots")
    suspend fun slots(@Query("stationId") stationId: String): List<BookingSlot>

    @GET("api/solarstations")
    suspend fun stations(): List<SolarStation>
}

package com.smartsolar.stations.reservations.model

import com.google.gson.annotations.SerializedName

/** One EnergyReservation document, matching the Member 3 API JSON. */
data class EnergyReservation(
    @SerializedName("reservationId") val reservationId: String = "",
    @SerializedName("userId") val userId: String = "",
    @SerializedName("stationId") val stationId: String = "",
    @SerializedName("slotId") val slotId: String = "",
    @SerializedName("date") val date: String = "",
    @SerializedName("startTime") val startTime: String = "",
    @SerializedName("endTime") val endTime: String = "",
    @SerializedName("energyAmount") val energyAmount: Double = 0.0,
    @SerializedName("status") val status: String = "",
    @SerializedName("verificationCode") val verificationCode: String = ""
)

/** One EnergyBookingSlots document. */
data class BookingSlot(
    @SerializedName("slotId") val slotId: String = "",
    @SerializedName("stationId") val stationId: String = "",
    @SerializedName("date") val date: String = "",
    @SerializedName("startTime") val startTime: String = "",
    @SerializedName("endTime") val endTime: String = "",
    @SerializedName("energyCapacity") val energyCapacity: Double = 0.0,
    @SerializedName("availableEnergy") val availableEnergy: Double = 0.0,
    @SerializedName("status") val status: String = ""
)

/** Body for POST /api/reservations. */
data class CreateReservationBody(
    val userId: String,
    val stationId: String,
    val slotId: String,
    val energyAmount: Double
)

/** Body for PUT /api/reservations/{id}. */
data class UpdateReservationBody(val slotId: String)

/** Wrapped response used by create, update, and cancel. */
data class ReservationEnvelope(
    val message: String? = null,
    val reservation: EnergyReservation? = null
)

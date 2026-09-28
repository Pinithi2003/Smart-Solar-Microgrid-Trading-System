package com.smartsolar.stations.models

import java.io.Serializable

/**
 * Booking / Reservation model for Solar Microgrid energy trading.
 * Lifecycle: PENDING -> APPROVED -> VERIFIED -> COMPLETED (or CANCELLED).
 */
data class Booking(
    val reservationId: String,
    val stationId: String,
    val stationName: String = "",
    val prosumerNic: String = "",
    val prosumerName: String = "",
    val date: String = "", // e.g. "25 September 2026"
    val time: String = "", // e.g. "10:00 AM - 11:00 AM"
    val energyAmount: Double = 15.0, // kWh
    val status: String = "PENDING", // PENDING | APPROVED | CANCELLED | COMPLETED
    val transactionId: String? = null,
    val createdDate: String = "",
    val verifiedAt: String? = null,
    val completedAt: String? = null,
    val operatorId: String? = null,
    val notes: String? = null
) : Serializable

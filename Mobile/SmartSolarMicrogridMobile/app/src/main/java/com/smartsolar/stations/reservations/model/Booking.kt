package com.smartsolar.stations.reservations.model

/**
 * Placeholder for Member 3 (reservations). Member 2 only caches these locally
 * for the SQLite demo; never implements booking logic here.
 */
data class Booking(
    val reservationId: String,
    val stationId: String,
    val date: String,
    val time: String,
    val status: String // PENDING | APPROVED | CANCELLED | COMPLETED
)

package com.smartsolar.stations.utils

import com.smartsolar.stations.models.Booking
import com.smartsolar.stations.models.SolarStation

/**
 * Enterprise Business Rules for Smart Solar Microgrid Trading System.
 * Contains Member 2 (Stations) and Member 4 (QR Verification & Field Operations) validation rules.
 */
object BusinessRules {

    /**
     * Member 2: Station Bookability Rule.
     * Stations can only be booked if they are in Active or Online status.
     */
    fun isBookable(status: String): Boolean =
        status.equals("Active", ignoreCase = true) || status.equals("Online", ignoreCase = true)

    /**
     * Member 2: Station Data Validation Rule.
     */
    fun validateStation(s: SolarStation): List<String> {
        val errors = mutableListOf<String>()
        if (!Regex("^ST\\d{3,}$").matches(s.stationId)) errors += "StationId must look like ST001."
        if (s.stationName.trim().length < 2) errors += "Station name must be 2-120 characters."
        if (s.location.trim().length < 2) errors += "Location must be 2-120 characters."
        if (s.totalCapacity <= 0) errors += "Total capacity must be greater than 0."
        if (s.availableCapacity < 0) errors += "Available capacity cannot be negative."
        return errors
    }

    /**
     * Member 4: QR Code Generation Rule.
     * QR generation is strictly unlocked only when the reservation status is APPROVED.
     */
    fun canGenerateQr(status: String): Pair<Boolean, String?> {
        return if (status.equals("APPROVED", ignoreCase = true)) {
            true to null
        } else {
            false to "Transaction QR is only available once your reservation is APPROVED."
        }
    }

    /**
     * Member 4: QR Code Verification Rule (Grid Operator Field Operations).
     * The transaction must:
     * 1. Exist
     * 2. Be approved
     * 3. Not be cancelled
     * 4. Not be completed
     * 5. Match the reservation transaction ID
     */
    fun verifyTransaction(booking: Booking?, scannedCode: String): Triple<Boolean, String, String?> {
        if (booking == null) {
            return Triple(false, "INVALID", "No reservation found matching code '$scannedCode'.")
        }
        if (booking.status.equals("COMPLETED", ignoreCase = true)) {
            return Triple(false, "ALREADY_COMPLETED", "This reservation has already been completed.")
        }
        if (booking.status.equals("CANCELLED", ignoreCase = true)) {
            return Triple(false, "CANCELLED", "This reservation has been cancelled.")
        }
        if (booking.status.equals("PENDING", ignoreCase = true)) {
            return Triple(false, "PENDING", "Reservation is pending approval and cannot be verified yet.")
        }
        if (!booking.status.equals("APPROVED", ignoreCase = true)) {
            return Triple(false, "UNAPPROVED", "Reservation status must be APPROVED to transfer energy.")
        }

        return Triple(true, "VERIFIED", null)
    }
}

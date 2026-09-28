package com.smartsolar.stations.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.smartsolar.stations.database.SmartSolarDbHelper
import com.smartsolar.stations.models.Booking
import com.smartsolar.stations.utils.BusinessRules

/**
 * Service managing QR code generation, verification, and finalization of energy transfers.
 * Directly maps to Member 4's Field Operations & QR domain (POST /api/qr/verify, /api/qr/finalize).
 */
object QrService {

    /**
     * Generates a high-resolution QR Bitmap using ZXing.
     * Encodes transaction identifier (e.g. TXN-2026-00001).
     */
    fun generateQrBitmap(content: String, size: Int = 600): Bitmap {
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    /**
     * Verifies scanned transaction code against the local database.
     * Checks all verification rules: existence, APPROVED status, non-cancellation, non-completion.
     */
    fun verifyScannedTransaction(context: Context, scannedCode: String): Triple<Boolean, Booking?, String?> {
        val trimmed = scannedCode.trim()
        val db = SmartSolarDbHelper(context)
        val booking = db.getBookingByTransactionId(trimmed)

        val (isValid, _, errorMsg) = BusinessRules.verifyTransaction(booking, trimmed)
        return Triple(isValid, booking, errorMsg)
    }

    /**
     * Finalizes energy transfer, transitioning status to COMPLETED and recording operator notes.
     */
    fun finalizeTransfer(
        context: Context,
        reservationId: String,
        operatorId: String = "OPR-FIELD-01",
        notes: String? = null
    ): Result<Booking> {
        val db = SmartSolarDbHelper(context)
        val booking = db.getBookingById(reservationId)
            ?: return Result.failure(Exception("Reservation not found."))

        val success = db.completeEnergyTransfer(reservationId, operatorId, notes)
        return if (success) {
            val updated = db.getBookingById(reservationId) ?: booking.copy(status = "COMPLETED")
            Result.success(updated)
        } else {
            Result.failure(Exception("Failed to finalize energy transfer."))
        }
    }

    /**
     * Retrieves all completed transactions for the Grid Operator ledger.
     */
    fun getCompletedTransactions(context: Context): List<Booking> {
        val db = SmartSolarDbHelper(context)
        return db.getAllBookings().filter { it.status.equals("COMPLETED", ignoreCase = true) }
    }

    /**
     * Calculates operational metrics for the Grid Operator dashboard.
     */
    fun getOperatorMetrics(context: Context): com.smartsolar.stations.models.OperatorMetrics {
        val db = SmartSolarDbHelper(context)
        val all = db.getAllBookings()
        val pending = all.count { it.status.equals("APPROVED", ignoreCase = true) }
        val completed = all.count { it.status.equals("COMPLETED", ignoreCase = true) }
        return com.smartsolar.stations.models.OperatorMetrics(
            todayTransactions = all.size,
            pendingVerification = pending,
            completedTransfers = completed
        )
    }
}

package com.smartsolar.stations.fieldops.member4.qrscanner

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.smartsolar.stations.R
import com.smartsolar.stations.models.Booking
import com.smartsolar.stations.services.QrService
import com.smartsolar.stations.infrastructure.MemberNav

/**
 * Member 4 - Official P2P Transaction QR Code screen.
 *
 * QR generation is allowed only for APPROVED reservations.
 */
class TransactionQrActivity : AppCompatActivity() {

    private lateinit var qrCard: MaterialCardView
    private lateinit var qrCodeImageView: ImageView
    private lateinit var qrTxnIdText: TextView
    private lateinit var qrNodeNameText: TextView
    private lateinit var qrDateTimeText: TextView
    private lateinit var qrEnergyAmountText: TextView
    private lateinit var qrNotApprovedLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_transaction_qr)
        MemberNav.bindBottomNav(this, MemberNav.QR)

        // ---------------------------------------------------------
        // Header
        // ---------------------------------------------------------

        findViewById<TextView>(
            R.id.headerTitle
        ).text = "Transaction QR"

        findViewById<ImageView>(
            R.id.btnBack
        ).setOnClickListener {
            finish()
        }

        // ---------------------------------------------------------
        // QR views
        // ---------------------------------------------------------

        qrCard =
            findViewById(R.id.qrCard)

        qrCodeImageView =
            findViewById(R.id.qrCodeImageView)

        qrTxnIdText =
            findViewById(R.id.qrTxnIdText)

        qrNodeNameText =
            findViewById(R.id.qrNodeNameText)

        qrDateTimeText =
            findViewById(R.id.qrDateTimeText)

        qrEnergyAmountText =
            findViewById(R.id.qrEnergyAmountText)

        qrNotApprovedLayout =
            findViewById(R.id.qrNotApprovedLayout)

        // ---------------------------------------------------------
        // Back to bookings
        // ---------------------------------------------------------

        findViewById<Button>(
            R.id.btnViewBookingsFromQr
        ).setOnClickListener {
            finish()
        }

        // ---------------------------------------------------------
        // Load QR
        // ---------------------------------------------------------

        loadQr()
    }

    override fun onResume() {
        super.onResume()

        loadQr()
    }

    // -------------------------------------------------------------
    // Load approved transaction
    // -------------------------------------------------------------

    private fun loadQr() {

        val passedReservationId =
            intent.getStringExtra(
                "EXTRA_RESERVATION_ID"
            )

        /*
         * Use the existing QrService data source.
         * This avoids depending on a database package that is not
         * currently available in this module.
         */

        val allBookings: List<Booking> =
            QrService.getCompletedTransactions(this)

        val targetBooking: Booking? =
            if (!passedReservationId.isNullOrBlank()) {

                allBookings.find { booking ->
                    booking.reservationId ==
                            passedReservationId
                }

            } else {

                allBookings.find { booking ->
                    booking.status.equals(
                        "APPROVED",
                        ignoreCase = true
                    )
                } ?: allBookings.firstOrNull()
            }

        // ---------------------------------------------------------
        // No booking / not approved
        // ---------------------------------------------------------

        if (
            targetBooking == null ||
            !targetBooking.status.equals(
                "APPROVED",
                ignoreCase = true
            )
        ) {

            qrCard.visibility =
                View.GONE

            qrNotApprovedLayout.visibility =
                View.VISIBLE

            return
        }

        // ---------------------------------------------------------
        // Approved booking
        // ---------------------------------------------------------

        qrCard.visibility =
            View.VISIBLE

        qrNotApprovedLayout.visibility =
            View.GONE

        val transactionId =
            targetBooking.transactionId
                ?: "TXN-2026-00001"

        qrTxnIdText.text =
            transactionId

        qrNodeNameText.text =
            if (targetBooking.stationName.isNotBlank()) {

                targetBooking.stationName

            } else {

                "Grid Node (${targetBooking.stationId})"
            }

        qrDateTimeText.text =
            "${targetBooking.date} • ${targetBooking.time}"

        qrEnergyAmountText.text =
            "⚡ Energy Amount: ${targetBooking.energyAmount} kWh"

        // ---------------------------------------------------------
        // Generate QR
        // ---------------------------------------------------------

        val qrBitmap =
            QrService.generateQrBitmap(
                transactionId,
                600
            )

        qrCodeImageView.setImageBitmap(
            qrBitmap
        )
    }
}
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
import com.smartsolar.stations.database.SmartSolarDbHelper
import com.smartsolar.stations.models.Booking
import com.smartsolar.stations.services.QrService

/**
 * Member 4: Screen generating and displaying the official P2P Transaction QR Code.
 * Enforces rule: QR generation is strictly permitted only when the reservation is APPROVED.
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

        findViewById<TextView>(R.id.headerTitle).text = getString(R.string.action_transaction_qr)
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        qrCard = findViewById(R.id.qrCard)
        qrCodeImageView = findViewById(R.id.qrCodeImageView)
        qrTxnIdText = findViewById(R.id.qrTxnIdText)
        qrNodeNameText = findViewById(R.id.qrNodeNameText)
        qrDateTimeText = findViewById(R.id.qrDateTimeText)
        qrEnergyAmountText = findViewById(R.id.qrEnergyAmountText)
        qrNotApprovedLayout = findViewById(R.id.qrNotApprovedLayout)

        findViewById<Button>(R.id.btnViewBookingsFromQr).setOnClickListener {
            finish()
        }

        loadQr()
    }

    override fun onResume() {
        super.onResume()
        loadQr()
    }

    private fun loadQr() {
        val passedResId = intent.getStringExtra("EXTRA_RESERVATION_ID")
        val dbHelper = SmartSolarDbHelper(this)
        val allBookings = dbHelper.getAllBookings()

        val targetBooking: Booking? = if (!passedResId.isNullOrBlank()) {
            allBookings.find { it.reservationId == passedResId }
        } else {
            // Find the active approved booking for demo
            allBookings.find { it.status.equals("APPROVED", ignoreCase = true) }
                ?: allBookings.firstOrNull()
        }

        if (targetBooking == null || !targetBooking.status.equals("APPROVED", ignoreCase = true)) {
            qrCard.visibility = View.GONE
            qrNotApprovedLayout.visibility = View.VISIBLE
            return
        }

        qrCard.visibility = View.VISIBLE
        qrNotApprovedLayout.visibility = View.GONE

        val txnId = targetBooking.transactionId ?: "TXN-2026-00001"
        qrTxnIdText.text = txnId
        qrNodeNameText.text = targetBooking.stationName.ifBlank { "Grid Node (${targetBooking.stationId})" }
        qrDateTimeText.text = "${targetBooking.date} • ${targetBooking.time}"
        qrEnergyAmountText.text = "⚡ Energy Amount: ${targetBooking.energyAmount} kWh"

        // Generate ZXing QR Bitmap
        val qrBitmap = QrService.generateQrBitmap(txnId, 600)
        qrCodeImageView.setImageBitmap(qrBitmap)
    }
}

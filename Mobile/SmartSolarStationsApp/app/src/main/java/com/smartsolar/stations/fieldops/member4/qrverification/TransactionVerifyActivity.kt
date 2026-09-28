package com.smartsolar.stations.fieldops.member4.qrverification

import android.content.Intent
import com.smartsolar.stations.fieldops.member4.operations.CompletedTransactionsActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.smartsolar.stations.R
import com.smartsolar.stations.models.Booking
import com.smartsolar.stations.services.QrService
import com.smartsolar.stations.utils.StatusBadgeHelper

/**
 * Transaction Verification & Transfer Finalization Activity (Member 4).
 * Validates scanned QR transaction rules and marks energy transfer COMPLETED.
 */
class TransactionVerifyActivity : AppCompatActivity() {

    private lateinit var verifyTxnId: TextView
    private lateinit var verifyProsumerName: TextView
    private lateinit var verifyStationName: TextView
    private lateinit var verifyDateTime: TextView
    private lateinit var verifyEnergyAmount: TextView
    private lateinit var verifyStatusBadge: TextView
    private lateinit var verificationSuccessBanner: LinearLayout
    private lateinit var verificationFailureBanner: LinearLayout
    private lateinit var failureReasonText: TextView
    private lateinit var finalizeSection: LinearLayout
    private lateinit var inputOperatorNotes: TextInputEditText
    private lateinit var btnFinalizeTransfer: Button

    private var activeBooking: Booking? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_verify)

        findViewById<TextView>(R.id.headerTitle).text = getString(R.string.title_verify_transaction)
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        verifyTxnId = findViewById(R.id.verifyTxnId)
        verifyProsumerName = findViewById(R.id.verifyProsumerName)
        verifyStationName = findViewById(R.id.verifyStationName)
        verifyDateTime = findViewById(R.id.verifyDateTime)
        verifyEnergyAmount = findViewById(R.id.verifyEnergyAmount)
        verifyStatusBadge = findViewById(R.id.verifyStatusBadge)
        verificationSuccessBanner = findViewById(R.id.verificationSuccessBanner)
        verificationFailureBanner = findViewById(R.id.verificationFailureBanner)
        failureReasonText = findViewById(R.id.failureReasonText)
        finalizeSection = findViewById(R.id.finalizeSection)
        inputOperatorNotes = findViewById(R.id.inputOperatorNotes)
        btnFinalizeTransfer = findViewById(R.id.btnFinalizeTransfer)

        val scannedCode = intent.getStringExtra("EXTRA_SCANNED_CODE") ?: "TXN-2026-00001"
        performVerification(scannedCode)

        btnFinalizeTransfer.setOnClickListener {
            val booking = activeBooking ?: return@setOnClickListener
            val notes = inputOperatorNotes.text.toString().trim()

            QrService.finalizeTransfer(this, booking.reservationId, "OPR-FIELD-01", notes)
                .onSuccess { completedBooking ->
                    AlertDialog.Builder(this)
                        .setTitle("✓ Energy Transfer Completed 🎉")
                        .setMessage(
                            "Transaction ID: ${completedBooking.transactionId ?: completedBooking.reservationId}\n" +
                            "Prosumer: ${completedBooking.prosumerName}\n" +
                            "Energy Injected: ${completedBooking.energyAmount} kWh\n" +
                            "Status: COMPLETED\n\n" +
                            "The reservation has been finalized and recorded in the microgrid transaction ledger."
                        )
                        .setPositiveButton("View Ledger") { _, _ ->
                            startActivity(Intent(this, CompletedTransactionsActivity::class.java))
                            finish()
                        }
                        .setNegativeButton("Done") { _, _ ->
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                }
                .onFailure { ex ->
                    Toast.makeText(this, ex.message ?: "Failed to finalize", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun performVerification(code: String) {
        val (isValid, booking, errorMsg) = QrService.verifyScannedTransaction(this, code)
        activeBooking = booking

        verifyTxnId.text = code

        if (booking != null) {
            verifyProsumerName.text = "${booking.prosumerName} (${booking.prosumerNic})"
            verifyStationName.text = booking.stationName
            verifyDateTime.text = "${booking.date} • ${booking.time}"
            verifyEnergyAmount.text = "⚡ ${booking.energyAmount} kWh"
            StatusBadgeHelper.applyBadge(verifyStatusBadge, booking.status)
        } else {
            verifyProsumerName.text = "Unknown Prosumer"
            verifyStationName.text = "Unknown Station"
            verifyDateTime.text = "N/A"
            verifyEnergyAmount.text = "0 kWh"
            verifyStatusBadge.text = "NOT FOUND"
        }

        if (isValid) {
            verificationSuccessBanner.visibility = View.VISIBLE
            verificationFailureBanner.visibility = View.GONE
            finalizeSection.visibility = View.VISIBLE
        } else {
            verificationSuccessBanner.visibility = View.GONE
            verificationFailureBanner.visibility = View.VISIBLE
            failureReasonText.text = errorMsg ?: "Verification rejected by microgrid trading policy."
            finalizeSection.visibility = View.GONE
        }
    }
}

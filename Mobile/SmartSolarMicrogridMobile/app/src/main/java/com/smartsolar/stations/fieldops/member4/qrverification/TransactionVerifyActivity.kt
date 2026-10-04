
package com.smartsolar.stations.fieldops.member4.qrverification

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.smartsolar.stations.R
import com.smartsolar.stations.fieldops.member4.operations.CompletedTransactionsActivity
import com.smartsolar.stations.models.Booking
import com.smartsolar.stations.services.QrService
import com.smartsolar.stations.stations.ui.MemberNav
import com.smartsolar.stations.utils.StatusBadgeHelper

/**
 * Member 4 - Transaction Verification & Transfer Finalization.
 *
 * Flow:
 * Scan QR
 *     ↓
 * Verify Transaction
 *     ↓
 * If valid → Finalize Transfer
 *     ↓
 * COMPLETED
 *     ↓
 * Completed Transactions
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

        // ---------------------------------------------------------
        // Header
        // ---------------------------------------------------------

        MemberNav.bindHeader(this, "Verify Transaction")

        // ---------------------------------------------------------
        // Find Views
        // ---------------------------------------------------------

        verifyTxnId =
            findViewById(R.id.verifyTxnId)

        verifyProsumerName =
            findViewById(R.id.verifyProsumerName)

        verifyStationName =
            findViewById(R.id.verifyStationName)

        verifyDateTime =
            findViewById(R.id.verifyDateTime)

        verifyEnergyAmount =
            findViewById(R.id.verifyEnergyAmount)

        verifyStatusBadge =
            findViewById(R.id.verifyStatusBadge)

        verificationSuccessBanner =
            findViewById(R.id.verificationSuccessBanner)

        verificationFailureBanner =
            findViewById(R.id.verificationFailureBanner)

        failureReasonText =
            findViewById(R.id.failureReasonText)

        finalizeSection =
            findViewById(R.id.finalizeSection)

        inputOperatorNotes =
            findViewById(R.id.inputOperatorNotes)

        btnFinalizeTransfer =
            findViewById(R.id.btnFinalizeTransfer)

        // ---------------------------------------------------------
        // Get scanned QR code
        // ---------------------------------------------------------

        val scannedCode =
            intent.getStringExtra("EXTRA_SCANNED_CODE")
                ?.trim()
                .orEmpty()

        if (scannedCode.isBlank()) {

            Toast.makeText(
                this,
                "No QR transaction code was received.",
                Toast.LENGTH_LONG
            ).show()

            showVerificationFailure(
                "No transaction code was provided."
            )

            return
        }

        // ---------------------------------------------------------
        // Verify transaction
        // ---------------------------------------------------------

        performVerification(scannedCode)

        // ---------------------------------------------------------
        // Finalize transfer
        // ---------------------------------------------------------

        btnFinalizeTransfer.setOnClickListener {

            val booking =
                activeBooking

            if (booking == null) {

                Toast.makeText(
                    this,
                    "No valid transaction is available.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val notes =
                inputOperatorNotes.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            finalizeTransfer(
                booking,
                notes
            )
        }
    }

    /**
     * Verify the scanned transaction QR code.
     */
    private fun performVerification(
        code: String
    ) {

        val result =
            QrService.verifyScannedTransaction(
                this,
                code
            )

        val isValid =
            result.first

        val booking =
            result.second

        val errorMsg =
            result.third

        activeBooking =
            booking

        // ---------------------------------------------------------
        // Transaction ID
        // ---------------------------------------------------------

        verifyTxnId.text =
            if (booking?.transactionId.isNullOrBlank()) {
                code
            } else {
                booking?.transactionId
            }

        // ---------------------------------------------------------
        // Booking information
        // ---------------------------------------------------------

        if (booking != null) {

            verifyProsumerName.text =
                if (booking.prosumerNic.isNotBlank()) {
                    "${booking.prosumerName} (${booking.prosumerNic})"
                } else {
                    booking.prosumerName
                }

            verifyStationName.text =
                if (booking.stationName.isNotBlank()) {
                    booking.stationName
                } else {
                    "Unknown Station"
                }

            verifyDateTime.text =
                "${booking.date} • ${booking.time}"

            verifyEnergyAmount.text =
                "⚡ ${booking.energyAmount} kWh"

            StatusBadgeHelper.applyBadge(
                verifyStatusBadge,
                booking.status
            )

        } else {

            verifyProsumerName.text =
                "Unknown Prosumer"

            verifyStationName.text =
                "Unknown Station"

            verifyDateTime.text =
                "N/A"

            verifyEnergyAmount.text =
                "0 kWh"

            verifyStatusBadge.text =
                "NOT FOUND"
        }

        // ---------------------------------------------------------
        // Verification result
        // ---------------------------------------------------------

        if (isValid && booking != null) {

            showVerificationSuccess()

        } else {

            showVerificationFailure(
                errorMsg
                    ?: "Verification rejected by microgrid trading policy."
            )
        }
    }

    /**
     * Show successful verification state.
     */
    private fun showVerificationSuccess() {

        verificationSuccessBanner.visibility =
            View.VISIBLE

        verificationFailureBanner.visibility =
            View.GONE

        finalizeSection.visibility =
            View.VISIBLE
    }

    /**
     * Show failed verification state.
     */
    private fun showVerificationFailure(
        reason: String
    ) {

        verificationSuccessBanner.visibility =
            View.GONE

        verificationFailureBanner.visibility =
            View.VISIBLE

        failureReasonText.text =
            reason

        finalizeSection.visibility =
            View.GONE
    }

    /**
     * Finalize the approved transaction.
     */
    private fun finalizeTransfer(
        booking: Booking,
        notes: String
    ) {

        btnFinalizeTransfer.isEnabled =
            false

        QrService.finalizeTransfer(
            this,
            booking.reservationId,
            "OPR-FIELD-01",
            notes
        )
            .onSuccess { completedBooking ->

                showCompletedDialog(
                    completedBooking
                )
            }
            .onFailure { exception ->

                btnFinalizeTransfer.isEnabled =
                    true

                Toast.makeText(
                    this,
                    exception.message
                        ?: "Failed to finalize energy transfer.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    /**
     * Show completion confirmation.
     */
    private fun showCompletedDialog(
        completedBooking: Booking
    ) {

        AlertDialog.Builder(this)
            .setTitle(
                "Energy Transfer Completed"
            )
            .setMessage(
                "Transaction ID: ${
                    completedBooking.transactionId
                        ?: completedBooking.reservationId
                }\n\n" +
                        "Prosumer: ${
                            completedBooking.prosumerName
                        }\n\n" +
                        "Energy Injected: ${
                            completedBooking.energyAmount
                        } kWh\n\n" +
                        "Status: COMPLETED\n\n" +
                        "The reservation has been finalized and recorded in the microgrid transaction ledger."
            )
            .setPositiveButton(
                "View Ledger"
            ) { _, _ ->

                val intent =
                    Intent(
                        this,
                        CompletedTransactionsActivity::class.java
                    )

                startActivity(intent)

                finish()
            }
            .setNegativeButton(
                "Done"
            ) { _, _ ->

                finish()
            }
            .setCancelable(false)
            .show()
    }
}


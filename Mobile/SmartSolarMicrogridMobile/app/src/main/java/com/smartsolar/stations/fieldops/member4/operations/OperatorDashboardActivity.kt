package com.smartsolar.stations.fieldops.member4.operations

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.smartsolar.stations.R
import com.smartsolar.stations.auth.data.SessionManager
import com.smartsolar.stations.auth.ui.LoginActivity
import com.smartsolar.stations.fieldops.member4.qrscanner.ScanQrActivity
import com.smartsolar.stations.services.QrService
import com.smartsolar.stations.stations.ui.MemberNav
import com.smartsolar.stations.ui.common.NavigationHelper

/**
 * Grid Operator Dashboard
 *
 * Member 4 - Field Operations & QR Integration
 *
 * Provides access to:
 * - QR scanning
 * - Transaction verification
 * - Completed transactions
 * - Operator statistics
 * - Logout
 */
class OperatorDashboardActivity : AppCompatActivity() {

    private lateinit var opStatToday: TextView
    private lateinit var opStatPendingVerify: TextView
    private lateinit var opStatCompleted: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_operator_dashboard)
        MemberNav.bindHeader(this, "Field Operations")

        // ---------------------------------------------------------
        // Statistics
        // ---------------------------------------------------------

        opStatToday =
            findViewById(R.id.opStatToday)

        opStatPendingVerify =
            findViewById(R.id.opStatPendingVerify)

        opStatCompleted =
            findViewById(R.id.opStatCompleted)

        // ---------------------------------------------------------
        // Scan QR
        // ---------------------------------------------------------

        findViewById<MaterialCardView>(
            R.id.opActionScanQr
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    ScanQrActivity::class.java
                )
            )
        }

        // ---------------------------------------------------------
        // Verify Transaction
        // ---------------------------------------------------------

        findViewById<MaterialCardView>(
            R.id.opActionVerify
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    ScanQrActivity::class.java
                )
            )
        }

        // ---------------------------------------------------------
        // Completed Transactions
        // ---------------------------------------------------------

        findViewById<MaterialCardView>(
            R.id.opActionCompleted
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    CompletedTransactionsActivity::class.java
                )
            )
        }

        // ---------------------------------------------------------
        // Logout
        // ---------------------------------------------------------

        findViewById<Button>(
            R.id.btnOperatorLogout
        ).setOnClickListener {

            logout()
        }

        // ---------------------------------------------------------
        // Bottom Navigation
        // ---------------------------------------------------------

        NavigationHelper.setupOperatorBottomNav(
            this,
            NavigationHelper.OperatorTab.HOME
        )
    }

    // -------------------------------------------------------------
    // Refresh dashboard statistics
    // -------------------------------------------------------------

    override fun onResume() {
        super.onResume()

        refreshMetrics()
    }

    private fun refreshMetrics() {

        val metrics =
            QrService.getOperatorMetrics(this)

        opStatToday.text =
            metrics.todayTransactions.toString()

        opStatPendingVerify.text =
            metrics.pendingVerification.toString()

        opStatCompleted.text =
            metrics.completedTransfers.toString()
    }

    // -------------------------------------------------------------
    // Logout
    // -------------------------------------------------------------

    private fun logout() {

        // Clear the locally saved login session.
        SessionManager(this).clear()

        // Open Login screen and remove previous screens
        // from the back stack.
        val intent =
            Intent(
                this,
                LoginActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

        startActivity(intent)

        finish()
    }
}
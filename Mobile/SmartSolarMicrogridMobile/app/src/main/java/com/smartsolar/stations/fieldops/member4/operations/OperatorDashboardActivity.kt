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

/**
 * Grid Operator Dashboard
 *
 * Member 4 - Field Operations & QR Integration
 */
class OperatorDashboardActivity : AppCompatActivity() {

    private lateinit var opStatToday: TextView
    private lateinit var opStatPendingVerify: TextView
    private lateinit var opStatCompleted: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_operator_dashboard)

        // Shared header
        MemberNav.bindHeader(
            this,
            "Field Operations"
        )

        // Statistics
        opStatToday = findViewById(R.id.opStatToday)
        opStatPendingVerify = findViewById(R.id.opStatPendingVerify)
        opStatCompleted = findViewById(R.id.opStatCompleted)

        // Scan QR
        findViewById<MaterialCardView>(R.id.opActionScanQr)
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        ScanQrActivity::class.java
                    )
                )
            }

        // Verify Transaction
        findViewById<MaterialCardView>(R.id.opActionVerify)
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        ScanQrActivity::class.java
                    )
                )
            }

        // Completed Transactions
        findViewById<MaterialCardView>(R.id.opActionCompleted)
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        CompletedTransactionsActivity::class.java
                    )
                )
            }

        // Logout
        findViewById<Button>(R.id.btnOperatorLogout)
            .setOnClickListener {
                logout()
            }

        // Shared bottom navigation.
        // QR tab is selected on Operator Dashboard.
        MemberNav.bindBottomNav(
            this,
            MemberNav.QR
        )
    }

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

    private fun logout() {

        SessionManager(this).clear()

        val intent = Intent(
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
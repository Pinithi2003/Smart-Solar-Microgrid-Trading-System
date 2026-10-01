package com.smartsolar.stations.fieldops.member4.qrscanner

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.smartsolar.stations.R
import com.smartsolar.stations.infrastructure.MemberNav

/**
 * QR Code scanning activity for Grid Operators.
 *
 * Supports:
 * - Live QR scanning using device camera
 * - Manual transaction code entry
 * - Demo transaction selection
 * - Demo pass generation
 */
class ScanQrActivity : AppCompatActivity() {

    private lateinit var inputScannedCode: TextInputEditText

    private lateinit var barcodeLauncher:
            ActivityResultLauncher<ScanOptions>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_scan_qr)

        // ---------------------------------------------------------
        // Header
        // ---------------------------------------------------------

        findViewById<TextView>(
            R.id.headerTitle
        ).text = "Scan QR Code"

        findViewById<ImageView>(
            R.id.btnBack
        ).setOnClickListener {
            finish()
        }

        // ---------------------------------------------------------
        // Input
        // ---------------------------------------------------------

        inputScannedCode =
            findViewById(R.id.inputScannedCode)

        // ---------------------------------------------------------
        // ZXing QR Scanner
        // ---------------------------------------------------------

        barcodeLauncher =
            registerForActivityResult(
                ScanContract()
            ) { result ->

                if (result.contents != null) {

                    val scannedCode =
                        result.contents.trim()

                    inputScannedCode.setText(
                        scannedCode
                    )

                    proceedToVerification(
                        scannedCode
                    )

                } else {

                    Toast.makeText(
                        this,
                        "Scan cancelled",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        // ---------------------------------------------------------
        // Launch Camera
        // ---------------------------------------------------------

        findViewById<Button>(
            R.id.btnLaunchCameraScan
        ).setOnClickListener {

            val options =
                ScanOptions().apply {

                    setDesiredBarcodeFormats(
                        ScanOptions.QR_CODE
                    )

                    setPrompt(
                        "Align Prosumer QR Code within frame"
                    )

                    setCameraId(0)

                    setBeepEnabled(true)

                    setBarcodeImageEnabled(false)

                    setOrientationLocked(true)
                }

            barcodeLauncher.launch(options)
        }

        // ---------------------------------------------------------
        // Demo Transaction 1
        // ---------------------------------------------------------

        findViewById<Button>(
            R.id.btnPickSampleTxn1
        ).setOnClickListener {

            inputScannedCode.setText(
                "TXN-2026-00001"
            )
        }

        // ---------------------------------------------------------
        // Demo Transaction 2
        // ---------------------------------------------------------

        findViewById<Button>(
            R.id.btnPickSampleTxn2
        ).setOnClickListener {

            inputScannedCode.setText(
                "RES10002"
            )
        }

        // ---------------------------------------------------------
        // Demo New Pass
        // ---------------------------------------------------------

        findViewById<Button>(
            R.id.btnCreateNewPass
        ).setOnClickListener {

            createDemoPass()
        }

        // ---------------------------------------------------------
        // Simulate Scan
        // ---------------------------------------------------------

        findViewById<Button>(
            R.id.btnSimulateScan
        ).setOnClickListener {

            val code =
                inputScannedCode.text
                    .toString()
                    .trim()

            if (code.isBlank()) {

                Toast.makeText(
                    this,
                    "Please enter or pick a transaction code",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            proceedToVerification(code)
        }

        // ---------------------------------------------------------
        // Bottom Navigation
        // ---------------------------------------------------------

        MemberNav.bindBottomNav(this, MemberNav.QR)
    }

    // -------------------------------------------------------------
    // Create demo pass
    // -------------------------------------------------------------

    private fun createDemoPass() {

        /*
         * For now we only generate a demo transaction code.
         *
         * We do not insert directly into SQLite here because the
         * exact SmartSolarDbHelper package/API is different in the
         * current project.
         */

        val randomNum =
            (10..99).random()

        val newTxnId =
            "TXN-2026-${randomNum.toString().padStart(5, '0')}"

        inputScannedCode.setText(
            newTxnId
        )

        Toast.makeText(
            this,
            "Generated fresh pass: $newTxnId",
            Toast.LENGTH_SHORT
        ).show()
    }

    // -------------------------------------------------------------
    // Continue after QR/manual scan
    // -------------------------------------------------------------

    private fun proceedToVerification(
        code: String
    ) {

        /*
         * TransactionVerifyActivity is not currently available
         * in the project.
         *
         * The existing QR-related activity is
         * TransactionQrActivity.
         */

        val intent =
            Intent(
                this,
                TransactionQrActivity::class.java
            ).apply {

                putExtra(
                    "EXTRA_SCANNED_CODE",
                    code
                )

                putExtra(
                    "EXTRA_RESERVATION_ID",
                    code
                )
            }

        startActivity(intent)
    }
}
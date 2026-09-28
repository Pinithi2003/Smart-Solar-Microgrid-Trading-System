package com.smartsolar.stations.fieldops.member4.qrscanner

import android.content.Intent
import com.smartsolar.stations.fieldops.member4.qrverification.TransactionVerifyActivity
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
import com.smartsolar.stations.ui.common.NavigationHelper

/**
 * QR Code scanning activity for Grid Operators (Member 4).
 * Supports both live device camera scanning via ZXing and demo emulator simulation.
 */
class ScanQrActivity : AppCompatActivity() {

    private lateinit var inputScannedCode: TextInputEditText
    private lateinit var barcodeLauncher: ActivityResultLauncher<ScanOptions>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_qr)

        findViewById<TextView>(R.id.headerTitle).text = getString(R.string.title_scan_qr)
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        inputScannedCode = findViewById(R.id.inputScannedCode)

        // ZXing Live Camera Scanner Launcher
        barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
            if (result.contents != null) {
                inputScannedCode.setText(result.contents)
                proceedToVerification(result.contents)
            } else {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnLaunchCameraScan).setOnClickListener {
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Align Prosumer QR Code within frame")
                setCameraId(0)
                setBeepEnabled(true)
                setBarcodeImageEnabled(false)
                setOrientationLocked(true)
            }
            barcodeLauncher.launch(options)
        }

        // Demo Simulator Mode Buttons
        findViewById<Button>(R.id.btnPickSampleTxn1).setOnClickListener {
            inputScannedCode.setText("TXN-2026-00001")
        }

        findViewById<Button>(R.id.btnPickSampleTxn2).setOnClickListener {
            inputScannedCode.setText("RES10002")
        }

        findViewById<Button>(R.id.btnCreateNewPass).setOnClickListener {
            val db = com.smartsolar.stations.database.SmartSolarDbHelper(this)
            val randomNum = (10..99).random()
            val newTxnId = "TXN-2026-000$randomNum"
            val newResId = "RES100$randomNum"
            val newBooking = com.smartsolar.stations.models.Booking(
                reservationId = newResId,
                stationId = "ST001",
                stationName = "Grid Node A (Colombo)",
                prosumerNic = "200012345678",
                prosumerName = "Nimal Perera",
                date = "25 September 2026",
                time = "03:00 PM – 04:00 PM",
                energyAmount = (10..30).random().toDouble(),
                status = "APPROVED",
                transactionId = newTxnId,
                createdDate = "2026-09-21"
            )
            db.insertBooking(newBooking)
            inputScannedCode.setText(newTxnId)
            Toast.makeText(this, "Generated fresh pass: $newTxnId", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnSimulateScan).setOnClickListener {
            val code = inputScannedCode.text.toString().trim()
            if (code.isBlank()) {
                Toast.makeText(this, "Please enter or pick a transaction code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            proceedToVerification(code)
        }

        NavigationHelper.setupOperatorBottomNav(this, NavigationHelper.OperatorTab.SCAN)
    }

    private fun proceedToVerification(code: String) {
        val intent = Intent(this, TransactionVerifyActivity::class.java).apply {
            putExtra("EXTRA_SCANNED_CODE", code)
        }
        startActivity(intent)
    }
}

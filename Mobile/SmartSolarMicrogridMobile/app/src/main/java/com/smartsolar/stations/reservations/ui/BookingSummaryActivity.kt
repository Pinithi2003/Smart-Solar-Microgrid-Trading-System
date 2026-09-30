package com.smartsolar.stations.reservations.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.smartsolar.stations.R
import com.smartsolar.stations.reservations.data.ReservationDbHelper
import com.smartsolar.stations.reservations.data.ReservationRepository
import com.smartsolar.stations.reservations.data.ReservationRules
import kotlinx.coroutines.launch

/** Confirmation shown after a booking is created, changed, or cancelled. */
class BookingSummaryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_summary)
        findViewById<TextView>(R.id.summaryTitle).text = intent.getStringExtra("heading") ?: "Booking summary"
        findViewById<Button>(R.id.summaryDone).setOnClickListener {
            startActivity(Intent(this, ReservationsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            finish()
        }
        val id = intent.getStringExtra("reservationId").orEmpty()
        val repository = ReservationRepository(ReservationDbHelper(this))
        lifecycleScope.launch {
            try {
                val booking = repository.one(id)
                val station = runCatching { repository.stations().find { it.stationId == booking.stationId }?.stationName }.getOrNull()
                    ?: booking.stationId
                findViewById<TextView>(R.id.summaryCode).text = booking.verificationCode.ifBlank { booking.reservationId }
                findViewById<ImageView>(R.id.summaryQr).setImageBitmap(
                    QrBitmaps.encode(booking.verificationCode.ifBlank { booking.reservationId }, 640)
                )
                findViewById<TextView>(R.id.summaryBody).text = listOf(
                    station,
                    ReservationRules.formatLong(booking.date),
                    "${booking.startTime} – ${booking.endTime}",
                    "${booking.energyAmount} kWh",
                    "Status  ${ReservationRules.statusLabel(booking.status)}",
                    "Reference  ${booking.reservationId}"
                ).joinToString("\n")
            } catch (thrown: Exception) {
                findViewById<TextView>(R.id.summaryBody).text = ReservationRepository.apiMessage(thrown)
            }
        }
    }
}

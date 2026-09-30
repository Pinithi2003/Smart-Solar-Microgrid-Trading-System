package com.smartsolar.stations.reservations.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.smartsolar.stations.R
import com.smartsolar.stations.reservations.data.ReservationDbHelper
import com.smartsolar.stations.reservations.data.ReservationRepository
import com.smartsolar.stations.reservations.data.ReservationRules
import com.smartsolar.stations.reservations.model.EnergyReservation
import com.smartsolar.stations.s_stations.ui.MemberNav
import kotlinx.coroutines.launch
import kotlin.math.floor

/** One booking. Change and cancel stay closed inside the 12-hour window. */
class ReservationDetailActivity : AppCompatActivity() {

    private lateinit var repository: ReservationRepository
    private var booking: EnergyReservation? = null
    private var stationName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservation_detail)
        repository = ReservationRepository(ReservationDbHelper(this))
        MemberNav.bindHeader(this, "Booking")
        findViewById<Button>(R.id.changeSlotButton).setOnClickListener { showSlots() }
        findViewById<Button>(R.id.cancelBookingButton).setOnClickListener { confirmCancel() }
        load()
    }

    private fun load() {
        val id = intent.getStringExtra("reservationId").orEmpty()
        lifecycleScope.launch {
            try {
                booking = repository.one(id)
                stationName = runCatching {
                    repository.stations().find { it.stationId == booking?.stationId }?.stationName
                }.getOrNull() ?: booking?.stationId.orEmpty()
                bind()
            } catch (thrown: Exception) {
                findViewById<TextView>(R.id.detailTitle).text = ReservationRepository.apiMessage(thrown)
            }
        }
    }

    private fun bind() {
        val item = booking ?: return
        findViewById<TextView>(R.id.detailTitle).text = stationName
        findViewById<TextView>(R.id.detailWhen).text =
            "${ReservationRules.formatLong(item.date)}  ·  ${item.startTime} – ${item.endTime}"
        val status = findViewById<TextView>(R.id.detailStatus)
        status.text = ReservationRules.statusLabel(item.status)
        status.setTextColor(if (item.status == "Cancelled") 0xFFD64545.toInt() else 0xFF1F9D55.toInt())
        val hours = ReservationRules.hoursUntil(item)
        val starts = if (hours <= 0) "Started" else "Starts in ${floor(hours).toInt()}h ${floor((hours % 1) * 60).toInt()}m"
        findViewById<TextView>(R.id.detailBody).text = listOf(
            "Reference    ${item.reservationId}",
            "Prosumer    ${item.userId}",
            "Station    $stationName",
            "Energy    ${item.energyAmount} kWh",
            "Code    ${item.verificationCode.ifBlank { "—" }}",
            starts
        ).joinToString("\n")
        val open = ReservationRules.canChange(item)
        findViewById<Button>(R.id.changeSlotButton).isEnabled = open
        findViewById<Button>(R.id.cancelBookingButton).isEnabled = open
        val note = findViewById<TextView>(R.id.closedNote)
        note.visibility = if (open) View.GONE else View.VISIBLE
        note.text = if (item.status != "Confirmed") {
            "Only an approved booking can be changed or cancelled. This one is ${ReservationRules.statusLabel(item.status)}."
        } else {
            "Changes close 12 hours before the booking starts."
        }
    }

    private fun showSlots() {
        val item = booking ?: return
        val box = findViewById<LinearLayout>(R.id.slotChoices)
        box.visibility = View.VISIBLE
        box.removeAllViews()
        lifecycleScope.launch {
            val choices = runCatching { repository.slots(item.stationId) }.getOrDefault(emptyList()).filter {
                it.slotId != item.slotId &&
                    ReservationRules.dayKey(it.date) >= java.time.LocalDate.now().toString() &&
                    it.status == "Available" &&
                    it.availableEnergy >= item.energyAmount
            }
            if (choices.isEmpty()) {
                box.addView(TextView(this@ReservationDetailActivity).apply { text = "No other open slot can take this energy." })
                return@launch
            }
            choices.forEach { choice ->
                val button = TextView(this@ReservationDetailActivity).apply {
                    text = "${ReservationRules.formatShort(choice.date)}   ${choice.startTime} – ${choice.endTime}\n${choice.availableEnergy} kWh left"
                    setPadding(28, 20, 28, 20)
                    background = getDrawable(R.drawable.bg_res_slot)
                    setOnClickListener { changeTo(choice.slotId) }
                }
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.bottomMargin = 10
                box.addView(button, params)
            }
        }
    }

    private fun changeTo(slotId: String) {
        val item = booking ?: return
        lifecycleScope.launch {
            try {
                val updated = repository.update(item.reservationId, slotId)
                openSummary(updated.reservationId, "Booking updated")
            } catch (thrown: Exception) {
                findViewById<TextView>(R.id.actionError).text = ReservationRepository.apiMessage(thrown)
            }
        }
    }

    private fun confirmCancel() {
        AlertDialog.Builder(this)
            .setTitle("Cancel this booking?")
            .setMessage("Energy is restored only when at least 12 hours remain.")
            .setPositiveButton("Cancel booking") { _, _ -> cancel() }
            .setNegativeButton("Keep", null)
            .show()
    }

    private fun cancel() {
        val item = booking ?: return
        lifecycleScope.launch {
            try {
                val updated = repository.cancel(item.reservationId)
                openSummary(updated.reservationId, "Booking cancelled")
            } catch (thrown: Exception) {
                findViewById<TextView>(R.id.actionError).text = ReservationRepository.apiMessage(thrown)
            }
        }
    }

    private fun openSummary(id: String, heading: String) {
        startActivity(
            Intent(this, BookingSummaryActivity::class.java)
                .putExtra("reservationId", id)
                .putExtra("heading", heading)
        )
        finish()
    }
}

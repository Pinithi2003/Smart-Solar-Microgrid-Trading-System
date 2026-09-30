package com.smartsolar.stations.reservations.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.smartsolar.stations.R
import com.smartsolar.stations.auth.data.SessionManager
import com.smartsolar.stations.reservations.data.ReservationDbHelper
import com.smartsolar.stations.reservations.data.ReservationRepository
import com.smartsolar.stations.reservations.data.ReservationRules
import com.smartsolar.stations.reservations.data.ReservationStubs
import com.smartsolar.stations.reservations.model.BookingSlot
import com.smartsolar.stations.s_stations.model.SolarStation
import com.smartsolar.stations.s_stations.ui.MemberNav
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Create a reservation inside the next seven days, then open the summary. */
class BookSlotActivity : AppCompatActivity() {

    private lateinit var repository: ReservationRepository
    private var stations: List<SolarStation> = emptyList()
    private var slots: List<BookingSlot> = emptyList()
    private var stationId: String = ""
    private var day: LocalDate = LocalDate.now()
    private var slot: BookingSlot? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_slot)
        repository = ReservationRepository(ReservationDbHelper(this))
        MemberNav.bindHeader(this, "New booking")
        stationId = intent.getStringExtra(ReservationStubs.EXTRA_STATION_ID).orEmpty()
        findViewById<Button>(R.id.confirmBooking).setOnClickListener { confirm() }
        findViewById<EditText>(R.id.energyInput).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = refreshConfirm()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })
        loadStations()
    }

    private fun userId(): String = SessionManager(this).current()?.nic?.takeIf { it.isNotBlank() } ?: "200012345678"

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun loadStations() {
        lifecycleScope.launch {
            stations = runCatching { repository.stations() }.getOrDefault(emptyList())
            if (stationId.isBlank()) stationId = stations.firstOrNull { it.status == "Active" }?.stationId.orEmpty()
            drawStations()
            drawDays()
            loadSlots()
        }
    }

    private fun drawStations() {
        val row = findViewById<LinearLayout>(R.id.stationRow)
        row.removeAllViews()
        stations.forEach { station ->
            val card = TextView(this).apply {
                val open = station.status == "Active"
                text = "${station.stationName}\n${station.location}\n${station.availableCapacity} kWh"
                setPadding(28, 24, 28, 24)
                textSize = 13f
                setTextColor(0xFF1C2434.toInt())
                background = getDrawable(if (station.stationId == stationId) R.drawable.bg_res_soft else R.drawable.bg_res_card)
                isEnabled = open
                alpha = if (open) 1f else 0.45f
                setOnClickListener {
                    stationId = station.stationId
                    slot = null
                    drawStations()
                    loadSlots()
                }
            }
            val params = LinearLayout.LayoutParams(dp(168), LinearLayout.LayoutParams.WRAP_CONTENT)
            params.marginEnd = 16
            row.addView(card, params)
        }
    }

    private fun drawDays() {
        val row = findViewById<LinearLayout>(R.id.dayRow)
        row.removeAllViews()
        ReservationRules.windowDays().forEach { date ->
            val label = if (date == LocalDate.now()) "TODAY" else date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase()
            val openCount = slots.count { ReservationRules.dayKey(it.date) == date.toString() && it.status == "Available" && it.availableEnergy > 0 }
            val chip = TextView(this).apply {
                text = "$label\n${date.dayOfMonth}\n${if (openCount > 0) "$openCount open" else "None"}"
                gravity = Gravity.CENTER
                setPadding(20, 16, 20, 16)
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                background = getDrawable(R.drawable.bg_res_day)
                isSelected = date == day
                setOnClickListener {
                    day = date
                    slot = null
                    drawDays()
                    drawSlots()
                }
            }
            val params = LinearLayout.LayoutParams(dp(72), LinearLayout.LayoutParams.WRAP_CONTENT)
            params.marginEnd = 12
            row.addView(chip, params)
        }
    }

    private fun loadSlots() {
        if (stationId.isBlank()) return
        lifecycleScope.launch {
            slots = runCatching { repository.slots(stationId) }.getOrDefault(emptyList())
            drawDays()
            drawSlots()
        }
    }

    private fun drawSlots() {
        val column = findViewById<LinearLayout>(R.id.slotColumn)
        column.removeAllViews()
        val daySlots = slots.filter { ReservationRules.dayKey(it.date) == day.toString() }
        if (daySlots.isEmpty()) {
            column.addView(TextView(this).apply {
                text = "No slots on this date."
                setTextColor(0xFF6B7288.toInt())
                setPadding(0, 12, 0, 12)
            })
            refreshConfirm()
            return
        }
        daySlots.forEach { item ->
            val open = item.status == "Available" && item.availableEnergy > 0
            val button = TextView(this).apply {
                text = "${item.startTime} – ${item.endTime}\n${item.availableEnergy} kWh left of ${item.energyCapacity}"
                setPadding(28, 22, 28, 22)
                textSize = 14f
                setTextColor(0xFF1C2434.toInt())
                background = getDrawable(R.drawable.bg_res_slot)
                isEnabled = open
                isSelected = slot?.slotId == item.slotId
                alpha = if (open) 1f else 0.5f
                setOnClickListener {
                    slot = item
                    drawSlots()
                }
            }
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = 12
            column.addView(button, params)
        }
        refreshConfirm()
    }

    private fun refreshConfirm() {
        val station = stations.find { it.stationId == stationId }
        val chosen = slot
        findViewById<TextView>(R.id.summaryLine).text = if (station == null || chosen == null) {
            "Select a station, a day, and a slot."
        } else {
            "${station.stationName}\n${ReservationRules.formatLong(day.toString())}  ·  ${chosen.startTime} – ${chosen.endTime}"
        }
        val energy = findViewById<EditText>(R.id.energyInput).text.toString().toDoubleOrNull() ?: 0.0
        findViewById<Button>(R.id.confirmBooking).isEnabled = station != null && chosen != null && energy > 0
    }

    private fun confirm() {
        val chosen = slot ?: return
        val energy = findViewById<EditText>(R.id.energyInput).text.toString().toDoubleOrNull() ?: 0.0
        val button = findViewById<Button>(R.id.confirmBooking)
        val error = findViewById<TextView>(R.id.bookError)
        button.isEnabled = false
        error.text = ""
        lifecycleScope.launch {
            try {
                val saved = repository.create(userId(), stationId, chosen.slotId, energy)
                startActivity(
                    Intent(this@BookSlotActivity, BookingSummaryActivity::class.java)
                        .putExtra("reservationId", saved.reservationId)
                        .putExtra("heading", "Booking confirmed")
                )
                finish()
            } catch (thrown: Exception) {
                error.text = ReservationRepository.apiMessage(thrown)
                button.isEnabled = true
            }
        }
    }
}

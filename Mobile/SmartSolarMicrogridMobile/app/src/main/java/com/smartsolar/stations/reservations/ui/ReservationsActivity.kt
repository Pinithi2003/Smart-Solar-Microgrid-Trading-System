package com.smartsolar.stations.reservations.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartsolar.stations.R
import com.smartsolar.stations.auth.data.SessionManager
import com.smartsolar.stations.reservations.data.ReservationDbHelper
import com.smartsolar.stations.reservations.data.ReservationRepository
import com.smartsolar.stations.reservations.data.ReservationRules
import com.smartsolar.stations.reservations.model.EnergyReservation
// Ensure this import matches your actual package structure
import com.smartsolar.stations.stations.ui.MemberNav
import kotlinx.coroutines.launch

class ReservationsActivity : AppCompatActivity() {

    private lateinit var repository: ReservationRepository
    private lateinit var adapter: ReservationAdapter
    private var bookings: List<EnergyReservation> = emptyList()
    private var stationNames: Map<String, String> = emptyMap()
    private var filter = "all"

    private val userId: String
        get() = SessionManager(this).current()?.nic?.takeIf { it.isNotBlank() } ?: "200012345678"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservations)

        repository = ReservationRepository(ReservationDbHelper(this))

        // This requires BOOKINGS to be defined in MemberNav object
        MemberNav.bindHeader(this, "Reservations")
        MemberNav.bindBottomNav(this, MemberNav.BOOKINGS)

        adapter = ReservationAdapter(emptyMap()) { booking: EnergyReservation ->
            startActivity(Intent(this, ReservationDetailActivity::class.java)
                .putExtra("reservationId", booking.reservationId))
        }

        findViewById<RecyclerView>(R.id.bookingList).apply {
            layoutManager = LinearLayoutManager(this@ReservationsActivity)
            this.adapter = this@ReservationsActivity.adapter
        }

        buildFilters()

        findViewById<EditText>(R.id.bookingSearch).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { render() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        findViewById<View>(R.id.newBookingButton).setOnClickListener {
            startActivity(Intent(this, BookSlotActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun buildFilters() {
        val row = findViewById<LinearLayout>(R.id.filterRow)
        row.removeAllViews() // Clear to prevent duplicates on reload

        val filters = listOf("all" to "All", "current" to "Current", "pending" to "Pending",
            "future" to "Ahead", "history" to "History", "mine" to "Mine")

        filters.forEach { (key, label) ->
            val chip = TextView(this).apply {
                text = label
                textSize = 13f
                val density = resources.displayMetrics.density
                val padH = (14 * density).toInt()
                val padV = (8 * density).toInt()
                setPadding(padH, padV, padH, padV)
                background = AppCompatResources.getDrawable(context, R.drawable.bg_res_chip)
                setTextColor(if (key == filter) 0xFF3A2A00.toInt() else 0xFF3D4658.toInt())
                isSelected = (key == filter)

                setOnClickListener {
                    filter = key
                    for (i in 0 until row.childCount) {
                        val child = row.getChildAt(i) as TextView
                        child.isSelected = (filters[i].first == filter)
                        child.setTextColor(if (child.isSelected) 0xFF3A2A00.toInt() else 0xFF3D4658.toInt())
                    }
                    render()
                }
            }
            val gap = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            gap.marginEnd = (16 * resources.displayMetrics.density).toInt()
            row.addView(chip, gap)
        }
    }

    private fun load() {
        lifecycleScope.launch {
            // Explicitly handle the result to avoid inference issues
            val result = repository.reservations()
            bookings = result.first
            val offline = result.second

            stationNames = runCatching {
                repository.stations().associate { it.stationId to it.stationName }
            }.getOrDefault(emptyMap())

            adapter = ReservationAdapter(stationNames) { booking: EnergyReservation ->
                startActivity(Intent(this@ReservationsActivity, ReservationDetailActivity::class.java)
                    .putExtra("reservationId", booking.reservationId))
            }

            findViewById<RecyclerView>(R.id.bookingList).adapter = adapter
            findViewById<View>(R.id.offlineNote).visibility = if (offline) View.VISIBLE else View.GONE
            findViewById<TextView>(R.id.countPending).text = bookings.count { ReservationRules.isPending(it) }.toString()
            findViewById<TextView>(R.id.countFuture).text = bookings.count { ReservationRules.isFutureApproved(it) }.toString()

            render()
        }
    }

    private fun render() {
        val query = findViewById<EditText>(R.id.bookingSearch).text.toString().trim().lowercase()

        // Added explicit type 'booking: EnergyReservation' to fix inference error
        val visible = bookings.filter { booking: EnergyReservation ->
            val matches = when (filter) {
                "current" -> ReservationRules.isCurrent(booking)
                "pending" -> ReservationRules.isPending(booking)
                "future" -> ReservationRules.isFutureApproved(booking)
                "history" -> ReservationRules.isHistory(booking)
                "mine" -> booking.userId.equals(userId, true)
                else -> true
            }

            if (!matches) return@filter false
            if (query.isEmpty()) return@filter true

            val station = stationNames[booking.stationId].orEmpty()
            listOf(
                booking.reservationId,
                booking.userId,
                booking.stationId,
                station,
                booking.verificationCode,
                ReservationRules.statusLabel(booking.status)
            ).joinToString(" ").lowercase().contains(query)
        }

        adapter.submit(visible)
        findViewById<View>(R.id.emptyBookings).visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
    }
}
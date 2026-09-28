package com.smartsolar.stations.ui

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smartsolar.stations.R
import com.smartsolar.stations.infrastructure.MapPreviewFragment
import com.smartsolar.stations.infrastructure.MemberNav
import com.smartsolar.stations.utils.BusinessRules
import com.smartsolar.stations.utils.MockData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Member 2 node detail in the Stitch theme: mock map, node card, date picker,
// slot picker and trade stepper are demo shells. The real booking flow that
// confirms and pays belongs to Member 3 and plugs in at bookButton.
class StationDetailActivity : AppCompatActivity() {

    private var tradeQty = 12.0
    private var tradeMax = 12.0
    private var selectedSlot: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_station_detail)

        val id = intent.getStringExtra("stationId")
            ?: MockData.stations.firstOrNull()?.stationId ?: ""
        val station = MockData.stations.find { it.stationId == id }
        if (station == null) {
            Toast.makeText(this, getString(R.string.error_station_not_found), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        MemberNav.bindHeader(this, getString(R.string.tab_map))
        MemberNav.bindChips(this)
        MemberNav.bindBottomNav(this, MemberNav.MAP)

        findViewById<EditText>(R.id.detailSearch).setOnEditorActionListener { v, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEARCH || action == EditorInfo.IME_ACTION_DONE) {
                startActivity(
                    Intent(this, StationsListActivity::class.java)
                        .putExtra("query", v.text.toString())
                )
                true
            } else false
        }

        MemberNav.stylePill(findViewById(R.id.detailStatus), station.status)
        findViewById<TextView>(R.id.detailDist).text =
            "• ${MockData.demoDistances[station.stationId] ?: "—"}"
        findViewById<TextView>(R.id.detailId).text = station.stationId
        findViewById<TextView>(R.id.detailName).text = station.stationName
        findViewById<TextView>(R.id.statCap).text = "${station.totalCapacity} kWh"
        findViewById<TextView>(R.id.statAvail).text = "${station.availableCapacity} kWh"
        findViewById<TextView>(R.id.statOp).text = station.operator
        findViewById<TextView>(R.id.detailMeta).text =
            "Location: ${station.location}\n" +
                "Position: ${station.latitude}, ${station.longitude}\n" +
                "Operator: ${station.operator}"

        buildDateRow()
        buildSlotGrid()

        tradeMax = maxOf(station.availableCapacity, 0.5)
        tradeQty = minOf(12.0, tradeMax)
        refreshTrade()
        findViewById<Button>(R.id.tradeMinus).setOnClickListener {
            tradeQty = maxOf(0.5, tradeQty - 0.5)
            refreshTrade()
        }
        findViewById<Button>(R.id.tradePlus).setOnClickListener {
            tradeQty = minOf(tradeMax, tradeQty + 0.5)
            refreshTrade()
        }

        val frag = MapPreviewFragment()
        // Activity's getString: the fragment is not attached until commit runs.
        frag.badgeText = getString(R.string.telemetry_badge)
        supportFragmentManager.beginTransaction()
            .replace(R.id.mapContainer, frag)
            .commitNowAllowingStateLoss()
        frag.setStations(MockData.stations, station.stationId)

        val book = findViewById<Button>(R.id.bookButton)
        val ok = BusinessRules.isBookable(station.status)
        book.isEnabled = ok
        book.alpha = if (ok) 1f else 0.5f
        book.setOnClickListener {
            Toast.makeText(
                this,
                "Reservations module (Member 3): booking ${station.stationId} opens here.",
                Toast.LENGTH_LONG
            ).show()
        }
        if (!ok) Toast.makeText(this, "${station.status} stations cannot be booked.", Toast.LENGTH_SHORT).show()
    }

    // Demo 7-day window starting today. Selection is local demo state.
    private fun buildDateRow() {
        val row = findViewById<LinearLayout>(R.id.dateRow)
        row.removeAllViews()
        val dayName = SimpleDateFormat("EEE", Locale.US)
        val dayNum = SimpleDateFormat("d MMM", Locale.US)
        val gap = (8 * resources.displayMetrics.density).toInt()
        val cal = Calendar.getInstance()
        repeat(7) { i ->
            val top = if (i == 0) "TODAY" else dayName.format(cal.time).uppercase()
            val pill = Button(this).apply {
                text = "$top\n${dayNum.format(cal.time)}"
                textSize = 11f
                isAllCaps = false
                setBackgroundResource(R.drawable.bg_date_sel)
                setTextColor(resources.getColorStateList(R.color.date_text_sel, theme))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (i < 6) marginEnd = gap
                }
                setOnClickListener {
                    (0 until row.childCount).forEach { k -> row.getChildAt(k).isSelected = false }
                    isSelected = true
                }
            }
            if (i == 0) pill.isSelected = true
            row.addView(pill)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    // Demo slot picker. States are placeholders until Member 3 serves real slots.
    private fun buildSlotGrid() {
        val grid = findViewById<GridLayout>(R.id.slotGrid)
        grid.removeAllViews()
        val slots = listOf(
            Pair("09:00 - 10:00", "Available"),
            Pair("10:00 - 11:00", "Available"),
            Pair("11:00 - 12:00", "Available"),
            Pair("14:00 - 15:00", "Booked")
        )
        slots.forEachIndexed { i, slot ->
            val btn = Button(this).apply {
                text = "${slot.first}\n${slot.second}"
                textSize = 12f
                isAllCaps = false
                setBackgroundResource(R.drawable.bg_slot_sel)
                setTextColor(resources.getColorStateList(R.color.slot_text_sel, theme))
                val gap = (8 * resources.displayMetrics.density).toInt()
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(i % 2, 1f)
                    rowSpec = GridLayout.spec(i / 2)
                    setMargins(0, 0, if (i % 2 == 0) gap else 0, gap)
                }
                if (slot.second == "Booked") {
                    isEnabled = false
                    alpha = 0.6f
                } else {
                    setOnClickListener {
                        selectedSlot?.isSelected = false
                        isSelected = true
                        selectedSlot = this
                    }
                }
            }
            if (i == 1) {
                btn.isSelected = true
                selectedSlot = btn
            }
            grid.addView(btn)
        }
    }

    // Demo trade estimate at a flat demo rate. Pricing lives with Member 3.
    private fun refreshTrade() {
        findViewById<TextView>(R.id.tradeQty).text = "%.1f kWh".format(tradeQty)
        findViewById<TextView>(R.id.tradeEst).text = "+$%.2f Est.".format(tradeQty * 0.24)
    }
}

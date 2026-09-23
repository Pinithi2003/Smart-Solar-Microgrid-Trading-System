package com.smartsolar.stations.s_stations.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.smartsolar.stations.BuildConfig
import com.smartsolar.stations.R
import com.smartsolar.stations.s_stations.data.StationsDbHelper
import com.smartsolar.stations.s_stations.model.SolarStation
import com.smartsolar.stations.s_stations.data.ApiClient
import com.smartsolar.stations.s_stations.data.StationRepository
import com.smartsolar.stations.s_stations.util.BusinessRules
import com.smartsolar.stations.core.MockData
import kotlinx.coroutines.launch

/**
 * Full station details page.
 * Loads the LIVE station (same source as Home/Map lists) so the header,
 * status and capacity always match the card tapped — not stale MockData.
 * Only demo distances stay local (Member 4 computes real ones later).
 */
class StationFullDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_station_full_detail)

        val id = intent.getStringExtra("stationId")
            ?: MockData.stations.firstOrNull()?.stationId ?: ""
        if (id.isBlank()) {
            Toast.makeText(this, getString(R.string.error_station_not_found), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        MemberNav.bindHeader(this, getString(R.string.tab_map))
        MemberNav.bindBottomNav(this, MemberNav.MAP)

        // Fast path: show cached/mock instantly, then refresh from live API.
        MockData.stations.find { it.stationId == id }?.let { bind(it) }

        lifecycleScope.launch {
            val db = StationsDbHelper(this@StationFullDetailActivity)
            val repo = try {
                StationRepository(ApiClient.stationApi(BuildConfig.API_BASE_URL)) { db.cacheStations(it) }
            } catch (_: Exception) {
                StationRepository(null)
            }
            var list = repo.loadStations()
            if (list == MockData.stations) {
                val cached = db.readCachedStations()
                if (cached.isNotEmpty()) list = cached
            }
            val station = list.find { it.stationId == id }
            if (station == null) {
                Toast.makeText(this@StationFullDetailActivity, getString(R.string.error_station_not_found), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                bind(station)
            }
        }
    }

    private fun bind(station: SolarStation) {
        MemberNav.bindHeader(this, station.stationName)
        MemberNav.stylePill(findViewById(R.id.fullStatus), station.status)

        // Demo distances exist only for ST001..ST003 — hide when unknown
        // instead of showing "-- away".
        val distView = findViewById<TextView>(R.id.fullDist)
        val dist = MockData.demoDistances[station.stationId]
        if (dist != null) {
            distView.visibility = View.VISIBLE
            distView.text = "• $dist away"
        } else {
            distView.visibility = View.GONE
        }

        findViewById<TextView>(R.id.fullId).text = station.stationId
        findViewById<TextView>(R.id.fullName).text = station.stationName
        findViewById<TextView>(R.id.fullLocation).text = station.location

        findViewById<TextView>(R.id.fullTotal).text = "${station.totalCapacity} kWh"
        findViewById<TextView>(R.id.fullAvail).text = "${station.availableCapacity} kWh"
        val inUse = (station.totalCapacity - station.availableCapacity).coerceAtLeast(0.0)
        findViewById<TextView>(R.id.fullUsed).text = "$inUse kWh"

        val pct = if (station.totalCapacity > 0)
            ((inUse / station.totalCapacity) * 100).toInt().coerceIn(0, 100)
        else 0
        findViewById<TextView>(R.id.fullUsageText).text =
            "$pct% utilized • ${station.availableCapacity} kWh free of ${station.totalCapacity} kWh"
        findViewById<ProgressBar>(R.id.fullUsageBar).progress = pct

        val bookable = BusinessRules.isBookable(station.status)
        val book = findViewById<Button>(R.id.fullBookButton)
        book.isEnabled = bookable
        book.alpha = if (bookable) 1f else 0.5f
        book.setOnClickListener {
            Toast.makeText(
                this,
                "Reservations module (Member 3): booking ${station.stationId} opens here.",
                Toast.LENGTH_LONG
            ).show()
        }

        findViewById<Button>(R.id.fullMapButton).setOnClickListener { finish() }
    }
}

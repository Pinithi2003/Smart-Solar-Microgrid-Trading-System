package com.smartsolar.stations.s_stations.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartsolar.stations.BuildConfig
import com.smartsolar.stations.R
import com.smartsolar.stations.s_stations.data.StationsDbHelper
import com.smartsolar.stations.s_stations.model.SolarStation
import com.smartsolar.stations.s_stations.data.ApiClient
import com.smartsolar.stations.s_stations.data.StationRepository
import com.smartsolar.stations.core.MockData
import kotlinx.coroutines.launch

/**
 * Map tab: same design as Home (Stations list).
 * Map on top with all stations + stats + filterable cards below.
 * Tap a card (e.g. Colombo) or a map marker -> full station details.
 * Booking UI (date/slot/trade) lives with Member 3, not here.
 */
class StationDetailActivity : AppCompatActivity() {

    private lateinit var db: StationsDbHelper
    private lateinit var adapter: StationAdapter
    private var all: List<SolarStation> = emptyList()
    private var highlightId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_station_detail)
        db = StationsDbHelper(this)

        // Incoming highlight from Home list tap (e.g. ST001 Colombo).
        highlightId = intent.getStringExtra("stationId")

        MemberNav.bindHeader(this, getString(R.string.tab_map))
        MemberNav.bindChips(this)
        MemberNav.bindBottomNav(this, MemberNav.MAP)

        val list = findViewById<RecyclerView>(R.id.mapStationsList)
        adapter = StationAdapter(emptyList()) {
            startActivity(
                Intent(this, StationFullDetailActivity::class.java)
                    .putExtra("stationId", it.stationId)
            )
        }
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        val spinner = findViewById<Spinner>(R.id.mapStatusFilter)
        spinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("All", "Active", "Inactive", "Maintenance")
        )
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) = applyFilter()
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) = Unit
        }

        findViewById<EditText>(R.id.detailSearch).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = applyFilter()
            override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) = Unit
            override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) = Unit
        })
        intent.getStringExtra("query")?.takeIf { it.isNotBlank() }?.let {
            findViewById<EditText>(R.id.detailSearch).setText(it)
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.mapContainer, MapPreviewFragment())
            .commitAllowingStateLoss()

        load()
    }

    private fun applyFilter() {
        val q = findViewById<EditText>(R.id.detailSearch).text.toString().lowercase()
        val status = findViewById<Spinner>(R.id.mapStatusFilter).selectedItem?.toString() ?: "All"
        val filtered = all.filter {
            (status == "All" || it.status == status) &&
                (q.isBlank() || it.stationName.lowercase().contains(q) ||
                    it.stationId.lowercase().contains(q) || it.location.lowercase().contains(q))
        }
        adapter.submit(filtered)
        // Keep the map in sync with the filtered list, preserve highlight.
        (supportFragmentManager.findFragmentById(R.id.mapContainer) as? MapPreviewFragment)
            ?.setStations(filtered.ifEmpty { all }, highlightId)
    }

    private fun load() {
        lifecycleScope.launch {
            val repo = try {
                StationRepository(ApiClient.stationApi(BuildConfig.API_BASE_URL)) { db.cacheStations(it) }
            } catch (_: Exception) {
                StationRepository(null)
            }
            val live = repo.loadStations()
            all = live
            if (live == MockData.stations) {
                val cached = db.readCachedStations()
                if (cached.isNotEmpty()) all = cached
            }
            findViewById<TextView>(R.id.mapStatTotal).text = "Total: ${all.size}"
            findViewById<TextView>(R.id.mapStatActive).text = "Active: ${all.count { it.status == "Active" }}"
            findViewById<TextView>(R.id.mapStatAvailable).text =
                "kWh: ${all.sumOf { it.availableCapacity }.toInt()}"
            applyFilter()
            if (all.isEmpty()) Toast.makeText(this@StationDetailActivity, "Station not found.", Toast.LENGTH_SHORT).show()
        }
    }
}

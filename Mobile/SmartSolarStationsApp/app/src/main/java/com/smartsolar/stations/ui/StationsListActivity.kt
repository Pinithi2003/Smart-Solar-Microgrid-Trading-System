package com.smartsolar.stations.ui

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
import com.smartsolar.stations.database.StationsDbHelper
import com.smartsolar.stations.infrastructure.MapPreviewFragment
import com.smartsolar.stations.infrastructure.MemberNav
import com.smartsolar.stations.infrastructure.StationAdapter
import com.smartsolar.stations.models.SolarStation
import com.smartsolar.stations.services.ApiClient
import com.smartsolar.stations.services.StationRepository
import com.smartsolar.stations.utils.MockData
import kotlinx.coroutines.launch

/**
 * Member 2 home screen: stats + search + status filter + list + mock map.
 * Search mirrors the web topSearchInput; filter mirrors statusFilter.
 */
class StationsListActivity : AppCompatActivity() {

    private lateinit var db: StationsDbHelper
    private lateinit var adapter: StationAdapter
    private var all: List<SolarStation> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stations_list)
        db = StationsDbHelper(this)

        MemberNav.bindHeader(this, getString(R.string.stations_title))
        MemberNav.bindChips(this)
        MemberNav.bindBottomNav(this, MemberNav.HOME)

        val list = findViewById<RecyclerView>(R.id.stationsList)
        adapter = StationAdapter(emptyList()) {
            startActivity(
                Intent(this, StationDetailActivity::class.java)
                    .putExtra("stationId", it.stationId)
            )
        }
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        val spinner = findViewById<Spinner>(R.id.statusFilter)
        spinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("All", "Active", "Inactive", "Maintenance")
        )
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) = applyFilter()
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) = Unit
        }

        findViewById<EditText>(R.id.searchInput).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = applyFilter()
            override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) = Unit
            override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) = Unit
        })
        // Deep link from node detail search: pre-fill the query Member 2 received.
        intent.getStringExtra("query")?.takeIf { it.isNotBlank() }?.let {
            findViewById<EditText>(R.id.searchInput).setText(it)
        }

        // Inline mock-map strip at the bottom of the header area.
        supportFragmentManager.beginTransaction()
            .replace(R.id.mapStrip, MapPreviewFragment())
            .commitNowAllowingStateLoss()

        load()
    }

    private fun applyFilter() {
        val q = findViewById<EditText>(R.id.searchInput).text.toString().lowercase()
        val status = findViewById<Spinner>(R.id.statusFilter).selectedItem?.toString() ?: "All"
        adapter.submit(all.filter {
            (status == "All" || it.status == status) &&
                (q.isBlank() || it.stationName.lowercase().contains(q) ||
                    it.stationId.lowercase().contains(q) || it.location.lowercase().contains(q))
        })
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
                // Offline/demo path — serve SQLite cache if present (mirrors map fallback note).
                val cached = db.readCachedStations()
                if (cached.isNotEmpty()) all = cached
                findViewById<TextView>(R.id.lastRefreshed).text = "Demo data (API unreachable) — cached ${cached.size} stations"
            } else {
                findViewById<TextView>(R.id.lastRefreshed).text = "Live: ${live.size} stations from /api/solarstations"
            }
            findViewById<TextView>(R.id.statTotal).text = "Total: ${all.size}"
            findViewById<TextView>(R.id.statActive).text = "Active: ${all.count { it.status == "Active" }}"
            findViewById<TextView>(R.id.statAvailable).text =
                "kWh: ${all.sumOf { it.availableCapacity }.toInt()}"
            applyFilter()
            (supportFragmentManager.findFragmentById(R.id.mapStrip) as? MapPreviewFragment)
                ?.setStations(all)
            if (all.isEmpty()) Toast.makeText(this@StationsListActivity, "Station not found.", Toast.LENGTH_SHORT).show()
        }
    }
}

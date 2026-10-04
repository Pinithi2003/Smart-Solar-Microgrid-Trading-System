package com.smartsolar.stations.infrastructure

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smartsolar.stations.R
import com.smartsolar.stations.models.SolarStation
import com.smartsolar.stations.shared.common.MockData

/** Card list for stations — RecyclerView where useful (spec §1). */
class StationAdapter(
    private var items: List<SolarStation>,
    private val onClick: (SolarStation) -> Unit
) : RecyclerView.Adapter<StationAdapter.Holder>() {

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val id: TextView = v.findViewById(R.id.itemStationId)
        val name: TextView = v.findViewById(R.id.itemName)
        val meta: TextView = v.findViewById(R.id.itemMeta)
        val status: TextView = v.findViewById(R.id.itemStatus)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int): Holder =
        Holder(LayoutInflater.from(p.context).inflate(R.layout.item_station, p, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: Holder, pos: Int) {
        val s = items[pos]
        h.id.text = s.stationId
        h.name.text = s.stationName
        MemberNav.stylePill(h.status, s.status)
        val dist = MockData.demoDistances[s.stationId] ?: "—"
        h.meta.text = "${s.location} • $dist • ${s.availableCapacity} kWh available"
        h.itemView.setOnClickListener { onClick(s) }
    }

    fun submit(next: List<SolarStation>) {
        items = next
        notifyDataSetChanged()
    }
}

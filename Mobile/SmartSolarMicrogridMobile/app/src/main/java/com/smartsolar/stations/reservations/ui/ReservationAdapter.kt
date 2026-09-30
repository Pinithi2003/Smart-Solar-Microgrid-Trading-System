package com.smartsolar.stations.reservations.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smartsolar.stations.R
import com.smartsolar.stations.reservations.data.ReservationRules
import com.smartsolar.stations.reservations.model.EnergyReservation

/** One row in the booking list. */
class ReservationAdapter(
    private val stationNames: Map<String, String>,
    private val onClick: (EnergyReservation) -> Unit
) : RecyclerView.Adapter<ReservationAdapter.Holder>() {

    private var items: List<EnergyReservation> = emptyList()

    fun submit(next: List<EnergyReservation>) {
        items = next
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reservation, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val name = stationNames[item.stationId] ?: item.stationId
        holder.initials.text = item.userId.take(2).uppercase()
        holder.title.text = name
        holder.meta.text = "${item.reservationId}  ·  ${ReservationRules.formatShort(item.date)}  ·  ${item.startTime} – ${item.endTime}"
        holder.status.text = ReservationRules.statusLabel(item.status)
        val colors = when (item.status) {
            "Cancelled" -> "#FDECEC" to "#D64545"
            "Completed" -> "#F1F2F6" to "#667085"
            else -> "#E8F8EE" to "#1F9D55"
        }
        holder.status.background = GradientDrawable().apply {
            cornerRadius = 100f
            setColor(Color.parseColor(colors.first))
        }
        holder.status.setTextColor(Color.parseColor(colors.second))
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val initials: TextView = view.findViewById(R.id.itemInitials)
        val title: TextView = view.findViewById(R.id.itemTitle)
        val meta: TextView = view.findViewById(R.id.itemMeta)
        val status: TextView = view.findViewById(R.id.itemStatus)
    }
}

package com.smartsolar.stations.fieldops.member4.operations

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smartsolar.stations.R
import com.smartsolar.stations.models.Booking

class CompletedTransactionAdapter(
    private var transactions: List<Booking>,
    private val onItemClick: (Booking) -> Unit
) : RecyclerView.Adapter<CompletedTransactionAdapter.TxnViewHolder>() {

    class TxnViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txnIdText: TextView = view.findViewById(R.id.txnIdText)
        val txnProsumerText: TextView = view.findViewById(R.id.txnProsumerText)
        val txnStationText: TextView = view.findViewById(R.id.txnStationText)
        val txnEnergyText: TextView = view.findViewById(R.id.txnEnergyText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TxnViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_completed_transaction_card, parent, false)
        return TxnViewHolder(view)
    }

    override fun onBindViewHolder(holder: TxnViewHolder, position: Int) {
        val t = transactions[position]
        holder.txnIdText.text = t.transactionId ?: t.reservationId
        holder.txnProsumerText.text = "Prosumer: ${t.prosumerName.ifBlank { "Nimal Perera" }}"
        holder.txnStationText.text = "${t.stationName} • ${t.date}"
        holder.txnEnergyText.text = "⚡ ${t.energyAmount} kWh"

        holder.itemView.setOnClickListener { onItemClick(t) }
    }

    override fun getItemCount(): Int = transactions.size

    fun updateData(newTransactions: List<Booking>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}

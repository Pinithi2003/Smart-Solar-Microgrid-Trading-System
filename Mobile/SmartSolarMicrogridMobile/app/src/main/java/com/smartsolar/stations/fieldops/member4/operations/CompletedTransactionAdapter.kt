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
) : RecyclerView.Adapter<CompletedTransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val transactionIdText: TextView =
            itemView.findViewById(R.id.transactionIdText)

        private val reservationIdText: TextView =
            itemView.findViewById(R.id.reservationIdText)

        private val stationNameText: TextView =
            itemView.findViewById(R.id.stationNameText)

        private val prosumerNameText: TextView =
            itemView.findViewById(R.id.prosumerNameText)

        private val transactionDateTimeText: TextView =
            itemView.findViewById(R.id.transactionDateTimeText)

        private val transactionStatusText: TextView =
            itemView.findViewById(R.id.transactionStatusText)

        fun bind(booking: Booking) {

            transactionIdText.text =
                "Transaction ID: ${booking.transactionId ?: "N/A"}"

            reservationIdText.text =
                "Reservation ID: ${booking.reservationId}"

            stationNameText.text =
                "Station: ${booking.stationName}"

            prosumerNameText.text =
                "Prosumer: ${booking.prosumerName}"

            transactionDateTimeText.text =
                "${booking.date} • ${booking.time}"

            transactionStatusText.text =
                booking.status

            itemView.setOnClickListener {
                onItemClick(booking)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TransactionViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_completed_transaction_card,
            parent,
            false
        )

        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: TransactionViewHolder,
        position: Int
    ) {
        holder.bind(transactions[position])
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    fun updateData(newTransactions: List<Booking>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}
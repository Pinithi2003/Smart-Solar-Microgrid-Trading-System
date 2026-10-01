package com.smartsolar.stations.fieldops.member4.operations

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.smartsolar.stations.R
import com.smartsolar.stations.fieldops.member4.qrscanner.TransactionQrActivity
import com.smartsolar.stations.models.Booking
import com.smartsolar.stations.services.QrService
import com.smartsolar.stations.infrastructure.MemberNav

class CompletedTransactionsActivity : AppCompatActivity() {

    private lateinit var adapter: CompletedTransactionAdapter
    private lateinit var emptyText: TextView

    private var allTransactions: List<Booking> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_completed_transactions)

        // ---------------------------------------------------------
        // Find views
        // ---------------------------------------------------------

        val recyclerView =
            findViewById<RecyclerView>(R.id.completedTxnRecyclerView)

        val searchInput =
            findViewById<TextInputEditText>(R.id.txnSearchInput)

        emptyText =
            findViewById(R.id.emptyTxnText)

        // ---------------------------------------------------------
        // RecyclerView setup
        // ---------------------------------------------------------

        recyclerView.layoutManager =
            LinearLayoutManager(this)

        adapter = CompletedTransactionAdapter(
            emptyList()
        ) { booking ->

            val intent = Intent(
                this,
                TransactionQrActivity::class.java
            )

            intent.putExtra(
                "EXTRA_RESERVATION_ID",
                booking.reservationId
            )

            startActivity(intent)
        }

        recyclerView.adapter = adapter

        MemberNav.bindBottomNav(this, MemberNav.QR)

        // ---------------------------------------------------------
        // Search
        // ---------------------------------------------------------

        searchInput.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing required
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    filterTransactions(
                        s?.toString() ?: ""
                    )
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                    // Nothing required
                }
            }
        )
    }

    // -------------------------------------------------------------
    // Reload transactions whenever activity becomes visible
    // -------------------------------------------------------------

    override fun onResume() {
        super.onResume()

        allTransactions =
            QrService.getCompletedTransactions(this)

        val searchInput =
            findViewById<TextInputEditText>(
                R.id.txnSearchInput
            )

        filterTransactions(
            searchInput.text?.toString() ?: ""
        )
    }

    // -------------------------------------------------------------
    // Filter completed transactions
    // -------------------------------------------------------------

    private fun filterTransactions(query: String) {

        val filteredTransactions: List<Booking>

        if (query.isBlank()) {

            filteredTransactions =
                allTransactions

        } else {

            val searchQuery =
                query.trim().lowercase()

            filteredTransactions =
                allTransactions.filter { booking ->

                    val transactionId =
                        booking.transactionId
                            ?.lowercase()
                            ?: ""

                    val reservationId =
                        booking.reservationId
                            .lowercase()

                    val stationName =
                        booking.stationName
                            .lowercase()

                    val prosumerName =
                        booking.prosumerName
                            .lowercase()

                    transactionId.contains(searchQuery) ||
                            reservationId.contains(searchQuery) ||
                            stationName.contains(searchQuery) ||
                            prosumerName.contains(searchQuery)
                }
        }

        // ---------------------------------------------------------
        // Update RecyclerView
        // ---------------------------------------------------------

        adapter.updateData(
            filteredTransactions
        )

        // ---------------------------------------------------------
        // Empty state
        // ---------------------------------------------------------

        emptyText.visibility =
            if (filteredTransactions.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }
}
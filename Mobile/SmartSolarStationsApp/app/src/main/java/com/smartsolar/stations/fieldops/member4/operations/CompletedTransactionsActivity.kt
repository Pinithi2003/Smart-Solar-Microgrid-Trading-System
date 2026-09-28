package com.smartsolar.stations.fieldops.member4.operations

import android.content.Intent
import com.smartsolar.stations.fieldops.member4.qrverification.TransactionVerifyActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.smartsolar.stations.R
import com.smartsolar.stations.models.Booking
import com.smartsolar.stations.services.QrService
import com.smartsolar.stations.fieldops.member4.operations.CompletedTransactionAdapter
import com.smartsolar.stations.ui.common.NavigationHelper

/**
 * Screen displaying the official audit ledger of completed energy transfers (Member 4).
 */
class CompletedTransactionsActivity : AppCompatActivity() {

    private lateinit var adapter: CompletedTransactionAdapter
    private lateinit var emptyText: TextView
    private var allTransactions: List<Booking> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_completed_transactions)

        findViewById<TextView>(R.id.headerTitle).text = getString(R.string.action_completed_transactions)
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        emptyText = findViewById(R.id.emptyTxnText)
        val recyclerView = findViewById<RecyclerView>(R.id.completedTxnRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = CompletedTransactionAdapter(emptyList()) { booking ->
            val intent = Intent(this, TransactionVerifyActivity::class.java).apply {
                putExtra("EXTRA_SCANNED_CODE", booking.transactionId ?: booking.reservationId)
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        val searchInput = findViewById<TextInputEditText>(R.id.txnSearchInput)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterTransactions(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        NavigationHelper.setupOperatorBottomNav(this, NavigationHelper.OperatorTab.TRANSFERS)
    }

    override fun onResume() {
        super.onResume()
        allTransactions = QrService.getCompletedTransactions(this)
        filterTransactions(findViewById<TextInputEditText>(R.id.txnSearchInput).text.toString())
    }

    private fun filterTransactions(query: String) {
        val filtered = if (query.isBlank()) {
            allTransactions
        } else {
            val q = query.trim().lowercase()
            allTransactions.filter {
                (it.transactionId ?: "").lowercase().contains(q) ||
                it.reservationId.lowercase().contains(q) ||
                it.stationName.lowercase().contains(q) ||
                it.prosumerName.lowercase().contains(q)
            }
        }
        adapter.updateData(filtered)
        emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }
}

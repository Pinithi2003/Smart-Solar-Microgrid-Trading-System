package com.smartsolar.stations.ui.common

import android.app.Activity
import android.content.Intent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.smartsolar.stations.R
import com.smartsolar.stations.authentication.SessionManager
import com.smartsolar.stations.ui.LoginActivity
import com.smartsolar.stations.fieldops.member4.operations.CompletedTransactionsActivity
import com.smartsolar.stations.fieldops.member4.operations.OperatorDashboardActivity
import com.smartsolar.stations.fieldops.member4.qrscanner.ScanQrActivity

/**
 * Member 4 Navigation helper: manages Grid Operator bottom navigation tabs.
 */
object NavigationHelper {

    enum class OperatorTab { HOME, SCAN, TRANSFERS, PROFILE }

    fun setupOperatorBottomNav(activity: Activity, activeTab: OperatorTab) {
        val opNavHome = activity.findViewById<LinearLayout>(R.id.opNavHome) ?: return
        val opNavScan = activity.findViewById<LinearLayout>(R.id.opNavScan)
        val opNavTransfers = activity.findViewById<LinearLayout>(R.id.opNavTransfers)
        val opNavProfile = activity.findViewById<LinearLayout>(R.id.opNavProfile)

        highlightTab(activity, R.id.opIconHome, R.id.opTextHome, activeTab == OperatorTab.HOME)
        highlightTab(activity, R.id.opIconScan, R.id.opTextScan, activeTab == OperatorTab.SCAN)
        highlightTab(activity, R.id.opIconTransfers, R.id.opTextTransfers, activeTab == OperatorTab.TRANSFERS)
        highlightTab(activity, R.id.opIconProfile, R.id.opTextProfile, activeTab == OperatorTab.PROFILE)

        opNavHome.setOnClickListener {
            if (activeTab != OperatorTab.HOME) {
                activity.startActivity(Intent(activity, OperatorDashboardActivity::class.java))
                if (activity !is OperatorDashboardActivity) activity.finish()
            }
        }
        opNavScan?.setOnClickListener {
            if (activeTab != OperatorTab.SCAN) {
                activity.startActivity(Intent(activity, ScanQrActivity::class.java))
                if (activity !is OperatorDashboardActivity) activity.finish()
            }
        }
        opNavTransfers?.setOnClickListener {
            if (activeTab != OperatorTab.TRANSFERS) {
                activity.startActivity(Intent(activity, CompletedTransactionsActivity::class.java))
                if (activity !is OperatorDashboardActivity) activity.finish()
            }
        }
        opNavProfile?.setOnClickListener {
            SessionManager(activity).clear()
            val intent = Intent(activity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            activity.startActivity(intent)
            activity.finish()
        }
    }

    private fun highlightTab(activity: Activity, iconId: Int, textId: Int, isActive: Boolean) {
        val icon = activity.findViewById<ImageView>(iconId) ?: return
        val text = activity.findViewById<TextView>(textId) ?: return
        val activeColor = ContextCompat.getColor(activity, R.color.solar_green)
        val inactiveColor = ContextCompat.getColor(activity, R.color.solar_muted)

        if (isActive) {
            icon.setColorFilter(activeColor)
            text.setTextColor(activeColor)
        } else {
            icon.setColorFilter(inactiveColor)
            text.setTextColor(inactiveColor)
        }
    }
}

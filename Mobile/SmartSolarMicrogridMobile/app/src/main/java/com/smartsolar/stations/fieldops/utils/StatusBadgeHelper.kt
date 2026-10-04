package com.smartsolar.stations.utils

import android.widget.TextView
import androidx.core.content.ContextCompat
import com.smartsolar.stations.R

/**
 * Visual styling helper for booking and transaction status badges.
 */
object StatusBadgeHelper {

    fun applyBadge(badgeView: TextView, status: String) {
        val context = badgeView.context
        badgeView.text = status.uppercase()

        when (status.uppercase()) {
            "PENDING" -> {
                badgeView.setBackgroundResource(R.drawable.bg_badge_pending)
                badgeView.setTextColor(ContextCompat.getColor(context, R.color.status_pending))
            }
            "APPROVED" -> {
                badgeView.setBackgroundResource(R.drawable.bg_badge_approved)
                badgeView.setTextColor(ContextCompat.getColor(context, R.color.status_approved))
            }
            "COMPLETED" -> {
                badgeView.setBackgroundResource(R.drawable.bg_badge_completed)
                badgeView.setTextColor(ContextCompat.getColor(context, R.color.status_completed))
            }
            "CANCELLED" -> {
                badgeView.setBackgroundResource(R.drawable.bg_badge_cancelled)
                badgeView.setTextColor(ContextCompat.getColor(context, R.color.status_cancelled))
            }
            else -> {
                badgeView.setBackgroundResource(R.drawable.bg_status_pill)
                badgeView.setTextColor(ContextCompat.getColor(context, R.color.solar_text))
            }
        }
    }
}

package com.smartsolar.stations.stations.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.smartsolar.stations.R
import com.smartsolar.stations.auth.ui.HomeActivity
import com.smartsolar.stations.auth.ui.ProfileActivity
import com.smartsolar.stations.fieldops.member4.operations.OperatorDashboardActivity

// Member 2 shared demo chrome for the Stitch theme.
// Shared station chrome with Home, bookings, QR and profile destinations.
// Bookings, QR and Profile are connected to their respective member modules.
object MemberNav {

    const val HOME = 0
    const val MAP = 1

    // Header with brand title.
    // Bell and avatar are connected to Member 1 profile functionality.
    fun bindHeader(activity: Activity, title: String) {

        activity.findViewById<TextView>(R.id.header_title)?.text = title

        activity.findViewById<View>(R.id.header_bell)?.setOnClickListener {
            Toast.makeText(
                activity,
                "Notifications arrive with Member 1.",
                Toast.LENGTH_SHORT
            ).show()
        }

        activity.findViewById<View>(R.id.header_avatar)?.setOnClickListener {
            activity.startActivity(
                Intent(activity, ProfileActivity::class.java)
            )
        }
    }

    // Demo filter chips - visual toggles only, no backend effect.
    fun bindChips(activity: Activity) {

        val ids = listOf(
            R.id.chip_dist,
            R.id.chip_sat,
            R.id.chip_bi,
            R.id.chip_tier
        )

        ids.forEach { id ->

            activity.findViewById<TextView>(id)?.setOnClickListener { chip ->

                chip.isSelected = !chip.isSelected

            }
        }
    }

    // Bottom navigation.
    // Selected tab gets the green pill, others stay plain.
    fun bindBottomNav(
        activity: Activity,
        selected: Int
    ) {

        // -----------------------------
        // HOME
        // -----------------------------

        styleTab(
            activity,
            R.id.nav_home,
            R.id.nav_home_icon,
            R.id.nav_home_label,
            selected == HOME
        )

        // -----------------------------
        // MAP
        // -----------------------------

        styleTab(
            activity,
            R.id.nav_map,
            R.id.nav_map_icon,
            R.id.nav_map_label,
            selected == MAP
        )

        // -----------------------------
        // BOOKINGS - MEMBER 3
        // -----------------------------

        styleTab(
            activity,
            R.id.nav_bookings,
            R.id.nav_bookings_icon,
            R.id.nav_bookings_label,
            false
        )

        // -----------------------------
        // QR - MEMBER 4
        // -----------------------------

        styleTab(
            activity,
            R.id.nav_qr,
            R.id.nav_qr_icon,
            R.id.nav_qr_label,
            false
        )

        // -----------------------------
        // PROFILE - MEMBER 1
        // -----------------------------

        styleTab(
            activity,
            R.id.nav_profile,
            R.id.nav_profile_icon,
            R.id.nav_profile_label,
            false
        )

        // ==========================================
        // HOME
        // ==========================================

        activity.findViewById<View>(R.id.nav_home)?.setOnClickListener {

            if (activity !is HomeActivity) {

                activity.startActivity(
                    Intent(
                        activity,
                        HomeActivity::class.java
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                )
                activity.finish()
            }
        }

        // ==========================================
        // MAP
        // ==========================================

        activity.findViewById<View>(R.id.nav_map)?.setOnClickListener {

            if (activity !is StationDetailActivity) {

                activity.startActivity(
                    Intent(
                        activity,
                        StationDetailActivity::class.java
                    )
                )
            }
        }

        // ==========================================
        // BOOKINGS - MEMBER 3
        // ==========================================

        activity.findViewById<View>(R.id.nav_bookings)?.setOnClickListener {

            Toast.makeText(
                activity,
                "Bookings module (Member 3) plugs in here.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // ==========================================
        // QR / FIELD OPERATIONS - MEMBER 4
        // ==========================================

        activity.findViewById<View>(R.id.nav_qr)?.setOnClickListener {

            if (activity !is OperatorDashboardActivity) {

                activity.startActivity(
                    Intent(
                        activity,
                        OperatorDashboardActivity::class.java
                    )
                )
            }
        }

        // ==========================================
        // PROFILE - MEMBER 1
        // ==========================================

        activity.findViewById<View>(R.id.nav_profile)?.setOnClickListener {

            activity.startActivity(
                Intent(
                    activity,
                    ProfileActivity::class.java
                )
            )
        }
    }

    // ==========================================
    // TAB STYLE
    // ==========================================

    private fun styleTab(
        activity: Activity,
        tab: Int,
        icon: Int,
        label: Int,
        on: Boolean
    ) {

        val tabView =
            activity.findViewById<LinearLayout>(tab)
                ?: return

        val iconView =
            activity.findViewById<ImageView>(icon)

        val labelView =
            activity.findViewById<TextView>(label)

        if (on) {

            tabView.setBackgroundResource(
                R.drawable.bg_nav_on
            )

            iconView?.setColorFilter(
                Color.WHITE
            )

            labelView?.setTextColor(
                Color.WHITE
            )

        } else {

            tabView.setBackgroundResource(
                0
            )

            iconView?.setColorFilter(
                Color.parseColor("#0E3B22")
            )

            labelView?.setTextColor(
                Color.parseColor("#0E3B22")
            )
        }
    }

    // ==========================================
    // STATUS PILL
    // ==========================================

    fun stylePill(
        pill: TextView,
        status: String
    ) {

        pill.text = status.uppercase()

        val colors = when (status) {

            "Active" ->
                Pair(
                    "#DCFCE7",
                    "#15803D"
                )

            "Maintenance" ->
                Pair(
                    "#FEF3C7",
                    "#B45309"
                )

            else ->
                Pair(
                    "#FEE2E2",
                    "#B91C1C"
                )
        }

        pill.background
            ?.mutate()
            ?.setTint(
                Color.parseColor(
                    colors.first
                )
            )

        pill.setTextColor(
            Color.parseColor(
                colors.second
            )
        )
    }
}
package com.smartsolar.stations.infrastructure

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.smartsolar.stations.R
import com.smartsolar.stations.auth.data.SessionManager
import com.smartsolar.stations.auth.ui.HomeActivity
import com.smartsolar.stations.auth.ui.LoginActivity
import com.smartsolar.stations.auth.ui.ProfileActivity
import com.smartsolar.stations.fieldops.member4.qrscanner.ScanQrActivity
import com.smartsolar.stations.fieldops.member4.qrscanner.TransactionQrActivity
import com.smartsolar.stations.fieldops.member4.operations.OperatorDashboardActivity
import com.smartsolar.stations.stations.ui.StationDetailActivity

/**
 * Shared navigation for the Smart Solar Microgrid mobile application.
 *
 * Home     -> Common application home
 * Map      -> Member 2 Solar Stations
 * Bookings -> Member 3 Reservations
 * QR       -> Member 4 QR / Field Operations
 * Profile  -> Member 1 Account Management
 */
object MemberNav {

    const val HOME = 0
    const val MAP = 1
    const val BOOKINGS = 2
    const val QR = 3
    const val PROFILE = 4

    /**
     * Configure the common application header.
     */
    fun bindHeader(
        activity: Activity,
        title: String
    ) {

        activity.findViewById<TextView>(
            R.id.header_title
        )?.text = title

        /*
         * Notification button
         */
        activity.findViewById<View>(
            R.id.header_bell
        )?.setOnClickListener {

            Toast.makeText(
                activity,
                "Notifications will be available here.",
                Toast.LENGTH_SHORT
            ).show()
        }

        /*
         * Profile / avatar button
         */
        activity.findViewById<View>(
            R.id.header_avatar
        )?.setOnClickListener {

            activity.startActivity(
                Intent(
                    activity,
                    ProfileActivity::class.java
                )
            )
        }
    }

    /**
     * Configure the filter chips used by Member 2.
     */
    fun bindChips(activity: Activity) {

        val ids = listOf(
            R.id.chip_dist,
            R.id.chip_sat,
            R.id.chip_bi,
            R.id.chip_tier
        )

        ids.forEach { id ->

            activity.findViewById<TextView>(
                id
            )?.setOnClickListener { chip ->

                chip.isSelected =
                    !chip.isSelected
            }
        }
    }

    /**
     * Configure the common bottom navigation.
     */
    fun bindBottomNav(
        activity: Activity,
        selected: Int
    ) {

        /*
         * ============================
         * SELECTED TAB
         * ============================
         */

        styleTab(
            activity,
            R.id.nav_home,
            R.id.nav_home_icon,
            R.id.nav_home_label,
            selected == HOME
        )

        styleTab(
            activity,
            R.id.nav_map,
            R.id.nav_map_icon,
            R.id.nav_map_label,
            selected == MAP
        )

        styleTab(
            activity,
            R.id.nav_bookings,
            R.id.nav_bookings_icon,
            R.id.nav_bookings_label,
            selected == BOOKINGS
        )

        styleTab(
            activity,
            R.id.nav_qr,
            R.id.nav_qr_icon,
            R.id.nav_qr_label,
            selected == QR
        )

        styleTab(
            activity,
            R.id.nav_profile,
            R.id.nav_profile_icon,
            R.id.nav_profile_label,
            selected == PROFILE
        )


        /*
         * ============================
         * HOME
         * ============================
         *
         * Any screen -> Home
         *
         * Home is the common application
         * landing page after login.
         */
        activity.findViewById<View>(
            R.id.nav_home
        )?.setOnClickListener {

            if (activity !is HomeActivity) {

                val intent = Intent(
                    activity,
                    HomeActivity::class.java
                ).apply {

                    flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                }

                activity.startActivity(intent)
            }
        }


        /*
         * ============================
         * MAP
         * ============================
         *
         * Opens Member 2 Solar Station
         * module.
         */
        activity.findViewById<View>(
            R.id.nav_map
        )?.setOnClickListener {

            if (activity !is StationDetailActivity) {

                val intent = Intent(
                    activity,
                    StationDetailActivity::class.java
                )

                activity.startActivity(intent)
            }
        }


        /*
         * ============================
         * BOOKINGS
         * ============================
         *
         * Member 3 will connect the
         * real reservation Activity here.
         */
        activity.findViewById<View>(
            R.id.nav_bookings
        )?.setOnClickListener {

            Toast.makeText(
                activity,
                "Bookings module is being connected.",
                Toast.LENGTH_SHORT
            ).show()
        }


        /*
         * ============================
         * QR
         * ============================
         *
         * Grid Operator
         *      -> Scan QR
         *
         * Prosumer
         *      -> Transaction QR
         */
        activity.findViewById<View>(
            R.id.nav_qr
        )?.setOnClickListener {
            if (activity !is OperatorDashboardActivity) {
                activity.startActivity(
                    Intent(
                        activity,
                        OperatorDashboardActivity::class.java
                    )
                )
            }
        }


        /*
         * ============================
         * PROFILE
         * ============================
         *
         * Opens Member 1 Profile.
         */
        activity.findViewById<View>(
            R.id.nav_profile
        )?.setOnClickListener {

            activity.startActivity(
                Intent(
                    activity,
                    ProfileActivity::class.java
                )
            )
        }
    }


    /**
     * Apply selected / unselected
     * navigation tab appearance.
     */
    private fun styleTab(
        activity: Activity,
        tab: Int,
        icon: Int,
        label: Int,
        on: Boolean
    ) {

        val tabView =
            activity.findViewById<LinearLayout>(
                tab
            ) ?: return

        val iconView =
            activity.findViewById<ImageView>(
                icon
            )

        val labelView =
            activity.findViewById<TextView>(
                label
            )

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

            tabView.setBackgroundResource(0)

            iconView?.setColorFilter(
                Color.parseColor(
                    "#0E3B22"
                )
            )

            labelView?.setTextColor(
                Color.parseColor(
                    "#0E3B22"
                )
            )
        }
    }


    /**
     * Apply status colour and text
     * to a station status pill.
     */
    fun stylePill(
        pill: TextView,
        status: String
    ) {

        pill.text =
            status.uppercase()

        val colors =
            when (status) {

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
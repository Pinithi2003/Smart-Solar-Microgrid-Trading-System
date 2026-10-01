package com.smartsolar.stations.auth.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.smartsolar.stations.R
import com.smartsolar.stations.auth.data.SessionManager
import com.smartsolar.stations.fieldops.member4.operations.OperatorDashboardActivity
import com.smartsolar.stations.infrastructure.MemberNav
import com.smartsolar.stations.stations.ui.StationDetailActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Get the currently logged-in user
        val session = SessionManager(this).current()

        // Home user information
        val userName =
            findViewById<TextView>(R.id.homeUserName)

        val userRole =
            findViewById<TextView>(R.id.homeUserRole)

        userName.text =
            session?.fullName ?: "Solar Prosumer"

        userRole.text =
            session?.role ?: "Prosumer"

        // Common header
        MemberNav.bindHeader(
            this,
            "Home"
        )

        // Common bottom navigation
        MemberNav.bindBottomNav(
            this,
            MemberNav.HOME
        )

        // Member 2 - Solar Stations
        findViewById<TextView>(R.id.btnHomeStations)
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        StationDetailActivity::class.java
                    )
                )
            }

        // Member 4 - QR / Field Operations
        findViewById<TextView>(R.id.btnHomeQr)
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        OperatorDashboardActivity::class.java
                    )
                )
            }

        // Member 1 - Profile
        findViewById<TextView>(R.id.btnHomeProfile)
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        ProfileActivity::class.java
                    )
                )
            }
    }
}
package com.smartsolar.stations.core

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.smartsolar.stations.R
import com.smartsolar.stations.auth.data.SessionManager
import com.smartsolar.stations.auth.ui.LoginActivity
import com.smartsolar.stations.stations.ui.StationDetailActivity

/** Splash → Login (no session) or Map (session exists). Home page retired — Map only. */
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Handler(Looper.getMainLooper()).postDelayed({
            val session = SessionManager(this).current()
            val next = if (session == null) LoginActivity::class.java else StationDetailActivity::class.java
            startActivity(Intent(this, next))
            finish()
        }, 1500)
    }
}

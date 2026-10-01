package com.smartsolar.stations.shared.navigation

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.smartsolar.stations.R
import com.smartsolar.stations.auth.data.SessionManager
import com.smartsolar.stations.auth.ui.HomeActivity
import com.smartsolar.stations.auth.ui.LoginActivity

/** Splash → Login (no session) or Home (session exists). */
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Handler(Looper.getMainLooper()).postDelayed({
            val session = SessionManager(this).current()
            val next = if (session == null) LoginActivity::class.java else HomeActivity::class.java
            startActivity(Intent(this, next))
            finish()
        }, 1500)
    }
}

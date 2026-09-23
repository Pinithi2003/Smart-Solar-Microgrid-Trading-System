package com.smartsolar.stations.auth.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.smartsolar.stations.R
import com.smartsolar.stations.auth.data.SessionManager
import com.smartsolar.stations.auth.data.AuthService
import com.smartsolar.stations.core.MockData
import com.smartsolar.stations.auth.util.Validators
import com.smartsolar.stations.stations.ui.StationDetailActivity

/** Demo login — local only. Later calls POST /api/auth/login (Member 1). */
class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val nic = findViewById<TextInputEditText>(R.id.inputNic)
        val pwd = findViewById<TextInputEditText>(R.id.inputPassword)
        val err = findViewById<TextView>(R.id.errorText)
        val progress = findViewById<ProgressBar>(R.id.loginProgress)

        fun attempt(nicVal: String, pwdVal: String) {
            err.visibility = View.GONE
            Validators.requireLogin(nicVal, pwdVal)?.let {
                err.text = it; err.visibility = View.VISIBLE; return
            }
            progress.visibility = View.VISIBLE
            // Simulate network latency for loading-state demo.
            nic.postDelayed({
                progress.visibility = View.GONE
                AuthService.demoLogin(nicVal, pwdVal)
                    .onSuccess {
                        SessionManager(this).save(it)
                        startActivity(Intent(this, StationDetailActivity::class.java))
                        finish()
                    }
                    .onFailure { err.text = getString(R.string.error_invalid_credentials); err.visibility = View.VISIBLE }
            }, 600)
        }

        findViewById<Button>(R.id.loginButton).setOnClickListener {
            attempt(nic.text.toString(), pwd.text.toString())
        }
        findViewById<Button>(R.id.demoProsumerButton).setOnClickListener {
            attempt(MockData.prosumer.nic, MockData.DEMO_PASSWORD)
        }
        findViewById<Button>(R.id.demoOperatorButton).setOnClickListener {
            attempt(MockData.operator.nic, MockData.DEMO_PASSWORD)
        }
    }
}

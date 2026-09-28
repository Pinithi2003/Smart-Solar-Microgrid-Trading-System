package com.smartsolar.stations.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.smartsolar.stations.R
import com.smartsolar.stations.services.AuthService
import com.smartsolar.stations.fieldops.member4.operations.OperatorDashboardActivity
import com.smartsolar.stations.utils.Validators

/**
 * Login screen supporting Solar Prosumers and Grid Operators.
 * Redirects to Member 2 (StationsListActivity) or Member 4 (OperatorDashboardActivity).
 */
class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val inputNic = findViewById<TextInputEditText>(R.id.inputNic)
        val inputPassword = findViewById<TextInputEditText>(R.id.inputPassword)
        val errorText = findViewById<TextView>(R.id.errorText)
        val progress = findViewById<ProgressBar>(R.id.loginProgress)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val demoProsumerButton = findViewById<Button>(R.id.demoProsumerButton)
        val demoOperatorButton = findViewById<Button>(R.id.demoOperatorButton)

        fun executeLogin(nicVal: String, pwdVal: String) {
            errorText.visibility = View.GONE
            val validationError = Validators.requireLogin(nicVal, pwdVal)
            if (validationError != null) {
                errorText.text = validationError
                errorText.visibility = View.VISIBLE
                return
            }

            progress.visibility = View.VISIBLE
            loginButton.isEnabled = false

            inputNic.postDelayed({
                progress.visibility = View.GONE
                loginButton.isEnabled = true

                AuthService.login(this, nicVal, pwdVal)
                    .onSuccess { user ->
                        val intent = if (user.role.equals("GridOperator", ignoreCase = true)) {
                            Intent(this, OperatorDashboardActivity::class.java)
                        } else {
                            Intent(this, StationsListActivity::class.java)
                        }
                        startActivity(intent)
                        finish()
                    }
                    .onFailure { ex ->
                        errorText.text = ex.message ?: getString(R.string.error_invalid_credentials)
                        errorText.visibility = View.VISIBLE
                    }
            }, 400)
        }

        loginButton.setOnClickListener {
            executeLogin(inputNic.text.toString(), inputPassword.text.toString())
        }

        demoProsumerButton.setOnClickListener {
            inputNic.setText("200012345678")
            inputPassword.setText("123456")
            executeLogin("200012345678", "123456")
        }

        demoOperatorButton.setOnClickListener {
            inputNic.setText("199512345678")
            inputPassword.setText("123456")
            executeLogin("199512345678", "123456")
        }
    }
}

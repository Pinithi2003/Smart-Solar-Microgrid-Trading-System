package com.smartsolar.stations.auth.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.smartsolar.stations.R
import com.smartsolar.stations.auth.data.AuthService
import com.smartsolar.stations.auth.data.RetrofitClient
import com.smartsolar.stations.auth.data.SessionManager
import com.smartsolar.stations.auth.ui.HomeActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Retrofit with application context
        RetrofitClient.initialize(this)

        val email =
            findViewById<TextInputEditText>(R.id.inputEmail)

        val password =
            findViewById<TextInputEditText>(R.id.inputPassword)

        val errorText =
            findViewById<TextView>(R.id.errorText)

        val progress =
            findViewById<ProgressBar>(R.id.loginProgress)

        val loginButton =
            findViewById<Button>(R.id.loginButton)

        // Register button
        val registerButton =
            findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {

            startActivity(
                Intent(
                    this@LoginActivity,
                    RegisterActivity::class.java
                )
            )
        }

        loginButton.setOnClickListener {

            val emailValue =
                email.text?.toString()?.trim().orEmpty()

            val passwordValue =
                password.text?.toString().orEmpty()

            errorText.visibility = View.GONE

            // Validate email
            if (emailValue.isEmpty()) {
                errorText.text = "Email is required."
                errorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Validate password
            if (passwordValue.isEmpty()) {
                errorText.text = "Password is required."
                errorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            loginButton.isEnabled = false
            progress.visibility = View.VISIBLE

            lifecycleScope.launch {

                try {

                    val result = AuthService.login(
                        this@LoginActivity,
                        emailValue,
                        passwordValue
                    )

                    result
                        .onSuccess { user ->

                            // Save logged-in user and JWT token locally
                            SessionManager(this@LoginActivity)
                                .save(user)

                            // Open the common Home screen
                            startActivity(
                                Intent(
                                    this@LoginActivity,
                                    HomeActivity::class.java
                                )
                            )

                            // Remove LoginActivity from back stack
                            finish()
                        }

                        .onFailure { error ->

                            errorText.text =
                                error.message ?: "Login failed."

                            errorText.visibility =
                                View.VISIBLE
                        }

                } catch (e: Exception) {

                    errorText.text =
                        "Unexpected error: ${
                            e.message ?: e.javaClass.simpleName
                        }"

                    errorText.visibility =
                        View.VISIBLE

                } finally {

                    progress.visibility =
                        View.GONE

                    loginButton.isEnabled =
                        true
                }
            }
        }
    }
}
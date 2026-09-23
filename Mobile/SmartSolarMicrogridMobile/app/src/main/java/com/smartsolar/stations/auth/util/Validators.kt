package com.smartsolar.stations.auth.util

import android.util.Patterns

/** Simple field validation with user-friendly messages (no technical leaks). */
object Validators {
    fun requireLogin(nic: String, password: String): String? {
        if (nic.isBlank() || password.isBlank()) return "Please enter all required fields."
        return null
    }

    fun isValidEmail(email: String): Boolean =
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

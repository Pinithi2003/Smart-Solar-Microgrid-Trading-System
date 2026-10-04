package com.smartsolar.stations.auth.data

import android.content.Context
import com.smartsolar.stations.auth.model.LocalUser

class SessionManager(context: Context) {

    private val prefs =
        context.getSharedPreferences(
            "solar_session",
            Context.MODE_PRIVATE
        )

    fun save(user: LocalUser) {

        // Save only the raw JWT token.
        // Do NOT save "Bearer " together with the token.
        val cleanToken = user.token
            .removePrefix("Bearer ")
            .trim()

        prefs.edit()
            .putString("id", user.id)
            .putString("nic", user.nic)
            .putString("fullName", user.fullName)
            .putString("email", user.email)
            .putString("phone", user.phone)
            .putString("role", user.role)
            .putString("status", user.status)
            .putBoolean("isActive", user.isActive)
            .putString("token", cleanToken)
            .apply()
    }

    fun current(): LocalUser? {

        val id =
            prefs.getString("id", null)
                ?: return null

        val nic =
            prefs.getString("nic", "")
                ?: ""

        val fullName =
            prefs.getString("fullName", "")
                ?: ""

        val email =
            prefs.getString("email", "")
                ?: ""

        val phone =
            prefs.getString("phone", "")
                ?: ""

        val role =
            prefs.getString("role", "Prosumer")
                ?: "Prosumer"

        val status =
            prefs.getString("status", "")
                ?: ""

        val isActive =
            prefs.getBoolean(
                "isActive",
                false
            )

        val token =
            prefs.getString("token", "")
                ?.removePrefix("Bearer ")
                ?.trim()
                ?: ""

        return LocalUser(
            id = id,
            nic = nic,
            fullName = fullName,
            email = email,
            phone = phone,
            role = role,
            status = status,
            isActive = isActive,
            token = token
        )
    }

    fun clear() {

        prefs.edit()
            .clear()
            .apply()
    }
}
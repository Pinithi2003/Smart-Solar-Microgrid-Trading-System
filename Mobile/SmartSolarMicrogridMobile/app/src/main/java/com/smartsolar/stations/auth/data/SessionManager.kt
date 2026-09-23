package com.smartsolar.stations.auth.data

import android.content.Context
import com.smartsolar.stations.auth.model.LocalUser

/**
 * Local session only (SharedPreferences). Token is a demo placeholder;
 * real JWT arrives later from POST /api/auth/login (Member 1).
 */
class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("solar_session", Context.MODE_PRIVATE)

    fun save(user: LocalUser) {
        prefs.edit()
            .putString("nic", user.nic)
            .putString("name", user.name)
            .putString("email", user.email)
            .putString("role", user.role)
            .putString("token", user.token)
            .apply()
    }

    fun current(): LocalUser? {
        val nic = prefs.getString("nic", null) ?: return null
        return LocalUser(
            nic = nic,
            name = prefs.getString("name", "") ?: "",
            email = prefs.getString("email", "") ?: "",
            role = prefs.getString("role", "Prosumer") ?: "Prosumer",
            token = prefs.getString("token", "demo-token") ?: "demo-token"
        )
    }

    fun clear() = prefs.edit().clear().apply()
}

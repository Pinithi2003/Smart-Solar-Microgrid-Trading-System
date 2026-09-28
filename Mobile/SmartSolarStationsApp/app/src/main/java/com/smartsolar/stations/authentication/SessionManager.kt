package com.smartsolar.stations.authentication

import android.content.Context
import com.smartsolar.stations.models.LocalUser

/**
 * Local session management using SharedPreferences.
 * Stores authenticated user details and active role.
 */
class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("solar_session", Context.MODE_PRIVATE)

    fun save(user: LocalUser) {
        prefs.edit()
            .putLong("id", user.id)
            .putString("nic", user.nic)
            .putString("name", user.name)
            .putString("email", user.email)
            .putString("phone", user.phone)
            .putString("role", user.role)
            .putString("token", user.token)
            .putBoolean("isActive", user.isActive)
            .apply()
    }

    fun current(): LocalUser? {
        val nic = prefs.getString("nic", null) ?: return null
        return LocalUser(
            id = prefs.getLong("id", 1L),
            nic = nic,
            name = prefs.getString("name", "") ?: "",
            email = prefs.getString("email", "") ?: "",
            phone = prefs.getString("phone", "+94 77 123 4567") ?: "+94 77 123 4567",
            role = prefs.getString("role", "Prosumer") ?: "Prosumer",
            token = prefs.getString("token", "demo-token") ?: "demo-token",
            isActive = prefs.getBoolean("isActive", true)
        )
    }

    fun isProsumer(): Boolean = current()?.role == "Prosumer"

    fun isOperator(): Boolean = current()?.role == "GridOperator"

    fun clear() = prefs.edit().clear().apply()
}

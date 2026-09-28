package com.smartsolar.stations.services

import android.content.Context
import com.smartsolar.stations.authentication.SessionManager
import com.smartsolar.stations.database.SmartSolarDbHelper
import com.smartsolar.stations.models.LocalUser

/**
 * Authentication service handling login and registration.
 * Operates against SQLite database for demo offline-first execution,
 * with structured hooks ready for POST /api/auth/login and POST /api/auth/register.
 */
object AuthService {

    fun login(context: Context, nicOrEmail: String, password: String): Result<LocalUser> {
        val trimmedIdentifier = nicOrEmail.trim()
        val db = SmartSolarDbHelper(context)

        val user = db.getUserByNic(trimmedIdentifier) ?: db.getUserByEmail(trimmedIdentifier)
        if (user == null || user.password != password) {
            return Result.failure(Exception("Invalid NIC or password."))
        }

        if (!user.isActive) {
            return Result.failure(Exception("This account has been deactivated. Please contact support."))
        }

        SessionManager(context).save(user)
        return Result.success(user)
    }

    fun register(
        context: Context,
        nic: String,
        name: String,
        email: String,
        phone: String,
        password: String
    ): Result<LocalUser> {
        val db = SmartSolarDbHelper(context)

        if (db.getUserByNic(nic.trim()) != null) {
            return Result.failure(Exception("An account with this NIC already exists."))
        }
        if (db.getUserByEmail(email.trim()) != null) {
            return Result.failure(Exception("An account with this email address already exists."))
        }

        val newUser = LocalUser(
            nic = nic.trim(),
            name = name.trim(),
            email = email.trim(),
            phone = phone.trim(),
            role = "Prosumer",
            password = password,
            isActive = true,
            token = "jwt-token-demo-${System.currentTimeMillis()}"
        )

        val success = db.insertUser(newUser)
        return if (success) {
            Result.success(newUser)
        } else {
            Result.failure(Exception("Failed to register account. Please try again."))
        }
    }

    fun logout(context: Context) {
        SessionManager(context).clear()
    }
}

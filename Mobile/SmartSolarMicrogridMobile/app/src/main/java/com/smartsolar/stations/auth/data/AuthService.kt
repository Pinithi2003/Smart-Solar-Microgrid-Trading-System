package com.smartsolar.stations.auth.data

import com.smartsolar.stations.auth.model.LocalUser
import com.smartsolar.stations.core.MockData

/**
 * STUB — Member 1 owns real auth (POST /api/auth/login, /api/auth/register).
 * Member 2 keeps only demo login so the stations flow runs standalone.
 */
object AuthService {
    fun demoLogin(nic: String, password: String): Result<LocalUser> {
        if (password != MockData.DEMO_PASSWORD) return Result.failure(Exception("Invalid NIC or password."))
        return when (nic.trim()) {
            MockData.prosumer.nic, MockData.prosumer.email -> Result.success(MockData.prosumer)
            MockData.operator.nic, MockData.operator.email -> Result.success(MockData.operator)
            else -> Result.failure(Exception("Invalid NIC or password."))
        }
    }
}

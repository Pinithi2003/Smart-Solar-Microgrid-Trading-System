package com.smartsolar.stations.auth.data

import android.content.Context
import android.util.Log
import com.smartsolar.stations.auth.model.LocalUser
import com.smartsolar.stations.auth.model.LoginRequest
import com.smartsolar.stations.auth.model.RegisterRequest
import com.smartsolar.stations.auth.model.RegisterResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

object AuthService {

    private const val TAG = "SmartSolarAuth"

    // ============================================================
    // LOGIN
    // ============================================================

    suspend fun login(
        context: Context,
        email: String,
        password: String
    ): Result<LocalUser> {

        return withContext(Dispatchers.IO) {

            try {

                Log.d(TAG, "================================")
                Log.d(TAG, "Starting login request")
                Log.d(TAG, "Email: $email")
                Log.d(TAG, "Calling: POST api/Auth/login")
                Log.d(TAG, "================================")

                val response = RetrofitClient.api.login(
                    LoginRequest(
                        email = email,
                        password = password
                    )
                )

                Log.d(TAG, "Login request SUCCESS")
                Log.d(TAG, "Message: ${response.message}")
                Log.d(TAG, "User ID: ${response.user.id}")
                Log.d(TAG, "User NIC: ${response.user.nic}")
                Log.d(TAG, "User Name: ${response.user.fullName}")
                Log.d(TAG, "User Email: ${response.user.email}")
                Log.d(TAG, "User Phone: ${response.user.phone}")
                Log.d(TAG, "User Role: ${response.user.role}")
                Log.d(TAG, "User Status: ${response.user.status}")
                Log.d(TAG, "User Active: ${response.user.isActive}")

                // Do not print the actual JWT token.
                Log.d(
                    TAG,
                    "Token received: ${response.token.isNotBlank()}"
                )

                val user = LocalUser(
                    id = response.user.id,
                    nic = response.user.nic,
                    fullName = response.user.fullName,
                    email = response.user.email,
                    phone = response.user.phone,
                    role = response.user.role,
                    status = response.user.status,
                    isActive = response.user.isActive,
                    token = response.token
                )

                val databaseHelper =
                    LocalDatabaseHelper(context)

                databaseHelper.saveUser(user)

                databaseHelper.close()

                Log.d(
                    TAG,
                    "User session saved locally."
                )

                Result.success(user)

            } catch (e: HttpException) {

                Log.e(TAG, "================================")
                Log.e(TAG, "LOGIN HTTP ERROR")
                Log.e(TAG, "HTTP CODE: ${e.code()}")
                Log.e(TAG, "HTTP MESSAGE: ${e.message()}")

                val errorBody = try {
                    e.response()
                        ?.errorBody()
                        ?.string()
                } catch (ex: Exception) {
                    null
                }

                Log.e(
                    TAG,
                    "HTTP ERROR BODY: ${
                        errorBody ?: "No error body returned"
                    }"
                )

                Log.e(TAG, "================================")

                Result.failure(
                    Exception(
                        "Server error: HTTP ${e.code()} - ${
                            errorBody ?: e.message()
                        }"
                    )
                )

            } catch (e: IOException) {

                Log.e(TAG, "================================")
                Log.e(TAG, "LOGIN NETWORK ERROR")
                Log.e(
                    TAG,
                    "Exception type: ${e.javaClass.name}"
                )
                Log.e(
                    TAG,
                    "Exception message: ${e.message}"
                )
                Log.e(TAG, "================================")

                Result.failure(
                    Exception(
                        "Network error: ${
                            e.message ?: "Cannot connect to server."
                        }"
                    )
                )

            } catch (e: Exception) {

                Log.e(TAG, "================================")
                Log.e(TAG, "UNKNOWN LOGIN ERROR")
                Log.e(
                    TAG,
                    "Exception type: ${e.javaClass.name}"
                )
                Log.e(
                    TAG,
                    "Exception message: ${e.message}"
                )
                Log.e(TAG, "================================")

                Result.failure(
                    Exception(
                        "Login error: ${
                            e.message ?: "Unknown error"
                        }"
                    )
                )
            }
        }
    }

    // ============================================================
    // REGISTER
    // ============================================================

    suspend fun register(
        context: Context,
        nic: String,
        fullName: String,
        email: String,
        phone: String,
        password: String
    ): Result<RegisterResponse> {

        return withContext(Dispatchers.IO) {

            try {

                Log.d(TAG, "================================")
                Log.d(TAG, "Starting registration request")
                Log.d(TAG, "NIC: $nic")
                Log.d(TAG, "Email: $email")
                Log.d(TAG, "Calling: POST api/Auth/register")
                Log.d(TAG, "================================")

                val response = RetrofitClient.api.register(
                    RegisterRequest(
                        nic = nic,
                        fullName = fullName,
                        email = email,
                        phone = phone,
                        password = password
                    )
                )

                Log.d(TAG, "Registration SUCCESS")
                Log.d(TAG, "Message: ${response.message}")
                Log.d(TAG, "Registered NIC: ${response.user.nic}")
                Log.d(TAG, "Registered Email: ${response.user.email}")
                Log.d(TAG, "Role: ${response.user.role}")
                Log.d(TAG, "Status: ${response.user.status}")
                Log.d(TAG, "Active: ${response.user.isActive}")

                Result.success(response)

            } catch (e: HttpException) {

                val errorMessage = when (e.code()) {

                    400 ->
                        "Invalid registration details."

                    409 ->
                        "NIC or email already exists."

                    else ->
                        "Registration failed. HTTP ${e.code()}"
                }

                Log.e(
                    TAG,
                    errorMessage,
                    e
                )

                Result.failure(
                    Exception(errorMessage)
                )

            } catch (e: IOException) {

                Log.e(
                    TAG,
                    "Network error during registration",
                    e
                )

                Result.failure(
                    Exception(
                        "Cannot connect to the server."
                    )
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Registration error",
                    e
                )

                Result.failure(
                    Exception(
                        e.message ?: "Registration failed."
                    )
                )
            }
        }
    }

    // ============================================================
    // GET SAVED USER
    // ============================================================

    fun getSavedUser(
        context: Context
    ): LocalUser? {

        val databaseHelper =
            LocalDatabaseHelper(context)

        val user =
            databaseHelper.getUser()

        databaseHelper.close()

        return user
    }

    // ============================================================
    // LOGOUT
    // ============================================================

    fun logout(
        context: Context
    ) {

        val databaseHelper =
            LocalDatabaseHelper(context)

        databaseHelper.clearUser()

        databaseHelper.close()

        Log.d(
            TAG,
            "Local user session cleared successfully."
        )
    }
}
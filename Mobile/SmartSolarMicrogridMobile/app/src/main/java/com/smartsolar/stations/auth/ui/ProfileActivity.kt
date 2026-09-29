package com.smartsolar.stations.auth.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.smartsolar.stations.R
import com.smartsolar.stations.auth.data.RetrofitClient
import com.smartsolar.stations.auth.data.SessionManager
import com.smartsolar.stations.auth.data.UpdateProfileRequest
import com.smartsolar.stations.auth.model.LocalUser
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ProfileActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    private lateinit var profileName: TextView
    private lateinit var profileEmail: TextView
    private lateinit var profilePhone: TextView
    private lateinit var profileRole: TextView
    private lateinit var profileStatus: TextView
    private lateinit var profileId: TextView

    companion object {
        private const val TAG = "ProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_profile)

        sessionManager = SessionManager(this)

        profileName = findViewById(R.id.profileName)
        profileEmail = findViewById(R.id.profileEmail)
        profilePhone = findViewById(R.id.profilePhone)
        profileRole = findViewById(R.id.profileRole)
        profileStatus = findViewById(R.id.profileStatus)
        profileId = findViewById(R.id.profileId)

        val editProfileButton =
            findViewById<Button>(R.id.editProfileButton)

        val deactivateButton =
            findViewById<Button>(R.id.deactivateButton)

        val logoutButton =
            findViewById<Button>(R.id.logoutButton)

        loadProfile()

        editProfileButton.setOnClickListener {
            showEditProfileDialog()
        }

        deactivateButton.setOnClickListener {
            showDeactivationConfirmation()
        }

        logoutButton.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    /**
     * Loads the currently logged-in user's profile.
     */
    private fun loadProfile() {

        val user = sessionManager.current()

        if (user == null) {

            profileName.text = "Not logged in"
            profileEmail.text = "-"
            profilePhone.text = "-"
            profileRole.text = "-"
            profileStatus.text = "-"
            profileId.text = "-"

            return
        }

        displayUser(user)

        lifecycleScope.launch {

            try {

                val token = buildAuthorizationHeader(user.token)

                Log.d(TAG, "Loading profile from API")
                Log.d(TAG, "Token exists: ${user.token.isNotBlank()}")

                val response =
                    RetrofitClient.api.getMyProfile(token)

                profileName.text = response.fullName
                profileEmail.text = response.email
                profilePhone.text = response.phone
                profileRole.text = response.role
                profileStatus.text = response.status
                profileId.text = response.nic

                val updatedUser = user.copy(
                    nic = response.nic,
                    fullName = response.fullName,
                    email = response.email,
                    phone = response.phone,
                    role = response.role,
                    status = response.status,
                    isActive = response.isActive
                )

                sessionManager.save(updatedUser)

                Log.d(TAG, "Profile loaded successfully")

            } catch (e: HttpException) {

                Log.e(
                    TAG,
                    "Profile loading HTTP error: ${e.code()}",
                    e
                )

                if (e.code() == 401) {

                    Toast.makeText(
                        this@ProfileActivity,
                        "Session expired. Please login again.",
                        Toast.LENGTH_LONG
                    ).show()

                } else {

                    Toast.makeText(
                        this@ProfileActivity,
                        "Could not load profile. Server error ${e.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Profile loading error",
                    e
                )

                Toast.makeText(
                    this@ProfileActivity,
                    "Could not refresh profile from server.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Displays user information on the profile screen.
     */
    private fun displayUser(user: LocalUser) {

        profileName.text = user.fullName
        profileEmail.text = user.email
        profilePhone.text = user.phone
        profileRole.text = user.role
        profileStatus.text = user.status
        profileId.text = user.nic
    }

    /**
     * Creates the Authorization header required by protected API endpoints.
     */
    private fun buildAuthorizationHeader(token: String): String {

        return if (token.startsWith("Bearer ", ignoreCase = true)) {
            token
        } else {
            "Bearer $token"
        }
    }

    /**
     * Shows the profile editing dialog.
     */
    private fun showEditProfileDialog() {

        val user = sessionManager.current()

        if (user == null) {

            Toast.makeText(
                this,
                "No logged-in user found.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val layout = LinearLayout(this)

        layout.orientation = LinearLayout.VERTICAL

        val padding =
            (24 * resources.displayMetrics.density).toInt()

        layout.setPadding(
            padding,
            padding / 2,
            padding,
            padding / 2
        )

        val nameInput = EditText(this)

        nameInput.hint = "Full Name"
        nameInput.setText(user.fullName)
        nameInput.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_WORDS

        layout.addView(nameInput)

        val emailInput = EditText(this)

        emailInput.hint = "Email"
        emailInput.setText(user.email)
        emailInput.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        layout.addView(emailInput)

        val phoneInput = EditText(this)

        phoneInput.hint = "Phone"
        phoneInput.setText(user.phone)
        phoneInput.inputType =
            InputType.TYPE_CLASS_PHONE

        layout.addView(phoneInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Profile")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()

        dialog.setOnShowListener {

            val saveButton =
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            saveButton.setOnClickListener {

                val fullName =
                    nameInput.text.toString().trim()

                val email =
                    emailInput.text.toString().trim()

                val phone =
                    phoneInput.text.toString().trim()

                if (fullName.isEmpty()) {

                    nameInput.error =
                        "Full name is required."

                    return@setOnClickListener
                }

                if (email.isEmpty()) {

                    emailInput.error =
                        "Email is required."

                    return@setOnClickListener
                }

                if (phone.isEmpty()) {

                    phoneInput.error =
                        "Phone number is required."

                    return@setOnClickListener
                }

                if (!android.util.Patterns.EMAIL_ADDRESS
                        .matcher(email)
                        .matches()
                ) {

                    emailInput.error =
                        "Enter a valid email address."

                    return@setOnClickListener
                }

                updateProfile(
                    user = user,
                    fullName = fullName,
                    email = email,
                    phone = phone,
                    dialog = dialog
                )
            }
        }

        dialog.show()
    }

    /**
     * Updates the logged-in user's profile through the central Web API.
     */
    private fun updateProfile(
        user: LocalUser,
        fullName: String,
        email: String,
        phone: String,
        dialog: AlertDialog
    ) {

        lifecycleScope.launch {

            try {

                val token =
                    buildAuthorizationHeader(user.token)

                Log.d(TAG, "Updating profile")
                Log.d(
                    TAG,
                    "Token exists: ${user.token.isNotBlank()}"
                )
                Log.d(
                    TAG,
                    "Token length: ${user.token.length}"
                )

                val request =
                    UpdateProfileRequest(
                        fullName = fullName,
                        email = email,
                        phone = phone
                    )

                val response =
                    RetrofitClient.api.updateMyProfile(
                        authorization = token,
                        request = request
                    )

                val updatedUser =
                    user.copy(
                        nic = response.user.nic,
                        fullName = response.user.fullName,
                        email = response.user.email,
                        phone = response.user.phone,
                        role = response.user.role,
                        status = response.user.status,
                        isActive = response.user.isActive
                    )

                sessionManager.save(updatedUser)

                displayUser(updatedUser)

                dialog.dismiss()

                Toast.makeText(
                    this@ProfileActivity,
                    response.message,
                    Toast.LENGTH_SHORT
                ).show()

                Log.d(
                    TAG,
                    "Profile updated successfully"
                )

            } catch (e: HttpException) {

                val errorBody = try {
                    e.response()
                        ?.errorBody()
                        ?.string()
                } catch (ex: Exception) {
                    null
                }

                Log.e(
                    TAG,
                    "Update profile HTTP error"
                )

                Log.e(
                    TAG,
                    "HTTP Code: ${e.code()}"
                )

                Log.e(
                    TAG,
                    "HTTP Message: ${e.message()}"
                )

                Log.e(
                    TAG,
                    "Error Body: ${errorBody ?: "No error body"}"
                )

                if (e.code() == 401) {

                    Toast.makeText(
                        this@ProfileActivity,
                        "Authentication failed (401). Please login again.",
                        Toast.LENGTH_LONG
                    ).show()

                } else {

                    Toast.makeText(
                        this@ProfileActivity,
                        "Server error ${e.code()}: ${
                            errorBody ?: "No details"
                        }",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Profile update error",
                    e
                )

                Toast.makeText(
                    this@ProfileActivity,
                    "Update failed: ${
                        e.message ?: "Unknown error"
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Shows confirmation before requesting account deactivation.
     */
    private fun showDeactivationConfirmation() {

        AlertDialog.Builder(this)
            .setTitle("Deactivate Account")
            .setMessage(
                "Are you sure you want to request account deactivation?"
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Yes, Deactivate"
            ) { _, _ ->

                requestDeactivation()
            }
            .show()
    }

    /**
     * Sends the account deactivation request to the central Web API.
     */
    private fun requestDeactivation() {

        val user = sessionManager.current()

        if (user == null) {

            Toast.makeText(
                this,
                "No logged-in user found.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        lifecycleScope.launch {

            try {

                val token =
                    buildAuthorizationHeader(user.token)

                Log.d(TAG, "Requesting account deactivation")
                Log.d(
                    TAG,
                    "Token exists: ${user.token.isNotBlank()}"
                )

                val response =
                    RetrofitClient.api.requestDeactivation(
                        authorization = token
                    )

                val updatedUser =
                    user.copy(
                        status = response.status,
                        isActive = response.isActive
                    )

                sessionManager.save(updatedUser)

                profileStatus.text =
                    response.status

                Toast.makeText(
                    this@ProfileActivity,
                    response.message,
                    Toast.LENGTH_LONG
                ).show()

                Log.d(
                    TAG,
                    "Account deactivation request successful"
                )

            } catch (e: HttpException) {

                val errorBody = try {
                    e.response()
                        ?.errorBody()
                        ?.string()
                } catch (ex: Exception) {
                    null
                }

                Log.e(
                    TAG,
                    "Deactivation HTTP error: ${e.code()}"
                )

                Log.e(
                    TAG,
                    "Deactivation error body: ${
                        errorBody ?: "No error body"
                    }"
                )

                if (e.code() == 401) {

                    Toast.makeText(
                        this@ProfileActivity,
                        "Authentication failed (401). Please login again.",
                        Toast.LENGTH_LONG
                    ).show()

                } else {

                    Toast.makeText(
                        this@ProfileActivity,
                        "Server error ${e.code()}: ${
                            errorBody ?: "No details"
                        }",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Deactivation error",
                    e
                )

                Toast.makeText(
                    this@ProfileActivity,
                    "Deactivation failed: ${
                        e.message ?: "Unknown error"
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Shows confirmation before logging out.
     */
    private fun showLogoutConfirmation() {

        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage(
                "Are you sure you want to logout?"
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Logout"
            ) { _, _ ->

                logout()
            }
            .show()
    }

    /**
     * Clears the saved session and returns to the login screen.
     */
    private fun logout() {

        Log.d(TAG, "Logging out user")

        sessionManager.clear()

        Toast.makeText(
            this,
            "Logged out successfully.",
            Toast.LENGTH_SHORT
        ).show()

        val intent = Intent(
            this,
            LoginActivity::class.java
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

        finish()
    }
}

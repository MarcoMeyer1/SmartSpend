package com.example.smartspend

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class Settings : BaseActivity() {

    private lateinit var checkboxNotifications: CheckBox
    private lateinit var checkboxSSO: CheckBox
    private lateinit var spinnerLanguage: Spinner
    private lateinit var btnSignOut: Button
    private lateinit var tvViewProfile: TextView

    private val client = OkHttpClient()
    private var userID: Int = -1

    private var settingsID: Int = -1

    private val TAG = "SettingsActivity"

    private var isUpdatingUI = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        setActiveNavButton(R.id.settings_nav)

        checkboxNotifications = findViewById(R.id.checkbox_notifications)
        checkboxSSO = findViewById(R.id.checkbox_sso)
        spinnerLanguage = findViewById(R.id.spinner_language)
        btnSignOut = findViewById(R.id.btn_sign_out)
        tvViewProfile = findViewById(R.id.view_profile)

        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userID = sharedPreferences.getInt("userID", -1)

        Log.d(TAG, "UserID retrieved from SharedPreferences: $userID")

        if (userID != -1) {
            fetchUserSettings()
        } else {
            Toast.makeText(this, "User ID not found. Please log in again.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "UserID is -1. Redirecting to Login activity.")
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }

        setupLanguageSpinner()

        checkboxNotifications.setOnCheckedChangeListener { _, _ ->
            if (!isUpdatingUI) {
                updateUserSettings()
            }
        }

        checkboxSSO.setOnCheckedChangeListener { _, _ ->
            if (!isUpdatingUI) {
                updateUserSettings()
            }
        }

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: android.view.View, position: Int, id: Long
            ) {
                if (!isUpdatingUI) {
                    updateUserSettings()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }

        btnSignOut.setOnClickListener {
            signOut()
        }

        tvViewProfile.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }
    }

    // Sets up the language spinner
    private fun setupLanguageSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.language_array,
            R.layout.spinner_item
        )
        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinnerLanguage.adapter = adapter
    }

    // Fetches the user settings from the server
    private fun fetchUserSettings() {
        val url = "https://smartspendapi.azurewebsites.net/api/Settings/$userID"

        Log.d(TAG, "Fetching user settings from URL: $url")

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        // Makes the network request
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network Error during fetchUserSettings: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@Settings, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                Log.d(TAG, "fetchUserSettings response code: ${response.code}")
                Log.d(TAG, "fetchUserSettings response body: $responseBody")

                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            settingsID = jsonResponse.getInt("settingID")
                            val allowNotifications = jsonResponse.getBoolean("allowNotifications")
                            val allowSSO = jsonResponse.getBoolean("allowSSO")
                            val language = jsonResponse.getString("language")

                            isUpdatingUI = true
                            checkboxNotifications.isChecked = allowNotifications
                            checkboxSSO.isChecked = allowSSO
                            setSpinnerSelection(spinnerLanguage, language)
                            isUpdatingUI = false

                            Log.d(TAG, "Settings fetched successfully.")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing settings data: ${e.message}")
                            Toast.makeText(this@Settings, "Error parsing settings data", Toast.LENGTH_LONG).show()
                        }
                    } else if (response.code == 404) {
                        Log.d(TAG, "Settings not found for the user. Creating default settings.")
                        createDefaultSettings()
                    } else {
                        Log.e(TAG, "Failed to fetch settings. Response code: ${response.code}")
                        Toast.makeText(this@Settings, "Failed to fetch settings", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    // Sets the selection in the spinner
    private fun setSpinnerSelection(spinner: Spinner, value: String) {
        val adapter = spinner.adapter as ArrayAdapter<String>
        val position = adapter.getPosition(value)
        if (position >= 0) {
            spinner.setSelection(position)
        }
    }

    // Updates the user settings on the server
    private fun updateUserSettings() {
        val allowNotifications = checkboxNotifications.isChecked
        val allowSSO = checkboxSSO.isChecked
        val language = spinnerLanguage.selectedItem.toString()

        val json = JSONObject()
        json.put("settingID", settingsID)
        json.put("userID", userID)
        json.put("allowNotifications", allowNotifications)
        json.put("allowSSO", allowSSO)
        json.put("language", language)

        val url: String
        val request: Request

        if (settingsID == -1) {
            url = "https://smartspendapi.azurewebsites.net/api/Settings"
            Log.d(TAG, "Creating new user settings at URL: $url")
            Log.d(TAG, "Settings JSON: ${json.toString()}")

            val body = RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                json.toString()
            )

            request = Request.Builder()
                .url(url)
                .post(body)
                .build()
        } else {
            url = "https://smartspendapi.azurewebsites.net/api/Settings/$settingsID"
            Log.d(TAG, "Updating user settings at URL: $url")
            Log.d(TAG, "Settings JSON: ${json.toString()}")

            val body = RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                json.toString()
            )

            request = Request.Builder()
                .url(url)
                .put(body)
                .build()
        }

        // Makes the network request
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network Error during updateUserSettings: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@Settings, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                Log.d(TAG, "updateUserSettings response code: ${response.code}")
                Log.d(TAG, "updateUserSettings response body: $responseBody")

                runOnUiThread {
                    if (response.isSuccessful) {
                        Log.d(TAG, "Settings updated successfully.")
                        if (settingsID == -1) {
                            try {
                                val jsonResponse = JSONObject(responseBody)
                                settingsID = jsonResponse.getInt("settingID")
                                Log.d(TAG, "New settingsID received: $settingsID")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing response after creating settings: ${e.message}")
                            }
                        }

                    } else {
                        Log.e(TAG, "Failed to update settings. Response code: ${response.code}")
                        val errorMessage = if (!responseBody.isNullOrEmpty()) responseBody else "Settings update failed"

                    }
                }
            }
        })
    }

    // Creates default settings
    private fun createDefaultSettings() {
        isUpdatingUI = true
        checkboxNotifications.isChecked = false
        checkboxSSO.isChecked = false
        setSpinnerSelection(spinnerLanguage, "English")
        isUpdatingUI = false

        // Updates settingsID to -1 to indicate that settings need to be created
        settingsID = -1

        updateUserSettings()
    }

    // Signs out the user
    private fun signOut() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Sign Out")
        builder.setMessage("Are you sure you want to sign out?")
        builder.setPositiveButton("Yes") { _, _ ->
            val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.clear()
            editor.apply()

            Log.d(TAG, "User signed out. SharedPreferences cleared.")

            val intent = Intent(this@Settings, Login::class.java)
            startActivity(intent)
            finish()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }
}

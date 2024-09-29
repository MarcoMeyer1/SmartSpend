package com.example.smartspend

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

    private var settingsID: Int = -1 // To track the settings ID if needed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        // Initialize UI elements
        checkboxNotifications = findViewById(R.id.checkbox_notifications)
        checkboxSSO = findViewById(R.id.checkbox_sso)
        spinnerLanguage = findViewById(R.id.spinner_language)
        btnSignOut = findViewById(R.id.btn_sign_out)
        tvViewProfile = findViewById(R.id.view_profile)

        // Get userID from SharedPreferences
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userID = sharedPreferences.getInt("userID", -1)

        if (userID != -1) {
            // Fetch settings data
            fetchUserSettings()
        } else {
            Toast.makeText(this, "User ID not found. Please log in again.", Toast.LENGTH_LONG).show()
            // Optionally, redirect to login screen
        }

        // Set up Spinner
        setupLanguageSpinner()

        // Set listeners for UI elements
        checkboxNotifications.setOnCheckedChangeListener { _, _ ->
            // Save settings when the checkbox state changes
            updateUserSettings()
        }

        checkboxSSO.setOnCheckedChangeListener { _, _ ->
            // Save settings when the checkbox state changes
            updateUserSettings()
        }

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: android.view.View, position: Int, id: Long
            ) {
                // Save settings when the selected language changes
                updateUserSettings()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
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

    private fun setupLanguageSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.language_array,
            R.layout.spinner_item
        )
        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinnerLanguage.adapter = adapter
    }

    private fun fetchUserSettings() {
        val url = "https://smartspendapi.azurewebsites.net/api/Settings/$userID"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@Settings, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            settingsID = jsonResponse.getInt("settingID")
                            val allowNotifications = jsonResponse.getBoolean("allowNotifications")
                            val allowSSO = jsonResponse.getBoolean("allowSSO")
                            val language = jsonResponse.getString("language")

                            // Update UI elements
                            checkboxNotifications.isChecked = allowNotifications
                            checkboxSSO.isChecked = allowSSO
                            setSpinnerSelection(spinnerLanguage, language)

                        } catch (e: Exception) {
                            Toast.makeText(this@Settings, "Error parsing settings data", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@Settings, "Failed to fetch settings", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun setSpinnerSelection(spinner: Spinner, value: String) {
        val adapter = spinner.adapter as ArrayAdapter<String>
        val position = adapter.getPosition(value)
        if (position >= 0) {
            spinner.setSelection(position)
        }
    }

    private fun updateUserSettings() {
        val allowNotifications = checkboxNotifications.isChecked
        val allowSSO = checkboxSSO.isChecked
        val language = spinnerLanguage.selectedItem.toString()

        // Prepare JSON object
        val json = JSONObject()
        json.put("userID", userID)
        json.put("allowNotifications", allowNotifications)
        json.put("allowSSO", allowSSO)
        json.put("language", language)

        val url = "https://smartspendapi.azurewebsites.net/api/Settings/update"

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url(url)
            .put(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@Settings, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful) {
                        // Optionally show a success message
                        // Toast.makeText(this@Settings, "Settings updated successfully", Toast.LENGTH_LONG).show()
                    } else {
                        val errorMessage = responseBody ?: "Settings update failed"
                        Toast.makeText(this@Settings, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun signOut() {
        // Show confirmation dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Sign Out")
        builder.setMessage("Are you sure you want to sign out?")
        builder.setPositiveButton("Yes") { _, _ ->
            // Clear SharedPreferences
            val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.clear()
            editor.apply()

            // Navigate to Login activity
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

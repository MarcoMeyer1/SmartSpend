package com.example.smartspend

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.Manifest
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    private var isUpdatingUI = false // Flag to prevent infinite loops during UI updates
    private val TAG = "SettingsActivity" // Tag for logging

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize UI elements
        checkboxNotifications = findViewById(R.id.checkbox_notifications)
        checkboxSSO = findViewById(R.id.checkbox_sso)
        spinnerLanguage = findViewById(R.id.spinner_language)
        btnSignOut = findViewById(R.id.btn_sign_out)
        tvViewProfile = findViewById(R.id.view_profile)

        // Load user preferences from SharedPreferences
        loadUserPreferences()

        // Set up the language spinner
        setupLanguageSpinner()

        // Set up listeners for user interactions
        setupListeners()
    }

    private fun setupLanguageSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.language_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerLanguage.adapter = adapter
        }

        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val selectedLanguage = sharedPreferences.getString("Locale.Helper.Selected.Language", "en")
        spinnerLanguage.setSelection(if (selectedLanguage == "af") 1 else 0)

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Check if view is null before proceeding
                if (view == null) return

                val languageCode = if (position == 0) "en" else "af"
                if (!isUpdatingUI) {
                    LocaleHelper.setLocale(this@Settings, languageCode)
                    recreate()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Handle case when nothing is selected, if needed
            }
        }
    }

    private fun loadUserPreferences() {
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        checkboxNotifications.isChecked = sharedPreferences.getBoolean("allowNotifications", false)
        checkboxSSO.isChecked = sharedPreferences.getBoolean("allowSSO", false)
    }

    private fun saveUserPreferences(selectedLanguage: String) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("Locale.Helper.Selected.Language", selectedLanguage)
        editor.putBoolean("allowNotifications", checkboxNotifications.isChecked)
        editor.putBoolean("allowSSO", checkboxSSO.isChecked)
        editor.apply() // Commit changes
    }

    private fun setupListeners() {
        checkboxNotifications.setOnCheckedChangeListener { _, _ ->
            saveUserPreferences(spinnerLanguage.selectedItem.toString()) // Save updated notification preference
        }

        checkboxSSO.setOnCheckedChangeListener { _, _ ->
            saveUserPreferences(spinnerLanguage.selectedItem.toString()) // Save updated SSO preference
        }

        btnSignOut.setOnClickListener {
            signOut() // Call the sign out function when the button is clicked
        }
    }

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
        builder.setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()
    }

    private fun checkAndRequestNotificationPermission() {
        // Implement notification permission check logic here if needed
    }
}
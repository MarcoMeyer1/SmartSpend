package com.example.smartspend

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.Manifest
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log
import androidx.activity.enableEdgeToEdge
import java.util.*

class Settings : BaseActivity() {

    private lateinit var checkboxNotifications: CheckBox
    private lateinit var checkboxSSO: CheckBox
    private lateinit var spinnerLanguage: Spinner
    private lateinit var btnSignOut: Button
    private lateinit var tvViewProfile: TextView

    private val TAG = "SettingsActivity"

    private var isSpinnerInitialized = false // Flag to track initial setup

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Attach the saved locale before super.onCreate to apply the correct language
        LocaleHelper.onAttach(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        setActiveNavButton(R.id.settings_nav)

        // Initialize Views
        checkboxNotifications = findViewById(R.id.checkbox_notifications)
        checkboxSSO = findViewById(R.id.checkbox_sso)
        spinnerLanguage = findViewById(R.id.spinner_language)
        btnSignOut = findViewById(R.id.btn_sign_out)
        tvViewProfile = findViewById(R.id.view_profile)

        // Setup Language Spinner
        setupLanguageSpinner()

        // Setup CheckBox Listeners
        checkboxNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkAndRequestNotificationPermission()
            } else {
                Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show()
                // Optionally handle disabling notifications
            }
        }

        checkboxSSO.setOnCheckedChangeListener { _, isChecked ->
            // Handle SSO checkbox changes if needed
            if (isChecked) {
                Toast.makeText(this, "SSO Enabled", Toast.LENGTH_SHORT).show()
                // Enable SSO-related features
            } else {
                Toast.makeText(this, "SSO Disabled", Toast.LENGTH_SHORT).show()
                // Disable SSO-related features
            }
        }

        // Setup Spinner Listener
        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long
            ) {
                if (!isSpinnerInitialized) {
                    // Skip the first selection (initial setup)
                    isSpinnerInitialized = true
                    return
                }

                val selectedLanguage = when (position) {
                    0 -> "en" // English
                    1 -> "af" // Afrikaans
                    else -> "en" // Default to English
                }

                val currentLanguage = LocaleHelper.getLocale(this@Settings).language
                if (selectedLanguage != currentLanguage) {
                    // Save selected language to SharedPreferences
                    saveLanguagePreference(selectedLanguage)

                    // Set the selected locale
                    LocaleHelper.setLocale(this@Settings, selectedLanguage)

                    // Recreate the activity to apply the language change
                    recreate() // This will refresh the activity and apply the new language
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        // Setup Sign Out Button
        btnSignOut.setOnClickListener {
            signOut()
        }

        // Setup View Profile TextView
        tvViewProfile.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }
    }

    /**
     * Sets up the language spinner with available language options
     */
    private fun setupLanguageSpinner() {
        // Create an ArrayAdapter using the string array and a default spinner layout
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.language_array,
            android.R.layout.simple_spinner_item
        )
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Apply the adapter to the spinner
        spinnerLanguage.adapter = adapter

        // Retrieve saved language preference or default to English
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val savedLanguage = sharedPreferences.getString("app_language", "en") ?: "en"

        // Determine spinner position based on saved language
        val languagePosition = when (savedLanguage) {
            "en" -> 0
            "af" -> 1
            else -> 0 // Default to English if unknown
        }

        // Log the current language and position for debugging
        Log.d(TAG, "Saved language: $savedLanguage, Spinner Position: $languagePosition")

        // Set the flag before setting selection to avoid triggering onItemSelected
        isSpinnerInitialized = false
        spinnerLanguage.setSelection(languagePosition)
        isSpinnerInitialized = true
    }

    /**
     * Checks and requests notification permission if necessary
     */
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Toast.makeText(this, "Notifications already enabled", Toast.LENGTH_SHORT).show()
                }
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) -> {
                    Toast.makeText(this, "Please enable notifications in app settings.", Toast.LENGTH_LONG).show()
                }
                else -> {
                    // Directly request for permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // For Android versions below Tiramisu, notifications are enabled by default or handled differently
            Toast.makeText(this, "Notifications are enabled by default on this Android version.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Signs out the user after confirmation
     */
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

    /**
     * Saves the selected language to SharedPreferences
     */
    private fun saveLanguagePreference(language: String) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("app_language", language)
        editor.apply()
        Log.d(TAG, "Language preference saved: $language")
    }
}

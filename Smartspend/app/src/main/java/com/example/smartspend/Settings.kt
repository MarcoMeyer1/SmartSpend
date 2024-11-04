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

    private var isSpinnerInitialized = false

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
        super.onCreate(savedInstanceState)
        val contextWithLocale = LocaleHelper.onAttach(this)
        applyLocale(contextWithLocale)

        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        setActiveNavButton(R.id.settings_nav)

        // Initializes the Views
        checkboxNotifications = findViewById(R.id.checkbox_notifications)
        checkboxSSO = findViewById(R.id.checkbox_sso)
        spinnerLanguage = findViewById(R.id.spinner_language)
        btnSignOut = findViewById(R.id.btn_sign_out)
        tvViewProfile = findViewById(R.id.view_profile)

        setupLanguageSpinner()

        checkboxNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkAndRequestNotificationPermission()
            } else {
                Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show()
            }
        }

        checkboxSSO.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "SSO Enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "SSO Disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup Spinner Listener
        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long
            ) {
                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                    return
                }

                val selectedLanguage = when (position) {
                    0 -> "en" // English
                    1 -> "af" // Afrikaans
                    else -> "en" // Default to English
                }

                // Get the currently set language
                val currentLanguage = LocaleHelper.getLocale(this@Settings).language
                if (selectedLanguage != currentLanguage) {
                    // Save the new language preference
                    saveLanguagePreference(selectedLanguage)

                    LocaleHelper.setLocale(this@Settings, selectedLanguage)
                    recreate()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        //Sign Out Button
        btnSignOut.setOnClickListener {
            signOut()
        }

        tvViewProfile.setOnClickListener {
            startActivity(Intent(this, Profile::class.java))
        }
    }

    /**
     * Sets up the language spinner with available language options
     */
    private fun setupLanguageSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.language_array,
            R.layout.spinner_item
        )
        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinnerLanguage.adapter = adapter

        // Retrieve saved language preference or default to English
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val savedLanguage = sharedPreferences.getString("app_language", "en") ?: "en"

        val languagePosition = when (savedLanguage) {
            "en" -> 0
            "af" -> 1
            else -> 0 // Default to English if unknown
        }

        Log.d(TAG, "Saved language: $savedLanguage, Spinner Position: $languagePosition")
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
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
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

            startActivity(Intent(this@Settings, Login::class.java))
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

    /**
     * Re-attach locale if needed
     */
    private fun applyLocale(context: Context) {
        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val language = sharedPreferences.getString("app_language", "en") ?: "en"
        LocaleHelper.setLocale(context, language)
    }
}

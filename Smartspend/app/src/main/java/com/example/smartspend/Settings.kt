package com.example.smartspend

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.Manifest
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log

class Settings : BaseActivity() {

    private lateinit var checkboxNotifications: CheckBox
    private lateinit var checkboxSSO: CheckBox
    private lateinit var spinnerLanguage: Spinner
    private lateinit var btnSignOut: Button
    private lateinit var tvViewProfile: TextView

    private val TAG = "SettingsActivity"

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
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        setActiveNavButton(R.id.settings_nav)

        checkboxNotifications = findViewById(R.id.checkbox_notifications)
        checkboxSSO = findViewById(R.id.checkbox_sso)
        spinnerLanguage = findViewById(R.id.spinner_language)
        btnSignOut = findViewById(R.id.btn_sign_out)
        tvViewProfile = findViewById(R.id.view_profile)

        setupLanguageSpinner()

        checkboxNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkAndRequestNotificationPermission()
            }
        }

        checkboxSSO.setOnCheckedChangeListener { _, _ ->
            // Handle SSO checkbox changes if needed
        }

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: android.view.View, position: Int, id: Long
            ) {
                // Handle language selection changes if needed
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
        }
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

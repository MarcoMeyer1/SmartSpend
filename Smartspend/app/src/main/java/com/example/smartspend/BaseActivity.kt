package com.example.smartspend

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        val homeNav: ImageView = findViewById(R.id.home_nav)
        val notificationsNav: ImageView = findViewById(R.id.notifications_nav)
        val addRecordNav: ImageView = findViewById(R.id.add_record_nav)
        val historyNav: ImageView = findViewById(R.id.history_nav)
        val settingsNav: ImageView = findViewById(R.id.settings_nav)

        // Tint all icons white at startup
        homeNav.setColorFilter(Color.parseColor("#FFFFFF")) // White
        notificationsNav.setColorFilter(Color.parseColor("#FFFFFF"))
        addRecordNav.setColorFilter(Color.parseColor("#FFFFFF"))
        historyNav.setColorFilter(Color.parseColor("#FFFFFF"))
        settingsNav.setColorFilter(Color.parseColor("#FFFFFF"))

        // Set click listeners for the navbar items
        homeNav.setOnClickListener {
            // Empty for now
        }

        notificationsNav.setOnClickListener {
            // Empty for now
        }

        addRecordNav.setOnClickListener {
            // Empty for now
        }

        historyNav.setOnClickListener {
            // Empty for now
        }

        settingsNav.setOnClickListener {
            // Empty for now
        }
    }

    // Function to highlight the active button
    protected fun setActiveNavButton(navButton: Int) {
        val homeNav: ImageView = findViewById(R.id.home_nav)
        val notificationsNav: ImageView = findViewById(R.id.notifications_nav)
        val addRecordNav: ImageView = findViewById(R.id.add_record_nav)
        val historyNav: ImageView = findViewById(R.id.history_nav)
        val settingsNav: ImageView = findViewById(R.id.settings_nav)

        // Reset all icons to white
        homeNav.setColorFilter(Color.parseColor("#FFFFFF"))
        notificationsNav.setColorFilter(Color.parseColor("#FFFFFF"))
        addRecordNav.setColorFilter(Color.parseColor("#FFFFFF"))
        historyNav.setColorFilter(Color.parseColor("#FFFFFF"))
        settingsNav.setColorFilter(Color.parseColor("#FFFFFF"))

        // Highlight the active button in green (#70FFB5)
        when (navButton) {
            R.id.home_nav -> homeNav.setColorFilter(Color.parseColor("#70FFB5"))
            R.id.notifications_nav -> notificationsNav.setColorFilter(Color.parseColor("#70FFB5"))
            R.id.add_record_nav -> addRecordNav.setColorFilter(Color.parseColor("#70FFB5"))
            R.id.history_nav -> historyNav.setColorFilter(Color.parseColor("#70FFB5"))
            R.id.settings_nav -> settingsNav.setColorFilter(Color.parseColor("#70FFB5"))
        }
    }
}

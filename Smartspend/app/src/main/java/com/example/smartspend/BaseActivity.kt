package com.example.smartspend

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        // Find the FrameLayouts that act as containers for each navigation button
        val homeNavContainer: FrameLayout = findViewById(R.id.home_nav_container)
        val notificationsNavContainer: FrameLayout = findViewById(R.id.notifications_nav_container)
        val addRecordNavContainer: FrameLayout = findViewById(R.id.add_record_nav_container)
        val historyNavContainer: FrameLayout = findViewById(R.id.history_nav_container)
        val settingsNavContainer: FrameLayout = findViewById(R.id.settings_nav_container)

        // Set click listeners for the entire FrameLayout (outer circle) containers
        homeNavContainer.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent) //TODO fix this it no click
        }

        notificationsNavContainer.setOnClickListener {
            // Empty for now, navigate to NotificationsActivity
            //TODO - Make this page
        }

        addRecordNavContainer.setOnClickListener {
            // Empty for now, navigate to AddRecordActivity
            //TODO - Make this page
        }

        historyNavContainer.setOnClickListener {
            // Empty for now, navigate to HistoryActivity
            //TODO - Make this page
        }

        settingsNavContainer.setOnClickListener {
            // Empty for now, navigate to SettingsActivity
            //TODO - Make this page
        }

        // Reset all icons to white at startup
        resetNavIcons()
    }

    // Reset all icons to white
    private fun resetNavIcons() {
        val homeNav: ImageView? = findViewById(R.id.home_nav)
        val notificationsNav: ImageView? = findViewById(R.id.notifications_nav)
        val addRecordNav: ImageView? = findViewById(R.id.add_record_nav)
        val historyNav: ImageView? = findViewById(R.id.history_nav)
        val settingsNav: ImageView? = findViewById(R.id.settings_nav)

        homeNav?.setColorFilter(Color.parseColor("#FFFFFF"))
        notificationsNav?.setColorFilter(Color.parseColor("#FFFFFF"))
        addRecordNav?.setColorFilter(Color.parseColor("#FFFFFF"))
        historyNav?.setColorFilter(Color.parseColor("#FFFFFF"))
        settingsNav?.setColorFilter(Color.parseColor("#FFFFFF"))
    }

    // Function to highlight the active button
    protected fun setActiveNavButton(navButton: Int?) {
        resetNavIcons() // Reset all icons to white before highlighting the active one

        // Highlight the selected button if a valid one is passed
        when (navButton) {
            R.id.home_nav -> findViewById<ImageView>(R.id.home_nav)?.setColorFilter(Color.parseColor("#70FFB5"))
            R.id.notifications_nav -> findViewById<ImageView>(R.id.notifications_nav)?.setColorFilter(Color.parseColor("#70FFB5"))
            R.id.add_record_nav -> findViewById<ImageView>(R.id.add_record_nav)?.setColorFilter(Color.parseColor("#70FFB5"))
            R.id.history_nav -> findViewById<ImageView>(R.id.history_nav)?.setColorFilter(Color.parseColor("#70FFB5"))
            R.id.settings_nav -> findViewById<ImageView>(R.id.settings_nav)?.setColorFilter(Color.parseColor("#70FFB5"))
            else -> {
                // Optional: Log or handle invalid button ID case, but nothing happens here for now
            }
        }
    }

}

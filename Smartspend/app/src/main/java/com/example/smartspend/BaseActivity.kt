// BaseActivity.kt
package com.example.smartspend

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Do not call setContentView here
        // setupNavbar will be called after setContentView in child classes
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        setupNavbar()
    }

    private fun setupNavbar() {
        // Find the LinearLayouts that act as containers for each navigation button
        val homeNavContainer: LinearLayout = findViewById(R.id.home_nav_container)
        val notificationsNavContainer: LinearLayout = findViewById(R.id.notifications_nav_container)
        val addRecordNavContainer: LinearLayout = findViewById(R.id.add_record_nav_container)
        val historyNavContainer: LinearLayout = findViewById(R.id.history_nav_container)
        val settingsNavContainer: LinearLayout = findViewById(R.id.settings_nav_container)

        // Set click listeners for the entire LinearLayout (outer container)
        homeNavContainer.setOnClickListener {
            if (this !is MainActivity) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }

        notificationsNavContainer.setOnClickListener {

                val intent = Intent(this, Notifications::class.java)
                startActivity(intent)

        }

        addRecordNavContainer.setOnClickListener {
         //   val intent = Intent(this, Notifications::class.java)
          //  startActivity(intent)

        }

        historyNavContainer.setOnClickListener {
          //  val intent = Intent(this, Notifications::class.java)
          //  startActivity(intent)

        }

        settingsNavContainer.setOnClickListener {
            val intent = Intent(this, Settings::class.java)
            startActivity(intent)

        }

        // Reset all icons to white at startup
        resetNavIcons()
    }

    // Reset all icons to white
    private fun resetNavIcons() {
        val homeNav: ImageView = findViewById(R.id.home_nav)
        val notificationsNav: ImageView = findViewById(R.id.notifications_nav)
        val addRecordNav: ImageView = findViewById(R.id.add_record_nav)
        val historyNav: ImageView = findViewById(R.id.history_nav)
        val settingsNav: ImageView = findViewById(R.id.settings_nav)

        homeNav.setColorFilter(Color.parseColor("#FFFFFF"))
        notificationsNav.setColorFilter(Color.parseColor("#FFFFFF"))
        addRecordNav.setColorFilter(Color.parseColor("#FFFFFF"))
        historyNav.setColorFilter(Color.parseColor("#FFFFFF"))
        settingsNav.setColorFilter(Color.parseColor("#FFFFFF"))
    }

    // Function to highlight the active button
    protected fun setActiveNavButton(navButtonId: Int?) {
        resetNavIcons() // Reset all icons to white before highlighting the active one

        // Highlight the selected button if a valid one is passed
        when (navButtonId) {
            R.id.home_nav -> findViewById<ImageView>(R.id.home_nav)?.setColorFilter(Color.parseColor("#70FFB5"))
            R.id.notifications_nav -> findViewById<ImageView>(R.id.notifications_nav)?.setColorFilter(Color.parseColor("#70FFB5"))
            R.id.add_record_nav -> findViewById<ImageView>(R.id.add_record_nav)?.setColorFilter(Color.parseColor("#70FFB5"))
            R.id.history_nav -> findViewById<ImageView>(R.id.history_nav)?.setColorFilter(Color.parseColor("#70FFB5"))
            R.id.settings_nav -> findViewById<ImageView>(R.id.settings_nav)?.setColorFilter(Color.parseColor("#70FFB5"))
        }
    }
}

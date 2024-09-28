package com.example.smartspend

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class MainActivity : BaseActivity() {

    // Declare the buttons
    private lateinit var detailedViewButton: Button
    private lateinit var savingGoalsButton: Button
    private lateinit var remindersButton: Button
    private lateinit var settingsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setActiveNavButton(R.id.home_nav)
        // Initialize buttons
        detailedViewButton = findViewById(R.id.buttonDetailedView)
        savingGoalsButton = findViewById(R.id.buttonSavingGoals)
        remindersButton = findViewById(R.id.buttonReminders)
        settingsButton = findViewById(R.id.buttonSettings)

        // Set click listeners for each button
        detailedViewButton.setOnClickListener {
            openDetailedView()
        }

        savingGoalsButton.setOnClickListener {
            openSavingGoals()
        }

        remindersButton.setOnClickListener {
            openReminders()
        }

        settingsButton.setOnClickListener {
            openSettings()
        }
    }

    // Method for Detailed View button
    private fun openDetailedView() {//TODO
        val intent = Intent(this, DetailedView::class.java)
        startActivity(intent)
    }

    // Method for Saving Goals button
    private fun openSavingGoals() {
        val intent = Intent(this, SavingGoals::class.java)
        startActivity(intent)
    }

    // Method for Reminders button
    private fun openReminders() {//TODO
        val intent = Intent(this, Reminders::class.java)
        startActivity(intent)
    }

    // Method for Settings button
    private fun openSettings() {//TODO
        val intent = Intent(this, Settings::class.java)
        startActivity(intent)

    }
}

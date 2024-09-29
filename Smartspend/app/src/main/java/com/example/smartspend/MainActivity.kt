package com.example.smartspend

import android.content.Intent
import android.os.Bundle
import android.widget.Button

class MainActivity : BaseActivity() {

    // Declare the buttons
    private lateinit var detailedViewButton: Button
    private lateinit var savingGoalsButton: Button
    private lateinit var remindersButton: Button
    private lateinit var historyButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setActiveNavButton(R.id.home_nav)

        // Initialize buttons
        detailedViewButton = findViewById(R.id.buttonDetailedView)
        savingGoalsButton = findViewById(R.id.buttonSavingGoals)
        remindersButton = findViewById(R.id.buttonReminders)
        historyButton = findViewById(R.id.buttonHistory)

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

        historyButton.setOnClickListener {
            openHistory()
        }
    }

    // Method for Detailed View button

    private fun openDetailedView() {
        val intent = Intent(this, DetailedView::class.java)
        startActivity(intent)

    }

    // Method for Saving Goals button
    private fun openSavingGoals() {
        val intent = Intent(this, SavingGoals::class.java)
        startActivity(intent)
    }

    // Method for Reminders button
    private fun openReminders() {
        val intent = Intent(this, Reminders::class.java)
        startActivity(intent)
    }

    // Method for Settings button
    private fun openHistory() {
        val intent = Intent(this, History::class.java)
        startActivity(intent)
    }
}

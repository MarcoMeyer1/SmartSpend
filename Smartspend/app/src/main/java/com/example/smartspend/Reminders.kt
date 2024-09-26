package com.example.smartspend

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*

class Reminders : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reminders)

        // Adjust padding for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Handle FAB click to show the Create Reminder dialog
        val fab = findViewById<FloatingActionButton>(R.id.fab_add_reminder)
        fab.setOnClickListener {
            showCreateReminderDialog()
        }
    }

    private fun showCreateReminderDialog() {
        // Inflate the custom layout for the dialog
        val builder = AlertDialog.Builder(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_reminder, null)

        // Set the custom view for the dialog
        builder.setView(dialogView)
        val dialog = builder.create()

        // Initialize UI elements from the dialog layout
        val dateDueField: EditText = dialogView.findViewById(R.id.date_due_button)
        val dateToNotifyField: EditText = dialogView.findViewById(R.id.date_to_notify_button)
        val createButton: Button = dialogView.findViewById(R.id.create_button)

        // Make the date fields clickable and show DatePickerDialogs
        dateDueField.setOnClickListener {
            showDatePickerDialog { date -> dateDueField.setText(date) }
        }

        dateToNotifyField.setOnClickListener {
            showDatePickerDialog { date -> dateToNotifyField.setText(date) }
        }

        // Handle create button click
        createButton.setOnClickListener {
            // Logic to handle the creation of a new reminder
            dialog.dismiss()  // Dismiss the dialog after creation
        }

        dialog.show()  // Show the dialog
    }

    // Function to display DatePickerDialog
    private fun showDatePickerDialog(onDateSet: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Create and display DatePickerDialog
        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            // Format the selected date as "dd/MM/yyyy"
            val selectedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
            onDateSet(selectedDate)
        }, year, month, day)

        datePickerDialog.show()
    }
}

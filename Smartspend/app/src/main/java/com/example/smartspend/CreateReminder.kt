package com.example.smartspend

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.EditText
import java.util.*

class CreateReminder : AppCompatActivity() {

    private lateinit var dateDueField: EditText
    private lateinit var dateToNotifyField: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_reminder)

        // Adjust padding for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize the date fields
        dateDueField = findViewById(R.id.date_due_button)
        dateToNotifyField = findViewById(R.id.date_to_notify_button)

        // Make the EditTexts non-editable and clickable
        dateDueField.isFocusable = false
        dateToNotifyField.isFocusable = false

        // Set click listeners to show the DatePickerDialog
        dateDueField.setOnClickListener {
            showDatePickerDialog { date ->
                dateDueField.setText(date)
            }
        }

        dateToNotifyField.setOnClickListener {
            showDatePickerDialog { date ->
                dateToNotifyField.setText(date)
            }
        }
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

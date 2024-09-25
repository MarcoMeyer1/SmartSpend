package com.example.smartspend

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SavingGoals : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_saving_goals)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setActiveNavButton(null)

        val fabAddGoal: FloatingActionButton = findViewById(R.id.fabAddGoal)

        fabAddGoal.setOnClickListener {
            val builder = AlertDialog.Builder(this)

            val dialogView = layoutInflater.inflate(R.layout.dialog_add_goal, null)

            builder.setView(dialogView)

            val dialog = builder.create()

            val goalNameEditText: EditText = dialogView.findViewById(R.id.goalName)
            val totalAmountEditText: EditText = dialogView.findViewById(R.id.totalAmount)
            val savedAmountEditText: EditText = dialogView.findViewById(R.id.savedAmount)
            val saveButton: Button = dialogView.findViewById(R.id.saveButton)

            saveButton.setOnClickListener {
                val goalName = goalNameEditText.text.toString()
                val totalAmount = totalAmountEditText.text.toString()
                val savedAmount = savedAmountEditText.text.toString()

                if (goalName.isNotEmpty() && totalAmount.isNotEmpty() && savedAmount.isNotEmpty()) {
                    Toast.makeText(this, "Goal saved: $goalName - Total: $totalAmount, Saved: $savedAmount", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
                }
            }

            dialog.show()
        }
    }
}
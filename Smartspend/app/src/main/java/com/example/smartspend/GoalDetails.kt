package com.example.smartspend

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class GoalDetails : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_goal_details)
        
        val addToGoal: Button = findViewById(R.id.btnAddToGoal)
        addToGoal.setOnClickListener {
            showAddToGoalDialog()
        }


    }

    private fun showAddToGoalDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_to_goal, null)

        val amountEditText = dialogView.findViewById<EditText>(R.id.edtAmount)
        val addToGoalButton = dialogView.findViewById<Button>(R.id.btnAddtoGoal)

        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(dialogView)

        val alertDialog = dialogBuilder.create()
        alertDialog.show()

        addToGoalButton.setOnClickListener {
            val enteredAmount = amountEditText.text.toString()
            if (enteredAmount.isNotEmpty()) {
                Toast.makeText(this, "Amount added: $enteredAmount", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
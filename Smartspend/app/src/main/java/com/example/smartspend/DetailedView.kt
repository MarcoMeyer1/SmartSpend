package com.example.smartspend

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton




class DetailedView : AppCompatActivity() {
    private var selectedColorHex: String = "#FFFFFF" // Default color

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Reference to the container where cards will be added
        val categoriesContainer = findViewById<LinearLayout>(R.id.categoriesContainer)

        // Initialize the Floating Action Button
        val fabAddGoal: FloatingActionButton = findViewById(R.id.fabAddGoal)
        fabAddGoal.setOnClickListener {
            showAddCategoryDialog(categoriesContainer)
        }
    }

    private fun showAddCategoryDialog(container: LinearLayout) {
        // Inflate the dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val editTextCategoryName: EditText = dialogView.findViewById(R.id.editTextCategoryName)
        val viewSelectedColor: View = dialogView.findViewById(R.id.viewSelectedColor)
        val buttonPickColor: Button = dialogView.findViewById(R.id.buttonPickColor)
        val btnCreateCategory: MaterialButton = dialogView.findViewById(R.id.btnCreateCategory)

        // Set up the dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add New Category")
            .setView(dialogView)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        // Create the dialog
        val dialog = builder.create()
        dialog.show()

        // Color Picker Logic
        buttonPickColor.setOnClickListener {
            ColorPickerDialogBuilder
                .with(this)
                .setTitle("Choose color")
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setPositiveButton("OK") { _, selectedColor, _ ->
                    selectedColorHex = String.format("#%06X", 0xFFFFFF and selectedColor)
                    viewSelectedColor.setBackgroundColor(selectedColor) // Update the view with the selected color
                }
                .setNegativeButton("Cancel", null)
                .build()
                .show()
        }

        // Create Category Button Logic
        btnCreateCategory.setOnClickListener {
            val categoryName = editTextCategoryName.text.toString()
            if (categoryName.isNotEmpty()) {
                addCategoryCard(container, categoryName, "R0", selectedColorHex) // Assuming amount is "R0" for now
                dialog.dismiss() // Close the dialog after adding
            } else {
                Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to add a category card dynamically
    private fun addCategoryCard(container: LinearLayout, categoryName: String, categoryAmount: String, categoryColor: String) {
        // Inflate the card_category.xml layout
        val inflater = LayoutInflater.from(this)
        val cardView = inflater.inflate(R.layout.card_category, container, false) as CardView

        // Get the TextViews from the card and set their values
        val categoryText = cardView.findViewById<TextView>(R.id.categoryName)
        val amountText = cardView.findViewById<TextView>(R.id.categoryAmount)

        categoryText.text = categoryName
        amountText.text = categoryAmount

        // Set the color of the category name dynamically based on the user-selected color
        try {
            categoryText.setTextColor(Color.parseColor(categoryColor)) // Apply the color to category name
        } catch (e: IllegalArgumentException) {
            // Fallback if color parsing fails (e.g., invalid color string)
            categoryText.setTextColor(Color.WHITE)
        }

        // Add the card to the container (LinearLayout)
        container.addView(cardView)
    }
}
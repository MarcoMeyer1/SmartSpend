package com.example.smartspend

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DetailedView : AppCompatActivity() {

    private var selectedColorHex: String = "#FFFFFF" // Default color
    private val categories = mutableListOf<Category>()
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Change to the correct layout file
        setContentView(R.layout.activity_detailed_view)

        // Set up the RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.categoriesRecyclerView)

        // Check if the RecyclerView is correctly referenced and exists in the layout
        if (recyclerView == null) {
            Toast.makeText(this, "RecyclerView not found in layout", Toast.LENGTH_LONG).show()
            return
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        categoryAdapter = CategoryAdapter(categories)
        recyclerView.adapter = categoryAdapter

        // Initialize the Floating Action Button
        val fabAddGoal: FloatingActionButton = findViewById(R.id.fabAddCategory)
        fabAddGoal?.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun showAddCategoryDialog() {
        // Inflate the dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val editTextCategoryName: EditText = dialogView.findViewById(R.id.editTextCategoryName)
        val editTextAllocatePercentage: EditText = dialogView.findViewById(R.id.editTextAllocatePercentage)
        val editTextSetAmountManually: EditText = dialogView.findViewById(R.id.editTextSetAmountManually)
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
            val categoryName = editTextCategoryName.text.toString().trim()
            val allocatePercentage = editTextAllocatePercentage.text.toString().trim()
            val setAmountManually = editTextSetAmountManually.text.toString().trim()

            if (categoryName.isEmpty()) {
                Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (allocatePercentage.isEmpty() && setAmountManually.isEmpty()) {
                Toast.makeText(this, "Please enter either allocation percentage or amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Add the new category (for now using amount as either percentage or manually set)
            val amount = if (setAmountManually.isNotEmpty()) "R$setAmountManually" else "$allocatePercentage%"

            addCategory(Category(categoryName, amount))
            categoryAdapter.notifyDataSetChanged()

            dialog.dismiss() // Close the dialog after adding
        }
    }

    // Function to add the new category to the list
    private fun addCategory(category: Category) {
        categories.add(category)
    }

    // Category data class to represent category items
    data class Category(
        val name: String,
        val amount: String
    )

    // Adapter to handle displaying categories in the RecyclerView
    class CategoryAdapter(private val categories: List<Category>) :
        RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

        inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val categoryName: TextView = view.findViewById(R.id.categoryName)
            val categoryAmount: TextView = view.findViewById(R.id.categoryAmount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_category, parent, false)
            return CategoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            val category = categories[position]
            holder.categoryName.text = category.name
            holder.categoryAmount.text = category.amount
        }

        override fun getItemCount(): Int {
            return categories.size
        }
    }
}

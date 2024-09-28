package com.example.smartspend

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anychart.AnyChartView
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class DetailedView : AppCompatActivity() {

    private var selectedColorHex: String = "#FFFFFF" // Default color
    private val categories = mutableListOf<Category>()
    private lateinit var categoryAdapter: CategoryAdapter
    private val client = OkHttpClient()
    private var userID: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the layout file
        setContentView(R.layout.activity_detailed_view)

        // Get userID from SharedPreferences
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userID = sharedPreferences.getInt("userID", -1)

        // Set up the RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.categoriesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        categoryAdapter = CategoryAdapter(categories)
        recyclerView.adapter = categoryAdapter

        // Initialize the Floating Action Button
        val fabAddCategory: FloatingActionButton = findViewById(R.id.fabAddCategory)
        fabAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }

        // Fetch categories from the API
        fetchCategories()
    }

    private fun fetchCategories() {
        val url = "https://smartspendapi.azurewebsites.net/api/Category/user/$userID"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@DetailedView, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonArray = JSONArray(responseBody)
                            categories.clear()
                            for (i in 0 until jsonArray.length()) {
                                val jsonObject = jsonArray.getJSONObject(i)
                                val categoryID = jsonObject.getInt("categoryID")
                                val categoryName = jsonObject.getString("categoryName")
                                val maxBudget = jsonObject.getDouble("maxBudget")
                                val colorCode = jsonObject.getString("colorCode")

                                val category = Category(categoryID, categoryName, maxBudget.toString(), colorCode)
                                categories.add(category)
                            }
                            categoryAdapter.notifyDataSetChanged()
                        } catch (e: Exception) {
                            Toast.makeText(this@DetailedView, "Error parsing categories", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@DetailedView, "Failed to fetch categories", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun showAddCategoryDialog() {
        // Inflate the dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val editTextCategoryName: EditText = dialogView.findViewById(R.id.editTextCategoryName)
        val editTextSetAmountManually: EditText = dialogView.findViewById(R.id.editTextSetAmountManually)
        val viewSelectedColor: View = dialogView.findViewById(R.id.viewSelectedColor)
        val buttonPickColor: Button = dialogView.findViewById(R.id.buttonPickColor)
        val btnCreateCategory: MaterialButton = dialogView.findViewById(R.id.btnCreateCategory)

        // Set up the dialog with a custom style
        val builder = AlertDialog.Builder(this, R.style.CustomDialog)
        builder.setTitle("Add New Category")
            .setView(dialogView)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        // Create the dialog
        val dialog = builder.create()

        // Show the dialog and adjust its width
        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

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
            val setAmountManually = editTextSetAmountManually.text.toString().trim()

            if (categoryName.isEmpty()) {
                Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (setAmountManually.isEmpty()) {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = setAmountManually.toDoubleOrNull()
            if (amount == null) {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Prepare JSON object
            val json = JSONObject()
            json.put("categoryName", categoryName)
            json.put("colorCode", selectedColorHex)
            json.put("userID", userID)
            json.put("maxBudget", amount)

            val url = "https://smartspendapi.azurewebsites.net/api/Category/create"

            val body = RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                json.toString()
            )

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@DetailedView, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()

                    runOnUiThread {
                        if (response.isSuccessful) {
                            Toast.makeText(this@DetailedView, "Category created successfully", Toast.LENGTH_LONG).show()
                            // Update the categories list
                            fetchCategories()
                            dialog.dismiss()
                        } else {
                            val errorMessage = responseBody ?: "Category creation failed"
                            Toast.makeText(this@DetailedView, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            })
        }
    }

    // Category data class to represent category items
    data class Category(
        val categoryID: Int,
        val name: String,
        val amount: String,
        val colorCode: String
    )

    // Adapter to handle displaying categories in the RecyclerView
    class CategoryAdapter(private val categories: List<Category>) :
        RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

        // Inside the CategoryViewHolder inner class
        inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val categoryName: TextView = view.findViewById(R.id.categoryName)
            val categoryAmount: TextView = view.findViewById(R.id.categoryAmount)
            val categoryContainer: View = view  // Use the entire item view as the container
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_category, parent, false)
            return CategoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            val category = categories[position]
            holder.categoryName.text = category.name
            holder.categoryAmount.text = "R${category.amount}"

            try {
                val color = Color.parseColor(category.colorCode)
                holder.categoryContainer.setBackgroundColor(color)
            } catch (e: Exception) {
                // Handle invalid color code
            }
        }

        override fun getItemCount(): Int {
            return categories.size
        }
    }
}

package com.example.smartspend

import android.content.Intent
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
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.charts.Cartesian
import com.anychart.core.cartesian.series.Column
import com.anychart.enums.*
import com.anychart.graphics.vector.SolidFill
import com.anychart.graphics.vector.text.HAlign
import com.anychart.graphics.vector.text.VAlign
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

    private lateinit var barChart: AnyChartView
    private val categoryTotals = mutableListOf<CategoryTotal>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the layout file
        setContentView(R.layout.activity_detailed_view)

        // Initialize the bar chart view
        barChart = findViewById(R.id.barChart)

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

        // Fetch category totals for the bar chart
        fetchCategoryTotals()
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

    private fun fetchCategoryTotals() {
        val url = "https://smartspendapi.azurewebsites.net/api/Expense/totals/user/$userID"

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
                            categoryTotals.clear()
                            for (i in 0 until jsonArray.length()) {
                                val jsonObject = jsonArray.getJSONObject(i)
                                val categoryID = jsonObject.getInt("categoryID")
                                val categoryName = jsonObject.getString("categoryName")
                                val colorCode = jsonObject.getString("colorCode")
                                val maxBudget = jsonObject.getDouble("maxBudget")
                                val totalSpent = jsonObject.getDouble("totalSpent")

                                val categoryTotal = CategoryTotal(
                                    categoryID,
                                    categoryName,
                                    colorCode,
                                    maxBudget,
                                    totalSpent
                                )
                                categoryTotals.add(categoryTotal)
                            }
                            setupBarChart()
                        } catch (e: Exception) {
                            Toast.makeText(this@DetailedView, "Error parsing category totals", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@DetailedView, "Failed to fetch category totals", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun setupBarChart() {
        val cartesian: Cartesian = AnyChart.column()

        val data: MutableList<DataEntry> = ArrayList()
        for (categoryTotal in categoryTotals) {
            data.add(CustomDataEntry(categoryTotal.categoryName, categoryTotal.totalSpent, categoryTotal.colorCode))
        }

        val column: Column = cartesian.column(data)

        // Set the colors and styles
        cartesian.background().fill("#232323") // Set chart background color

        // Set axes labels and titles to white
        cartesian.xAxis(0).labels().fontColor("#FFFFFF")
        cartesian.yAxis(0).labels().fontColor("#FFFFFF")
        cartesian.xAxis(0).title().fontColor("#FFFFFF")
        cartesian.yAxis(0).title().fontColor("#FFFFFF")

        // Set chart title color
        cartesian.title().fontColor("#FFFFFF")

        // Customize tooltip
        cartesian.tooltip()
            .title(true)
            .titleFormat("{%X}")
            .format("R{%Value}{groupsSeparator: }")
            .background().fill("#232323") // Tooltip background color
        cartesian.tooltip().fontColor("#FFFFFF")

        // Customize legend
        cartesian.legend().enabled(false) // Disable legend if not needed

        column.tooltip()
            .titleFormat("{%X}")
            .position(Position.CENTER_BOTTOM)
            .anchor(Anchor.CENTER_BOTTOM)
            .offsetX(0.0)
            .offsetY(5.0)
            .format("R{%Value}{groupsSeparator: }")
            .fontColor("#FFFFFF") // Tooltip text color

        cartesian.animation(true)
        cartesian.title("Total Spent per Category")

        cartesian.yScale().minimum(0.0)

        cartesian.yAxis(0).labels().format("R{%Value}{groupsSeparator: }")

        cartesian.tooltip().positionMode(TooltipPositionMode.POINT)

        cartesian.interactivity().hoverMode(HoverMode.BY_X)

        cartesian.xAxis(0).title("Categories")
        cartesian.yAxis(0).title("Amount Spent")

        barChart.setChart(cartesian)
    }

    // Custom DataEntry class to include color
    inner class CustomDataEntry(
        x: String,
        value: Number,
        color: String
    ) : ValueDataEntry(x, value) {
        init {
            setValue("fill", color)
        }
    }

    // Data class for category totals
    data class CategoryTotal(
        val categoryID: Int,
        val categoryName: String,
        val colorCode: String,
        val maxBudget: Double,
        val totalSpent: Double
    )

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
                            fetchCategoryTotals() // Refresh the bar chart data
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

        inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val categoryName: TextView = view.findViewById(R.id.categoryName)
            val categoryAmount: TextView = view.findViewById(R.id.categoryAmount)
            val categoryContainer: View = view  // Use the entire item view as the container

            init {
                // Set click listener on the itemView
                view.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val category = categories[position]
                        // Start CategoryClicked activity
                        val intent = Intent(view.context, CategoryClicked::class.java)
                        intent.putExtra("categoryName", category.name)
                        intent.putExtra("colorCode", category.colorCode)
                        intent.putExtra("categoryID", category.categoryID) // Pass categoryID
                        // Pass other necessary data if needed
                        view.context.startActivity(intent)
                    }
                }
            }
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
                // Set the text color of the category name to the saved color
                holder.categoryName.setTextColor(color)
            } catch (e: Exception) {
                // Handle invalid color code
                holder.categoryName.setTextColor(Color.WHITE) // Set to default color
            }
        }

        override fun getItemCount(): Int {
            return categories.size
        }
    }
}

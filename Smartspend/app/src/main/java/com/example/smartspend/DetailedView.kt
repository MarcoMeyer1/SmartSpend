package com.example.smartspend

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.charts.Cartesian
import com.anychart.core.cartesian.series.Column
import com.anychart.enums.*
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class DetailedView : BaseActivity() {

    private var selectedColorHex: String = "#FFFFFF" // Default color
    private lateinit var categoryAdapter: CategoryAdapter
    private val client = OkHttpClient()
    private var userID: Int = -1

    private lateinit var barChart: AnyChartView
    private val categoryTotals = mutableListOf<CategoryTotal>()

    private lateinit var totalAmountTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_detailed_view)

        barChart = findViewById(R.id.barChart)

        totalAmountTextView = findViewById(R.id.edtAmount)

        // Retrieve user ID from SharedPreferences
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userID = sharedPreferences.getInt("userID", -1)

        // Sets up the RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.categoriesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        // Initialize the adapter with categoryTotals
        categoryAdapter = CategoryAdapter(categoryTotals)
        recyclerView.adapter = categoryAdapter

        val fabAddCategory: FloatingActionButton = findViewById(R.id.fabAddCategory)
        fabAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }

        fetchCategoryTotals()
    }

    // Fetches category totals from the server
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

            // Parses the category totals response
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonArray = JSONArray(responseBody)
                            categoryTotals.clear()
                            var totalExpenses = 0.0 // Variable to store the total expenses

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

                                totalExpenses += totalSpent // Add each category's totalSpent to totalExpenses
                            }

                            // Update the total expenses TextView with the calculated total
                            totalAmountTextView.text = "R${"%.2f".format(totalExpenses)}"

                            // Notify the adapter that data has changed
                            categoryAdapter.notifyDataSetChanged()

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

    // Sets up the bar chart
    private fun setupBarChart() {
        val cartesian: Cartesian = AnyChart.column()

        val data: MutableList<DataEntry> = ArrayList()
        for (categoryTotal in categoryTotals) {
            data.add(CustomDataEntry(categoryTotal.categoryName, categoryTotal.totalSpent, categoryTotal.colorCode))
        }

        val column: Column = cartesian.column(data)

        cartesian.background().fill("#232323")

        cartesian.xAxis(0).labels().fontColor("#FFFFFF")
        cartesian.yAxis(0).labels().fontColor("#FFFFFF")
        cartesian.xAxis(0).title().fontColor("#FFFFFF")
        cartesian.yAxis(0).title().fontColor("#FFFFFF")

        cartesian.title().fontColor("#FFFFFF")

        cartesian.tooltip()
            .title(true)
            .titleFormat("{%X}")
            .format("R{%Value}{groupsSeparator: }")
            .background().fill("#232323")
        cartesian.tooltip().fontColor("#FFFFFF")

        cartesian.legend().enabled(false)

        column.tooltip()
            .titleFormat("{%X}")
            .position(Position.CENTER_BOTTOM)
            .anchor(Anchor.CENTER_BOTTOM)
            .offsetX(0.0)
            .offsetY(5.0)
            .format("R{%Value}{groupsSeparator: }")
            .fontColor("#FFFFFF")

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

    // Custom data entry class for the bar chart
    inner class CustomDataEntry(
        x: String,
        value: Number,
        color: String
    ) : ValueDataEntry(x, value) {
        init {
            setValue("fill", color)
        }
    }

    // Data class to represent category totals
    data class CategoryTotal(
        val categoryID: Int,
        val categoryName: String,
        val colorCode: String,
        val maxBudget: Double,
        val totalSpent: Double
    )

    // Shows the add category dialog
    private fun showAddCategoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val editTextCategoryName: EditText = dialogView.findViewById(R.id.editTextCategoryName)
        val editTextSetAmountManually: EditText = dialogView.findViewById(R.id.editTextSetAmountManually)
        val viewSelectedColor: View = dialogView.findViewById(R.id.viewSelectedColor)
        val buttonPickColor: Button = dialogView.findViewById(R.id.buttonPickColor)
        val btnCreateCategory: MaterialButton = dialogView.findViewById(R.id.btnCreateCategory)

        val builder = AlertDialog.Builder(this, R.style.CustomDialog)
        builder.setTitle("Add New Category")
            .setView(dialogView)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()

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

        // Create Category Logic
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

            // Makes the API request
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
                            fetchCategoryTotals()
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

    // Adapter for the RecyclerView
    class CategoryAdapter(private val categories: List<CategoryTotal>) :
        RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

        inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val categoryName: TextView = view.findViewById(R.id.categoryName)
            val categoryAmount: TextView = view.findViewById(R.id.categoryAmount)
            val categoryContainer: View = view

            init {
                view.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val category = categories[position]
                        val intent = Intent(view.context, CategoryClicked::class.java)
                        intent.putExtra("categoryName", category.categoryName)
                        intent.putExtra("colorCode", category.colorCode)
                        intent.putExtra("categoryID", category.categoryID)
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
            holder.categoryName.text = category.categoryName

            // Calculate budget remaining
            val budgetRemaining = category.maxBudget - category.totalSpent
            holder.categoryAmount.text = "R${"%.2f".format(budgetRemaining)}"

            // Set text color based on budget remaining
            if (budgetRemaining < 0) {
                holder.categoryAmount.setTextColor(Color.RED)
            } else {
                holder.categoryAmount.setTextColor(Color.WHITE) // Or your default color
            }

            try {
                val color = Color.parseColor(category.colorCode)
                holder.categoryName.setTextColor(color)
            } catch (e: Exception) {
                holder.categoryName.setTextColor(Color.WHITE)
            }
        }


        // Returns the number of categories
        override fun getItemCount(): Int {
            return categories.size
        }
    }
}

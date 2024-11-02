package com.example.smartspend

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.*
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.core.cartesian.series.Column
import com.anychart.enums.Anchor
import com.anychart.enums.HoverMode
import com.anychart.enums.Position
import com.anychart.enums.TooltipPositionMode
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal

class DetailedView : BaseActivity() {

    private var selectedColorHex: String = "#FFFFFF" // Default color
    private lateinit var categoryAdapter: CategoryAdapter
    private val client = OkHttpClient()
    private var userID: Int = -1

    private lateinit var barChart: AnyChartView
    private val categoryTotals = mutableListOf<CategoryTotal>()

    private lateinit var totalAmountTextView: TextView

    private lateinit var appDatabase: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    private val apiBaseUrl = "https://smartspendapi.azurewebsites.net/api"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detailed_view)

        barChart = findViewById(R.id.barChart)

        totalAmountTextView = findViewById(R.id.edtAmount)

        // Retrieve user ID from SharedPreferences
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userID = sharedPreferences.getInt("userID", -1)

        // Initialize AppDatabase and CategoryDao
        appDatabase = AppDatabase.getDatabase(this)
        categoryDao = appDatabase.categoryDao()

        // Initialize ConnectivityManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // Register network callback
        registerNetworkCallback()

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

        // Initial sync if network is available
        if (isNetworkAvailable()) {
            syncLocalDataWithServer()
            fetchCategoryTotals()
        } else {
            Toast.makeText(this, "No internet connection. Categories will sync when online.", Toast.LENGTH_SHORT).show()
        }
    }

    // Register network callback
    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                // Network is available, trigger sync
                runOnUiThread {
                    syncLocalDataWithServer()
                    fetchCategoryTotals()
                }
            }
        }

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    // Check if network is available
    private fun isNetworkAvailable(): Boolean {
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities != null &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Fetches category totals from the server
    private fun fetchCategoryTotals() {
        val url = "$apiBaseUrl/Expense/totals/user/$userID"

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
                            Log.e("DetailedView", "Exception parsing category totals", e)
                        }
                    } else {
                        Toast.makeText(this@DetailedView, "Failed to fetch category totals", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    // Sync local unsynced categories with the server
    private fun syncLocalDataWithServer() {
        scope.launch {
            if (userID != -1) {
                val localUnsyncedCategories = withContext(Dispatchers.IO) {
                    categoryDao.getUnsyncedCategories(userID)
                }

                for (category in localUnsyncedCategories) {
                    createCategoryOnServer(category) { success ->
                        if (success) {
                            // Update local category to mark as synced
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    categoryDao.insertCategory(category.copy(isSynced = true))
                                }
                                // Fetch updated totals from server
                                fetchCategoryTotals()
                            }
                        }
                    }
                }
            }
        }
    }

    // Creates a new category on the server
    private fun createCategoryOnServer(category: CategoryEntity, callback: (Boolean) -> Unit) {
        val url = "$apiBaseUrl/Category/create"

        val json = JSONObject().apply {
            put("categoryName", category.categoryName)
            put("colorCode", category.colorCode)
            put("userID", category.userID)
            put("maxBudget", category.maxBudget)
        }

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
                    Toast.makeText(this@DetailedView, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                    callback(false)
                }
                Log.e("DetailedView", "Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("DetailedView", "Server Response: $responseBody")

                if (response.isSuccessful && responseBody != null) {
                    runOnUiThread {
                        try {
                            // Try to parse the response as JSON
                            val jsonResponse = JSONObject(responseBody)
                            val serverCategoryID = jsonResponse.getInt("categoryID")

                            // Update local category with server categoryID and mark as synced
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    categoryDao.deleteCategory(category)
                                    categoryDao.insertCategory(
                                        category.copy(categoryID = serverCategoryID, isSynced = true)
                                    )
                                }
                                // Fetch updated totals from server
                                fetchCategoryTotals()
                            }
                            callback(true)
                        } catch (e: Exception) {
                            // Handle response as plain text
                            if (responseBody.contains("Category created successfully", ignoreCase = true)) {
                                // Mark category as synced
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        categoryDao.updateCategory(
                                            category.copy(isSynced = true)
                                        )
                                    }
                                    // Fetch updated totals from server
                                    fetchCategoryTotals()
                                }
                                callback(true)
                                Toast.makeText(
                                    this@DetailedView,
                                    "Category created successfully.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@DetailedView,
                                    "Unexpected response: $responseBody",
                                    Toast.LENGTH_LONG
                                ).show()
                                callback(false)
                            }
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@DetailedView,
                            "Server error: ${response.code}",
                            Toast.LENGTH_LONG
                        ).show()
                        callback(false)
                    }
                    Log.e("DetailedView", "Server error: ${response.code}")
                }
            }
        })
    }

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

            val amountDecimal = setAmountManually.toBigDecimalOrNull()
            if (amountDecimal == null) {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newCategory = CategoryEntity(
                categoryID = 0,
                categoryName = categoryName,
                colorCode = selectedColorHex,
                userID = userID,
                maxBudget = amountDecimal
            )

            scope.launch {
                // Save to local database
                withContext(Dispatchers.IO) {
                    categoryDao.insertCategory(newCategory)
                }

                if (isNetworkAvailable()) {
                    // Sync with server
                    createCategoryOnServer(newCategory) { success ->
                        if (success) {
                            // Fetch updated totals from server
                            fetchCategoryTotals()
                            dialog.dismiss()
                        } else {
                            Toast.makeText(
                                this@DetailedView,
                                "Failed to sync with server",
                                Toast.LENGTH_SHORT
                            ).show()
                            dialog.dismiss()
                        }
                    }
                } else {
                    Toast.makeText(
                        this@DetailedView,
                        "Saved locally. Will sync when online.",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                }
            }
        }
    }

    // Sets up the bar chart
    private fun setupBarChart() {
        val cartesian = AnyChart.column()

        val data = categoryTotals.map {
            ValueDataEntry(it.categoryName, it.totalSpent).apply {
                setValue("fill", it.colorCode)
            }
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

    // Data class to represent category totals
    data class CategoryTotal(
        val categoryID: Int,
        val categoryName: String,
        val colorCode: String,
        val maxBudget: Double,
        val totalSpent: Double
    )

    // Adapter for the RecyclerView
    class CategoryAdapter(private val categories: List<CategoryTotal>) :
        RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

        inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val categoryName: TextView = view.findViewById(R.id.categoryName)
            val categoryAmount: TextView = view.findViewById(R.id.categoryAmount)

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
                holder.categoryAmount.setTextColor(Color.WHITE)
            }

            try {
                val color = Color.parseColor(category.colorCode)
                holder.categoryName.setTextColor(color)
            } catch (e: Exception) {
                holder.categoryName.setTextColor(Color.WHITE)
            }
        }

        override fun getItemCount(): Int {
            return categories.size
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}

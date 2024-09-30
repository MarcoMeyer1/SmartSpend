package com.example.smartspend

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

class MainActivity : BaseActivity() {

    private lateinit var detailedViewButton: Button
    private lateinit var savingGoalsButton: Button
    private lateinit var remindersButton: Button
    private lateinit var historyButton: Button

    private lateinit var budgetAmountTextView: TextView
    private lateinit var anyChartView: AnyChartView

    private val client = OkHttpClient()
    private var userID: Int = -1
    private val categoryTotals = mutableListOf<CategoryTotal>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setActiveNavButton(R.id.home_nav)

        // Initializes the buttons
        detailedViewButton = findViewById(R.id.buttonDetailedView)
        savingGoalsButton = findViewById(R.id.buttonSavingGoals)
        remindersButton = findViewById(R.id.buttonReminders)
        historyButton = findViewById(R.id.buttonHistory)

        // Initializes the TextView and AnyChartView
        budgetAmountTextView = findViewById(R.id.budgetAmount)
        anyChartView = findViewById(R.id.anyChartView)

        // Retrieves the user ID from SharedPreferences
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userID = sharedPreferences.getInt("userID", -1)

        fetchCategoryTotals()

        detailedViewButton.setOnClickListener {
            openDetailedView()
        }

        savingGoalsButton.setOnClickListener {
            openSavingGoals()
        }

        remindersButton.setOnClickListener {
            openReminders()
        }

        historyButton.setOnClickListener {
            openHistory()
        }
    }

    // Fetches category totals from the server
    private fun fetchCategoryTotals() {
        val url = "https://smartspendapi.azurewebsites.net/api/Expense/totals/user/$userID"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        // Makes the network request
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonArray = JSONArray(responseBody)
                            categoryTotals.clear()
                            var totalBudget = 0.0
                            var totalExpenses = 0.0

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

                                totalBudget += maxBudget
                                totalExpenses += totalSpent
                            }

                            val remainingBudget = totalBudget - totalExpenses

                            // Updates the budget amount TextView
                            budgetAmountTextView.text = "R${"%.2f".format(remainingBudget)}"

                            setupChart()

                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Error parsing category totals", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to fetch category totals", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    // Sets up the chart
    private fun setupChart() {
        val pie = AnyChart.pie()

        val dataEntries = ArrayList<DataEntry>()

        for (categoryTotal in categoryTotals) {
            dataEntries.add(ValueDataEntry(categoryTotal.categoryName, categoryTotal.totalSpent))
        }

        pie.data(dataEntries)

        pie.title("Expenses per Category")


        pie.background().fill("#272727")
        pie.labels().fontColor("#FFFFFF")
        pie.title().fontColor("#FFFFFF")
        pie.legend().enabled(true)
        pie.legend().title().enabled(false)
        pie.legend().fontColor("#FFFFFF")

        anyChartView.setChart(pie)
    }


    private fun openDetailedView() {
        val intent = Intent(this, DetailedView::class.java)
        startActivity(intent)
    }

    private fun openSavingGoals() {
        val intent = Intent(this, SavingGoals::class.java)
        startActivity(intent)
    }

    private fun openReminders() {
        val intent = Intent(this, Reminders::class.java)
        startActivity(intent)
    }


    private fun openHistory() {
        val intent = Intent(this, History::class.java)
        startActivity(intent)
    }

    // Represents a category total
    data class CategoryTotal(
        val categoryID: Int,
        val categoryName: String,
        val colorCode: String,
        val maxBudget: Double,
        val totalSpent: Double
    )
}

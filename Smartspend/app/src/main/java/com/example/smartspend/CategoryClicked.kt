package com.example.smartspend

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CategoryClicked : AppCompatActivity() {

    private lateinit var tvCategoryTitle: TextView
    private lateinit var ivEditCategory: ImageView
    private lateinit var tvAvailableBudget: TextView
    private lateinit var recyclerViewHistory: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter

    private var categoryName: String? = null
    private var colorCode: String? = null
    private var categoryID: Int = -1
    private var userID: Int = -1

    private val client = OkHttpClient()
    public val transactions = mutableListOf<Transaction>()
    private var maxBudget: Double = 0.0
    private var usedBudget: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_category_clicked)

        // Adjust padding for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize UI elements
        tvCategoryTitle = findViewById(R.id.tv_category_title)
        ivEditCategory = findViewById(R.id.iv_edit_category)
        tvAvailableBudget = findViewById(R.id.tv_available_budget)
        recyclerViewHistory = findViewById(R.id.recycler_view_history)

        // Initialize RecyclerView
        transactionAdapter = TransactionAdapter(transactions)
        recyclerViewHistory.layoutManager = LinearLayoutManager(this)
        recyclerViewHistory.adapter = transactionAdapter

        // Get userID from SharedPreferences
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userID = sharedPreferences.getInt("userID", -1)

        // Retrieve Intent extras
        categoryName = intent.getStringExtra("categoryName")
        colorCode = intent.getStringExtra("colorCode")
        categoryID = intent.getIntExtra("categoryID", -1)

        // Set the title text and color
        tvCategoryTitle.text = categoryName ?: "Category"
        try {
            val color = Color.parseColor(colorCode)
            tvCategoryTitle.setTextColor(color)
        } catch (e: Exception) {
            // Handle invalid color code
            tvCategoryTitle.setTextColor(Color.WHITE) // Set to default color
        }

        if (categoryID != -1) {
            fetchCategoryDetails()
            fetchExpenses()
        } else {
            Toast.makeText(this, "Invalid category ID", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun fetchCategoryDetails() {
        val url = "https://smartspendapi.azurewebsites.net/api/Category/$categoryID"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@CategoryClicked, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonObject = JSONObject(responseBody)
                            maxBudget = jsonObject.getDouble("maxBudget")
                            updateAvailableBudget()
                        } catch (e: Exception) {
                            Toast.makeText(this@CategoryClicked, "Error parsing category details", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@CategoryClicked, "Failed to fetch category details", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    public fun fetchExpenses() {
        val url = "https://smartspendapi.azurewebsites.net/api/Expense/category/$userID/$categoryID"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@CategoryClicked, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonArray = JSONArray(responseBody)
                            transactions.clear()
                            usedBudget = 0.0
                            for (i in 0 until jsonArray.length()) {
                                val jsonObject = jsonArray.getJSONObject(i)
                                val expenseName = jsonObject.getString("expenseName")
                                val amount = jsonObject.getDouble("amount")
                                val expenseDate = jsonObject.getString("expenseDate")

                                usedBudget += amount

                                val transaction = Transaction(expenseDate, expenseName, amount)
                                transactions.add(transaction)
                            }
                            transactionAdapter.notifyDataSetChanged()
                            updateAvailableBudget()
                        } catch (e: Exception) {
                            Toast.makeText(this@CategoryClicked, "Error parsing expenses", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@CategoryClicked, "No expenses found for this category", Toast.LENGTH_LONG).show()
                        updateAvailableBudget()
                    }
                }
            }
        })
    }

    private fun updateAvailableBudget() {
        val remainingBudget = maxBudget - usedBudget
        tvAvailableBudget.text = "R${remainingBudget.format(2)}/R${maxBudget.format(2)}"
    }

    // Extension function to format Double values
    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    // Transaction data class
    data class Transaction(
        val date: String,
        val description: String,
        val amount: Double
    )

    // Adapter for transactions
    // Adapter for transactions
    class TransactionAdapter(private val transactions: List<Transaction>) :
        RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

        inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvDate: TextView = itemView.findViewById(R.id.tv_date)
            val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
            val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction, parent, false)
            return TransactionViewHolder(view)
        }

        override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
            val transaction = transactions[position]

            // Format the date
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(transaction.date)
            val dateString = outputFormat.format(date)

            holder.tvDate.text = dateString
            holder.tvDescription.text = transaction.description
            holder.tvAmount.text = "R${transaction.amount.format(2)}"
        }

        override fun getItemCount(): Int {
            return transactions.size
        }

        // Extension function to format Double values
        private fun Double.format(digits: Int) = "%.${digits}f".format(this)
    }
}

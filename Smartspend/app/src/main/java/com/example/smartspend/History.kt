package com.example.smartspend

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class History : BaseActivity() {

    private val client = OkHttpClient()

    private lateinit var rvTransactionHistory: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private val transactions = mutableListOf<Transaction>()

    private lateinit var tvIncomeValue: TextView
    private lateinit var tvExpensesValue: TextView

    private var userID: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)

        // Initialize RecyclerView and its adapter
        rvTransactionHistory = findViewById(R.id.rv_transaction_history)
        rvTransactionHistory.layoutManager = LinearLayoutManager(this)
        transactionAdapter = TransactionAdapter(transactions)
        rvTransactionHistory.adapter = transactionAdapter

        // Initialize TextViews for total income and expenses
        tvIncomeValue = findViewById(R.id.tv_income_value)
        tvExpensesValue = findViewById(R.id.tv_expenses_value)

        // Get userID from SharedPreferences
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userID = sharedPreferences.getInt("userID", -1)

        if (userID != -1) {
            fetchData()
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun fetchData() {
        fetchIncome()
        fetchExpenses()
    }

    private var incomeList = mutableListOf<Transaction>()
    private var expenseList = mutableListOf<Transaction>()

    private var totalIncome = 0.0
    private var totalExpenses = 0.0

    private fun fetchIncome() {
        val url = "https://smartspendapi.azurewebsites.net/api/Income/$userID"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@History, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonArray = JSONArray(responseBody)
                            incomeList.clear()
                            totalIncome = 0.0
                            for (i in 0 until jsonArray.length()) {
                                val jsonObject = jsonArray.getJSONObject(i)
                                val incomeReference = jsonObject.getString("incomeReference")
                                val amount = jsonObject.getDouble("amount")
                                val incomeDate = jsonObject.getString("incomeDate")

                                totalIncome += amount

                                val transaction = Transaction(
                                    date = incomeDate,
                                    description = incomeReference,
                                    amount = amount,
                                    isIncome = true
                                )
                                incomeList.add(transaction)
                            }
                            updateTotalIncome()
                            mergeAndSortTransactions()
                        } catch (e: Exception) {
                            Toast.makeText(this@History, "Error parsing income data", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@History, "No income entries found", Toast.LENGTH_LONG).show()
                        updateTotalIncome()
                        mergeAndSortTransactions()
                    }
                }
            }
        })
    }

    private fun fetchExpenses() {
        val url = "https://smartspendapi.azurewebsites.net/api/Expense/$userID"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@History, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonArray = JSONArray(responseBody)
                            expenseList.clear()
                            totalExpenses = 0.0
                            for (i in 0 until jsonArray.length()) {
                                val jsonObject = jsonArray.getJSONObject(i)
                                val expenseName = jsonObject.getString("expenseName")
                                val amount = jsonObject.getDouble("amount")
                                val expenseDate = jsonObject.getString("expenseDate")

                                totalExpenses += amount

                                val transaction = Transaction(
                                    date = expenseDate,
                                    description = expenseName,
                                    amount = amount,
                                    isIncome = false
                                )
                                expenseList.add(transaction)
                            }
                            updateTotalExpenses()
                            mergeAndSortTransactions()
                        } catch (e: Exception) {
                            Toast.makeText(this@History, "Error parsing expenses data", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@History, "No expenses found", Toast.LENGTH_LONG).show()
                        updateTotalExpenses()
                        mergeAndSortTransactions()
                    }
                }
            }
        })
    }

    private fun updateTotalIncome() {
        tvIncomeValue.text = "R${totalIncome.format(2)}"
    }

    private fun updateTotalExpenses() {
        tvExpensesValue.text = "R${totalExpenses.format(2)}"
    }

    private fun mergeAndSortTransactions() {
        transactions.clear()
        transactions.addAll(incomeList)
        transactions.addAll(expenseList)

        transactions.sortByDescending { parseDate(it.date) }

        transactionAdapter.notifyDataSetChanged()
    }

    private fun parseDate(dateString: String): Date {
        val inputFormats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        )
        for (format in inputFormats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                return sdf.parse(dateString)!!
            } catch (e: Exception) {
                continue
            }
        }
        return Date()
    }

    // Extension function to format Double values
    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    // Transaction data class
    data class Transaction(
        val date: String,
        val description: String,
        val amount: Double,
        val isIncome: Boolean
    )

    // Adapter for transactions
    class TransactionAdapter(private val transactions: List<Transaction>) :
        RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

        inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvDate: TextView = itemView.findViewById(R.id.tv_transaction_date)
            val tvDescription: TextView = itemView.findViewById(R.id.tv_transaction_title)
            val tvAmount: TextView = itemView.findViewById(R.id.tv_transaction_amount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction_card, parent, false)
            return TransactionViewHolder(view)
        }

        override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
            val transaction = transactions[position]

            // Format the date
            val inputFormats = arrayOf(
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
            )
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            var date: Date? = null
            for (format in inputFormats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.getDefault())
                    date = sdf.parse(transaction.date)
                    break
                } catch (e: Exception) {
                    continue
                }
            }
            val dateString = if (date != null) outputFormat.format(date) else transaction.date

            holder.tvDate.text = dateString
            holder.tvDescription.text = transaction.description

            if (transaction.isIncome) {
                holder.tvAmount.text = "+ R${transaction.amount.format(2)}"
                holder.tvAmount.setTextColor(Color.parseColor("#70FFB5")) // Green color
            } else {
                holder.tvAmount.text = "- R${transaction.amount.format(2)}"
                holder.tvAmount.setTextColor(Color.parseColor("#FF7070")) // Red color
            }
        }

        override fun getItemCount(): Int {
            return transactions.size
        }

        // Extension function to format Double values
        private fun Double.format(digits: Int) = "%.${digits}f".format(this)
    }
}

package com.example.smartspend

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.EditText
import android.widget.Spinner
import android.widget.ArrayAdapter

abstract class BaseActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private var categoryList: List<Category> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Do not call setContentView here
        // setupNavbar will be called after setContentView in child classes
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        setupNavbar()
    }

    private fun setupNavbar() {
        val homeNavContainer: LinearLayout = findViewById(R.id.home_nav_container)
        val notificationsNavContainer: LinearLayout = findViewById(R.id.notifications_nav_container)
        val addRecordNavContainer: LinearLayout = findViewById(R.id.add_record_nav_container)
        val historyNavContainer: LinearLayout = findViewById(R.id.history_nav_container)
        val settingsNavContainer: LinearLayout = findViewById(R.id.settings_nav_container)

        homeNavContainer.setOnClickListener {
            if (this !is MainActivity) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }

        notificationsNavContainer.setOnClickListener {
            val intent = Intent(this, Notifications::class.java)
            startActivity(intent)
        }

        addRecordNavContainer.setOnClickListener {
            showAddRecordDialog()
        }

        historyNavContainer.setOnClickListener {
            // TODO: Implement history navigation
        }

        settingsNavContainer.setOnClickListener {
            val intent = Intent(this, Settings::class.java)
            startActivity(intent)
        }

        resetNavIcons()
    }

    private fun resetNavIcons() {
        val homeNav: ImageView = findViewById(R.id.home_nav)
        val notificationsNav: ImageView = findViewById(R.id.notifications_nav)
        val addRecordNav: ImageView = findViewById(R.id.add_record_nav)
        val historyNav: ImageView = findViewById(R.id.history_nav)
        val settingsNav: ImageView = findViewById(R.id.settings_nav)

        homeNav.setColorFilter(Color.parseColor("#FFFFFF"))
        notificationsNav.setColorFilter(Color.parseColor("#FFFFFF"))
        addRecordNav.setColorFilter(Color.parseColor("#FFFFFF"))
        historyNav.setColorFilter(Color.parseColor("#FFFFFF"))
        settingsNav.setColorFilter(Color.parseColor("#FFFFFF"))
    }

    protected fun setActiveNavButton(navButtonId: Int?) {
        resetNavIcons()

        when (navButtonId) {
            R.id.home_nav -> findViewById<ImageView>(R.id.home_nav)?.setColorFilter(Color.parseColor("#70FFB5"))
            R.id.notifications_nav -> findViewById<ImageView>(R.id.notifications_nav)?.setColorFilter(Color.parseColor("#70FFB5"))
            R.id.add_record_nav -> findViewById<ImageView>(R.id.add_record_nav)?.setColorFilter(Color.parseColor("#70FFB5"))
            R.id.history_nav -> findViewById<ImageView>(R.id.history_nav)?.setColorFilter(Color.parseColor("#70FFB5"))
            R.id.settings_nav -> findViewById<ImageView>(R.id.settings_nav)?.setColorFilter(Color.parseColor("#70FFB5"))
        }
    }

    private fun showAddRecordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_record, null)
        val incomeButton: Button = dialogView.findViewById(R.id.button_income)
        val expenseButton: Button = dialogView.findViewById(R.id.button_expense)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView).setCancelable(true)

        val dialog = builder.create()
        dialog.show()

        incomeButton.setOnClickListener {
            showIncomeDialog()
            dialog.dismiss()
        }

        expenseButton.setOnClickListener {
            fetchCategoriesAndShowExpenseDialog()
            dialog.dismiss()
        }
    }

    private fun showIncomeDialog() {
        val incomeDialogView = layoutInflater.inflate(R.layout.dialog_add_income, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(incomeDialogView).setCancelable(true)
        val dialog = builder.create()
        dialog.show()

        val etReference: EditText = incomeDialogView.findViewById(R.id.et_reference)
        val etAmount: EditText = incomeDialogView.findViewById(R.id.et_amount)
        val confirmButton: Button = incomeDialogView.findViewById(R.id.btn_confirm)

        confirmButton.setOnClickListener {
            val amountStr = etAmount.text.toString().trim()
            val reference = etReference.text.toString().trim()

            if (amountStr.isEmpty()) {
                etAmount.error = "Amount is required"
                etAmount.requestFocus()
                return@setOnClickListener
            }
            if (reference.isEmpty()) {
                etReference.error = "Record Reference is required"
                etReference.requestFocus()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null) {
                etAmount.error = "Invalid amount"
                etAmount.requestFocus()
                return@setOnClickListener
            }

            val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val userID = sharedPreferences.getInt("userID", -1)
            if (userID == -1) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@setOnClickListener
            }

            // Format the current date to ISO 8601
            val currentDateTime = Date()
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) // ISO 8601 format
            val incomeDate = formatter.format(currentDateTime)

            val incomeJson = JSONObject()
            incomeJson.put("userID", userID)
            incomeJson.put("incomeReference", reference)
            incomeJson.put("amount", amount)
            incomeJson.put("incomeDate", incomeDate)

            sendIncomeDataToServer(incomeJson)
            dialog.dismiss()
        }
    }

    private fun sendIncomeDataToServer(incomeJson: JSONObject) {
        val url = "https://smartspendapi.azurewebsites.net/api/Income/create"
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, incomeJson.toString())

        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@BaseActivity, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@BaseActivity, "Income added successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorMessage = responseBody ?: "Failed to add income"
                        Toast.makeText(this@BaseActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun fetchCategoriesAndShowExpenseDialog() {
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val userID = sharedPreferences.getInt("userID", -1)
        if (userID == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "https://smartspendapi.azurewebsites.net/api/Category/user/$userID"
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@BaseActivity, "Failed to load categories: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val categoriesJsonArray = JSONArray(responseBody)
                    categoryList = mutableListOf()

                    for (i in 0 until categoriesJsonArray.length()) {
                        val categoryJson = categoriesJsonArray.getJSONObject(i)
                        val category = Category(
                            categoryID = categoryJson.getInt("categoryID"),
                            categoryName = categoryJson.getString("categoryName"),
                            colorCode = categoryJson.getString("colorCode"),
                            userID = categoryJson.getInt("userID"),
                            maxBudget = categoryJson.getDouble("maxBudget")
                        )
                        (categoryList as MutableList<Category>).add(category)
                    }

                    runOnUiThread {
                        showExpenseDialog()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@BaseActivity, "Failed to load categories", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun showExpenseDialog() {
        val expenseDialogView = layoutInflater.inflate(R.layout.dialog_add_expense, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(expenseDialogView).setCancelable(true)
        val dialog = builder.create()
        dialog.show()

        val etExpenseName: EditText = expenseDialogView.findViewById(R.id.expenseName)
        val spinnerCategory: Spinner = expenseDialogView.findViewById(R.id.spinner_category)
        val etAmount: EditText = expenseDialogView.findViewById(R.id.amount)
        val confirmButton: Button = expenseDialogView.findViewById(R.id.confirmButton)

        // Populate Spinner with categories
        val categoryNames = categoryList.map { it.categoryName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        confirmButton.setOnClickListener {
            val amountStr = etAmount.text.toString().trim()
            val expenseName = etExpenseName.text.toString().trim()
            val selectedCategoryPosition = spinnerCategory.selectedItemPosition

            if (amountStr.isEmpty()) {
                etAmount.error = "Amount is required"
                etAmount.requestFocus()
                return@setOnClickListener
            }
            if (expenseName.isEmpty()) {
                etExpenseName.error = "Expense Name is required"
                etExpenseName.requestFocus()
                return@setOnClickListener
            }
            if (selectedCategoryPosition == Spinner.INVALID_POSITION) {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null) {
                etAmount.error = "Invalid amount"
                etAmount.requestFocus()
                return@setOnClickListener
            }

            val categoryID = categoryList[selectedCategoryPosition].categoryID

            val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val userID = sharedPreferences.getInt("userID", -1)
            if (userID == -1) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@setOnClickListener
            }

            val currentDateTime = Date()
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) // ISO 8601 format
            val expenseDate = formatter.format(currentDateTime)

            val expenseJson = JSONObject()
            expenseJson.put("userID", userID)
            expenseJson.put("expenseName", expenseName)
            expenseJson.put("categoryID", categoryID)
            expenseJson.put("amount", amount)
            expenseJson.put("expenseDate", expenseDate)

            sendExpenseDataToServer(expenseJson)
            dialog.dismiss()
        }
    }

    private fun sendExpenseDataToServer(expenseJson: JSONObject) {
        val url = "https://smartspendapi.azurewebsites.net/api/Expense/create"
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, expenseJson.toString())

        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@BaseActivity, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@BaseActivity, "Expense added successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorMessage = responseBody ?: "Failed to add expense"
                        Toast.makeText(this@BaseActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // Define the Category data class
    data class Category(
        val categoryID: Int,
        val categoryName: String,
        val colorCode: String,
        val userID: Int,
        val maxBudget: Double
    )
}

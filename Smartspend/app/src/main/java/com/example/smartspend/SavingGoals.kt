package com.example.smartspend

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class SavingGoals : BaseActivity() {

    private lateinit var recyclerViewGoals: RecyclerView
    private lateinit var goalAdapter: GoalAdapter
    private val goalList = mutableListOf<Goal>()
    private val client = OkHttpClient()

    private val apiBaseUrl = "https://smartspendapi.azurewebsites.net/api"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_saving_goals)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setActiveNavButton(null)

        val fabAddGoal: FloatingActionButton = findViewById(R.id.fabAddGoal)
        recyclerViewGoals = findViewById(R.id.recyclerViewGoals)
        recyclerViewGoals.layoutManager = LinearLayoutManager(this)
        goalAdapter = GoalAdapter(goalList)
        recyclerViewGoals.adapter = goalAdapter

        // Fetch goals from API and populate RecyclerView
        fetchGoals()

        fabAddGoal.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_goal, null)
            builder.setView(dialogView)
            val dialog = builder.create()

            val goalNameEditText: EditText = dialogView.findViewById(R.id.goalName)
            val totalAmountEditText: EditText = dialogView.findViewById(R.id.totalAmount)
            val savedAmountEditText: EditText = dialogView.findViewById(R.id.savedAmount)
            val saveButton: Button = dialogView.findViewById(R.id.saveButton)

            saveButton.setOnClickListener {
                val goalName = goalNameEditText.text.toString()
                val totalAmount = totalAmountEditText.text.toString()
                val savedAmount = savedAmountEditText.text.toString()

                if (goalName.isNotEmpty() && totalAmount.isNotEmpty() && savedAmount.isNotEmpty()) {
                    val totalAmountDecimal = totalAmount.toBigDecimalOrNull()
                    val savedAmountDecimal = savedAmount.toBigDecimalOrNull()

                    if (totalAmountDecimal == null || savedAmountDecimal == null) {
                        Toast.makeText(
                            this,
                            "Please enter valid amounts",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    // Fetch the userID from SharedPreferences
                    val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                    val userID = sharedPreferences.getInt("userID", -1)

                    if (userID != -1) {
                        val newGoal = Goal(
                            goalID = 0, // Will be set by the server
                            userID = userID,
                            goalName = goalName,
                            totalAmount = totalAmountDecimal,
                            savedAmount = savedAmountDecimal,
                            completionDate = null
                        )

                        createGoal(newGoal) { success ->
                            if (success) {
                                // Goal created successfully, refresh the goals list
                                fetchGoals()
                                dialog.dismiss()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Failed to create goal",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "User ID not found. Please log in.", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
                }
            }

            dialog.show()
        }
    }

    private fun fetchGoals() {
        // Fetch the userID from SharedPreferences
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val userID = sharedPreferences.getInt("userID", -1)

        if (userID != -1) {
            val url = "$apiBaseUrl/Goal/$userID"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@SavingGoals, "Failed to load goals", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()

                    runOnUiThread {
                        if (response.isSuccessful && responseBody != null) {
                            val jsonArray = JSONArray(responseBody)
                            goalList.clear()
                            for (i in 0 until jsonArray.length()) {
                                val jsonObject = jsonArray.getJSONObject(i)
                                val goal = Goal(
                                    goalID = jsonObject.getInt("goalID"),
                                    userID = jsonObject.getInt("userID"),
                                    goalName = jsonObject.getString("goalName"),
                                    totalAmount = BigDecimal(jsonObject.getDouble("totalAmount")),
                                    savedAmount = BigDecimal(jsonObject.getDouble("savedAmount")),
                                    completionDate = jsonObject.optString("completionDate", null)
                                )
                                goalList.add(goal)
                            }
                            goalAdapter.notifyDataSetChanged()
                        } else {
                            Toast.makeText(
                                this@SavingGoals,
                                "Failed to load goals",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            })
        } else {
            Toast.makeText(this, "User ID not found. Please log in.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createGoal(goal: Goal, callback: (Boolean) -> Unit) {
        val url = "$apiBaseUrl/Goal/create"

        val json = JSONObject()
        json.put("userID", goal.userID)
        json.put("goalName", goal.goalName)
        json.put("totalAmount", goal.totalAmount)
        json.put("savedAmount", goal.savedAmount)
        json.put("completionDate", JSONObject.NULL)

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
                    callback(false)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    callback(response.isSuccessful)
                }
            }
        })
    }

    // Method to handle the login and save userID to SharedPreferences
    fun loginUser(email: String, password: String) {
        val url = "$apiBaseUrl/login"

        val json = JSONObject()
        json.put("email", email)
        json.put("password", password)

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@SavingGoals, "Login failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)

                    // Assuming the response contains a field called "userID"
                    val userID = jsonResponse.getInt("userID")

                    // Save the userID to SharedPreferences
                    saveUserID(userID)

                    runOnUiThread {
                        Toast.makeText(this@SavingGoals, "Login successful", Toast.LENGTH_SHORT).show()
                        // Redirect to the main activity or dashboard
                    }
                }
            }
        })
    }

    // Method to save userID to SharedPreferences
    private fun saveUserID(userID: Int) {
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("userID", userID)
        editor.apply()
    }
}

// Data class for Goal
data class Goal(
    val goalID: Int,
    val userID: Int,
    val goalName: String,
    val totalAmount: BigDecimal,
    val savedAmount: BigDecimal,
    val completionDate: String?
)

// Adapter for RecyclerView
class GoalAdapter(private val goals: List<Goal>) :
    RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGoalTitle: TextView = itemView.findViewById(R.id.tvGoalTitle)
        val tvGoalProgress: TextView = itemView.findViewById(R.id.tvGoalProgress)
        val tvGoalPercentage: TextView = itemView.findViewById(R.id.tvGoalPercentage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.goal_card_layout, parent, false)
        return GoalViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]
        holder.tvGoalTitle.text = goal.goalName

        // Format amounts with currency symbols
        val numberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
        holder.tvGoalProgress.text =
            "${numberFormat.format(goal.savedAmount)}/${numberFormat.format(goal.totalAmount)}"

        val progressPercentage = (goal.savedAmount.divide(goal.totalAmount, 2, BigDecimal.ROUND_HALF_UP)
            .multiply(BigDecimal(100))).toInt()
        holder.tvGoalPercentage.text = "$progressPercentage%"

        // Set the color of the percentage text based on progress
        val percentageColor = when {
            progressPercentage <= 30 -> {
                Color.parseColor("#FFB3B3") // Pastel red
            }
            progressPercentage <= 60 -> {
                Color.parseColor("#FFD9B3") // Pastel orange
            }
            progressPercentage <= 90 -> {
                Color.parseColor("#FFFFCC") // Pastel yellow
            }
            else -> {
                Color.parseColor("#B3FFB3") // Pastel green
            }
        }

        holder.tvGoalPercentage.setTextColor(percentageColor)
    }

    override fun getItemCount(): Int {
        return goals.size
    }
}

package com.example.smartspend

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*
import android.animation.ObjectAnimator
import android.util.Log
import java.text.SimpleDateFormat

class GoalDetails : AppCompatActivity() {

    private lateinit var tvGoalName: TextView
    private lateinit var tvGoalAmount: TextView
    private lateinit var progressGoalCompletion: ProgressBar
    private lateinit var tvGoalPercentage: TextView
    private lateinit var appDatabase: AppDatabase
    private val client = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val apiBaseUrl = "https://smartspendapi.azurewebsites.net/api"

    private var goalID: Int = -1
    private var goalName: String? = null
    private var totalAmount: BigDecimal? = null
    private var savedAmount: BigDecimal? = null
    private var completionDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_goal_details)

        // Initialize views
        tvGoalName = findViewById(R.id.tvGoalName)
        tvGoalAmount = findViewById(R.id.tvGoalAmount)
        progressGoalCompletion = findViewById(R.id.progressGoalCompletion)
        tvGoalPercentage = findViewById(R.id.tvGoalPercentage)

        appDatabase = AppDatabase.getDatabase(this)

        // Get intent extras
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val userID = sharedPreferences.getInt("userID", -1)

        goalID = intent.getIntExtra("goalID", -1)
        goalName = intent.getStringExtra("goalName")
        totalAmount = intent.getStringExtra("totalAmount")?.toBigDecimalOrNull()
        savedAmount = intent.getStringExtra("savedAmount")?.toBigDecimalOrNull()
        completionDate = intent.getStringExtra("completionDate")

        // Update UI
        tvGoalName.text = goalName
        // Set progress with animation
        if (totalAmount != null && savedAmount != null) {
            val numberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
            tvGoalAmount.text = "${numberFormat.format(savedAmount)} / ${numberFormat.format(totalAmount)}"

            val progressPercentage = (savedAmount!!.divide(totalAmount, 2, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal(100))).toInt()

            tvGoalPercentage.text = "$progressPercentage%"

            // Animate progress
            val animator = ObjectAnimator.ofInt(progressGoalCompletion, "progress", 0, progressPercentage)
            animator.duration = 2000
            animator.start()
        }

        val addToGoal: Button = findViewById(R.id.btnAddToGoal)
        addToGoal.setOnClickListener {
            showAddToGoalDialog()
        }
    }

    private fun showAddToGoalDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_to_goal, null)

        val amountEditText = dialogView.findViewById<EditText>(R.id.edtAmount)
        val addToGoalButton = dialogView.findViewById<Button>(R.id.btnAddtoGoal)

        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(dialogView)

        val alertDialog = dialogBuilder.create()
        alertDialog.show()

        addToGoalButton.setOnClickListener {
            val enteredAmountStr = amountEditText.text.toString()
            val enteredAmount = enteredAmountStr.toBigDecimalOrNull()
            if (enteredAmount != null) {
                // Logging the entered amount
                Log.d("GoalDetails", "Entered amount: $enteredAmount")

                // Update savedAmount safely
                val newSavedAmount = savedAmount?.add(enteredAmount)

                // Logging the updated saved amount
                Log.d("GoalDetails", "Updated saved amount: $newSavedAmount")

                // Update UI
                if (totalAmount != null && newSavedAmount != null) {
                    val numberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
                    tvGoalAmount.text = "${numberFormat.format(newSavedAmount)} / ${numberFormat.format(totalAmount)}"

                    val progressPercentage = (newSavedAmount.divide(totalAmount, 2, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal(100))).toInt()

                    tvGoalPercentage.text = "$progressPercentage%"
                    progressGoalCompletion.progress = progressPercentage

                    // Check if the goal has been met
                    if (newSavedAmount >= totalAmount) {
                        completionDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        Log.d("GoalDetails", "Goal met! Completion date set to: $completionDate")
                    } else {
                        completionDate = null // Explicitly setting to null when the goal isn't met
                    }
                }
                // Update backend
                savedAmount = newSavedAmount // Update savedAmount with the new value
                updateGoalOnServer()
                alertDialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateGoalOnServer() {
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val userID = sharedPreferences.getInt("userID", -1)
        if (userID != -1 && goalID != -1 && goalName != null && totalAmount != null && savedAmount != null) {
            val url = "$apiBaseUrl/Goal/update"

            val json = JSONObject().apply {
                put("goalID", goalID)
                put("userID", userID)
                put("goalName", goalName)
                put("totalAmount", totalAmount)
                put("savedAmount", savedAmount)
                put("completionDate", completionDate ?: JSONObject.NULL) // Send null if completionDate is null
            }

            Log.d("GoalDetails", "Payload to send: $json") // Log the JSON payload

            val body = RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                json.toString()
            )

            val request = Request.Builder()
                .url(url)
                .put(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@GoalDetails, "Failed to update goal: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        runOnUiThread {
                            Toast.makeText(this@GoalDetails, "Goal updated successfully", Toast.LENGTH_SHORT).show()

                            // Update local database
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val goalEntity = GoalEntity(
                                        goalID = goalID,
                                        userID = userID,
                                        goalName = goalName!!,
                                        totalAmount = totalAmount!!,
                                        savedAmount = savedAmount!!,
                                        completionDate = completionDate,
                                        isSynced = true
                                    )
                                    appDatabase.goalDao().insertGoal(goalEntity)
                                }
                            }

                            // Redirect to SavingGoals activity
                            val intent = Intent(this@GoalDetails, SavingGoals::class.java)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@GoalDetails, "Failed to update goal", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        } else {
            Toast.makeText(this, "Invalid goal data", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
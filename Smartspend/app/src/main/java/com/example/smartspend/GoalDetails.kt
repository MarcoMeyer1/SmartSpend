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
import android.os.Handler
import android.os.Looper
import android.util.Log
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.text.SimpleDateFormat


import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.models.Size


import java.util.concurrent.TimeUnit
import nl.dionsegijn.konfetti.xml.KonfettiView

class GoalDetails : AppCompatActivity() {

    private lateinit var tvGoalName: TextView
    private lateinit var tvGoalAmount: TextView
    private lateinit var progressGoalCompletion: ProgressBar
    private lateinit var tvGoalPercentage: TextView
    private lateinit var konfettiView: KonfettiView // Added for confetti animation
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

        tvGoalName = findViewById(R.id.tvGoalName)
        tvGoalAmount = findViewById(R.id.tvGoalAmount)
        progressGoalCompletion = findViewById(R.id.progressRank)
        tvGoalPercentage = findViewById(R.id.tvGoalPercentage)
        konfettiView = findViewById(R.id.konfettiView) // Initialize the KonfettiView

        appDatabase = AppDatabase.getDatabase(this)

        // Gets the intent extras
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val userID = sharedPreferences.getInt("userID", -1)

        goalID = intent.getIntExtra("goalID", -1)
        goalName = intent.getStringExtra("goalName")
        totalAmount = intent.getStringExtra("totalAmount")?.toBigDecimalOrNull()
        savedAmount = intent.getStringExtra("savedAmount")?.toBigDecimalOrNull()
        completionDate = intent.getStringExtra("completionDate")

        // Updates the UI
        tvGoalName.text = goalName
        // Sets the progress with the animation
        if (totalAmount != null && savedAmount != null) {
            val numberFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
            tvGoalAmount.text = "R${savedAmount?.setScale(2, BigDecimal.ROUND_HALF_UP)} / R${totalAmount?.setScale(2, BigDecimal.ROUND_HALF_UP)}"


            val progressPercentage = (savedAmount!!.divide(totalAmount, 2, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal(100))).toInt()

            tvGoalPercentage.text = "$progressPercentage%"

            val animator = ObjectAnimator.ofInt(progressGoalCompletion, "progress", 0, progressPercentage)
            animator.duration = 2000
            animator.start()

            // Checks if the goal has been met and which plays the confetti animation
            if (progressPercentage >= 100) {
                playConfettiAnimation()
            }
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

                val newSavedAmount = savedAmount?.add(enteredAmount)

                Log.d("GoalDetails", "Updated saved amount: $newSavedAmount")

                // Updates the UI
                if (totalAmount != null && newSavedAmount != null) {
                    val numberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
                    tvGoalAmount.text = "R${newSavedAmount?.setScale(2, BigDecimal.ROUND_HALF_UP)} / R${totalAmount?.setScale(2, BigDecimal.ROUND_HALF_UP)}"


                    val progressPercentage = (newSavedAmount.divide(totalAmount, 2, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal(100))).toInt()

                    tvGoalPercentage.text = "$progressPercentage%"

                    val animator = ObjectAnimator.ofInt(progressGoalCompletion, "progress", progressGoalCompletion.progress, progressPercentage)
                    animator.duration = 2000
                    animator.start()

                    // Checks if the goal has been met
                    if (newSavedAmount >= totalAmount) {
                        completionDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        Log.d("GoalDetails", "Goal met! Completion date set to: $completionDate")
                        playConfettiAnimation() // Plays the confetti when the goal has been met
                    } else {
                        completionDate = null
                    }
                }
                savedAmount = newSavedAmount // Updates the savedAmount with the new value
                updateGoalOnServer()
                alertDialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Method to play confetti animation
    private fun playConfettiAnimation() {
        val colors = listOf(
            android.graphics.Color.YELLOW,
            android.graphics.Color.GREEN,
            android.graphics.Color.MAGENTA,
            android.graphics.Color.RED,
            android.graphics.Color.BLUE
        )

        val party = Party(
            speed = 10f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = colors,
            shapes = listOf(Shape.Square, Shape.Circle),
            size = listOf(Size.SMALL, Size.LARGE),
            timeToLive = 2000L,
            position = Position.Relative(0.5, 0.5),
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(50)
        )

        Handler(Looper.getMainLooper()).postDelayed({
            konfettiView.start(party)
        }, 500)
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
                put("completionDate", completionDate ?: JSONObject.NULL)
            }

            Log.d("GoalDetails", "Payload to send: $json")

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

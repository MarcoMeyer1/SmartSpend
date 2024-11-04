package com.example.smartspend

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class Profile : BaseActivity() {

    private lateinit var etFirstName: EditText
    private lateinit var etSurname: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var btnSet: Button

    // TextViews for user stats
    private lateinit var tvActiveGoals: TextView
    private lateinit var tvCompletedGoals: TextView
    private lateinit var tvDifferentCategories: TextView
    private lateinit var tvUpcomingReminders: TextView

    private val client = OkHttpClient()
    private var userID: Int = -1

    // Tag for logging
    private val TAG = "ProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        setActiveNavButton(R.id.profile_nav)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initializes the UI elements
        etFirstName = findViewById(R.id.etFirstName)
        etSurname = findViewById(R.id.etSurname)
        etEmail = findViewById(R.id.etEmail)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        btnSet = findViewById(R.id.btn_set)

        // Initialize the TextViews for stats
        tvActiveGoals = findViewById(R.id.active_goals_value)
        tvCompletedGoals = findViewById(R.id.completed_goals_value)
        tvDifferentCategories = findViewById(R.id.different_categories_value)
        tvUpcomingReminders = findViewById(R.id.upcoming_reminders_value)

        etEmail.isEnabled = false

        // Retrieves the user ID from SharedPreferences
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userID = sharedPreferences.getInt("userID", -1)

        if (userID != -1) {
            fetchUserProfile()
            fetchUserProfileStats()
        } else {
            Toast.makeText(this, "User ID not found. Please log in again.", Toast.LENGTH_LONG).show()
        }

        btnSet.setOnClickListener {
            updateUserProfile()
        }
    }

    // Fetches user profile stats from the server
    private fun fetchUserProfileStats() {
        val url = "https://smartspendapi.azurewebsites.net/api/ProfileRework/stats/$userID"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        Log.d(TAG, "Fetching user profile stats from $url")

        // Makes the network request
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network Error: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@Profile, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                Log.d(TAG, "Response code: ${response.code}")
                Log.d(TAG, "Response body: $responseBody")

                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val activeGoals = jsonResponse.getInt("activeGoals")
                            val completedGoals = jsonResponse.getInt("completedGoals")
                            val differentCategories = jsonResponse.getInt("differentCategories")
                            val upcomingReminders = jsonResponse.getInt("upcomingReminders")

                            tvActiveGoals.text = "$activeGoals"
                            tvCompletedGoals.text = "$completedGoals"
                            tvDifferentCategories.text = "$differentCategories"
                            tvUpcomingReminders.text = "$upcomingReminders"

                            Log.d(TAG, "Stats updated successfully")

                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing stats data", e)
                            Toast.makeText(this@Profile, "Error parsing stats data", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Log.e(TAG, "Failed to fetch user stats: ${response.message}")
                        Toast.makeText(this@Profile, "Failed to fetch user stats", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    // Fetches user profile from the server
    private fun fetchUserProfile() {
        val url = "https://smartspendapi.azurewebsites.net/api/User/$userID"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        Log.d(TAG, "Fetching user profile from $url")

        // Makes the network request
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network Error: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@Profile, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                Log.d(TAG, "Response code: ${response.code}")
                Log.d(TAG, "Response body: $responseBody")

                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val firstName = jsonResponse.getString("firstName")
                            val lastName = jsonResponse.getString("lastName")
                            val email = jsonResponse.getString("email")
                            val phoneNumber = jsonResponse.optString("phoneNumber", "")

                            etFirstName.setText(firstName)
                            etSurname.setText(lastName)
                            etEmail.setText(email)
                            etPhoneNumber.setText(phoneNumber)

                            Log.d(TAG, "User profile fetched successfully")

                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing user data", e)
                            Toast.makeText(this@Profile, "Error parsing user data", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Log.e(TAG, "Failed to fetch user profile: ${response.message}")
                        Toast.makeText(this@Profile, "Failed to fetch user profile", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    // Updates the user profile on the server
    private fun updateUserProfile() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etSurname.text.toString().trim()
        val phoneNumber = etPhoneNumber.text.toString().trim()

        if (firstName.isEmpty()) {
            etFirstName.error = "First name is required"
            etFirstName.requestFocus()
            return
        }

        if (lastName.isEmpty()) {
            etSurname.error = "Surname is required"
            etSurname.requestFocus()
            return
        }

        val json = JSONObject()
        json.put("userID", userID)
        json.put("firstName", firstName)
        json.put("lastName", lastName)
        json.put("email", etEmail.text.toString())
        json.put("phoneNumber", if (phoneNumber.isNotEmpty()) phoneNumber else JSONObject.NULL)

        val url = "https://smartspendapi.azurewebsites.net/api/User/update"

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url(url)
            .put(body)
            .build()

        Log.d(TAG, "Updating user profile at $url with data: ${json.toString()}")

        // Makes the network request
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network Error: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@Profile, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                Log.d(TAG, "Response code: ${response.code}")
                Log.d(TAG, "Response body: $responseBody")

                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@Profile, "Profile updated successfully", Toast.LENGTH_LONG).show()
                        Log.d(TAG, "Profile updated successfully")
                    } else {
                        val errorMessage = responseBody ?: "Profile update failed"
                        Toast.makeText(this@Profile, errorMessage, Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Profile update failed: $errorMessage")
                    }
                }
            }
        })
    }
}

package com.example.smartspend

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
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

    private val client = OkHttpClient()
    private var userID: Int = -1

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

        // Initialize UI elements
        etFirstName = findViewById(R.id.etFirstName)
        etSurname = findViewById(R.id.etSurname)
        etEmail = findViewById(R.id.etEmail)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        btnSet = findViewById(R.id.btn_set)

        // Disable editing of email field
        etEmail.isEnabled = false

        // Get userID from SharedPreferences
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userID = sharedPreferences.getInt("userID", -1)

        if (userID != -1) {
            // Fetch user profile data
            fetchUserProfile()
        } else {
            Toast.makeText(this, "User ID not found. Please log in again.", Toast.LENGTH_LONG).show()
            // Optionally, redirect to login screen
        }

        // Set click listener for the 'Set' button
        btnSet.setOnClickListener {
            updateUserProfile()
        }
    }

    private fun fetchUserProfile() {
        val url = "https://smartspendapi.azurewebsites.net/api/User/$userID"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@Profile, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val firstName = jsonResponse.getString("firstName")
                            val lastName = jsonResponse.getString("lastName")
                            val email = jsonResponse.getString("email")
                            val phoneNumber = jsonResponse.optString("phoneNumber", "")

                            // Populate the UI fields
                            etFirstName.setText(firstName)
                            etSurname.setText(lastName)
                            etEmail.setText(email)
                            etPhoneNumber.setText(phoneNumber)

                        } catch (e: Exception) {
                            Toast.makeText(this@Profile, "Error parsing user data", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@Profile, "Failed to fetch user profile", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

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

        // Prepare JSON object
        val json = JSONObject()
        json.put("userID", userID)
        json.put("firstName", firstName)
        json.put("lastName", lastName)
        json.put("email", etEmail.text.toString()) // Email is disabled, but include it
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

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@Profile, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@Profile, "Profile updated successfully", Toast.LENGTH_LONG).show()
                    } else {
                        val errorMessage = responseBody ?: "Profile update failed"
                        Toast.makeText(this@Profile, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}

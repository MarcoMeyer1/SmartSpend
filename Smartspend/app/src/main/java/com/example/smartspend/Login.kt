package com.example.smartspend

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class Login : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnSignIn: MaterialButton
    private lateinit var tvCreateAccount: TextView
    private lateinit var btnGoogleSignIn: MaterialButton
    private lateinit var googleSignInClient: GoogleSignInClient

    private val client = OkHttpClient()
    private val RC_SIGN_IN = 1 // Request code for Google sign-in

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize UI elements
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSignIn = findViewById(R.id.btnSignIn)
        tvCreateAccount = findViewById(R.id.createAccountText)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)

        // Google Sign-In configuration
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("943237529042-a7mmtuj8nfb2f91nhqfpgjrbvhtreet5.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnSignIn.setOnClickListener {
            loginUser()
        }

        btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        tvCreateAccount.setOnClickListener {
            // Navigate to Register activity
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }
    }

    // Function for regular login
    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            etPassword.requestFocus()
            return
        }

        val url = "https://smartspendapi.azurewebsites.net/api/User/login"

        val json = JSONObject()
        json.put("email", email)
        json.put("password", password)

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
                    Toast.makeText(this@Login, "Network Error: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            // Parse the response as JSON
                            val jsonResponse = JSONObject(responseBody)
                            val userID = jsonResponse.getInt("userID")
                            val message = jsonResponse.getString("message")

                            // Save userID to SharedPreferences
                            saveUserIDToPreferences(userID)

                            // Handle successful login
                            Toast.makeText(this@Login, message, Toast.LENGTH_LONG).show()

                            // Navigate to the next activity
                            val intent = Intent(this@Login, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } catch (e: Exception) {
                            // Handle parsing error
                            Toast.makeText(this@Login, "Error parsing response", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // Handle failed login
                        val errorMessage = responseBody ?: "Login failed"
                        Toast.makeText(this@Login, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    // Function to save userID to SharedPreferences
    private fun saveUserIDToPreferences(userID: Int) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("userID", userID)
        editor.apply()
    }

    // Function to start Google Sign-In process
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    // Handle Google Sign-In result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    // Process the Google Sign-In result
    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)

            // Send ID token to your backend
            val idToken = account?.idToken
            Log.d("Login", "Google sign-in successful, ID Token: $idToken")
            sendIdTokenToServer(idToken)

        } catch (e: ApiException) {
            // Log error with status code for better debugging
            Log.e("Login", "Google sign-in failed. Status Code: ${e.statusCode}", e)
            Toast.makeText(this, "Google Sign-In failed: ${e.statusCode}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("Login", "Google sign-in failed", e)
            Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_LONG).show()
        }
    }

    // Function to send the Google ID Token to your backend for verification
    private fun sendIdTokenToServer(idToken: String?) {
        if (idToken == null) {
            Toast.makeText(this, "Failed to get ID Token", Toast.LENGTH_SHORT).show()
            Log.e("Login", "ID Token is null, cannot send to server.")
            return
        }

        val url = "https://smartspendapi.azurewebsites.net/api/User/googleSignIn"

        val json = JSONObject()
        json.put("idToken", idToken)

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
                    Log.e("Login", "Network error while sending ID Token to server: ${e.message}", e)
                    Toast.makeText(this@Login, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            // Parse response and proceed as necessary
                            val jsonResponse = JSONObject(responseBody)
                            val userID = jsonResponse.getInt("userID")

                            // Save userID to SharedPreferences
                            saveUserIDToPreferences(userID)

                            // Navigate to the main activity
                            val intent = Intent(this@Login, MainActivity::class.java)
                            startActivity(intent)
                            finish()

                        } catch (e: Exception) {
                            Log.e("Login", "Error parsing response from server", e)
                            Toast.makeText(this@Login, "Error parsing response", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Log.e("Login", "Google Sign-In server error: ${response.message}")
                        Toast.makeText(this@Login, "Google Sign-In failed", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}

package com.example.smartspend

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.Executor
import java.util.regex.Pattern

class Login : AppCompatActivity() {

    public lateinit var etEmail: TextInputEditText
    public lateinit var etPassword: TextInputEditText
    private lateinit var btnSignIn: MaterialButton
    private lateinit var tvCreateAccount: TextView
    private lateinit var btnGoogleSignIn: MaterialButton
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var executor: Executor

    // OkHttpClient for making network requests
    private val client = OkHttpClient()
    private val RC_SIGN_IN = 1  // Request code for Google Sign-In

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
        val fingerprintIcon = findViewById<ImageView>(R.id.imageView2)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("1085780885439-1iuludmfbgbhgp4k41nlbi7emqf0j43n.apps.googleusercontent.com")
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
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

        // Biometric Authentication Setup
        setupBiometricAuthentication()

        // Set up the biometric login trigger
        fingerprintIcon.setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    // Set up Biometric Authentication
    private fun setupBiometricAuthentication() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                Log.d("Login", "App can authenticate using biometrics.")
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                Log.e("Login", "No biometric features available on this device.")
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                Log.e("Login", "Biometric features are currently unavailable.")
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                Log.e("Login", "The user hasn't associated any biometric credentials with their account.")
        }

        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this@Login, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(applicationContext, "Authentication succeeded!", Toast.LENGTH_SHORT)
                    .show()
                // Auto-login the user here
                autoLoginUser()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT)
                    .show()
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for SmartSpend")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use account password")
            .build()
    }

    // Auto-login the user upon successful biometric authentication
    private fun autoLoginUser() {
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val userID = sharedPreferences.getInt("userID", -1)
        if (userID != -1) {
            // User is logged in, proceed to MainActivity
            val intent = Intent(this@Login, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "No user data found, please log in first", Toast.LENGTH_LONG).show()
        }
    }

    // Performs login procedure
    fun loginUser() {
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

        // Makes the network request
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@Login, "Network Error: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
            }

            // Parses the response
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val userID = jsonResponse.getInt("userID")
                            val message = jsonResponse.getString("message")

                            saveUserIDToPreferences(userID)

                            Toast.makeText(this@Login, message, Toast.LENGTH_LONG).show()

                            val intent = Intent(this@Login, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } catch (e: Exception) {
                            Toast.makeText(this@Login, "Error parsing response", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val errorMessage = responseBody ?: "Login failed"
                        Toast.makeText(this@Login, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    // Saves userID to SharedPreferences
    public fun saveUserIDToPreferences(userID: Int) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("userID", userID)
        editor.apply()
    }

    // Initiates Google Sign-In
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    // Handles the result of Google Sign-In
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    // Handles the result of Google Sign-In
    public fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)

            val idToken = account?.idToken
            Log.d("Login", "Google sign-in successful, ID Token: $idToken")

            if (account != null) {
                val email = account.email
                val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("userEmail", email)
                editor.apply()

                val intent = Intent(this@Login, MainActivity::class.java)
                startActivity(intent)
                finish()
            }

        } catch (e: ApiException) {
            Log.e("Login", "Google sign-in failed. Status Code: ${e.statusCode}", e)
            Toast.makeText(this, "Google Sign-In failed: ${e.statusCode}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("Login", "Google sign-in failed", e)
            Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_LONG).show()
        }
    }
}

object LoginValidator {
    // Regular expression pattern for email validation
    private val EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
    )
    // Email validation method
    fun isValidEmail(email: String): Boolean {
        return EMAIL_PATTERN.matcher(email).matches()
    }
    // Password validation method
    fun isValidPassword(password: String): Boolean {
        return password.isNotEmpty()
    }
    // Mock login function (just a simulation for testing)
    fun loginUserMock(email: String, password: String): Map<String, Any> {
        return if (isValidEmail(email) && isValidPassword(password)) {
            mapOf(
                "userID" to 123,
                "message" to "Login successful"
            )
        } else {
            mapOf(
                "message" to "Login failed"
            )
        }
    }
    // Mock function to save userID to SharedPreferences
    fun saveUserIDToPreferencesMock(userID: Int): Boolean {
        return userID > 0
    }
}

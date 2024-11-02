package com.example.smartspend

import android.content.Context
import android.content.Intent
import android.net.*
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.*
import java.io.IOException
import java.math.BigDecimal
import java.util.Locale

class SavingGoals : BaseActivity(), GoalAdapter.OnItemClickListener {

    private lateinit var recyclerViewGoals: RecyclerView
    private lateinit var goalAdapter: GoalAdapter
    private val goalList = mutableListOf<GoalEntity>()
    private val client = OkHttpClient()
    private lateinit var appDatabase: AppDatabase
    private val apiBaseUrl = "https://smartspendapi.azurewebsites.net/api"
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_saving_goals)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize ConnectivityManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // Register network callback
        registerNetworkCallback()

        setActiveNavButton(null)

        appDatabase = AppDatabase.getDatabase(this)

        val fabAddGoal: FloatingActionButton = findViewById(R.id.fabAddGoal)
        recyclerViewGoals = findViewById(R.id.recyclerViewGoals)
        recyclerViewGoals.layoutManager = LinearLayoutManager(this)
        goalAdapter = GoalAdapter(goalList, this)
        recyclerViewGoals.adapter = goalAdapter

        fabAddGoal.setOnClickListener {
            showAddGoalDialog()
        }

        // Fetch goals from local database initially
        fetchGoalsFromLocal()

        // Initial sync if network is available
        if (isNetworkAvailable()) {
            syncLocalDataWithServer()
            fetchGoalsFromServer()
        }
    }

    // Implement the OnItemClickListener method
    override fun onItemClick(goal: GoalEntity) {
        // Start the GoalDetails activity and pass the goal details
        val intent = Intent(this, GoalDetails::class.java)
        intent.putExtra("goalID", goal.goalID)
        intent.putExtra("goalName", goal.goalName)
        intent.putExtra("totalAmount", goal.totalAmount.toString())
        intent.putExtra("savedAmount", goal.savedAmount.toString())
        intent.putExtra("completionDate", goal.completionDate)
        startActivity(intent)
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                // Network is available, trigger sync
                runOnUiThread {
                    syncLocalDataWithServer()
                    fetchGoalsFromServer()
                }
            }
        }

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun showAddGoalDialog() {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_goal, null)
        builder.setView(dialogView)
        val dialog = builder.create()

        val goalNameEditText: EditText = dialogView.findViewById(R.id.goalName)
        val totalAmountEditText: EditText = dialogView.findViewById(R.id.edtAmount)
        val savedAmountEditText: EditText = dialogView.findViewById(R.id.savedAmount)
        val saveButton: Button = dialogView.findViewById(R.id.btnAddtoGoal)

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

                // Fetches the userID from SharedPreferences
                val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                val userID = sharedPreferences.getInt("userID", -1)

                if (userID != -1) {
                    val newGoal = GoalEntity(
                        goalID = 0,
                        userID = userID,
                        goalName = goalName,
                        totalAmount = totalAmountDecimal,
                        savedAmount = savedAmountDecimal,
                        completionDate = null
                    )

                    scope.launch {
                        // Save to local database
                        withContext(Dispatchers.IO) {
                            appDatabase.goalDao().insertGoal(newGoal)
                        }

                        if (isNetworkAvailable()) {
                            // Sync with server
                            createGoalOnServer(newGoal) { success ->
                                if (success) {
                                    fetchGoalsFromServer()
                                    dialog.dismiss()
                                } else {
                                    Toast.makeText(
                                        this@SavingGoals,
                                        "Failed to sync with server",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    fetchGoalsFromLocal()
                                    dialog.dismiss()
                                }
                            }
                        } else {
                            Toast.makeText(
                                this@SavingGoals,
                                "Saved locally. Will sync when online.",
                                Toast.LENGTH_SHORT
                            ).show()
                            fetchGoalsFromLocal()
                            dialog.dismiss()
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

    // Check if network is available
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities != null &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Fetches the goals from the local database
    private fun fetchGoalsFromLocal() {
        scope.launch {
            val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val userID = sharedPreferences.getInt("userID", -1)

            if (userID != -1) {
                val localGoals = withContext(Dispatchers.IO) {
                    appDatabase.goalDao().getGoalsByUser(userID)
                }
                goalList.clear()
                goalList.addAll(localGoals)
                goalAdapter.notifyDataSetChanged()
            } else {
                Toast.makeText(this@SavingGoals, "User ID not found. Please log in.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fetches the goals from the server
    private fun fetchGoalsFromServer() {
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
                        Toast.makeText(this@SavingGoals, "Failed to load goals from server", Toast.LENGTH_SHORT)
                            .show()
                        // Load from local database
                        fetchGoalsFromLocal()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()

                    if (response.isSuccessful && responseBody != null) {
                        val jsonArray = JSONArray(responseBody)
                        val serverGoals = mutableListOf<GoalEntity>()
                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            val goal = GoalEntity(
                                goalID = jsonObject.getInt("goalID"),
                                userID = jsonObject.getInt("userID"),
                                goalName = jsonObject.getString("goalName"),
                                totalAmount = BigDecimal(jsonObject.getDouble("totalAmount")),
                                savedAmount = BigDecimal(jsonObject.getDouble("savedAmount")),
                                completionDate = jsonObject.optString("completionDate", null),
                                isSynced = true // Mark as synced
                            )
                            serverGoals.add(goal)
                        }

                        scope.launch {
                            withContext(Dispatchers.IO) {
                                // Merge local unsynced goals with server goals
                                val localUnsyncedGoals = appDatabase.goalDao().getUnsyncedGoals(userID)
                                val allGoals = localUnsyncedGoals + serverGoals
                                appDatabase.goalDao().deleteGoalsByUser(userID)
                                appDatabase.goalDao().insertGoals(allGoals)
                            }
                            // Update UI
                            fetchGoalsFromLocal()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@SavingGoals,
                                "Failed to load goals from server",
                                Toast.LENGTH_SHORT
                            ).show()
                            // Load from local database
                            fetchGoalsFromLocal()
                        }
                    }
                }
            })
        } else {
            Toast.makeText(this, "User ID not found. Please log in.", Toast.LENGTH_SHORT).show()
        }
    }

    // Sync local unsynced goals with the server
    private fun syncLocalDataWithServer() {
        scope.launch {
            val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val userID = sharedPreferences.getInt("userID", -1)

            if (userID != -1) {
                val localUnsyncedGoals = withContext(Dispatchers.IO) {
                    appDatabase.goalDao().getUnsyncedGoals(userID)
                }

                for (goal in localUnsyncedGoals) {
                    createGoalOnServer(goal) { success ->
                        if (success) {
                            // Update local goal to mark as synced
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    appDatabase.goalDao().insertGoal(goal.copy(isSynced = true))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createGoalOnServer(goal: GoalEntity, callback: (Boolean) -> Unit) {
        val url = "$apiBaseUrl/Goal/create"

        val json = JSONObject().apply {
            put("userID", goal.userID)
            put("goalName", goal.goalName)
            put("totalAmount", goal.totalAmount)
            put("savedAmount", goal.savedAmount)
            put("completionDate", goal.completionDate ?: JSONObject.NULL)
        }

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
                    Toast.makeText(this@SavingGoals, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                    callback(false)
                }
                Log.e("SavingGoals", "Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("SavingGoals", "Server Response: $responseBody")

                if (response.isSuccessful && responseBody != null) {
                    runOnUiThread {
                        try {
                            // Try to parse the response as JSON
                            val jsonResponse = JSONObject(responseBody)
                            val serverGoalID = jsonResponse.getInt("goalID")

                            // Update local goal with server goalID and mark as synced
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    appDatabase.goalDao().updateGoal(goal.copy(goalID = serverGoalID, isSynced = true))
                                }
                            }
                            fetchGoalsFromLocal()  // Refresh goals after creating
                            callback(true)
                        } catch (e: JSONException) {
                            Toast.makeText(
                                this@SavingGoals,
                                "Unexpected response: $responseBody",
                                Toast.LENGTH_LONG
                            ).show()
                            callback(false)
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@SavingGoals,
                            "Server error: ${response.code}",
                            Toast.LENGTH_LONG
                        ).show()
                        callback(false)
                    }
                    Log.e("SavingGoals", "Server error: ${response.code}")
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}

package com.example.smartspend

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import android.provider.Settings
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class Reminders : BaseActivity() {

    private lateinit var rvUpcomingReminders: RecyclerView
    private lateinit var rvCompletedReminders: RecyclerView
    private lateinit var upcomingRemindersAdapter: ReminderAdapter
    private lateinit var completedRemindersAdapter: ReminderAdapter
    private val client = OkHttpClient()
    private var userID: Int = -1

    private val REQUEST_NOTIFICATION_PERMISSION = 1 // Permission request code
    private val TAG = "Reminders" // Tag for logging

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reminders)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Retrieve user ID
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userID = sharedPreferences.getInt("userID", -1)
        if (userID == -1) {
            Log.e(TAG, "User ID not found in SharedPreferences")
            finish()
            return
        }

        // Initialize RecyclerViews
        rvUpcomingReminders = findViewById(R.id.rv_upcoming_reminders)
        rvCompletedReminders = findViewById(R.id.rv_completed_reminders)

        rvUpcomingReminders.layoutManager = LinearLayoutManager(this)
        rvCompletedReminders.layoutManager = LinearLayoutManager(this)

        upcomingRemindersAdapter = ReminderAdapter { reminder ->
            updateReminder(reminder)
        }
        completedRemindersAdapter = ReminderAdapter { reminder ->
            updateReminder(reminder)
        }

        rvUpcomingReminders.adapter = upcomingRemindersAdapter
        rvCompletedReminders.adapter = completedRemindersAdapter

        // Check and request notification permission
        checkNotificationPermission()
        checkExactAlarmPermission()
        fetchReminders()

        val fab = findViewById<FloatingActionButton>(R.id.fab_add_reminder)
        fab.setOnClickListener {
            showCreateReminderDialog()
        }
    }

    // Check and request notification permission for Android 13+
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                // Permission denied
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("This app requires permission to schedule exact alarms for reminders. Please grant this permission in settings.")
                    .setPositiveButton("Open Settings") { _, _ ->
                        val intent = Intent().apply {
                            action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
    // Fetch reminders from the server
    private fun fetchReminders() {
        val url = "https://smartspendapi.azurewebsites.net/api/Reminder/$userID"
        Log.d(TAG, "Fetching reminders from URL: $url")

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        // Make the network request
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network request failed: ${e.message}")
                runOnUiThread {
                    Toast.makeText(
                        this@Reminders,
                        "Network Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d(TAG, "Response received: $responseBody")

                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonArray = JSONArray(responseBody)
                            Log.d(TAG, "Parsed JSON array: $jsonArray")

                            val upcomingReminders = mutableListOf<Reminder>()
                            val completedReminders = mutableListOf<Reminder>()

                            for (i in 0 until jsonArray.length()) {
                                val jsonObject = jsonArray.getJSONObject(i)
                                Log.d(TAG, "Processing reminder JSON object: $jsonObject")

                                val reminderID = jsonObject.getInt("reminderID")
                                val userID = jsonObject.getInt("userID")
                                val description = jsonObject.getString("description")
                                val dateDue = jsonObject.getString("dateDue")
                                val notificationDate =
                                    if (jsonObject.isNull("notificationDate")) null else jsonObject.getString(
                                        "notificationDate"
                                    )
                                val isEnabled = jsonObject.getBoolean("isEnabled")
                                val isCompleted = jsonObject.getBoolean("isCompleted")

                                Log.d(
                                    TAG,
                                    "Parsed fields - reminderID: $reminderID, userID: $userID, description: $description, dateDue: $dateDue, notificationDate: $notificationDate, isEnabled: $isEnabled, isCompleted: $isCompleted"
                                )

                                val reminder = Reminder(
                                    reminderID = reminderID,
                                    userID = userID,
                                    description = description,
                                    dateDue = dateDue,
                                    notificationDate = notificationDate,
                                    isEnabled = isEnabled,
                                    isCompleted = isCompleted
                                )

                                Log.d(TAG, "Created Reminder object: $reminder")

                                if (reminder.isCompleted) {
                                    completedReminders.add(reminder)
                                } else {
                                    upcomingReminders.add(reminder)
                                }

                                // Schedule notifications for enabled reminders with notification dates
                                if (reminder.isEnabled && reminder.notificationDate != null) {
                                    scheduleReminderNotification(reminder)
                                }
                            }

                            upcomingRemindersAdapter.setReminders(upcomingReminders)
                            completedRemindersAdapter.setReminders(completedReminders)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing reminders: ${e.message}", e)
                            Toast.makeText(
                                this@Reminders,
                                "Error parsing reminders: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val errorBody = responseBody ?: "No response body"
                        Log.e(TAG, "Error fetching reminders: $errorBody")
                        Toast.makeText(
                            this@Reminders,
                            "Error fetching reminders",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
    }

    // Show the dialog to create a new reminder
    private fun showCreateReminderDialog() {
        val builder = AlertDialog.Builder(this)
        val dialogView =
            LayoutInflater.from(this).inflate(R.layout.dialog_create_reminder, null)

        builder.setView(dialogView)
        val dialog = builder.create()

        val descriptionField: EditText = dialogView.findViewById(R.id.reminder_description)
        val dateDueField: EditText = dialogView.findViewById(R.id.date_due_button)
        val dateToNotifyField: EditText = dialogView.findViewById(R.id.date_to_notify_button)
        val createButton: Button = dialogView.findViewById(R.id.create_button)

        val enabledText: TextView = dialogView.findViewById(R.id.enabled_text)
        val disabledText: TextView = dialogView.findViewById(R.id.disabled_text)

        var isEnabled = true

        enabledText.setTextColor(Color.parseColor("#70FFB5"))
        disabledText.setTextColor(Color.parseColor("#FFFFFF"))

        enabledText.setOnClickListener {
            isEnabled = true
            enabledText.setTextColor(Color.parseColor("#70FFB5"))
            disabledText.setTextColor(Color.parseColor("#FFFFFF"))
            dateToNotifyField.isEnabled = true
            dateToNotifyField.setTextColor(Color.parseColor("#FFFFFF"))
        }

        disabledText.setOnClickListener {
            isEnabled = false
            enabledText.setTextColor(Color.parseColor("#FFFFFF"))
            disabledText.setTextColor(Color.parseColor("#FF5C5C"))
            dateToNotifyField.isEnabled = false
            dateToNotifyField.setText("")
            dateToNotifyField.setTextColor(Color.parseColor("#AAAAAA"))
        }

        dateDueField.setOnClickListener {
            showDateTimePickerDialog { date -> dateDueField.setText(date) }
        }

        dateToNotifyField.setOnClickListener {
            if (isEnabled) {
                showDateTimePickerDialog { date -> dateToNotifyField.setText(date) }
            }
        }

        createButton.setOnClickListener {
            val description = descriptionField.text.toString().trim()
            val dateDue = dateDueField.text.toString().trim()
            val dateToNotify = dateToNotifyField.text.toString().trim()

            if (description.isEmpty()) {
                descriptionField.error = "Description is required"
                descriptionField.requestFocus()
                return@setOnClickListener
            }
            if (dateDue.isEmpty()) {
                dateDueField.error = "Date due is required"
                dateDueField.requestFocus()
                return@setOnClickListener
            }

            createReminder(description, dateDue, dateToNotify, isEnabled)
            dialog.dismiss()
        }

        dialog.show()
    }

    // Create a new reminder on the server
    private fun createReminder(
        description: String,
        dateDue: String,
        dateToNotify: String?,
        isEnabled: Boolean
    ) {
        val url = "https://smartspendapi.azurewebsites.net/api/Reminder/create"
        Log.d(TAG, "Creating reminder with URL: $url")

        val json = JSONObject()
        json.put("userID", userID)
        json.put("description", description)
        val dateDueFormatted = parseDateToApiFormat(dateDue)
        val notificationDateFormatted =
            if (dateToNotify.isNullOrEmpty()) null else parseDateToApiFormat(dateToNotify)

        json.put("dateDue", dateDueFormatted)
        if (isEnabled && notificationDateFormatted != null) {
            json.put("notificationDate", notificationDateFormatted)
        } else {
            json.put("notificationDate", JSONObject.NULL)
        }
        json.put("isEnabled", isEnabled)
        json.put("isCompleted", false) // New reminders are not completed

        Log.d(TAG, "Reminder JSON to be sent: $json")

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        // Make the network request
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network request failed: ${e.message}")
                runOnUiThread {
                    Toast.makeText(
                        this@Reminders,
                        "Network Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d(TAG, "Create reminder response: $responseBody")

                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@Reminders,
                            "Reminder created successfully",
                            Toast.LENGTH_LONG
                        ).show()
                        // Fetch reminders to get updated list and schedule notifications
                        fetchReminders()
                    } else {
                        val errorMessage = responseBody ?: "Error creating reminder"
                        Log.e(TAG, "Error creating reminder: $errorMessage")
                        Toast.makeText(
                            this@Reminders,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
    }

    // Update an existing reminder on the server
    private fun updateReminder(reminder: Reminder) {
        val url = "https://smartspendapi.azurewebsites.net/api/Reminder/update"
        Log.d(TAG, "Updating reminder with URL: $url")

        val json = JSONObject()
        json.put("reminderID", reminder.reminderID)
        json.put("userID", reminder.userID)
        json.put("description", reminder.description)
        json.put("dateDue", reminder.dateDue)
        json.put("notificationDate", reminder.notificationDate ?: JSONObject.NULL)
        json.put("isEnabled", reminder.isEnabled)
        json.put("isCompleted", reminder.isCompleted) // Include isCompleted

        Log.d(TAG, "Reminder JSON to be sent for update: $json")

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
                Log.e(TAG, "Network request failed: ${e.message}")
                runOnUiThread {
                    Toast.makeText(
                        this@Reminders,
                        "Network Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d(TAG, "Update reminder response: $responseBody")

                runOnUiThread {
                    if (response.isSuccessful) {
                        fetchReminders()
                    } else {
                        val errorMessage = responseBody ?: "Error updating reminder"
                        Log.e(TAG, "Error updating reminder: $errorMessage")
                        Toast.makeText(
                            this@Reminders,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
    }

    // Schedule a reminder notification
    private fun scheduleReminderNotification(reminder: Reminder) {
        Log.d(TAG, "Scheduling notification for reminder: ${reminder.reminderID}")
        if (reminder.notificationDate != null) {
            val title = "Reminder"
            val message = reminder.description

            val intent = Intent(this, NotificationReceiver::class.java).apply {
                putExtra("title", title)
                putExtra("message", message)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                reminder.reminderID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            try {
                val reminderTime = parseApiDateTime(reminder.notificationDate)?.time ?: return

                if (reminderTime > System.currentTimeMillis()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
                            Log.d(TAG, "Notification scheduled at $reminderTime for reminder ID ${reminder.reminderID}")
                        } else {
                            Log.e(TAG, "Exact alarm permission not granted.")
                        }
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
                        Log.d(TAG, "Notification scheduled at $reminderTime for reminder ID ${reminder.reminderID}")
                    }

                    // Save notification to the database
                    saveNotificationToDatabase(title, message)
                } else {
                    Log.d(TAG, "Notification date is in the past for reminder ID ${reminder.reminderID}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling notification: ${e.message}", e)
            }
        } else {
            Log.d(TAG, "No notification date set for reminder ID ${reminder.reminderID}")
        }
    }

    // Show date and time picker dialog
    private fun showDateTimePickerDialog(onDateTimeSet: (String) -> Unit) {
        val calendar = Calendar.getInstance()

        val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)

            val timeListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                val dateTimeString = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(
                    calendar.time
                )
                onDateTimeSet(dateTimeString)
            }

            TimePickerDialog(
                this,
                timeListener,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        DatePickerDialog(
            this,
            dateListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Parse date to API format
    private fun parseDateToApiFormat(dateString: String): String {
        val inputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        return outputFormat.format(date)
    }

    // Parse API date to Date object
    private fun parseApiDateTime(dateString: String): Date? {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        return inputFormat.parse(dateString)
    }

    private fun saveNotificationToDatabase(title: String, message: String) {
        val url = "https://smartspendapi.azurewebsites.net/api/Notification/create"

        val json = JSONObject().apply {
            put("userID", userID)
            put("notificationText", "$title: $message")
            put("notificationDate", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()))
        }

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to save notification to database: ${e.message}", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Notification saved to database successfully")
                } else {
                    Log.e(TAG, "Failed to save notification to database: ${response.message}")
                }
            }
        })
    }
}



// Reminder data class
data class Reminder(
    val reminderID: Int,
    val userID: Int,
    val description: String,
    val dateDue: String,
    val notificationDate: String?,
    val isEnabled: Boolean,
    var isCompleted: Boolean // Added isCompleted field
)

// Reminder adapter
class ReminderAdapter(
    private val onUpdateReminder: (Reminder) -> Unit
) : RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

    private val reminders = mutableListOf<Reminder>()

    // Update the list of reminders
    fun setReminders(reminders: List<Reminder>) {
        this.reminders.clear()
        this.reminders.addAll(reminders)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder, parent, false)
        return ReminderViewHolder(view, onUpdateReminder)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminder = reminders[position]
        holder.bind(reminder)
    }

    override fun getItemCount(): Int {
        return reminders.size
    }

    class ReminderViewHolder(
        itemView: View,
        private val onUpdateReminder: (Reminder) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val tvReminder: TextView = itemView.findViewById(R.id.tv_reminder)
        private val cbReminder: CheckBox = itemView.findViewById(R.id.cb_reminder)

        private val TAG = "ReminderViewHolder" // Tag for logging

        // Bind reminder data to the view
        fun bind(reminder: Reminder) {
            tvReminder.text = reminder.description

            try {
                Log.d(TAG, "Parsing dateDue: ${reminder.dateDue}")
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                val date = inputFormat.parse(reminder.dateDue)
                val dateString = outputFormat.format(date)
                tvDate.text = dateString
                Log.d(TAG, "Formatted dateDue: $dateString")
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing dateDue: ${e.message}", e)
                tvDate.text = "Invalid date"
            }

            cbReminder.setOnCheckedChangeListener(null)
            cbReminder.isChecked = reminder.isCompleted

            cbReminder.setOnCheckedChangeListener { _, isChecked ->
                reminder.isCompleted = isChecked
                onUpdateReminder(reminder)
            }
        }
    }
}

package com.example.smartspend

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
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
            finish()
            return
        }

        // Initializes the RecyclerView
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

        fetchReminders()

        val fab = findViewById<FloatingActionButton>(R.id.fab_add_reminder)
        fab.setOnClickListener {
            showCreateReminderDialog()
        }
    }

    // Fetches reminders from the server
    private fun fetchReminders() {
        val url = "https://smartspendapi.azurewebsites.net/api/Reminder/$userID"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        // Makes the network request
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@Reminders, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonArray = JSONArray(responseBody)
                            val upcomingReminders = mutableListOf<Reminder>()
                            val completedReminders = mutableListOf<Reminder>()

                            for (i in 0 until jsonArray.length()) {
                                val jsonObject = jsonArray.getJSONObject(i)
                                val reminder = Reminder(
                                    reminderID = jsonObject.getInt("reminderID"),
                                    userID = jsonObject.getInt("userID"),
                                    description = jsonObject.getString("description"),
                                    dateDue = jsonObject.getString("dateDue"),
                                    notificationDate = if (jsonObject.isNull("notificationDate")) null else jsonObject.getString("notificationDate"),
                                    isEnabled = jsonObject.getBoolean("isEnabled"),
                                    isCompleted = jsonObject.getBoolean("isCompleted")
                                )

                                if (reminder.isCompleted) {
                                    completedReminders.add(reminder)
                                } else {
                                    upcomingReminders.add(reminder)
                                }
                            }

                            upcomingRemindersAdapter.setReminders(upcomingReminders)
                            completedRemindersAdapter.setReminders(completedReminders)
                        } catch (e: Exception) {
                            Toast.makeText(this@Reminders, "Error parsing reminders: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@Reminders, "Error fetching reminders", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    // Shows the dialog to create a new reminder
    private fun showCreateReminderDialog() {
        val builder = AlertDialog.Builder(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_reminder, null)

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
            showDatePickerDialog { date -> dateDueField.setText(date) }
        }

        dateToNotifyField.setOnClickListener {
            if (isEnabled) {
                showDatePickerDialog { date -> dateToNotifyField.setText(date) }
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

    // Creates a new reminder on the server
    private fun createReminder(description: String, dateDue: String, dateToNotify: String?, isEnabled: Boolean) {
        val url = "https://smartspendapi.azurewebsites.net/api/Reminder/create"

        val json = JSONObject()
        json.put("userID", userID)
        json.put("description", description)
        val dateDueFormatted = parseDateToApiFormat(dateDue)
        val notificationDateFormatted = if (dateToNotify.isNullOrEmpty()) null else parseDateToApiFormat(dateToNotify)

        json.put("dateDue", dateDueFormatted)
        if (isEnabled && notificationDateFormatted != null) {
            json.put("notificationDate", notificationDateFormatted)
        } else {
            json.put("notificationDate", JSONObject.NULL)
        }
        json.put("isEnabled", isEnabled)
        json.put("isCompleted", false) // New reminders are not completed

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
                    Toast.makeText(this@Reminders, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@Reminders, "Reminder created successfully", Toast.LENGTH_LONG).show()
                        fetchReminders()
                    } else {
                        val errorMessage = response.body?.string() ?: "Error creating reminder"
                        Toast.makeText(this@Reminders, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    // Updates an existing reminder on the server
    private fun updateReminder(reminder: Reminder) {
        val url = "https://smartspendapi.azurewebsites.net/api/Reminder/update"

        val json = JSONObject()
        json.put("reminderID", reminder.reminderID)
        json.put("userID", reminder.userID)
        json.put("description", reminder.description)
        json.put("dateDue", reminder.dateDue)
        json.put("notificationDate", reminder.notificationDate ?: JSONObject.NULL)
        json.put("isEnabled", reminder.isEnabled)
        json.put("isCompleted", reminder.isCompleted) // Include isCompleted

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
                    Toast.makeText(this@Reminders, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        fetchReminders()
                    } else {
                        val errorMessage = response.body?.string() ?: "Error updating reminder"
                        Toast.makeText(this@Reminders, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun scheduleReminderNotification(reminder: Reminder) {
        if (reminder.notificationDate != null) {
            val intent = Intent(this, NotificationReceiver::class.java).apply {
                putExtra("title", "Reminder")
                putExtra("message", reminder.description)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                reminder.reminderID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val reminderTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .parse(reminder.notificationDate)?.time ?: return

            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
        }
    }

    // Shows the date picker dialog
    private fun showDatePickerDialog(onDateSet: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
            onDateSet(selectedDate)
        }, year, month, day)

        datePickerDialog.show()
    }

    // Parses the date string to API format
    private fun parseDateToApiFormat(dateString: String): String {
        val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        return outputFormat.format(date)
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

    // Updates the list of reminders
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

    // Returns the number of reminders
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

        // Binds the reminder data to the view
        fun bind(reminder: Reminder) {
            tvReminder.text = reminder.description

            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(reminder.dateDue)
            val dateString = outputFormat.format(date)
            tvDate.text = dateString

            cbReminder.setOnCheckedChangeListener(null)
            cbReminder.isChecked = reminder.isCompleted

            cbReminder.setOnCheckedChangeListener { _, isChecked ->
                reminder.isCompleted = isChecked
                onUpdateReminder(reminder)
            }
        }
    }
}

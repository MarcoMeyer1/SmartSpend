package com.example.smartspend

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class NotificationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var noNotificationsMessage: MaterialTextView
    private lateinit var notificationAdapter: NotificationAdapter
    private val notifications = mutableListOf<Notification>()
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        recyclerView = findViewById(R.id.notificationsRecyclerView)
        noNotificationsMessage = findViewById(R.id.noNotificationsMessage)

        notificationAdapter = NotificationAdapter(notifications)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = notificationAdapter

        loadNotifications()
    }

    private fun loadNotifications() {
        // Replace this with the actual user ID from SharedPreferences or any other source
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userID", -1)

        if (userId != -1) {
            val url = "https://smartspendapi.azurewebsites.net/api/Notification/$userId"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(
                            this@NotificationsActivity,
                            "Error fetching notifications: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val jsonArray = JSONArray(responseBody)
                        notifications.clear()

                        for (i in 0 until jsonArray.length()) {
                            val jsonNotification = jsonArray.getJSONObject(i)
                            val notification = Notification(
                                jsonNotification.getString("NotificationText"),
                                jsonNotification.getString("NotificationDate")
                            )
                            notifications.add(notification)
                        }

                        runOnUiThread {
                            if (notifications.isEmpty()) {
                                noNotificationsMessage.visibility = MaterialTextView.VISIBLE
                                recyclerView.visibility = RecyclerView.GONE
                            } else {
                                noNotificationsMessage.visibility = MaterialTextView.GONE
                                recyclerView.visibility = RecyclerView.VISIBLE
                                notificationAdapter.notifyDataSetChanged()
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@NotificationsActivity,
                                "Error fetching notifications: ${response.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            })
        }
    }

    data class Notification(
        val message: String,
        val timestamp: String
    )

    class NotificationAdapter(private val notifications: List<Notification>) :
        RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

        class NotificationViewHolder(itemView: android.view.View) :
            RecyclerView.ViewHolder(itemView) {
            val message: MaterialTextView = itemView.findViewById(R.id.notificationMessage)
            val time: MaterialTextView = itemView.findViewById(R.id.notificationTime)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): NotificationViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notification, parent, false)
            return NotificationViewHolder(view)
        }

        override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
            val notification = notifications[position]
            holder.message.text = notification.message
            holder.time.text = notification.timestamp
        }

        override fun getItemCount(): Int = notifications.size
    }
}

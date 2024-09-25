package com.example.smartspend

import NotificationAdapter
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView

class Notifications : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var noNotificationsMessage: TextView
    private lateinit var notificationAdapter: NotificationAdapter
    private var notifications = mutableListOf<NotificationAdapter.Notification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        recyclerView = findViewById(R.id.notificationsRecyclerView)
        noNotificationsMessage = findViewById(R.id.noNotificationsMessage)

        notificationAdapter = NotificationAdapter(notifications)
        recyclerView.adapter = notificationAdapter

        loadNotifications()
    }

    private fun loadNotifications() {

        if (notifications.isEmpty()) {
            noNotificationsMessage.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noNotificationsMessage.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            notificationAdapter.notifyDataSetChanged()
        }
    }
}
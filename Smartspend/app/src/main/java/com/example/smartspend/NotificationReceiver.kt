package com.example.smartspend

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val title = intent?.getStringExtra("title")
        val message = intent?.getStringExtra("message")

        context?.let {
            try {
                if (ContextCompat.checkSelfPermission(
                        it,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val builder = NotificationCompat.Builder(it, "reminder_notifications")
                        .setSmallIcon(R.drawable.smartspend_app_logo)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)

                    NotificationManagerCompat.from(it).notify(1, builder.build())
                } else {
                    Log.w("NotificationReceiver", "Notification permission not granted")
                }
            } catch (e: SecurityException) {
                Log.e("NotificationReceiver", "SecurityException: ${e.message}")
            }
        }
    }
}
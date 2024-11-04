package com.example.smartspend

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class NotificationReceiver : BroadcastReceiver() {

    private val TAG = "NotificationReceiver" // Tag for loggings

    override fun onReceive(context: Context?, intent: Intent?) {
        val title = intent?.getStringExtra("title")
        val message = intent?.getStringExtra("message")

        Log.d(TAG, "Received broadcast for notification - Title: $title, Message: $message")

        context?.let {
            try {
                val permissionGranted = ContextCompat.checkSelfPermission(
                    it,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                val sdkVersion = Build.VERSION.SDK_INT

                if (permissionGranted || sdkVersion < Build.VERSION_CODES.TIRAMISU) {
                    val notificationId = System.currentTimeMillis().toInt()
                    val channelId = "reminder_notifications"

                    val notificationManager =
                        it.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val channel = NotificationChannel(
                            channelId,
                            "Reminder Notifications",
                            NotificationManager.IMPORTANCE_DEFAULT
                        )
                        channel.enableLights(true)
                        channel.lightColor = Color.BLUE
                        channel.enableVibration(true)
                        notificationManager.createNotificationChannel(channel)
                    }

                    val notificationBuilder = NotificationCompat.Builder(it, channelId)
                        .setSmallIcon(R.drawable.smartspend_logo)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                    notificationManager.notify(notificationId, notificationBuilder.build())
                    Log.d(TAG, "Notification displayed with ID: $notificationId")
                } else {
                    Log.e(TAG, "Notification permission not granted")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException while displaying notification: ${e.message}", e)
            } catch (e: Exception) {
                Log.e(TAG, "Exception while displaying notification: ${e.message}", e)
            }
        } ?: run {
            Log.e(TAG, "Context is null in onReceive")
        }
    }
}

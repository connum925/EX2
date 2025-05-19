package com.example.exm2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {


    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")


        // If you need to send the token to server after every app restart
        // Get username from shared preferences
        val username = getUsernameFromSharedPreferences()


        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(username, token)
    }


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Received RemoteMessage: ${remoteMessage.toString()}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataPayload(remoteMessage.data)
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification: ${it.toString()}")
            sendNotification(it)
        }
    }


    private fun handleDataPayload(data: Map<String, String>) {
        // Handle data payload here (e.g., update UI, process data)
        val title = data["title"]
        val message = data["message"]
        if (title != null && message != null) {
            sendNotification(title, message)
        }
    }


    private fun sendNotification(notification: RemoteMessage.Notification) {
        sendNotification(notification.title, notification.body)
    }


    private fun sendNotification(title: String?, messageBody: String?) {
        if (title == null || messageBody == null) {
            Log.w(TAG, "Notification title or body is null. Notification not sent.")
            return
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }


    private fun sendRegistrationToServer(username: String?, token: String?) {
        // TODO: Implement this method to send token to your app server.
        Log.d(TAG, "sendRegistrationTokenToServer - Username: $username, Token: $token")
        // Replace this with your actual server call.
    }


    private fun getUsernameFromSharedPreferences(): String? {
        // TODO: Retrieve the username from SharedPreferences or wherever you store it
        // This is a placeholder!
        return "example_user"
    }


    companion object {
        private const val TAG = "MyFirebaseMessaging"
    }
}
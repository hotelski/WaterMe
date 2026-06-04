package com.hotelski.waterme.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.hotelski.waterme.R

object NotificationChannels {
    const val CARE_CHANNEL_ID = "plant_care_reminders"

    fun ensureCreated(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CARE_CHANNEL_ID,
            context.getString(R.string.care_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.care_notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }
}

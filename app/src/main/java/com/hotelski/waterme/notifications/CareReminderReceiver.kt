package com.hotelski.waterme.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hotelski.waterme.MainActivity
import com.hotelski.waterme.R

class CareReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!context.canPostNotifications()) return

        val plantName = intent.getStringExtra(EXTRA_PLANT_NAME) ?: context.getString(R.string.app_name)
        val careType = intent.getStringExtra(EXTRA_CARE_TYPE) ?: "care"
        val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, plantName.hashCode())
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            requestCode,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.CARE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_leaf)
            .setContentTitle("$plantName needs $careType")
            .setContentText("Open WaterMe to log care and keep your plant on track.")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
        manager?.notify(requestCode, notification)
    }

    private fun Context.canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    companion object {
        const val EXTRA_PLANT_NAME = "extra_plant_name"
        const val EXTRA_CARE_TYPE = "extra_care_type"
        const val EXTRA_REQUEST_CODE = "extra_request_code"
    }
}

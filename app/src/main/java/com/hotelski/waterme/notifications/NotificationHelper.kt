package com.hotelski.waterme.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hotelski.waterme.MainActivity
import com.hotelski.waterme.R
import com.hotelski.waterme.model.CareType

class NotificationHelper(
    private val context: Context,
) {
    fun ensureChannels() {
        NotificationChannels.ensureCreated(context)
    }

    fun canPostNotifications(): Boolean =
        NotificationPermissionHelper.canPostNotifications(context)

    fun showCareReminder(schedule: CareReminderSchedule): Boolean {
        if (!schedule.notificationsEnabled || !canPostNotifications()) return false

        ensureChannels()
        val manager = notificationManager() ?: return false

        val notification = NotificationCompat.Builder(context, NotificationChannels.CARE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_leaf)
            .setContentTitle("${schedule.plantName} needs ${schedule.careType.notificationLabel()}")
            .setContentText("Open WaterMe to log care and keep your plant on track.")
            .setContentIntent(openAppPendingIntent(schedule))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(
                R.drawable.ic_notification_leaf,
                "Done",
                actionPendingIntent(schedule, ReminderNotificationAction.COMPLETE),
            )
            .addAction(
                R.drawable.ic_notification_leaf,
                "Snooze",
                actionPendingIntent(schedule, ReminderNotificationAction.SNOOZE),
            )
            .addAction(
                R.drawable.ic_notification_leaf,
                "Skip",
                actionPendingIntent(schedule, ReminderNotificationAction.SKIP),
            )
            .build()

        manager.notify(schedule.notificationId, notification)
        return true
    }

    fun cancel(schedule: CareReminderSchedule) {
        notificationManager()?.cancel(schedule.notificationId)
    }

    fun cancelAll() {
        notificationManager()?.cancelAll()
    }

    private fun openAppPendingIntent(schedule: CareReminderSchedule): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(KEY_PLANT_ID, schedule.plantId)
            putExtra(KEY_TASK_ID, schedule.taskId)
        }

        return PendingIntent.getActivity(
            context,
            "open:${schedule.taskId}".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun actionPendingIntent(
        schedule: CareReminderSchedule,
        action: ReminderNotificationAction,
    ): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action.intentAction
            putReminderExtras(schedule)
            putExtra(KEY_REMINDER_ACTION, action.name)
            putExtra(KEY_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES)
        }

        return PendingIntent.getBroadcast(
            context,
            "${action.name}:${schedule.taskId}".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun Intent.putReminderExtras(schedule: CareReminderSchedule) {
        putExtra(KEY_PLANT_ID, schedule.plantId)
        putExtra(KEY_PLANT_NAME, schedule.plantName)
        putExtra(KEY_REMINDER_ID, schedule.reminderId)
        putExtra(KEY_TASK_ID, schedule.taskId)
        putExtra(KEY_CARE_TYPE, schedule.careType.name)
        putExtra(KEY_DUE_AT_MILLIS, schedule.dueAtMillis)
        putExtra(KEY_FREQUENCY_DAYS, schedule.frequencyDays)
        putExtra(KEY_NOTIFICATIONS_ENABLED, schedule.notificationsEnabled)
    }

    private fun notificationManager(): NotificationManager? =
        ContextCompat.getSystemService(context, NotificationManager::class.java)

    private fun CareType.notificationLabel(): String =
        when (this) {
            CareType.WATERING -> "watering"
            CareType.FERTILIZING -> "fertilizing"
            CareType.REPOTTING -> "repotting"
            CareType.MISTING -> "misting"
            CareType.PRUNING -> "pruning"
        }

    companion object {
        const val DEFAULT_SNOOZE_MINUTES = 180L
    }
}

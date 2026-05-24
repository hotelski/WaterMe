package com.hotelski.waterme.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.hotelski.waterme.model.CareType

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val schedule = intent.toCareReminderSchedule() ?: return
        val action = ReminderNotificationAction.entries.firstOrNull { it.intentAction == intent.action }
            ?: return

        val request = OneTimeWorkRequestBuilder<ReminderActionWorker>()
            .setInputData(
                Data.Builder()
                    .putAll(schedule.toWorkData())
                    .putString(KEY_REMINDER_ACTION, action.name)
                    .putLong(
                        KEY_SNOOZE_MINUTES,
                        intent.getLongExtra(KEY_SNOOZE_MINUTES, NotificationHelper.DEFAULT_SNOOZE_MINUTES),
                    )
                    .build(),
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "reminder-action-${schedule.taskId}-${action.name.lowercase()}",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun Intent.toCareReminderSchedule(): CareReminderSchedule? {
        val plantId = getStringExtra(KEY_PLANT_ID) ?: return null
        val plantName = getStringExtra(KEY_PLANT_NAME) ?: return null
        val reminderId = getStringExtra(KEY_REMINDER_ID) ?: return null
        val taskId = getStringExtra(KEY_TASK_ID) ?: return null
        val careType = getStringExtra(KEY_CARE_TYPE)?.let(CareType::valueOf) ?: return null
        val dueAtMillis = getLongExtra(KEY_DUE_AT_MILLIS, Long.MIN_VALUE)
        val frequencyDays = getIntExtra(KEY_FREQUENCY_DAYS, Int.MIN_VALUE)

        if (dueAtMillis == Long.MIN_VALUE || frequencyDays == Int.MIN_VALUE) return null

        return CareReminderSchedule(
            plantId = plantId,
            plantName = plantName,
            reminderId = reminderId,
            taskId = taskId,
            careType = careType,
            dueAtMillis = dueAtMillis,
            frequencyDays = frequencyDays,
            notificationsEnabled = getBooleanExtra(KEY_NOTIFICATIONS_ENABLED, true),
        )
    }
}

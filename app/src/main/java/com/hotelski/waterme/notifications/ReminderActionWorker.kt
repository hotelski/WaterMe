package com.hotelski.waterme.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.Duration

class ReminderActionWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val schedule = inputData.toCareReminderSchedule() ?: return Result.failure()
        val action = inputData.getString(KEY_REMINDER_ACTION)
            ?.let(ReminderNotificationAction::valueOf)
            ?: return Result.failure()

        when (action) {
            ReminderNotificationAction.COMPLETE -> ReminderScheduler.markCompleted(applicationContext, schedule)
            ReminderNotificationAction.SNOOZE -> {
                val minutes = inputData.getLong(KEY_SNOOZE_MINUTES, NotificationHelper.DEFAULT_SNOOZE_MINUTES)
                ReminderScheduler.snooze(applicationContext, schedule, Duration.ofMinutes(minutes))
            }
            ReminderNotificationAction.SKIP -> ReminderScheduler.skip(applicationContext, schedule)
        }

        return Result.success()
    }
}

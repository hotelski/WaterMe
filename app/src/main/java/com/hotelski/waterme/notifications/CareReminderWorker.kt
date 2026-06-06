package com.hotelski.waterme.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class CareReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val schedule = inputData.toCareReminderSchedule() ?: return Result.failure()

        if (!schedule.notificationsEnabled) return Result.success()
        if (ReminderEventStore.hasHandledNotification(applicationContext, schedule)) return Result.success()
        if (AppForegroundState.isInForeground) {
            ReminderEventStore.recordSuppressedInForeground(
                applicationContext,
                schedule,
                System.currentTimeMillis(),
            )
            return Result.success()
        }

        val wasShown = NotificationHelper(applicationContext).showCareReminder(schedule)
        if (wasShown) {
            ReminderEventStore.recordDelivered(applicationContext, schedule, System.currentTimeMillis())
        }
        return Result.success()
    }
}

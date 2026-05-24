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

        NotificationHelper(applicationContext).showCareReminder(schedule)
        return Result.success()
    }
}

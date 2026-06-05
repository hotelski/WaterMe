package com.hotelski.waterme.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hotelski.waterme.appstate.WaterMeAppContainer
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

        val careRepository = WaterMeAppContainer.careRepository(applicationContext)
        val reminderNotifications = WaterMeAppContainer.reminderNotificationCoordinator(applicationContext)
        NotificationHelper(applicationContext).cancel(schedule)

        when (action) {
            ReminderNotificationAction.COMPLETE -> {
                val completedAt = System.currentTimeMillis()
                ReminderEventStore.recordCompleted(applicationContext, schedule, completedAt)
                careRepository.markTaskCompleted(schedule.taskId, completedAt = completedAt)
            }
            ReminderNotificationAction.SNOOZE -> {
                val minutes = inputData.getLong(KEY_SNOOZE_MINUTES, NotificationHelper.DEFAULT_SNOOZE_MINUTES)
                val snoozedUntil = System.currentTimeMillis() + Duration.ofMinutes(minutes).toMillis()
                ReminderEventStore.recordSnoozed(applicationContext, schedule, snoozedUntil)
                careRepository.snoozeTask(schedule.taskId, snoozedUntil)
            }
            ReminderNotificationAction.SKIP -> {
                val skippedAt = System.currentTimeMillis()
                ReminderEventStore.recordSkipped(applicationContext, schedule, skippedAt)
                careRepository.skipTask(schedule.taskId, skippedAt = skippedAt)
            }
        }
        reminderNotifications.syncScheduledReminders()

        return Result.success()
    }
}

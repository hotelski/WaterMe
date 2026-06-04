package com.hotelski.waterme.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.hotelski.waterme.model.CareType
import java.time.Duration
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    fun scheduleWateringReminder(
        context: Context,
        plantId: String,
        plantName: String,
        reminderId: String,
        dueAtMillis: Long,
        frequencyDays: Int,
    ) {
        scheduleCareTypeReminder(
            context = context,
            plantId = plantId,
            plantName = plantName,
            reminderId = reminderId,
            careType = CareType.WATERING,
            dueAtMillis = dueAtMillis,
            frequencyDays = frequencyDays,
        )
    }

    fun scheduleFertilizingReminder(
        context: Context,
        plantId: String,
        plantName: String,
        reminderId: String,
        dueAtMillis: Long,
        frequencyDays: Int,
    ) {
        scheduleCareTypeReminder(
            context = context,
            plantId = plantId,
            plantName = plantName,
            reminderId = reminderId,
            careType = CareType.FERTILIZING,
            dueAtMillis = dueAtMillis,
            frequencyDays = frequencyDays,
        )
    }

    fun scheduleReminder(
        context: Context,
        schedule: CareReminderSchedule,
    ) {
        if (!schedule.notificationsEnabled) return

        val delayMillis = (schedule.dueAtMillis - System.currentTimeMillis()).coerceAtLeast(MINIMUM_DELAY_MS)
        val request = OneTimeWorkRequestBuilder<CareReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(schedule.toWorkData())
            .addTag(WORK_TAG_CARE_REMINDERS)
            .addTag(reminderTag(schedule.reminderId))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(schedule),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun snooze(
        context: Context,
        schedule: CareReminderSchedule,
        duration: Duration = DEFAULT_SNOOZE_DURATION,
    ) {
        val snoozedUntil = System.currentTimeMillis() + duration.toMillis()
        ReminderEventStore.recordSnoozed(context, schedule, snoozedUntil)
        NotificationHelper(context).cancel(schedule)

        scheduleReminder(
            context = context,
            schedule = schedule.copy(
                dueAtMillis = snoozedUntil,
                taskId = "${schedule.taskId}-snoozed-${snoozedUntil}",
            ),
        )
    }

    fun skip(context: Context, schedule: CareReminderSchedule) {
        val skippedAt = System.currentTimeMillis()
        ReminderEventStore.recordSkipped(context, schedule, skippedAt)
        cancelTask(context, schedule)
        NotificationHelper(context).cancel(schedule)
        rescheduleNextReminder(context, schedule, fromMillis = schedule.dueAtMillis)
    }

    fun markCompleted(context: Context, schedule: CareReminderSchedule) {
        val completedAt = System.currentTimeMillis()
        ReminderEventStore.recordCompleted(context, schedule, completedAt)
        cancelTask(context, schedule)
        NotificationHelper(context).cancel(schedule)
        rescheduleNextReminder(context, schedule, fromMillis = completedAt)
    }

    fun rescheduleNextReminder(
        context: Context,
        schedule: CareReminderSchedule,
        fromMillis: Long,
    ) {
        val nextDueAt = nextDueAfter(fromMillis, schedule.frequencyDays)
        scheduleReminder(
            context = context,
            schedule = schedule.copy(
                taskId = "${schedule.reminderId}-${nextDueAt}",
                dueAtMillis = nextDueAt,
            ),
        )
    }

    fun cancelTask(context: Context, schedule: CareReminderSchedule) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(schedule))
    }

    fun cancelReminder(context: Context, reminderId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag(reminderTag(reminderId))
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG_CARE_REMINDERS)
    }

    fun uniqueWorkName(schedule: CareReminderSchedule): String =
        "care-reminder-${schedule.reminderId}-${schedule.taskId}"

    fun reminderTag(reminderId: String): String =
        "care-reminder-id-$reminderId"

    private fun scheduleCareTypeReminder(
        context: Context,
        plantId: String,
        plantName: String,
        reminderId: String,
        careType: CareType,
        dueAtMillis: Long,
        frequencyDays: Int,
    ) {
        scheduleReminder(
            context = context,
            schedule = CareReminderSchedule(
                plantId = plantId,
                plantName = plantName,
                reminderId = reminderId,
                taskId = "$reminderId-$dueAtMillis",
                careType = careType,
                dueAtMillis = dueAtMillis,
                frequencyDays = frequencyDays,
            ),
        )
    }

    private fun nextDueAfter(fromMillis: Long, frequencyDays: Int): Long {
        val interval = Duration.ofDays(frequencyDays.coerceAtLeast(1).toLong()).toMillis()
        val now = System.currentTimeMillis()
        var nextDueAt = fromMillis + interval
        while (nextDueAt <= now) {
            nextDueAt += interval
        }
        return nextDueAt
    }

    private val DEFAULT_SNOOZE_DURATION: Duration = Duration.ofHours(3)
    private const val WORK_TAG_CARE_REMINDERS = "waterme-care-reminders"
    private const val MINIMUM_DELAY_MS = 1_000L
}

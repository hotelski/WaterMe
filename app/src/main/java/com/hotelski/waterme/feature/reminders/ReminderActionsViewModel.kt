package com.hotelski.waterme.feature.reminders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.notifications.CareReminderSchedule
import com.hotelski.waterme.notifications.NotificationPermissionHelper
import com.hotelski.waterme.notifications.ReminderScheduler
import java.time.Duration
import kotlinx.coroutines.launch

class ReminderActionsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext

    fun scheduleWateringReminder(
        plantId: String,
        plantName: String,
        reminderId: String,
        dueAtMillis: Long,
        frequencyDays: Int,
    ) {
        ReminderScheduler.scheduleWateringReminder(
            context = appContext,
            plantId = plantId,
            plantName = plantName,
            reminderId = reminderId,
            dueAtMillis = dueAtMillis,
            frequencyDays = frequencyDays,
        )
    }

    fun scheduleCareReminder(schedule: CareReminderSchedule) {
        ReminderScheduler.scheduleReminder(appContext, schedule)
    }

    fun markTaskCompleted(schedule: CareReminderSchedule) {
        viewModelScope.launch {
            // A Room-backed implementation should first mark the task completed and insert care history.
            ReminderScheduler.markCompleted(appContext, schedule)
        }
    }

    fun skipTask(schedule: CareReminderSchedule) {
        viewModelScope.launch {
            // A Room-backed implementation should first mark the task skipped and insert care history.
            ReminderScheduler.skip(appContext, schedule)
        }
    }

    fun snoozeTask(schedule: CareReminderSchedule, minutes: Long = 180L) {
        viewModelScope.launch {
            ReminderScheduler.snooze(appContext, schedule, Duration.ofMinutes(minutes))
        }
    }

    fun updateNotificationPermissionResult(granted: Boolean, pendingSchedules: List<CareReminderSchedule>) {
        viewModelScope.launch {
            if (granted) {
                pendingSchedules.forEach { ReminderScheduler.scheduleReminder(appContext, it) }
            } else {
                ReminderScheduler.cancelAll(appContext)
            }
        }
    }

    fun canPostNotifications(): Boolean =
        NotificationPermissionHelper.canPostNotifications(appContext)

    fun sampleFertilizingSchedule(
        plantId: String,
        plantName: String,
        reminderId: String,
        dueAtMillis: Long,
    ): CareReminderSchedule =
        CareReminderSchedule(
            plantId = plantId,
            plantName = plantName,
            reminderId = reminderId,
            taskId = "$reminderId-$dueAtMillis",
            careType = CareType.FERTILIZING,
            dueAtMillis = dueAtMillis,
            frequencyDays = 30,
        )
}

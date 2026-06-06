package com.hotelski.waterme.notifications

import android.content.Context
import androidx.work.WorkManager
import com.hotelski.waterme.data.local.WaterMeDatabase
import com.hotelski.waterme.data.local.entity.ReminderEntity
import com.hotelski.waterme.data.preferences.SettingsDataStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ReminderNotificationCoordinator(
    context: Context,
    private val database: WaterMeDatabase,
    private val settingsDataStore: SettingsDataStoreManager,
) {
    private val appContext = context.applicationContext

    suspend fun syncScheduledReminders() {
        cancelScheduledReminderWork()
        NotificationHelper(appContext).cancelAll()

        val settings = settingsDataStore.settings.first()
        if (!settings.notificationsEnabled || !NotificationPermissionHelper.canPostNotifications(appContext)) {
            return
        }

        database.reminderDao()
            .getActiveNotificationReminders()
            .forEach { reminder ->
                val schedule = reminder.toSchedule() ?: return@forEach
                if (ReminderEventStore.hasHandledNotification(appContext, schedule)) return@forEach
                if (AppForegroundState.isInForeground && schedule.dueAtMillis <= System.currentTimeMillis()) {
                    ReminderEventStore.recordSuppressedInForeground(
                        appContext,
                        schedule,
                        System.currentTimeMillis(),
                    )
                    return@forEach
                }
                ReminderScheduler.scheduleReminder(appContext, schedule)
            }
    }

    suspend fun cancelScheduledReminders() {
        cancelScheduledReminderWork()
        NotificationHelper(appContext).cancelAll()
    }

    private suspend fun ReminderEntity.toSchedule(): CareReminderSchedule? {
        val plant = database.plantDao().getPlant(plantId) ?: return null
        val task = database.careTaskDao().getNextOpenTaskForReminder(reminderId)
        val dueAtMillis = task?.effectiveDueAt ?: nextDueAt

        return CareReminderSchedule(
            plantId = plantId,
            plantName = plant.name,
            reminderId = reminderId,
            taskId = task?.taskId ?: "$reminderId-$dueAtMillis",
            careType = careType,
            dueAtMillis = dueAtMillis,
            frequencyDays = frequencyDays,
            notificationsEnabled = notificationsEnabled,
        )
    }

    private suspend fun cancelScheduledReminderWork() {
        withContext(Dispatchers.IO) {
            WorkManager.getInstance(appContext)
                .cancelAllWorkByTag(ReminderScheduler.WORK_TAG_CARE_REMINDERS)
                .result
                .get()
        }
    }
}

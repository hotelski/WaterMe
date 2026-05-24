package com.hotelski.waterme.data.repository

import androidx.room.withTransaction
import com.hotelski.waterme.data.local.WaterMeDatabase
import com.hotelski.waterme.data.local.dao.CareTaskDao
import com.hotelski.waterme.data.local.dao.ReminderDao
import com.hotelski.waterme.data.local.entity.CareTaskEntity
import com.hotelski.waterme.data.local.entity.ReminderEntity
import com.hotelski.waterme.data.local.entity.TaskStatus
import com.hotelski.waterme.model.CareType
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class RoomReminderRepository(
    private val database: WaterMeDatabase,
    private val reminderDao: ReminderDao = database.reminderDao(),
    private val careTaskDao: CareTaskDao = database.careTaskDao(),
) {
    fun observeRemindersForPlant(plantId: String): Flow<List<ReminderEntity>> =
        reminderDao.observeRemindersForPlant(plantId)

    suspend fun countActiveRemindersForUser(userId: String): Int =
        reminderDao.countActiveRemindersForUser(userId)

    suspend fun addReminder(
        plantId: String,
        careType: CareType,
        frequencyDays: Int,
        nextDueAt: Long,
        preferredHour: Int,
        preferredMinute: Int,
        notificationsEnabled: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ): String {
        val reminderId = UUID.randomUUID().toString()
        database.withTransaction {
            reminderDao.upsertReminder(
                ReminderEntity(
                    reminderId = reminderId,
                    plantId = plantId,
                    careType = careType,
                    frequencyDays = frequencyDays,
                    preferredHour = preferredHour,
                    preferredMinute = preferredMinute,
                    nextDueAt = nextDueAt,
                    notificationsEnabled = notificationsEnabled,
                    createdAt = nowMillis,
                    updatedAt = nowMillis,
                ),
            )
            careTaskDao.upsertTask(
                CareTaskEntity(
                    taskId = UUID.randomUUID().toString(),
                    plantId = plantId,
                    reminderId = reminderId,
                    careType = careType,
                    scheduledFor = nextDueAt,
                    effectiveDueAt = nextDueAt,
                    status = TaskStatus.PENDING,
                    createdAt = nowMillis,
                    updatedAt = nowMillis,
                ),
            )
        }
        return reminderId
    }

    suspend fun updateReminder(
        reminderId: String,
        frequencyDays: Int,
        nextDueAt: Long,
        preferredHour: Int,
        preferredMinute: Int,
        notificationsEnabled: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        val current = reminderDao.getReminder(reminderId) ?: return
        database.withTransaction {
            reminderDao.upsertReminder(
                current.copy(
                    frequencyDays = frequencyDays,
                    nextDueAt = nextDueAt,
                    preferredHour = preferredHour,
                    preferredMinute = preferredMinute,
                    notificationsEnabled = notificationsEnabled,
                    updatedAt = nowMillis,
                ),
            )
            careTaskDao.cancelOpenTasksForReminder(reminderId, nowMillis)
            careTaskDao.upsertTask(
                CareTaskEntity(
                    taskId = UUID.randomUUID().toString(),
                    plantId = current.plantId,
                    reminderId = reminderId,
                    careType = current.careType,
                    scheduledFor = nextDueAt,
                    effectiveDueAt = nextDueAt,
                    status = TaskStatus.PENDING,
                    createdAt = nowMillis,
                    updatedAt = nowMillis,
                ),
            )
        }
    }

    suspend fun setReminderEnabled(
        reminderId: String,
        enabled: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        database.withTransaction {
            reminderDao.setReminderEnabled(reminderId, enabled, nowMillis)
            if (!enabled) {
                careTaskDao.cancelOpenTasksForReminder(reminderId, nowMillis)
            }
        }
    }

    suspend fun deleteReminder(reminderId: String, nowMillis: Long = System.currentTimeMillis()) {
        database.withTransaction {
            reminderDao.softDeleteReminder(reminderId, nowMillis)
            careTaskDao.cancelOpenTasksForReminder(reminderId, nowMillis)
        }
    }
}

package com.hotelski.waterme.data.repository

import androidx.room.withTransaction
import com.hotelski.waterme.data.local.WaterMeDatabase
import com.hotelski.waterme.data.local.dao.CareHistoryDao
import com.hotelski.waterme.data.local.dao.CareTaskDao
import com.hotelski.waterme.data.local.dao.ReminderDao
import com.hotelski.waterme.data.local.entity.CareHistoryEntity
import com.hotelski.waterme.data.local.entity.CareTaskEntity
import com.hotelski.waterme.data.local.entity.HistoryAction
import com.hotelski.waterme.data.local.entity.TaskStatus
import com.hotelski.waterme.data.local.model.CareTaskWithPlant
import com.hotelski.waterme.model.CareType
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class RoomCareRepository(
    private val database: WaterMeDatabase,
    private val careTaskDao: CareTaskDao = database.careTaskDao(),
    private val careHistoryDao: CareHistoryDao = database.careHistoryDao(),
    private val reminderDao: ReminderDao = database.reminderDao(),
) {
    fun observeTodayTasks(endOfDayMillis: Long): Flow<List<CareTaskWithPlant>> =
        careTaskDao.observeOpenTasksDueBy(endOfDayMillis)

    fun observeTasksDueBy(endOfDayMillis: Long): Flow<List<CareTaskWithPlant>> =
        careTaskDao.observeOpenTasksDueBy(endOfDayMillis)

    fun observeCalendarTasks(startMillis: Long, endMillis: Long): Flow<List<CareTaskWithPlant>> =
        careTaskDao.observeUpcomingTasks(startMillis, endMillis)

    fun observeUpcomingTasks(startMillis: Long, endMillis: Long): Flow<List<CareTaskWithPlant>> =
        careTaskDao.observeUpcomingTasks(startMillis, endMillis)

    fun observeCareHistoryForPlant(plantId: String): Flow<List<CareHistoryEntity>> =
        careHistoryDao.observeHistoryForPlant(plantId)

    suspend fun markTaskCompleted(
        taskId: String,
        completedAt: Long = System.currentTimeMillis(),
        notes: String? = null,
    ) {
        val task = careTaskDao.getTask(taskId) ?: return
        database.withTransaction {
            careTaskDao.markTaskCompleted(taskId, completedAt)
            careHistoryDao.insertHistory(task.toHistory(HistoryAction.COMPLETED, completedAt, notes))
            scheduleNextTaskAfter(task, completedAt, completedAt, isCompletion = true)
        }
    }

    suspend fun skipTask(
        taskId: String,
        skippedAt: Long = System.currentTimeMillis(),
        notes: String? = null,
    ) {
        val task = careTaskDao.getTask(taskId) ?: return
        database.withTransaction {
            careTaskDao.markTaskSkipped(taskId, skippedAt)
            careHistoryDao.insertHistory(task.toHistory(HistoryAction.SKIPPED, skippedAt, notes))
            scheduleNextTaskAfter(task, task.effectiveDueAt, skippedAt, isCompletion = false)
        }
    }

    suspend fun snoozeTask(
        taskId: String,
        snoozedUntil: Long,
        nowMillis: Long = System.currentTimeMillis(),
        notes: String? = null,
    ) {
        val task = careTaskDao.getTask(taskId) ?: return
        database.withTransaction {
            careTaskDao.markTaskSnoozed(taskId, snoozedUntil, nowMillis)
            careHistoryDao.insertHistory(task.toHistory(HistoryAction.SNOOZED, nowMillis, notes))
        }
    }

    suspend fun logManualCare(
        plantId: String,
        careType: CareType,
        performedAt: Long = System.currentTimeMillis(),
        notes: String? = null,
    ) {
        careHistoryDao.insertHistory(
            CareHistoryEntity(
                historyId = UUID.randomUUID().toString(),
                plantId = plantId,
                careType = careType,
                action = HistoryAction.MANUAL_LOG,
                performedAt = performedAt,
                notes = notes,
                createdAt = performedAt,
            ),
        )
    }

    private suspend fun scheduleNextTaskAfter(
        task: CareTaskEntity,
        anchorMillis: Long,
        nowMillis: Long,
        isCompletion: Boolean,
    ) {
        val reminderId = task.reminderId ?: return
        val reminder = reminderDao.getReminder(reminderId) ?: return
        if (!reminder.isEnabled) return

        val nextDueAt = nextDueAfter(anchorMillis, reminder.frequencyDays, nowMillis)
        if (isCompletion) {
            reminderDao.updateAfterCompletion(reminderId, anchorMillis, nextDueAt)
        } else {
            reminderDao.updateAfterSkip(reminderId, nowMillis, nextDueAt)
        }
        careTaskDao.upsertTask(
            CareTaskEntity(
                taskId = UUID.randomUUID().toString(),
                plantId = task.plantId,
                reminderId = reminderId,
                careType = task.careType,
                scheduledFor = nextDueAt,
                effectiveDueAt = nextDueAt,
                status = TaskStatus.PENDING,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
        )
    }

    private fun CareTaskEntity.toHistory(
        action: HistoryAction,
        performedAt: Long,
        notes: String?,
    ): CareHistoryEntity =
        CareHistoryEntity(
            historyId = UUID.randomUUID().toString(),
            plantId = plantId,
            reminderId = reminderId,
            taskId = taskId,
            careType = careType,
            action = action,
            performedAt = performedAt,
            notes = notes,
            createdAt = performedAt,
        )

    private fun nextDueAfter(anchorMillis: Long, frequencyDays: Int, nowMillis: Long): Long {
        val intervalMillis = frequencyDays.coerceAtLeast(1) * DAY_MILLIS
        var nextDueAt = anchorMillis + intervalMillis
        while (nextDueAt <= nowMillis) {
            nextDueAt += intervalMillis
        }
        return nextDueAt
    }

    private companion object {
        const val DAY_MILLIS = 86_400_000L
    }
}

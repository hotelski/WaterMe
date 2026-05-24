package com.hotelski.waterme.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hotelski.waterme.data.local.entity.CareTaskEntity
import com.hotelski.waterme.data.local.model.CareTaskWithPlant
import kotlinx.coroutines.flow.Flow

@Dao
interface CareTaskDao {
    @Query(
        """
        SELECT
            t.task_id,
            t.plant_id,
            p.name AS plant_name,
            p.plant_type,
            p.location,
            (
                SELECT pp.local_uri
                FROM plant_photos AS pp
                WHERE pp.plant_id = p.plant_id AND pp.is_primary = 1
                ORDER BY pp.created_at DESC
                LIMIT 1
            ) AS primary_photo_uri,
            t.reminder_id,
            t.care_type,
            t.scheduled_for,
            t.effective_due_at,
            t.status,
            t.snoozed_until
        FROM care_tasks AS t
        INNER JOIN plants AS p ON p.plant_id = t.plant_id
        WHERE t.status IN ('PENDING', 'SNOOZED')
            AND t.effective_due_at <= :endOfDayMillis
            AND p.deleted_at IS NULL
        ORDER BY t.effective_due_at ASC, p.name COLLATE NOCASE ASC
        """,
    )
    fun observeOpenTasksDueBy(endOfDayMillis: Long): Flow<List<CareTaskWithPlant>>

    @Query(
        """
        SELECT
            t.task_id,
            t.plant_id,
            p.name AS plant_name,
            p.plant_type,
            p.location,
            (
                SELECT pp.local_uri
                FROM plant_photos AS pp
                WHERE pp.plant_id = p.plant_id AND pp.is_primary = 1
                ORDER BY pp.created_at DESC
                LIMIT 1
            ) AS primary_photo_uri,
            t.reminder_id,
            t.care_type,
            t.scheduled_for,
            t.effective_due_at,
            t.status,
            t.snoozed_until
        FROM care_tasks AS t
        INNER JOIN plants AS p ON p.plant_id = t.plant_id
        WHERE t.status IN ('PENDING', 'SNOOZED')
            AND t.effective_due_at >= :startMillis
            AND t.effective_due_at < :endMillis
            AND p.deleted_at IS NULL
        ORDER BY t.effective_due_at ASC, p.name COLLATE NOCASE ASC
        """,
    )
    fun observeUpcomingTasks(startMillis: Long, endMillis: Long): Flow<List<CareTaskWithPlant>>

    @Query(
        """
        SELECT * FROM care_tasks
        WHERE plant_id = :plantId
        ORDER BY effective_due_at DESC
        """,
    )
    fun observeTasksForPlant(plantId: String): Flow<List<CareTaskEntity>>

    @Query("SELECT * FROM care_tasks WHERE task_id = :taskId")
    suspend fun getTask(taskId: String): CareTaskEntity?

    @Query(
        """
        SELECT * FROM care_tasks
        WHERE reminder_id = :reminderId AND status IN ('PENDING', 'SNOOZED')
        ORDER BY effective_due_at ASC
        LIMIT 1
        """,
    )
    suspend fun getNextOpenTaskForReminder(reminderId: String): CareTaskEntity?

    @Upsert
    suspend fun upsertTask(task: CareTaskEntity)

    @Upsert
    suspend fun upsertTasks(tasks: List<CareTaskEntity>)

    @Query(
        """
        UPDATE care_tasks
        SET status = 'COMPLETED', completed_at = :completedAt, updated_at = :completedAt
        WHERE task_id = :taskId
        """,
    )
    suspend fun markTaskCompleted(taskId: String, completedAt: Long)

    @Query(
        """
        UPDATE care_tasks
        SET status = 'SKIPPED', skipped_at = :skippedAt, updated_at = :skippedAt
        WHERE task_id = :taskId
        """,
    )
    suspend fun markTaskSkipped(taskId: String, skippedAt: Long)

    @Query(
        """
        UPDATE care_tasks
        SET status = 'SNOOZED', snoozed_until = :snoozedUntil, effective_due_at = :snoozedUntil, updated_at = :updatedAt
        WHERE task_id = :taskId
        """,
    )
    suspend fun markTaskSnoozed(taskId: String, snoozedUntil: Long, updatedAt: Long)

    @Query(
        """
        UPDATE care_tasks
        SET status = 'CANCELED', updated_at = :updatedAt
        WHERE reminder_id = :reminderId AND status IN ('PENDING', 'SNOOZED')
        """,
    )
    suspend fun cancelOpenTasksForReminder(reminderId: String, updatedAt: Long)
}

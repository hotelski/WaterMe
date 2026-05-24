package com.hotelski.waterme.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hotelski.waterme.data.local.entity.ReminderEntity
import com.hotelski.waterme.model.CareType
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query(
        """
        SELECT * FROM reminders
        WHERE plant_id = :plantId AND deleted_at IS NULL
        ORDER BY care_type ASC
        """,
    )
    fun observeRemindersForPlant(plantId: String): Flow<List<ReminderEntity>>

    @Query(
        """
        SELECT * FROM reminders
        WHERE reminder_id = :reminderId AND deleted_at IS NULL
        """,
    )
    suspend fun getReminder(reminderId: String): ReminderEntity?

    @Query(
        """
        SELECT * FROM reminders
        WHERE plant_id = :plantId AND care_type = :careType AND deleted_at IS NULL
        LIMIT 1
        """,
    )
    suspend fun getReminderForPlantAndType(plantId: String, careType: CareType): ReminderEntity?

    @Query(
        """
        SELECT * FROM reminders
        WHERE is_enabled = 1 AND notifications_enabled = 1 AND deleted_at IS NULL
        ORDER BY next_due_at ASC
        """,
    )
    suspend fun getActiveNotificationReminders(): List<ReminderEntity>

    @Query(
        """
        SELECT COUNT(*) FROM reminders
        WHERE plant_id = :plantId AND deleted_at IS NULL
        """,
    )
    suspend fun countRemindersForPlant(plantId: String): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM reminders AS r
        INNER JOIN plants AS p ON p.plant_id = r.plant_id
        WHERE p.user_id = :userId
            AND p.deleted_at IS NULL
            AND r.deleted_at IS NULL
            AND r.is_enabled = 1
        """,
    )
    suspend fun countActiveRemindersForUser(userId: String): Int

    @Upsert
    suspend fun upsertReminder(reminder: ReminderEntity)

    @Query(
        """
        UPDATE reminders
        SET next_due_at = :nextDueAt, last_completed_at = :completedAt, updated_at = :completedAt
        WHERE reminder_id = :reminderId
        """,
    )
    suspend fun updateAfterCompletion(reminderId: String, completedAt: Long, nextDueAt: Long)

    @Query(
        """
        UPDATE reminders
        SET next_due_at = :nextDueAt, last_skipped_at = :skippedAt, updated_at = :skippedAt
        WHERE reminder_id = :reminderId
        """,
    )
    suspend fun updateAfterSkip(reminderId: String, skippedAt: Long, nextDueAt: Long)

    @Query(
        """
        UPDATE reminders
        SET is_enabled = :isEnabled, updated_at = :updatedAt
        WHERE reminder_id = :reminderId
        """,
    )
    suspend fun setReminderEnabled(reminderId: String, isEnabled: Boolean, updatedAt: Long)

    @Query(
        """
        UPDATE reminders
        SET notifications_enabled = :notificationsEnabled, updated_at = :updatedAt
        WHERE reminder_id = :reminderId
        """,
    )
    suspend fun setNotificationsEnabled(reminderId: String, notificationsEnabled: Boolean, updatedAt: Long)

    @Query("UPDATE reminders SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE reminder_id = :reminderId")
    suspend fun softDeleteReminder(reminderId: String, deletedAt: Long)

    @Query("DELETE FROM reminders WHERE reminder_id = :reminderId")
    suspend fun deleteReminderHard(reminderId: String)
}

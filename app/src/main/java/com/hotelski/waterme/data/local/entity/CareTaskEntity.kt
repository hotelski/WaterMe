package com.hotelski.waterme.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hotelski.waterme.model.CareType

@Entity(
    tableName = "care_tasks",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["plant_id"],
            childColumns = ["plant_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ReminderEntity::class,
            parentColumns = ["reminder_id"],
            childColumns = ["reminder_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["plant_id"]),
        Index(value = ["reminder_id"]),
        Index(value = ["effective_due_at"]),
        Index(value = ["status", "effective_due_at"]),
    ],
)
data class CareTaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "task_id")
    val taskId: String,
    @ColumnInfo(name = "plant_id")
    val plantId: String,
    @ColumnInfo(name = "reminder_id")
    val reminderId: String?,
    @ColumnInfo(name = "care_type")
    val careType: CareType,
    @ColumnInfo(name = "scheduled_for")
    val scheduledFor: Long,
    @ColumnInfo(name = "effective_due_at")
    val effectiveDueAt: Long,
    @ColumnInfo(name = "status")
    val status: TaskStatus,
    @ColumnInfo(name = "snoozed_until")
    val snoozedUntil: Long? = null,
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,
    @ColumnInfo(name = "skipped_at")
    val skippedAt: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

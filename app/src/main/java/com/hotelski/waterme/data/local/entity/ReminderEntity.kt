package com.hotelski.waterme.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hotelski.waterme.model.CareType

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["plant_id"],
            childColumns = ["plant_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["plant_id"]),
        Index(value = ["plant_id", "care_type"]),
        Index(value = ["next_due_at"]),
    ],
)
data class ReminderEntity(
    @PrimaryKey
    @ColumnInfo(name = "reminder_id")
    val reminderId: String,
    @ColumnInfo(name = "plant_id")
    val plantId: String,
    @ColumnInfo(name = "care_type")
    val careType: CareType,
    @ColumnInfo(name = "frequency_days")
    val frequencyDays: Int,
    @ColumnInfo(name = "preferred_hour")
    val preferredHour: Int,
    @ColumnInfo(name = "preferred_minute")
    val preferredMinute: Int,
    @ColumnInfo(name = "next_due_at")
    val nextDueAt: Long,
    @ColumnInfo(name = "last_completed_at")
    val lastCompletedAt: Long? = null,
    @ColumnInfo(name = "last_skipped_at")
    val lastSkippedAt: Long? = null,
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,
    @ColumnInfo(name = "notifications_enabled")
    val notificationsEnabled: Boolean = true,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
)

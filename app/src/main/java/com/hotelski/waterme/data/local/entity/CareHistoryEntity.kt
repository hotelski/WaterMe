package com.hotelski.waterme.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.model.HealthMood

@Entity(
    tableName = "care_history",
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
        ForeignKey(
            entity = CareTaskEntity::class,
            parentColumns = ["task_id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["plant_id"]),
        Index(value = ["reminder_id"]),
        Index(value = ["task_id"]),
        Index(value = ["performed_at"]),
    ],
)
data class CareHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "history_id")
    val historyId: String,
    @ColumnInfo(name = "plant_id")
    val plantId: String,
    @ColumnInfo(name = "reminder_id")
    val reminderId: String? = null,
    @ColumnInfo(name = "task_id")
    val taskId: String? = null,
    @ColumnInfo(name = "care_type")
    val careType: CareType,
    @ColumnInfo(name = "action")
    val action: HistoryAction,
    @ColumnInfo(name = "health_mood")
    val healthMood: HealthMood? = null,
    @ColumnInfo(name = "performed_at")
    val performedAt: Long,
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)

package com.hotelski.waterme.data.local.model

import androidx.room.ColumnInfo
import com.hotelski.waterme.data.local.entity.TaskStatus
import com.hotelski.waterme.model.CareType

data class CareTaskWithPlant(
    @ColumnInfo(name = "task_id")
    val taskId: String,
    @ColumnInfo(name = "plant_id")
    val plantId: String,
    @ColumnInfo(name = "plant_name")
    val plantName: String,
    @ColumnInfo(name = "plant_type")
    val plantType: String,
    @ColumnInfo(name = "location")
    val location: String,
    @ColumnInfo(name = "primary_photo_uri")
    val primaryPhotoUri: String?,
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
    val snoozedUntil: Long?,
)

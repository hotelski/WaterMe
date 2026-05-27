package com.hotelski.waterme.data.local.model

import androidx.room.ColumnInfo
import com.hotelski.waterme.data.local.entity.HistoryAction
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.model.HealthMood

data class CareHistoryWithPlant(
    @ColumnInfo(name = "history_id")
    val historyId: String,
    @ColumnInfo(name = "plant_id")
    val plantId: String,
    @ColumnInfo(name = "plant_name")
    val plantName: String,
    @ColumnInfo(name = "plant_type")
    val plantType: String,
    @ColumnInfo(name = "care_type")
    val careType: CareType,
    @ColumnInfo(name = "action")
    val action: HistoryAction,
    @ColumnInfo(name = "health_mood")
    val healthMood: HealthMood?,
    @ColumnInfo(name = "performed_at")
    val performedAt: Long,
    @ColumnInfo(name = "notes")
    val notes: String?,
    @ColumnInfo(name = "photo_uri")
    val photoUri: String?,
)

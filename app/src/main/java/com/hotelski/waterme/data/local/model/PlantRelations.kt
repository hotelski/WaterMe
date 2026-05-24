package com.hotelski.waterme.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.hotelski.waterme.data.local.entity.CareHistoryEntity
import com.hotelski.waterme.data.local.entity.PlantEntity
import com.hotelski.waterme.data.local.entity.PlantPhotoEntity
import com.hotelski.waterme.data.local.entity.ReminderEntity

data class PlantWithDetails(
    @Embedded val plant: PlantEntity,
    @Relation(
        parentColumn = "plant_id",
        entityColumn = "plant_id",
    )
    val photos: List<PlantPhotoEntity>,
    @Relation(
        parentColumn = "plant_id",
        entityColumn = "plant_id",
    )
    val reminders: List<ReminderEntity>,
    @Relation(
        parentColumn = "plant_id",
        entityColumn = "plant_id",
    )
    val careHistory: List<CareHistoryEntity>,
)

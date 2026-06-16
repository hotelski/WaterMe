package com.hotelski.waterme.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_care_advice_cache",
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
    ],
)
data class AiCareAdviceCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "cache_key")
    val cacheKey: String,
    @ColumnInfo(name = "plant_id")
    val plantId: String?,
    @ColumnInfo(name = "plant_name")
    val plantName: String,
    @ColumnInfo(name = "scientific_name")
    val scientificName: String?,
    @ColumnInfo(name = "generated_at")
    val generatedAt: Long,
    @ColumnInfo(name = "model_name")
    val modelName: String,
    @ColumnInfo(name = "advice_plant_name")
    val advicePlantName: String,
    @ColumnInfo(name = "advice_scientific_name")
    val adviceScientificName: String?,
    @ColumnInfo(name = "short_description")
    val shortDescription: String,
    @ColumnInfo(name = "care_difficulty")
    val careDifficulty: String,
    @ColumnInfo(name = "mature_height")
    val matureHeight: String,
    @ColumnInfo(name = "watering")
    val watering: String,
    @ColumnInfo(name = "light")
    val light: String,
    @ColumnInfo(name = "temperature")
    val temperature: String,
    @ColumnInfo(name = "humidity")
    val humidity: String,
    @ColumnInfo(name = "fertilizing")
    val fertilizing: String,
    @ColumnInfo(name = "repotting")
    val repotting: String,
    @ColumnInfo(name = "flowering")
    val flowering: String,
    @ColumnInfo(name = "growth")
    val growth: String,
    @ColumnInfo(name = "toxicity")
    val toxicity: String,
    @ColumnInfo(name = "origin")
    val origin: String,
    @ColumnInfo(name = "disclaimer")
    val disclaimer: String,
    @ColumnInfo(name = "suggested_watering_interval_days")
    val suggestedWateringIntervalDays: Int?,
    @ColumnInfo(name = "suggested_fertilizing_interval_days")
    val suggestedFertilizingIntervalDays: Int?,
    @ColumnInfo(name = "suggested_light_level")
    val suggestedLightLevel: String?,
    @ColumnInfo(name = "suggested_note")
    val suggestedNote: String?,
)

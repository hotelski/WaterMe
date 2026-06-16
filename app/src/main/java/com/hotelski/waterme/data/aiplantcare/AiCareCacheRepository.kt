package com.hotelski.waterme.data.aiplantcare

import com.hotelski.waterme.data.ai.AiModelConfig
import com.hotelski.waterme.data.local.WaterMeDatabase
import com.hotelski.waterme.data.local.dao.AiCareAdviceCacheDao
import com.hotelski.waterme.data.local.entity.AiCareAdviceCacheEntity
import com.hotelski.waterme.model.PlantCareAdvice
import java.util.Locale

data class AiCareCachedAdvice(
    val plantId: String?,
    val plantName: String,
    val scientificName: String?,
    val generatedAt: Long,
    val modelName: String,
    val advice: PlantCareAdvice,
)

interface AiCareCacheRepository {
    suspend fun getCachedAdvice(
        plantId: String?,
        plantName: String,
        scientificName: String?,
    ): AiCareCachedAdvice?

    suspend fun saveAdvice(
        plantId: String?,
        plantName: String,
        scientificName: String?,
        advice: PlantCareAdvice,
        generatedAt: Long = System.currentTimeMillis(),
        modelName: String = AiModelConfig.GeminiFlashLiteModel,
    ): AiCareCachedAdvice
}

class RoomAiCareCacheRepository(
    database: WaterMeDatabase,
    private val dao: AiCareAdviceCacheDao = database.aiCareAdviceCacheDao(),
) : AiCareCacheRepository {
    override suspend fun getCachedAdvice(
        plantId: String?,
        plantName: String,
        scientificName: String?,
    ): AiCareCachedAdvice? =
        dao.getCachedAdvice(
            cacheKey = buildAiCareCacheKey(
                plantId = plantId,
                plantName = plantName,
                scientificName = scientificName,
            ),
        )?.toCachedAdvice()

    override suspend fun saveAdvice(
        plantId: String?,
        plantName: String,
        scientificName: String?,
        advice: PlantCareAdvice,
        generatedAt: Long,
        modelName: String,
    ): AiCareCachedAdvice {
        val cacheKey = buildAiCareCacheKey(
            plantId = plantId,
            plantName = plantName,
            scientificName = scientificName,
        )
        val cachedAdvice = AiCareCachedAdvice(
            plantId = plantId?.trim()?.ifBlank { null },
            plantName = plantName.trim(),
            scientificName = scientificName?.trim()?.ifBlank { null },
            generatedAt = generatedAt,
            modelName = modelName,
            advice = advice,
        )
        dao.upsertCachedAdvice(cachedAdvice.toEntity(cacheKey))
        return cachedAdvice
    }
}

fun buildAiCareCacheKey(
    plantId: String?,
    plantName: String,
    scientificName: String?,
): String {
    val normalizedPlantId = plantId?.trim()?.ifBlank { null }
    if (normalizedPlantId != null) return "plant:$normalizedPlantId"

    val normalizedPlantName = plantName.normalizeForAiCareCache()
    val normalizedScientificName = scientificName.orEmpty().normalizeForAiCareCache()
    return "name:$normalizedPlantName|scientific:$normalizedScientificName"
}

private fun AiCareCachedAdvice.toEntity(cacheKey: String): AiCareAdviceCacheEntity =
    AiCareAdviceCacheEntity(
        cacheKey = cacheKey,
        plantId = plantId,
        plantName = plantName,
        scientificName = scientificName,
        generatedAt = generatedAt,
        modelName = modelName,
        advicePlantName = advice.plantName,
        adviceScientificName = advice.scientificName,
        shortDescription = advice.shortDescription,
        careDifficulty = advice.careDifficulty,
        matureHeight = advice.matureHeight,
        watering = advice.watering,
        light = advice.light,
        temperature = advice.temperature,
        humidity = advice.humidity,
        fertilizing = advice.fertilizing,
        repotting = advice.repotting,
        flowering = advice.flowering,
        growth = advice.growth,
        toxicity = advice.toxicity,
        origin = advice.origin,
        disclaimer = advice.disclaimer,
        suggestedWateringIntervalDays = advice.suggestedWateringIntervalDays,
        suggestedFertilizingIntervalDays = advice.suggestedFertilizingIntervalDays,
        suggestedLightLevel = advice.suggestedLightLevel,
        suggestedNote = advice.suggestedNote,
    )

private fun AiCareAdviceCacheEntity.toCachedAdvice(): AiCareCachedAdvice =
    AiCareCachedAdvice(
        plantId = plantId,
        plantName = plantName,
        scientificName = scientificName,
        generatedAt = generatedAt,
        modelName = modelName,
        advice = PlantCareAdvice(
            plantName = advicePlantName,
            scientificName = adviceScientificName,
            shortDescription = shortDescription,
            careDifficulty = careDifficulty,
            matureHeight = matureHeight,
            watering = watering,
            light = light,
            temperature = temperature,
            humidity = humidity,
            fertilizing = fertilizing,
            repotting = repotting,
            flowering = flowering,
            growth = growth,
            toxicity = toxicity,
            origin = origin,
            disclaimer = disclaimer,
            suggestedWateringIntervalDays = suggestedWateringIntervalDays,
            suggestedFertilizingIntervalDays = suggestedFertilizingIntervalDays,
            suggestedLightLevel = suggestedLightLevel,
            suggestedNote = suggestedNote,
        ),
    )

private fun String.normalizeForAiCareCache(): String =
    trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")

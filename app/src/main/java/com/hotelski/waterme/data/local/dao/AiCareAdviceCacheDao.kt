package com.hotelski.waterme.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hotelski.waterme.data.local.entity.AiCareAdviceCacheEntity

@Dao
interface AiCareAdviceCacheDao {
    @Query("SELECT * FROM ai_care_advice_cache WHERE cache_key = :cacheKey LIMIT 1")
    suspend fun getCachedAdvice(cacheKey: String): AiCareAdviceCacheEntity?

    @Upsert
    suspend fun upsertCachedAdvice(advice: AiCareAdviceCacheEntity)
}

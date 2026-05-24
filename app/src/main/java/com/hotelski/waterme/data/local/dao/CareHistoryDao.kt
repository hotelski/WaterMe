package com.hotelski.waterme.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hotelski.waterme.data.local.entity.CareHistoryEntity
import com.hotelski.waterme.data.local.model.CareHistoryWithPlant
import com.hotelski.waterme.model.CareType
import kotlinx.coroutines.flow.Flow

@Dao
interface CareHistoryDao {
    @Query(
        """
        SELECT * FROM care_history
        WHERE plant_id = :plantId
        ORDER BY performed_at DESC
        """,
    )
    fun observeHistoryForPlant(plantId: String): Flow<List<CareHistoryEntity>>

    @Query("SELECT * FROM care_history ORDER BY performed_at DESC")
    fun observeAllHistory(): Flow<List<CareHistoryEntity>>

    @Query(
        """
        SELECT
            h.history_id,
            h.plant_id,
            p.name AS plant_name,
            h.care_type,
            h.action,
            h.health_mood,
            h.performed_at,
            h.notes,
            h.photo_uri
        FROM care_history AS h
        INNER JOIN plants AS p ON p.plant_id = h.plant_id
        WHERE p.user_id = :userId AND p.deleted_at IS NULL
        ORDER BY h.performed_at DESC
        """,
    )
    fun observeHistoryForUser(userId: String): Flow<List<CareHistoryWithPlant>>

    @Query(
        """
        SELECT
            h.history_id,
            h.plant_id,
            p.name AS plant_name,
            h.care_type,
            h.action,
            h.health_mood,
            h.performed_at,
            h.notes,
            h.photo_uri
        FROM care_history AS h
        INNER JOIN plants AS p ON p.plant_id = h.plant_id
        WHERE p.user_id = :userId
            AND p.deleted_at IS NULL
            AND h.action != 'HEALTH_NOTE'
            AND (:plantId IS NULL OR h.plant_id = :plantId)
            AND (:careType IS NULL OR h.care_type = :careType)
            AND (:startMillis IS NULL OR h.performed_at >= :startMillis)
            AND (:endMillis IS NULL OR h.performed_at <= :endMillis)
        ORDER BY h.performed_at DESC
        """,
    )
    fun observeFilteredHistoryForUser(
        userId: String,
        plantId: String?,
        careType: String?,
        startMillis: Long?,
        endMillis: Long?,
    ): Flow<List<CareHistoryWithPlant>>

    @Query(
        """
        SELECT
            h.history_id,
            h.plant_id,
            p.name AS plant_name,
            h.care_type,
            h.action,
            h.health_mood,
            h.performed_at,
            h.notes,
            h.photo_uri
        FROM care_history AS h
        INNER JOIN plants AS p ON p.plant_id = h.plant_id
        WHERE p.user_id = :userId
            AND p.deleted_at IS NULL
            AND h.action = 'HEALTH_NOTE'
        ORDER BY h.performed_at DESC
        LIMIT :limit
        """,
    )
    fun observeRecentHealthNotesForUser(userId: String, limit: Int): Flow<List<CareHistoryWithPlant>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: CareHistoryEntity)

    @Query("SELECT * FROM care_history WHERE history_id = :historyId")
    suspend fun getHistory(historyId: String): CareHistoryEntity?

    @Query(
        """
        UPDATE care_history
        SET plant_id = :plantId,
            care_type = :careType,
            performed_at = :performedAt,
            notes = :notes,
            photo_uri = :photoUri
        WHERE history_id = :historyId
        """,
    )
    suspend fun updateHistoryEntry(
        historyId: String,
        plantId: String,
        careType: CareType,
        performedAt: Long,
        notes: String?,
        photoUri: String?,
    )

    @Query("DELETE FROM care_history WHERE history_id = :historyId")
    suspend fun deleteHistoryEntry(historyId: String)

    @Query("SELECT COUNT(*) FROM care_history WHERE plant_id = :plantId")
    suspend fun countHistoryForPlant(plantId: String): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM care_history AS h
        INNER JOIN plants AS p ON p.plant_id = h.plant_id
        WHERE p.user_id = :userId AND p.deleted_at IS NULL
        """,
    )
    suspend fun countHistoryForUser(userId: String): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM care_history AS h
        INNER JOIN plants AS p ON p.plant_id = h.plant_id
        WHERE p.user_id = :userId
            AND p.deleted_at IS NULL
            AND h.action = 'HEALTH_NOTE'
        """,
    )
    suspend fun countHealthNotesForUser(userId: String): Int
}

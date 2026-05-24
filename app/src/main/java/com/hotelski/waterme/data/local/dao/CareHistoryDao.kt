package com.hotelski.waterme.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hotelski.waterme.data.local.entity.CareHistoryEntity
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: CareHistoryEntity)

    @Query("SELECT COUNT(*) FROM care_history WHERE plant_id = :plantId")
    suspend fun countHistoryForPlant(plantId: String): Int
}

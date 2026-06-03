package com.hotelski.waterme.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.hotelski.waterme.data.local.entity.PlantEntity
import com.hotelski.waterme.data.local.model.PlantWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    @Query(
        """
        SELECT * FROM plants
        WHERE user_id = :userId AND deleted_at IS NULL AND archived_at IS NULL
        ORDER BY name COLLATE NOCASE ASC
        """,
    )
    fun observePlants(userId: String): Flow<List<PlantEntity>>

    @Transaction
    @Query(
        """
        SELECT * FROM plants
        WHERE user_id = :userId AND deleted_at IS NULL AND archived_at IS NULL
        ORDER BY name COLLATE NOCASE ASC
        """,
    )
    fun observePlantsWithDetails(userId: String): Flow<List<PlantWithDetails>>

    @Query("SELECT * FROM plants WHERE plant_id = :plantId AND deleted_at IS NULL")
    fun observePlant(plantId: String): Flow<PlantEntity?>

    @Transaction
    @Query("SELECT * FROM plants WHERE plant_id = :plantId AND deleted_at IS NULL")
    fun observePlantWithDetails(plantId: String): Flow<PlantWithDetails?>

    @Query("SELECT * FROM plants WHERE plant_id = :plantId AND deleted_at IS NULL")
    suspend fun getPlant(plantId: String): PlantEntity?

    @Query("SELECT COUNT(*) FROM plants WHERE user_id = :userId AND deleted_at IS NULL")
    suspend fun countActivePlantsForUser(userId: String): Int

    @Upsert
    suspend fun upsertPlant(plant: PlantEntity)

    @Query("UPDATE plants SET archived_at = :archivedAt, updated_at = :archivedAt WHERE plant_id = :plantId")
    suspend fun archivePlant(plantId: String, archivedAt: Long)

    @Query("UPDATE plants SET is_favorite = :isFavorite, updated_at = :updatedAt WHERE plant_id = :plantId")
    suspend fun setPlantFavorite(plantId: String, isFavorite: Boolean, updatedAt: Long)

    @Query("UPDATE plants SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE plant_id = :plantId")
    suspend fun softDeletePlant(plantId: String, deletedAt: Long)

    @Query("DELETE FROM plants WHERE plant_id = :plantId")
    suspend fun deletePlantHard(plantId: String)
}

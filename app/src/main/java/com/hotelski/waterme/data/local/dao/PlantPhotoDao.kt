package com.hotelski.waterme.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hotelski.waterme.data.local.entity.PlantPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantPhotoDao {
    @Query("SELECT * FROM plant_photos WHERE plant_id = :plantId ORDER BY is_primary DESC, created_at DESC")
    fun observePhotosForPlant(plantId: String): Flow<List<PlantPhotoEntity>>

    @Query(
        """
        SELECT * FROM plant_photos
        WHERE plant_id = :plantId AND is_primary = 1
        ORDER BY created_at DESC
        LIMIT 1
        """,
    )
    fun observePrimaryPhoto(plantId: String): Flow<PlantPhotoEntity?>

    @Upsert
    suspend fun upsertPhoto(photo: PlantPhotoEntity)

    @Upsert
    suspend fun upsertPhotos(photos: List<PlantPhotoEntity>)

    @Query("UPDATE plant_photos SET is_primary = 0, updated_at = :updatedAt WHERE plant_id = :plantId")
    suspend fun clearPrimaryPhoto(plantId: String, updatedAt: Long)

    @Query("UPDATE plant_photos SET is_primary = 1, updated_at = :updatedAt WHERE photo_id = :photoId")
    suspend fun setPrimaryPhoto(photoId: String, updatedAt: Long)

    @Query("DELETE FROM plant_photos WHERE photo_id = :photoId")
    suspend fun deletePhoto(photoId: String)

    @Query("DELETE FROM plant_photos WHERE plant_id = :plantId")
    suspend fun deletePhotosForPlant(plantId: String)
}

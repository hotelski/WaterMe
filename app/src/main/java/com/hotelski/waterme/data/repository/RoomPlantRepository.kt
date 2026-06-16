package com.hotelski.waterme.data.repository

import androidx.room.withTransaction
import com.hotelski.waterme.data.local.WaterMeDatabase
import com.hotelski.waterme.data.local.dao.PlantDao
import com.hotelski.waterme.data.local.dao.PlantPhotoDao
import com.hotelski.waterme.data.local.entity.PlantEntity
import com.hotelski.waterme.data.local.entity.PlantPhotoEntity
import com.hotelski.waterme.data.local.model.PlantWithDetails
import com.hotelski.waterme.model.PlantEnvironment
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class RoomPlantRepository(
    private val database: WaterMeDatabase,
    private val plantDao: PlantDao = database.plantDao(),
    private val plantPhotoDao: PlantPhotoDao = database.plantPhotoDao(),
) {
    fun observePlants(userId: String): Flow<List<PlantEntity>> =
        plantDao.observePlants(userId)

    fun observePlantsWithDetails(userId: String): Flow<List<PlantWithDetails>> =
        plantDao.observePlantsWithDetails(userId)

    fun observePlant(plantId: String): Flow<PlantEntity?> =
        plantDao.observePlant(plantId)

    fun observePlantWithDetails(plantId: String): Flow<PlantWithDetails?> =
        plantDao.observePlantWithDetails(plantId)

    suspend fun addPlant(
        userId: String,
        name: String,
        plantType: String,
        location: String,
        environment: PlantEnvironment,
        notes: String,
        primaryPhotoUri: String? = null,
        nowMillis: Long = System.currentTimeMillis(),
    ): String {
        val plantId = UUID.randomUUID().toString()
        database.withTransaction {
            plantDao.upsertPlant(
                PlantEntity(
                    plantId = plantId,
                    userId = userId,
                    name = name.trim(),
                    plantType = plantType.trim(),
                    location = location.trim(),
                    environment = environment,
                    notes = notes.trim(),
                    createdAt = nowMillis,
                    updatedAt = nowMillis,
                ),
            )
            if (!primaryPhotoUri.isNullOrBlank()) {
                plantPhotoDao.upsertPhoto(
                    PlantPhotoEntity(
                        photoId = UUID.randomUUID().toString(),
                        plantId = plantId,
                        localUri = primaryPhotoUri,
                        isPrimary = true,
                        createdAt = nowMillis,
                        updatedAt = nowMillis,
                    ),
                )
            }
        }
        return plantId
    }

    suspend fun updatePlant(
        plantId: String,
        name: String,
        plantType: String,
        location: String,
        environment: PlantEnvironment,
        notes: String,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        val current = plantDao.getPlant(plantId) ?: return
        plantDao.upsertPlant(
            current.copy(
                name = name.trim(),
                plantType = plantType.trim(),
                location = location.trim(),
                environment = environment,
                notes = notes.trim(),
                updatedAt = nowMillis,
            ),
        )
    }

    suspend fun updatePlantNotes(
        plantId: String,
        notes: String,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        val current = plantDao.getPlant(plantId) ?: return
        plantDao.upsertPlant(
            current.copy(
                notes = notes.trim(),
                updatedAt = nowMillis,
            ),
        )
    }

    suspend fun setPrimaryPhoto(
        plantId: String,
        photoId: String,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        database.withTransaction {
            plantPhotoDao.clearPrimaryPhoto(plantId, nowMillis)
            plantPhotoDao.setPrimaryPhoto(photoId, nowMillis)
        }
    }

    suspend fun replacePrimaryPhoto(
        plantId: String,
        localUri: String?,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        if (localUri.isNullOrBlank()) return

        database.withTransaction {
            plantPhotoDao.clearPrimaryPhoto(plantId, nowMillis)
            plantPhotoDao.upsertPhoto(
                PlantPhotoEntity(
                    photoId = UUID.randomUUID().toString(),
                    plantId = plantId,
                    localUri = localUri,
                    isPrimary = true,
                    createdAt = nowMillis,
                    updatedAt = nowMillis,
                ),
            )
        }
    }

    suspend fun setPlantFavorite(
        plantId: String,
        isFavorite: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        plantDao.setPlantFavorite(plantId, isFavorite, nowMillis)
    }

    suspend fun deletePlant(plantId: String, nowMillis: Long = System.currentTimeMillis()) {
        plantDao.softDeletePlant(plantId, nowMillis)
    }
}

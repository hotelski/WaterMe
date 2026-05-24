package com.hotelski.waterme.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plant_photos",
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
        Index(value = ["plant_id", "is_primary"]),
    ],
)
data class PlantPhotoEntity(
    @PrimaryKey
    @ColumnInfo(name = "photo_id")
    val photoId: String,
    @ColumnInfo(name = "plant_id")
    val plantId: String,
    @ColumnInfo(name = "local_uri")
    val localUri: String,
    @ColumnInfo(name = "remote_url")
    val remoteUrl: String? = null,
    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean,
    @ColumnInfo(name = "caption")
    val caption: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

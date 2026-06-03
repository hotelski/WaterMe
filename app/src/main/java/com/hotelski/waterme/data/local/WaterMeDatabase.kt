package com.hotelski.waterme.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hotelski.waterme.data.local.dao.CareHistoryDao
import com.hotelski.waterme.data.local.dao.CareTaskDao
import com.hotelski.waterme.data.local.dao.PlantDao
import com.hotelski.waterme.data.local.dao.PlantPhotoDao
import com.hotelski.waterme.data.local.dao.ReminderDao
import com.hotelski.waterme.data.local.dao.UserDao
import com.hotelski.waterme.data.local.dao.UserSettingsDao
import com.hotelski.waterme.data.local.entity.CareHistoryEntity
import com.hotelski.waterme.data.local.entity.CareTaskEntity
import com.hotelski.waterme.data.local.entity.PlantEntity
import com.hotelski.waterme.data.local.entity.PlantPhotoEntity
import com.hotelski.waterme.data.local.entity.ReminderEntity
import com.hotelski.waterme.data.local.entity.UserEntity
import com.hotelski.waterme.data.local.entity.UserSettingsEntity

@Database(
    entities = [
        UserEntity::class,
        PlantEntity::class,
        PlantPhotoEntity::class,
        ReminderEntity::class,
        CareTaskEntity::class,
        CareHistoryEntity::class,
        UserSettingsEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
@TypeConverters(WaterMeTypeConverters::class)
abstract class WaterMeDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun plantDao(): PlantDao
    abstract fun plantPhotoDao(): PlantPhotoDao
    abstract fun reminderDao(): ReminderDao
    abstract fun careTaskDao(): CareTaskDao
    abstract fun careHistoryDao(): CareHistoryDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        const val DATABASE_NAME = "waterme.db"

        fun build(context: Context): WaterMeDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                WaterMeDatabase::class.java,
                DATABASE_NAME,
            )
                .addMigrations(*WaterMeMigrations.ALL)
                .build()
    }
}

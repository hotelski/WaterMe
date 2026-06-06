package com.hotelski.waterme.data.local

import androidx.room.withTransaction
import com.hotelski.waterme.data.local.entity.BackupSyncProvider
import com.hotelski.waterme.data.local.entity.MeasurementUnits
import com.hotelski.waterme.data.local.entity.NotificationPermissionState
import com.hotelski.waterme.data.local.entity.ThemePreference
import com.hotelski.waterme.data.local.entity.UserEntity
import com.hotelski.waterme.data.local.entity.UserSettingsEntity

object WaterMeSeedData {
    const val LOCAL_USER_ID = "local-user"

    suspend fun seedIfEmpty(database: WaterMeDatabase, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val userDao = database.userDao()
        val userSettingsDao = database.userSettingsDao()
        val plantDao = database.plantDao()
        val hasLocalUser = userDao.getUser(LOCAL_USER_ID) != null
        val hasLocalSettings = userSettingsDao.getSettings(LOCAL_USER_ID) != null
        val hasLegacySeedPlants = legacySeedPlantIds.any { plantDao.getPlant(it) != null }
        if (hasLocalUser && hasLocalSettings && !hasLegacySeedPlants) return false

        database.withTransaction {
            legacySeedPlantIds.forEach { plantId ->
                database.plantDao().deletePlantHard(plantId)
            }
            if (!hasLocalUser) {
                database.userDao().upsertUser(defaultUser(nowMillis))
            }
            if (!hasLocalSettings) {
                database.userSettingsDao().upsertSettings(defaultSettings(nowMillis))
            }
        }
        return hasLegacySeedPlants
    }

    suspend fun ensureLocalUser(database: WaterMeDatabase, nowMillis: Long = System.currentTimeMillis()) {
        database.withTransaction {
            database.userDao().upsertUser(defaultUser(nowMillis))
            database.userSettingsDao().upsertSettings(defaultSettings(nowMillis))
        }
    }

    private fun defaultUser(nowMillis: Long): UserEntity =
        UserEntity(
            userId = LOCAL_USER_ID,
            displayName = "Plant keeper",
            createdAt = nowMillis,
            updatedAt = nowMillis,
        )

    private fun defaultSettings(nowMillis: Long): UserSettingsEntity =
        UserSettingsEntity(
            userId = LOCAL_USER_ID,
            notificationsEnabled = true,
            notificationPermissionState = NotificationPermissionState.NOT_REQUESTED,
            defaultReminderHour = 9,
            defaultReminderMinute = 0,
            darkModePreference = ThemePreference.SYSTEM,
            measurementUnits = MeasurementUnits.METRIC,
            backupSyncEnabled = false,
            backupSyncProvider = BackupSyncProvider.NONE,
            createdAt = nowMillis,
            updatedAt = nowMillis,
        )

    private val legacySeedPlantIds = listOf(
        "plant-monstera",
        "plant-snake",
        "plant-pothos",
    )
}

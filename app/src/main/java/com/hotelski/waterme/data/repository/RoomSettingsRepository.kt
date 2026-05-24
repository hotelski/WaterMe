package com.hotelski.waterme.data.repository

import com.hotelski.waterme.data.local.WaterMeDatabase
import com.hotelski.waterme.data.local.dao.UserSettingsDao
import com.hotelski.waterme.data.local.entity.BackupSyncProvider
import com.hotelski.waterme.data.local.entity.MeasurementUnits
import com.hotelski.waterme.data.local.entity.NotificationPermissionState
import com.hotelski.waterme.data.local.entity.ThemePreference
import com.hotelski.waterme.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

class RoomSettingsRepository(
    database: WaterMeDatabase,
    private val userSettingsDao: UserSettingsDao = database.userSettingsDao(),
) {
    fun observeSettings(userId: String): Flow<UserSettingsEntity?> =
        userSettingsDao.observeSettings(userId)

    suspend fun updateNotificationSettings(
        userId: String,
        enabled: Boolean,
        permissionState: NotificationPermissionState,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        userSettingsDao.updateNotificationSettings(userId, enabled, permissionState, nowMillis)
    }

    suspend fun updateThemePreference(
        userId: String,
        themePreference: ThemePreference,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        userSettingsDao.updateThemePreference(userId, themePreference, nowMillis)
    }

    suspend fun updateMeasurementUnits(
        userId: String,
        measurementUnits: MeasurementUnits,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        userSettingsDao.updateMeasurementUnits(userId, measurementUnits, nowMillis)
    }

    suspend fun updateBackupSync(
        userId: String,
        enabled: Boolean,
        provider: BackupSyncProvider,
        lastBackupAt: Long? = null,
        lastSyncedAt: Long? = null,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        userSettingsDao.updateBackupSync(
            userId = userId,
            enabled = enabled,
            provider = provider,
            lastBackupAt = lastBackupAt,
            lastSyncedAt = lastSyncedAt,
            updatedAt = nowMillis,
        )
    }
}

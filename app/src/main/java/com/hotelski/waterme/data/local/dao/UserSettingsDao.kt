package com.hotelski.waterme.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hotelski.waterme.data.local.entity.BackupSyncProvider
import com.hotelski.waterme.data.local.entity.MeasurementUnits
import com.hotelski.waterme.data.local.entity.NotificationPermissionState
import com.hotelski.waterme.data.local.entity.ThemePreference
import com.hotelski.waterme.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE user_id = :userId")
    fun observeSettings(userId: String): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE user_id = :userId")
    suspend fun getSettings(userId: String): UserSettingsEntity?

    @Upsert
    suspend fun upsertSettings(settings: UserSettingsEntity)

    @Query(
        """
        UPDATE user_settings
        SET notifications_enabled = :enabled,
            notification_permission_state = :permissionState,
            updated_at = :updatedAt
        WHERE user_id = :userId
        """,
    )
    suspend fun updateNotificationSettings(
        userId: String,
        enabled: Boolean,
        permissionState: NotificationPermissionState,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE user_settings
        SET dark_mode_preference = :themePreference, updated_at = :updatedAt
        WHERE user_id = :userId
        """,
    )
    suspend fun updateThemePreference(userId: String, themePreference: ThemePreference, updatedAt: Long)

    @Query(
        """
        UPDATE user_settings
        SET measurement_units = :measurementUnits, updated_at = :updatedAt
        WHERE user_id = :userId
        """,
    )
    suspend fun updateMeasurementUnits(userId: String, measurementUnits: MeasurementUnits, updatedAt: Long)

    @Query(
        """
        UPDATE user_settings
        SET backup_sync_enabled = :enabled,
            backup_sync_provider = :provider,
            last_backup_at = :lastBackupAt,
            last_synced_at = :lastSyncedAt,
            updated_at = :updatedAt
        WHERE user_id = :userId
        """,
    )
    suspend fun updateBackupSync(
        userId: String,
        enabled: Boolean,
        provider: BackupSyncProvider,
        lastBackupAt: Long?,
        lastSyncedAt: Long?,
        updatedAt: Long,
    )
}

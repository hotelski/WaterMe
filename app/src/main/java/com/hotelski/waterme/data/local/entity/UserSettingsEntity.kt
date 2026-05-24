package com.hotelski.waterme.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_settings",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class UserSettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "notifications_enabled")
    val notificationsEnabled: Boolean,
    @ColumnInfo(name = "notification_permission_state")
    val notificationPermissionState: NotificationPermissionState,
    @ColumnInfo(name = "default_reminder_hour")
    val defaultReminderHour: Int,
    @ColumnInfo(name = "default_reminder_minute")
    val defaultReminderMinute: Int,
    @ColumnInfo(name = "dark_mode_preference")
    val darkModePreference: ThemePreference,
    @ColumnInfo(name = "measurement_units")
    val measurementUnits: MeasurementUnits,
    @ColumnInfo(name = "backup_sync_enabled")
    val backupSyncEnabled: Boolean,
    @ColumnInfo(name = "backup_sync_provider")
    val backupSyncProvider: BackupSyncProvider,
    @ColumnInfo(name = "last_backup_at")
    val lastBackupAt: Long? = null,
    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

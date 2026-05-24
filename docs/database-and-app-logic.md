# WaterMe Database And App Logic

This document defines a clean Android-friendly data layer for WaterMe using Kotlin, Jetpack Compose, Room Database, WorkManager, and local notifications.

## Architecture

Recommended package structure:

```text
com.hotelski.waterme
  data
    db
      WaterMeDatabase.kt
      Converters.kt
      dao/
      entity/
      relation/
    repository/
  domain
    model/
    logic/
  notifications
    CareNotificationWorker.kt
    NotificationChannels.kt
    NotificationScheduler.kt
  workers
    TaskGenerationWorker.kt
    TaskReconciliationWorker.kt
  ui
```

Primary rules:

- Room is the local source of truth.
- `Reminder` stores the recurring rule.
- `CareTask` stores each concrete due task generated from a reminder.
- `CareHistory` stores completed, skipped, and manually logged care events.
- `PlantPhoto` stores local `content://` or app-file URIs, not raw image blobs.
- WorkManager schedules notification work and runs reconciliation work.
- Compose screens observe DAO or repository `Flow` values.

## Enums

Store enums as `TEXT` in Room through type converters.

```kotlin
enum class CareType {
    WATERING,
    FERTILIZING,
    REPOTTING,
    MISTING,
    PRUNING,
}

enum class ReminderRecurrenceUnit {
    DAYS,
    WEEKS,
    MONTHS,
}

enum class CareTaskStatus {
    PENDING,
    COMPLETED,
    SKIPPED,
    SNOOZED,
    CANCELED,
}

enum class CareHistoryAction {
    COMPLETED,
    SKIPPED,
    MANUAL_LOG,
    HEALTH_NOTE,
}

enum class NotificationPermissionState {
    NOT_REQUESTED,
    GRANTED,
    DENIED,
}

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class MeasurementUnits {
    METRIC,
    IMPERIAL,
}

enum class BackupSyncProvider {
    NONE,
    GOOGLE_DRIVE,
    CLOUD_SYNC,
}
```

## Database Schema

Use epoch milliseconds as `INTEGER` for timestamps. Use `TEXT` for UUID primary keys.

### `users`

Stores the local user profile. The app can create one local user automatically on first launch.

| Field | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `user_id` | `TEXT` | Primary key | UUID |
| `display_name` | `TEXT` | Not null | Default: `Plant Parent` |
| `email` | `TEXT` | Nullable | For future sync |
| `created_at` | `INTEGER` | Not null | Epoch millis |
| `updated_at` | `INTEGER` | Not null | Epoch millis |
| `deleted_at` | `INTEGER` | Nullable | Soft delete for sync |

### `plants`

Stores each houseplant.

| Field | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `plant_id` | `TEXT` | Primary key | UUID |
| `user_id` | `TEXT` | Foreign key to `users.user_id`, cascade delete | Owner |
| `name` | `TEXT` | Not null | Plant name |
| `plant_type` | `TEXT` | Not null | Example: `Monstera`, `Pothos` |
| `location` | `TEXT` | Not null | Example: `Living room` |
| `notes` | `TEXT` | Not null | Free-form notes |
| `created_at` | `INTEGER` | Not null | Epoch millis |
| `updated_at` | `INTEGER` | Not null | Epoch millis |
| `archived_at` | `INTEGER` | Nullable | Hide without deleting |
| `deleted_at` | `INTEGER` | Nullable | Soft delete |

Indexes:

- `index_plants_user_id` on `user_id`
- `index_plants_name` on `name`

### `plant_photos`

Stores photo references for plants.

| Field | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `photo_id` | `TEXT` | Primary key | UUID |
| `plant_id` | `TEXT` | Foreign key to `plants.plant_id`, cascade delete | Owner plant |
| `local_uri` | `TEXT` | Not null | `content://` or app storage URI |
| `remote_url` | `TEXT` | Nullable | Future backup/sync |
| `is_primary` | `INTEGER` | Not null | Boolean, `0` or `1` |
| `caption` | `TEXT` | Not null | Optional label |
| `created_at` | `INTEGER` | Not null | Epoch millis |
| `updated_at` | `INTEGER` | Not null | Epoch millis |

Indexes:

- `index_plant_photos_plant_id` on `plant_id`
- `index_plant_photos_plant_id_is_primary` on `plant_id, is_primary`

### `reminders`

Stores recurring care rules for a plant.

| Field | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `reminder_id` | `TEXT` | Primary key | UUID |
| `plant_id` | `TEXT` | Foreign key to `plants.plant_id`, cascade delete | Owner plant |
| `care_type` | `TEXT` | Not null | `CareType` |
| `recurrence_unit` | `TEXT` | Not null | Usually `DAYS` |
| `interval_value` | `INTEGER` | Not null | Every N units |
| `preferred_hour` | `INTEGER` | Not null | `0` to `23` |
| `preferred_minute` | `INTEGER` | Not null | `0` to `59` |
| `start_at` | `INTEGER` | Not null | First eligible due time |
| `next_due_at` | `INTEGER` | Not null | Next generated due time |
| `last_completed_at` | `INTEGER` | Nullable | Most recent completion |
| `last_skipped_at` | `INTEGER` | Nullable | Most recent skip |
| `is_enabled` | `INTEGER` | Not null | Boolean |
| `notifications_enabled` | `INTEGER` | Not null | Boolean |
| `created_at` | `INTEGER` | Not null | Epoch millis |
| `updated_at` | `INTEGER` | Not null | Epoch millis |
| `deleted_at` | `INTEGER` | Nullable | Soft delete |

Indexes:

- `index_reminders_plant_id` on `plant_id`
- `index_reminders_plant_id_care_type` on `plant_id, care_type`
- `index_reminders_next_due_at` on `next_due_at`

### `care_tasks`

Stores concrete task instances generated from reminders.

| Field | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `task_id` | `TEXT` | Primary key | UUID |
| `plant_id` | `TEXT` | Foreign key to `plants.plant_id`, cascade delete | Owner plant |
| `reminder_id` | `TEXT` | Foreign key to `reminders.reminder_id`, cascade delete | Source reminder |
| `care_type` | `TEXT` | Not null | Copied from reminder for fast display |
| `scheduled_for` | `INTEGER` | Not null | Original due time |
| `effective_due_at` | `INTEGER` | Not null | `scheduled_for` or snooze time |
| `status` | `TEXT` | Not null | `CareTaskStatus` |
| `snoozed_until` | `INTEGER` | Nullable | Set when snoozed |
| `completed_at` | `INTEGER` | Nullable | Set when completed |
| `skipped_at` | `INTEGER` | Nullable | Set when skipped |
| `notification_work_name` | `TEXT` | Not null | Unique WorkManager name |
| `created_at` | `INTEGER` | Not null | Epoch millis |
| `updated_at` | `INTEGER` | Not null | Epoch millis |

Indexes:

- `index_care_tasks_plant_id` on `plant_id`
- `index_care_tasks_reminder_id` on `reminder_id`
- `index_care_tasks_status_effective_due_at` on `status, effective_due_at`
- Unique index on `reminder_id, scheduled_for`

### `care_history`

Stores completed care, skipped care, manual care logs, and optional note-style events.

| Field | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `history_id` | `TEXT` | Primary key | UUID |
| `plant_id` | `TEXT` | Foreign key to `plants.plant_id`, cascade delete | Owner plant |
| `reminder_id` | `TEXT` | Nullable foreign key to `reminders.reminder_id`, set null on delete | Source reminder |
| `task_id` | `TEXT` | Nullable foreign key to `care_tasks.task_id`, set null on delete | Source task |
| `care_type` | `TEXT` | Not null | Care type |
| `action` | `TEXT` | Not null | `CareHistoryAction` |
| `performed_at` | `INTEGER` | Not null | When the event happened |
| `notes` | `TEXT` | Not null | User notes |
| `created_at` | `INTEGER` | Not null | Epoch millis |

Indexes:

- `index_care_history_plant_id_performed_at` on `plant_id, performed_at`
- `index_care_history_task_id` on `task_id`

### `user_settings`

Stores user-level preferences.

| Field | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `user_id` | `TEXT` | Primary key and foreign key to `users.user_id`, cascade delete | One settings row per user |
| `notifications_enabled` | `INTEGER` | Not null | Global notification toggle |
| `notification_permission_state` | `TEXT` | Not null | Permission state |
| `default_reminder_hour` | `INTEGER` | Not null | Default: `9` |
| `default_reminder_minute` | `INTEGER` | Not null | Default: `0` |
| `dark_mode_preference` | `TEXT` | Not null | `ThemePreference` |
| `measurement_units` | `TEXT` | Not null | `MeasurementUnits` |
| `backup_sync_enabled` | `INTEGER` | Not null | Boolean |
| `backup_sync_provider` | `TEXT` | Not null | `BackupSyncProvider` |
| `last_backup_at` | `INTEGER` | Nullable | Epoch millis |
| `created_at` | `INTEGER` | Not null | Epoch millis |
| `updated_at` | `INTEGER` | Not null | Epoch millis |

## Relationships

```text
User 1 -- 1 UserSettings
User 1 -- N Plant
Plant 1 -- N PlantPhoto
Plant 1 -- N Reminder
Plant 1 -- N CareTask
Plant 1 -- N CareHistory
Reminder 1 -- N CareTask
Reminder 1 -- N CareHistory
CareTask 0..1 -- 0..1 CareHistory
```

Delete behavior:

- Deleting a user cascades plants, photos, reminders, tasks, history, and settings.
- Deleting a plant cascades photos, reminders, tasks, and history.
- Deleting a reminder cascades pending task rows for that reminder.
- History can keep nullable `reminder_id` and `task_id` when references are removed by a future migration.

## Example Room Data Classes

```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    val email: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long?,
)

@Entity(
    tableName = "plants",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("user_id"),
        Index("name"),
    ],
)
data class PlantEntity(
    @PrimaryKey
    @ColumnInfo(name = "plant_id")
    val plantId: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    val name: String,
    @ColumnInfo(name = "plant_type")
    val plantType: String,
    val location: String,
    val notes: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "archived_at")
    val archivedAt: Long?,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long?,
)

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
        Index("plant_id"),
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
    val remoteUrl: String?,
    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean,
    val caption: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["plant_id"],
            childColumns = ["plant_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("plant_id"),
        Index(value = ["plant_id", "care_type"]),
        Index("next_due_at"),
    ],
)
data class ReminderEntity(
    @PrimaryKey
    @ColumnInfo(name = "reminder_id")
    val reminderId: String,
    @ColumnInfo(name = "plant_id")
    val plantId: String,
    @ColumnInfo(name = "care_type")
    val careType: CareType,
    @ColumnInfo(name = "recurrence_unit")
    val recurrenceUnit: ReminderRecurrenceUnit,
    @ColumnInfo(name = "interval_value")
    val intervalValue: Int,
    @ColumnInfo(name = "preferred_hour")
    val preferredHour: Int,
    @ColumnInfo(name = "preferred_minute")
    val preferredMinute: Int,
    @ColumnInfo(name = "start_at")
    val startAt: Long,
    @ColumnInfo(name = "next_due_at")
    val nextDueAt: Long,
    @ColumnInfo(name = "last_completed_at")
    val lastCompletedAt: Long?,
    @ColumnInfo(name = "last_skipped_at")
    val lastSkippedAt: Long?,
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean,
    @ColumnInfo(name = "notifications_enabled")
    val notificationsEnabled: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long?,
)

@Entity(
    tableName = "care_tasks",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["plant_id"],
            childColumns = ["plant_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ReminderEntity::class,
            parentColumns = ["reminder_id"],
            childColumns = ["reminder_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("plant_id"),
        Index("reminder_id"),
        Index(value = ["status", "effective_due_at"]),
        Index(value = ["reminder_id", "scheduled_for"], unique = true),
    ],
)
data class CareTaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "task_id")
    val taskId: String,
    @ColumnInfo(name = "plant_id")
    val plantId: String,
    @ColumnInfo(name = "reminder_id")
    val reminderId: String,
    @ColumnInfo(name = "care_type")
    val careType: CareType,
    @ColumnInfo(name = "scheduled_for")
    val scheduledFor: Long,
    @ColumnInfo(name = "effective_due_at")
    val effectiveDueAt: Long,
    val status: CareTaskStatus,
    @ColumnInfo(name = "snoozed_until")
    val snoozedUntil: Long?,
    @ColumnInfo(name = "completed_at")
    val completedAt: Long?,
    @ColumnInfo(name = "skipped_at")
    val skippedAt: Long?,
    @ColumnInfo(name = "notification_work_name")
    val notificationWorkName: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

@Entity(
    tableName = "care_history",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["plant_id"],
            childColumns = ["plant_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["plant_id", "performed_at"]),
        Index("task_id"),
    ],
)
data class CareHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "history_id")
    val historyId: String,
    @ColumnInfo(name = "plant_id")
    val plantId: String,
    @ColumnInfo(name = "reminder_id")
    val reminderId: String?,
    @ColumnInfo(name = "task_id")
    val taskId: String?,
    @ColumnInfo(name = "care_type")
    val careType: CareType,
    val action: CareHistoryAction,
    @ColumnInfo(name = "performed_at")
    val performedAt: Long,
    val notes: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)

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
    val lastBackupAt: Long?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
```

## Type Converters

```kotlin
class WaterMeConverters {
    @TypeConverter
    fun careTypeToString(value: CareType): String = value.name

    @TypeConverter
    fun stringToCareType(value: String): CareType = CareType.valueOf(value)

    @TypeConverter
    fun recurrenceUnitToString(value: ReminderRecurrenceUnit): String = value.name

    @TypeConverter
    fun stringToRecurrenceUnit(value: String): ReminderRecurrenceUnit =
        ReminderRecurrenceUnit.valueOf(value)

    @TypeConverter
    fun taskStatusToString(value: CareTaskStatus): String = value.name

    @TypeConverter
    fun stringToTaskStatus(value: String): CareTaskStatus = CareTaskStatus.valueOf(value)

    @TypeConverter
    fun historyActionToString(value: CareHistoryAction): String = value.name

    @TypeConverter
    fun stringToHistoryAction(value: String): CareHistoryAction =
        CareHistoryAction.valueOf(value)

    @TypeConverter
    fun themePreferenceToString(value: ThemePreference): String = value.name

    @TypeConverter
    fun stringToThemePreference(value: String): ThemePreference =
        ThemePreference.valueOf(value)
}
```

## Room Database

```kotlin
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
    version = 1,
    exportSchema = true,
)
@TypeConverters(WaterMeConverters::class)
abstract class WaterMeDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun plantDao(): PlantDao
    abstract fun photoDao(): PlantPhotoDao
    abstract fun reminderDao(): ReminderDao
    abstract fun careTaskDao(): CareTaskDao
    abstract fun careHistoryDao(): CareHistoryDao
    abstract fun settingsDao(): UserSettingsDao
}
```

## Example DAO Methods

### User DAO

```kotlin
@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE user_id = :userId AND deleted_at IS NULL")
    fun observeUser(userId: String): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)
}
```

### Plant DAO

```kotlin
@Dao
interface PlantDao {
    @Query(
        """
        SELECT * FROM plants
        WHERE user_id = :userId
          AND archived_at IS NULL
          AND deleted_at IS NULL
        ORDER BY name COLLATE NOCASE
        """,
    )
    fun observePlants(userId: String): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plants WHERE plant_id = :plantId AND deleted_at IS NULL")
    fun observePlant(plantId: String): Flow<PlantEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlant(plant: PlantEntity)

    @Query("UPDATE plants SET deleted_at = :deletedAt, updated_at = :updatedAt WHERE plant_id = :plantId")
    suspend fun softDeletePlant(plantId: String, deletedAt: Long, updatedAt: Long)
}
```

### Photo DAO

```kotlin
@Dao
interface PlantPhotoDao {
    @Query("SELECT * FROM plant_photos WHERE plant_id = :plantId ORDER BY is_primary DESC, created_at DESC")
    fun observePhotos(plantId: String): Flow<List<PlantPhotoEntity>>

    @Query("SELECT * FROM plant_photos WHERE plant_id = :plantId AND is_primary = 1 LIMIT 1")
    fun observePrimaryPhoto(plantId: String): Flow<PlantPhotoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPhoto(photo: PlantPhotoEntity)

    @Query("DELETE FROM plant_photos WHERE photo_id = :photoId")
    suspend fun deletePhoto(photoId: String)
}
```

### Reminder DAO

```kotlin
@Dao
interface ReminderDao {
    @Query(
        """
        SELECT * FROM reminders
        WHERE plant_id = :plantId
          AND deleted_at IS NULL
        ORDER BY care_type
        """,
    )
    fun observeRemindersForPlant(plantId: String): Flow<List<ReminderEntity>>

    @Query(
        """
        SELECT * FROM reminders
        WHERE is_enabled = 1
          AND deleted_at IS NULL
          AND next_due_at <= :until
        ORDER BY next_due_at
        """,
    )
    suspend fun getEnabledRemindersDueBefore(until: Long): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(reminder: ReminderEntity)

    @Query(
        """
        UPDATE reminders
        SET next_due_at = :nextDueAt,
            last_completed_at = :completedAt,
            updated_at = :updatedAt
        WHERE reminder_id = :reminderId
        """,
    )
    suspend fun markReminderCompleted(reminderId: String, completedAt: Long, nextDueAt: Long, updatedAt: Long)

    @Query("UPDATE reminders SET is_enabled = :enabled, updated_at = :updatedAt WHERE reminder_id = :reminderId")
    suspend fun setReminderEnabled(reminderId: String, enabled: Boolean, updatedAt: Long)
}
```

### Care Task DAO

```kotlin
@Dao
interface CareTaskDao {
    @Query(
        """
        SELECT * FROM care_tasks
        WHERE status IN ('PENDING', 'SNOOZED')
          AND effective_due_at BETWEEN :startOfDay AND :endOfDay
        ORDER BY effective_due_at
        """,
    )
    fun observeTodayTasks(startOfDay: Long, endOfDay: Long): Flow<List<CareTaskEntity>>

    @Query(
        """
        SELECT * FROM care_tasks
        WHERE status IN ('PENDING', 'SNOOZED')
          AND effective_due_at BETWEEN :startAt AND :endAt
        ORDER BY effective_due_at
        """,
    )
    fun observeUpcomingTasks(startAt: Long, endAt: Long): Flow<List<CareTaskEntity>>

    @Query("SELECT * FROM care_tasks WHERE task_id = :taskId")
    suspend fun getTask(taskId: String): CareTaskEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTask(task: CareTaskEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTasks(tasks: List<CareTaskEntity>)

    @Query(
        """
        UPDATE care_tasks
        SET status = 'COMPLETED',
            completed_at = :completedAt,
            updated_at = :updatedAt
        WHERE task_id = :taskId
        """,
    )
    suspend fun markCompleted(taskId: String, completedAt: Long, updatedAt: Long)

    @Query(
        """
        UPDATE care_tasks
        SET status = 'SKIPPED',
            skipped_at = :skippedAt,
            updated_at = :updatedAt
        WHERE task_id = :taskId
        """,
    )
    suspend fun markSkipped(taskId: String, skippedAt: Long, updatedAt: Long)

    @Query(
        """
        UPDATE care_tasks
        SET status = 'SNOOZED',
            snoozed_until = :snoozedUntil,
            effective_due_at = :snoozedUntil,
            updated_at = :updatedAt
        WHERE task_id = :taskId
        """,
    )
    suspend fun snoozeTask(taskId: String, snoozedUntil: Long, updatedAt: Long)

    @Query("DELETE FROM care_tasks WHERE reminder_id = :reminderId AND status = 'PENDING'")
    suspend fun deletePendingTasksForReminder(reminderId: String)
}
```

### Care History DAO

```kotlin
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: CareHistoryEntity)

    @Query(
        """
        SELECT * FROM care_history
        WHERE performed_at BETWEEN :startAt AND :endAt
        ORDER BY performed_at DESC
        """,
    )
    fun observeHistoryBetween(startAt: Long, endAt: Long): Flow<List<CareHistoryEntity>>
}
```

### Settings DAO

```kotlin
@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE user_id = :userId")
    fun observeSettings(userId: String): Flow<UserSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: UserSettingsEntity)

    @Query(
        """
        UPDATE user_settings
        SET notification_permission_state = :state,
            updated_at = :updatedAt
        WHERE user_id = :userId
        """,
    )
    suspend fun updateNotificationPermission(
        userId: String,
        state: NotificationPermissionState,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE user_settings
        SET dark_mode_preference = :preference,
            updated_at = :updatedAt
        WHERE user_id = :userId
        """,
    )
    suspend fun updateThemePreference(userId: String, preference: ThemePreference, updatedAt: Long)

    @Query(
        """
        UPDATE user_settings
        SET measurement_units = :units,
            updated_at = :updatedAt
        WHERE user_id = :userId
        """,
    )
    suspend fun updateMeasurementUnits(userId: String, units: MeasurementUnits, updatedAt: Long)

    @Query(
        """
        UPDATE user_settings
        SET backup_sync_enabled = :enabled,
            backup_sync_provider = :provider,
            updated_at = :updatedAt
        WHERE user_id = :userId
        """,
    )
    suspend fun updateBackupSync(
        userId: String,
        enabled: Boolean,
        provider: BackupSyncProvider,
        updatedAt: Long,
    )
}
```

## Business Logic

### Add Plant

```text
function addPlant(input):
  now = clock.now()
  userId = currentUser.id

  create PlantEntity
  save selected PlantPhotoEntity rows

  for each enabled reminder input:
    create ReminderEntity
    create first CareTaskEntity at reminder.startAt

  transaction:
    insert plant
    insert photos
    insert reminders
    insert care tasks

  schedule notifications for created tasks
```

### Edit Plant

```text
function editPlant(plantId, update):
  now = clock.now()

  transaction:
    update plant profile fields
    upsert photo changes
    for each reminder change:
      if interval, time, care type, or enabled state changed:
        cancel pending notification work for future tasks
        delete future pending tasks for that reminder
        update reminder row
        generate next pending task if reminder is enabled

  schedule notifications for new pending tasks
```

### Delete Plant

```text
function deletePlant(plantId):
  tasks = get pending tasks for plant
  for each task:
    WorkManager.cancelUniqueWork(task.notificationWorkName)

  transaction:
    soft delete plant
    hard delete or cascade child rows depending on sync strategy
```

Recommended approach:

- Use soft delete when backup/sync is enabled.
- Use hard delete with cascade when the app is local-only.

### Generate Recurring Tasks

```text
function generateTasksForReminder(reminder, horizonEnd):
  dueAt = reminder.nextDueAt
  tasks = []

  while dueAt <= horizonEnd:
    tasks.add(task for dueAt)
    dueAt = addInterval(dueAt, reminder.recurrenceUnit, reminder.intervalValue)

  insert tasks with OnConflictStrategy.IGNORE
```

Suggested generation horizon:

- Generate 90 days ahead for calendar display.
- Run `TaskReconciliationWorker` daily to keep the next 90 days full.

### Complete Task

```text
function completeTask(taskId, note):
  now = clock.now()
  task = get task
  reminder = get reminder

  nextDueAt = addInterval(
    from = task.scheduledFor,
    unit = reminder.recurrenceUnit,
    value = reminder.intervalValue
  )

  nextTask = create task for nextDueAt
  history = create CareHistory(action = COMPLETED, performedAt = now, notes = note)

  transaction:
    mark task COMPLETED
    insert history
    update reminder lastCompletedAt = now, nextDueAt = nextDueAt
    insert nextTask

  cancel notification work for completed task
  schedule notification for nextTask
```

### Skip Task

```text
function skipTask(taskId, note):
  now = clock.now()
  task = get task
  reminder = get reminder
  nextDueAt = addInterval(task.scheduledFor, reminder.recurrenceUnit, reminder.intervalValue)

  transaction:
    mark task SKIPPED
    insert CareHistory(action = SKIPPED, performedAt = now, notes = note)
    update reminder lastSkippedAt = now, nextDueAt = nextDueAt
    insert next task for nextDueAt

  cancel notification work for skipped task
  schedule notification for next task
```

### Snooze Task

```text
function snoozeTask(taskId, durationMinutes):
  now = clock.now()
  snoozedUntil = now + durationMinutes

  transaction:
    update task status = SNOOZED
    update task snoozedUntil = snoozedUntil
    update task effectiveDueAt = snoozedUntil

  cancel existing notification work
  schedule notification work for snoozedUntil
```

Snoozing does not create care history because care was not performed. It only changes the effective due time.

### Manual Care History

```text
function logManualCare(plantId, careType, note):
  now = clock.now()

  transaction:
    insert CareHistory(
      plantId = plantId,
      reminderId = null,
      taskId = null,
      careType = careType,
      action = MANUAL_LOG,
      performedAt = now,
      notes = note
    )
```

### Today's Tasks

```text
function observeTodayTasks():
  start = startOfLocalDay(clock.today())
  end = endOfLocalDay(clock.today())

  return careTaskDao.observeTodayTasks(start, end)
    .map { join with plant and primary photo for UI }
```

Include tasks with:

- `PENDING`
- `SNOOZED` if `effective_due_at` falls today

Exclude tasks with:

- `COMPLETED`
- `SKIPPED`
- `CANCELED`

### Calendar Upcoming Tasks

```text
function observeCalendarTasks(startDate, endDate):
  start = startOfLocalDay(startDate)
  end = endOfLocalDay(endDate)

  return careTaskDao.observeUpcomingTasks(start, end)
    .map { group by local date }
```

Recommended default window:

- Start: today
- End: today plus 90 days

## Notification Logic

### Notification Requirements

Use:

- `POST_NOTIFICATIONS` runtime permission on Android 13 and newer.
- `NotificationChannel` on Android 8 and newer.
- WorkManager for durable scheduled notification work.

Notification channel:

- ID: `plant_care_reminders`
- Name: `Plant care reminders`
- Description: `Notifications for watering, fertilizing, repotting, misting, and pruning.`

Notification title:

```text
{plantName} needs {careTypeLabel}
```

Notification body:

```text
Open WaterMe to log care and keep your plant on track.
```

### Scheduling Flow

```text
function scheduleNotificationForTask(task):
  settings = settingsDao.getSettings(currentUserId)
  reminder = reminderDao.getReminder(task.reminderId)

  if settings.notificationsEnabled is false:
    return

  if settings.notificationPermissionState != GRANTED:
    return

  if reminder.notificationsEnabled is false:
    return

  if task.status not in PENDING, SNOOZED:
    return

  triggerAt = task.effectiveDueAt
  delay = max(0, triggerAt - clock.now())

  request = OneTimeWorkRequestBuilder<CareNotificationWorker>()
    .setInitialDelay(delay)
    .setInputData(taskId = task.taskId)
    .build()

  WorkManager.enqueueUniqueWork(
    task.notificationWorkName,
    ExistingWorkPolicy.REPLACE,
    request
  )
```

### Notification Worker Flow

```text
CareNotificationWorker.doWork():
  taskId = inputData.taskId
  task = careTaskDao.getTask(taskId)

  if task is null:
    return success

  if task.status not in PENDING, SNOOZED:
    return success

  plant = plantDao.getPlant(task.plantId)
  reminder = reminderDao.getReminder(task.reminderId)
  settings = settingsDao.getSettings(plant.userId)

  if settings.notificationsEnabled is false:
    return success

  if settings.notificationPermissionState != GRANTED:
    return success

  if reminder.notificationsEnabled is false:
    return success

  show local notification:
    title = "{plant.name} needs {task.careType.label}"
    body = "Open WaterMe to log care and keep your plant on track."
    deepLink = waterme://plants/{plant.plantId}/tasks/{task.taskId}

  return success
```

### Rescheduling Flow

Reschedule notifications when:

- A plant is added.
- A reminder is created or edited.
- A task is completed.
- A task is skipped.
- A task is snoozed.
- Notifications are enabled after being disabled.
- Notification permission changes to granted.
- App starts after an update.
- `TaskReconciliationWorker` runs.

Cancel notifications when:

- A task is completed.
- A task is skipped.
- A task is canceled.
- A plant is deleted.
- A reminder is disabled or deleted.
- Global notifications are disabled.

## User Settings Logic

### First Launch Defaults

```text
create default UserSettings:
  notificationsEnabled = true
  notificationPermissionState = NOT_REQUESTED
  defaultReminderHour = 9
  defaultReminderMinute = 0
  darkModePreference = SYSTEM
  measurementUnits = METRIC
  backupSyncEnabled = false
  backupSyncProvider = NONE
```

### Notification Permission

```text
function onNotificationPermissionResult(isGranted):
  state = if isGranted then GRANTED else DENIED
  settingsDao.updateNotificationPermission(userId, state, now)

  if isGranted and settings.notificationsEnabled:
    schedule all pending task notifications
  else:
    cancel all pending notification work
```

### Dark Mode Preference

```text
function updateDarkModePreference(preference):
  settingsDao.updateThemePreference(userId, preference, now)

Compose theme observes settings:
  SYSTEM -> follow system theme
  LIGHT -> force light theme
  DARK -> force dark theme
```

### Measurement Units

```text
function updateMeasurementUnits(units):
  settingsDao.updateMeasurementUnits(userId, units, now)

Use units for:
  pot size
  plant height
  water amount
  optional future care metrics
```

### Backup And Sync

```text
function updateBackupSync(enabled, provider):
  settingsDao.updateBackupSync(userId, enabled, provider, now)

  if enabled:
    enqueue BackupSyncWorker
  else:
    cancel BackupSyncWorker
```

Sync-friendly rules:

- Keep stable UUID primary keys.
- Keep `created_at`, `updated_at`, and `deleted_at`.
- Prefer soft delete while sync is enabled.
- Store `remote_url` for photos after upload.
- Resolve conflicts by latest `updated_at`, except care history should append rather than overwrite.

## Repository Layer API

```kotlin
interface PlantRepository {
    fun observePlants(userId: String): Flow<List<PlantListItem>>
    fun observePlantDetails(plantId: String): Flow<PlantDetails>
    suspend fun addPlant(input: AddPlantInput): String
    suspend fun updatePlant(plantId: String, input: EditPlantInput)
    suspend fun deletePlant(plantId: String)
}

interface CareRepository {
    fun observeTodayTasks(): Flow<List<CareTaskListItem>>
    fun observeCalendarTasks(startAt: Long, endAt: Long): Flow<List<CalendarCareTask>>
    suspend fun completeTask(taskId: String, note: String = "")
    suspend fun skipTask(taskId: String, note: String = "")
    suspend fun snoozeTask(taskId: String, durationMinutes: Int)
    suspend fun logManualCare(plantId: String, careType: CareType, note: String)
}

interface SettingsRepository {
    fun observeSettings(userId: String): Flow<UserSettingsEntity>
    suspend fun updateNotificationPermission(state: NotificationPermissionState)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setThemePreference(preference: ThemePreference)
    suspend fun setMeasurementUnits(units: MeasurementUnits)
    suspend fun setBackupSync(enabled: Boolean, provider: BackupSyncProvider)
}
```

## Screen Data Queries

### Today Screen

Use:

- `CareTaskDao.observeTodayTasks(startOfDay, endOfDay)`
- Join each task with:
  - `PlantEntity`
  - primary `PlantPhotoEntity`

Display:

- Plant name
- Plant location
- Care type
- Effective due time
- Snooze state if status is `SNOOZED`

### My Plants Screen

Use:

- `PlantDao.observePlants(userId)`
- primary photo per plant
- count of due tasks per plant
- next pending task per plant

### Plant Details Screen

Use:

- `PlantDao.observePlant(plantId)`
- `PlantPhotoDao.observePhotos(plantId)`
- `ReminderDao.observeRemindersForPlant(plantId)`
- `CareHistoryDao.observeHistoryForPlant(plantId)`
- pending task count grouped by reminder

### Calendar Screen

Use:

- `CareTaskDao.observeUpcomingTasks(startAt, endAt)`
- Join with plant and primary photo.
- Group rows by local date in the repository or ViewModel.

### Settings Screen

Use:

- `UserSettingsDao.observeSettings(userId)`
- counts:
  - plant count
  - active reminder count
  - pending task count
  - care history count

## Edge Cases

- If notification permission is denied, tasks still appear in Today and Calendar.
- If global notifications are disabled, keep reminders active but cancel WorkManager notification work.
- If a reminder is disabled, cancel future pending tasks for that reminder or mark them `CANCELED`.
- If a plant is archived, hide it from normal lists and suppress notifications.
- If a task is overdue, keep it visible until completed, skipped, snoozed, or canceled.
- If the user completes an overdue task, schedule the next task from the original scheduled date to preserve cadence. If the product wants adaptive scheduling, schedule from completion time instead.
- If the user changes timezone, run reconciliation and update notification delays from stored epoch timestamps.
- If backup/sync is enabled, avoid hard deletes until the deletion is synced.

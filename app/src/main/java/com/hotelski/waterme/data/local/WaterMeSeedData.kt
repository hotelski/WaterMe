package com.hotelski.waterme.data.local

import androidx.room.withTransaction
import com.hotelski.waterme.data.local.entity.BackupSyncProvider
import com.hotelski.waterme.data.local.entity.CareTaskEntity
import com.hotelski.waterme.data.local.entity.HistoryAction
import com.hotelski.waterme.data.local.entity.MeasurementUnits
import com.hotelski.waterme.data.local.entity.NotificationPermissionState
import com.hotelski.waterme.data.local.entity.PlantEntity
import com.hotelski.waterme.data.local.entity.ReminderEntity
import com.hotelski.waterme.data.local.entity.TaskStatus
import com.hotelski.waterme.data.local.entity.ThemePreference
import com.hotelski.waterme.data.local.entity.UserEntity
import com.hotelski.waterme.data.local.entity.UserSettingsEntity
import com.hotelski.waterme.data.local.entity.CareHistoryEntity
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.model.PlantEnvironment
import java.util.UUID

object WaterMeSeedData {
    const val LOCAL_USER_ID = "local-user"

    suspend fun seedIfEmpty(database: WaterMeDatabase, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val plantDao = database.plantDao()
        if (plantDao.countActivePlantsForUser(LOCAL_USER_ID) > 0) return false

        database.withTransaction {
            database.userDao().upsertUser(
                UserEntity(
                    userId = LOCAL_USER_ID,
                    displayName = "Plant keeper",
                    createdAt = nowMillis,
                    updatedAt = nowMillis,
                ),
            )
            database.userSettingsDao().upsertSettings(defaultSettings(nowMillis))

            val monstera = plant("plant-monstera", "Monstera Deliciosa", "Tropical foliage", "Living room", nowMillis)
            val snakePlant = plant("plant-snake", "Snake Plant", "Sansevieria", "Bedroom", nowMillis)
            val pothos = plant("plant-pothos", "Golden Pothos", "Trailing vine", "Kitchen shelf", nowMillis)
            listOf(monstera, snakePlant, pothos).forEach { database.plantDao().upsertPlant(it) }

            val reminders = listOf(
                reminder("reminder-monstera-water", monstera.plantId, CareType.WATERING, 5, nowMillis, nowMillis),
                reminder("reminder-monstera-mist", monstera.plantId, CareType.MISTING, 3, nowMillis + DAY_MILLIS, nowMillis),
                reminder("reminder-snake-water", snakePlant.plantId, CareType.WATERING, 14, nowMillis + 2 * DAY_MILLIS, nowMillis),
                reminder("reminder-pothos-water", pothos.plantId, CareType.WATERING, 6, nowMillis, nowMillis),
                reminder("reminder-pothos-prune", pothos.plantId, CareType.PRUNING, 30, nowMillis + 4 * DAY_MILLIS, nowMillis),
            )
            reminders.forEach { database.reminderDao().upsertReminder(it) }
            database.careTaskDao().upsertTasks(reminders.map { it.toOpenTask(nowMillis) })

            database.careHistoryDao().insertHistory(
                history(monstera.plantId, CareType.PRUNING, "Trimmed one yellowing lower leaf.", nowMillis - 19 * DAY_MILLIS),
            )
            database.careHistoryDao().insertHistory(
                history(pothos.plantId, CareType.FERTILIZING, "Half-strength liquid fertilizer.", nowMillis - 21 * DAY_MILLIS),
            )
            database.careHistoryDao().insertHistory(
                history(monstera.plantId, CareType.WATERING, "Deep watered and wiped leaves.", nowMillis - 2 * DAY_MILLIS),
            )
        }
        return true
    }

    suspend fun ensureLocalUser(database: WaterMeDatabase, nowMillis: Long = System.currentTimeMillis()) {
        database.withTransaction {
            database.userDao().upsertUser(
                UserEntity(
                    userId = LOCAL_USER_ID,
                    displayName = "Plant keeper",
                    createdAt = nowMillis,
                    updatedAt = nowMillis,
                ),
            )
            database.userSettingsDao().upsertSettings(defaultSettings(nowMillis))
        }
    }

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

    private fun plant(
        plantId: String,
        name: String,
        plantType: String,
        location: String,
        nowMillis: Long,
    ): PlantEntity =
        PlantEntity(
            plantId = plantId,
            userId = LOCAL_USER_ID,
            name = name,
            plantType = plantType,
            location = location,
            environment = PlantEnvironment.INDOOR,
            notes = "Seed plant for first-run previews and development builds.",
            createdAt = nowMillis,
            updatedAt = nowMillis,
        )

    private fun reminder(
        reminderId: String,
        plantId: String,
        careType: CareType,
        frequencyDays: Int,
        nextDueAt: Long,
        nowMillis: Long,
    ): ReminderEntity =
        ReminderEntity(
            reminderId = reminderId,
            plantId = plantId,
            careType = careType,
            frequencyDays = frequencyDays,
            preferredHour = 9,
            preferredMinute = 0,
            nextDueAt = nextDueAt,
            createdAt = nowMillis,
            updatedAt = nowMillis,
        )

    private fun ReminderEntity.toOpenTask(nowMillis: Long): CareTaskEntity =
        CareTaskEntity(
            taskId = UUID.randomUUID().toString(),
            plantId = plantId,
            reminderId = reminderId,
            careType = careType,
            scheduledFor = nextDueAt,
            effectiveDueAt = nextDueAt,
            status = TaskStatus.PENDING,
            createdAt = nowMillis,
            updatedAt = nowMillis,
        )

    private fun history(
        plantId: String,
        careType: CareType,
        notes: String,
        performedAt: Long,
    ): CareHistoryEntity =
        CareHistoryEntity(
            historyId = UUID.randomUUID().toString(),
            plantId = plantId,
            careType = careType,
            action = HistoryAction.MANUAL_LOG,
            performedAt = performedAt,
            notes = notes,
            createdAt = performedAt,
        )

    private const val DAY_MILLIS = 86_400_000L
}

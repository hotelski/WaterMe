package com.hotelski.waterme.data.local

import androidx.room.TypeConverter
import com.hotelski.waterme.data.local.entity.BackupSyncProvider
import com.hotelski.waterme.data.local.entity.HistoryAction
import com.hotelski.waterme.data.local.entity.MeasurementUnits
import com.hotelski.waterme.data.local.entity.NotificationPermissionState
import com.hotelski.waterme.data.local.entity.TaskStatus
import com.hotelski.waterme.data.local.entity.ThemePreference
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.model.HealthMood
import com.hotelski.waterme.model.PlantEnvironment

class WaterMeTypeConverters {
    @TypeConverter
    fun careTypeToString(value: CareType): String = value.name

    @TypeConverter
    fun stringToCareType(value: String): CareType = CareType.valueOf(value)

    @TypeConverter
    fun healthMoodToString(value: HealthMood?): String? = value?.name

    @TypeConverter
    fun stringToHealthMood(value: String?): HealthMood? = value?.let { HealthMood.valueOf(it) }

    @TypeConverter
    fun plantEnvironmentToString(value: PlantEnvironment): String = value.name

    @TypeConverter
    fun stringToPlantEnvironment(value: String): PlantEnvironment = PlantEnvironment.valueOf(value)

    @TypeConverter
    fun taskStatusToString(value: TaskStatus): String = value.name

    @TypeConverter
    fun stringToTaskStatus(value: String): TaskStatus = TaskStatus.valueOf(value)

    @TypeConverter
    fun historyActionToString(value: HistoryAction): String = value.name

    @TypeConverter
    fun stringToHistoryAction(value: String): HistoryAction = HistoryAction.valueOf(value)

    @TypeConverter
    fun notificationPermissionStateToString(value: NotificationPermissionState): String = value.name

    @TypeConverter
    fun stringToNotificationPermissionState(value: String): NotificationPermissionState =
        NotificationPermissionState.valueOf(value)

    @TypeConverter
    fun themePreferenceToString(value: ThemePreference): String = value.name

    @TypeConverter
    fun stringToThemePreference(value: String): ThemePreference = ThemePreference.valueOf(value)

    @TypeConverter
    fun measurementUnitsToString(value: MeasurementUnits): String = value.name

    @TypeConverter
    fun stringToMeasurementUnits(value: String): MeasurementUnits = MeasurementUnits.valueOf(value)

    @TypeConverter
    fun backupSyncProviderToString(value: BackupSyncProvider): String = value.name

    @TypeConverter
    fun stringToBackupSyncProvider(value: String): BackupSyncProvider = BackupSyncProvider.valueOf(value)
}

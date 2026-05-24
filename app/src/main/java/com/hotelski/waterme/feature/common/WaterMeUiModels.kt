package com.hotelski.waterme.feature.common

import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.model.HealthMood

data class PlantCardUiModel(
    val id: String,
    val name: String,
    val plantType: String,
    val location: String,
    val photoUri: String? = null,
    val dueTaskCount: Int = 0,
    val nextCareLabel: String? = null,
    val scheduleSummary: String = "No reminder set",
    val careLogCount: Int = 0,
    val recentCareLogs: List<CareHistoryUiModel> = emptyList(),
    val notes: String = "",
)

data class PlantDetailsUiModel(
    val id: String,
    val name: String,
    val plantType: String,
    val location: String,
    val notes: String,
    val primaryPhotoUri: String? = null,
    val photoCount: Int = 0,
    val reminderCount: Int = 0,
    val careHistoryCount: Int = 0,
)

data class CareTaskUiModel(
    val id: String,
    val plantId: String,
    val plantName: String,
    val plantLocation: String,
    val careType: CareType,
    val dueLabel: String,
    val isOverdue: Boolean = false,
    val isSnoozed: Boolean = false,
)

data class ReminderUiModel(
    val id: String,
    val careType: CareType,
    val frequencyLabel: String,
    val nextDueLabel: String,
    val enabled: Boolean = true,
)

data class ReminderDraftUiModel(
    val careType: CareType,
    val enabled: Boolean,
    val everyDays: String,
    val startsInDays: String,
)

data class CareHistoryUiModel(
    val id: String,
    val plantId: String = "",
    val plantName: String,
    val careType: CareType,
    val actionLabel: String,
    val dateLabel: String,
    val notes: String,
    val performedAtMillis: Long = 0L,
    val photoUri: String? = null,
)

data class HealthNoteUiModel(
    val id: String,
    val plantName: String,
    val mood: HealthMood,
    val dateLabel: String,
    val note: String,
)

data class CalendarDayUiModel(
    val dateLabel: String,
    val isToday: Boolean = false,
    val tasks: List<CareTaskUiModel> = emptyList(),
)

fun CareType.label(): String =
    when (this) {
        CareType.WATERING -> "Watering"
        CareType.FERTILIZING -> "Fertilizing"
        CareType.REPOTTING -> "Repotting"
        CareType.MISTING -> "Misting"
        CareType.PRUNING -> "Pruning"
    }

fun CareType.shortLabel(): String =
    when (this) {
        CareType.WATERING -> "Water"
        CareType.FERTILIZING -> "Feed"
        CareType.REPOTTING -> "Repot"
        CareType.MISTING -> "Mist"
        CareType.PRUNING -> "Prune"
    }

fun HealthMood.label(): String =
    when (this) {
        HealthMood.ATTENTION -> "Needs attention"
        HealthMood.HEALTHY -> "Healthy"
        HealthMood.GROWTH -> "New growth"
    }

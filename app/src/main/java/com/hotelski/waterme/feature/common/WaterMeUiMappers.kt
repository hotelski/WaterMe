package com.hotelski.waterme.feature.common

import com.hotelski.waterme.data.local.entity.CareHistoryEntity
import com.hotelski.waterme.data.local.entity.CareTaskEntity
import com.hotelski.waterme.data.local.entity.HistoryAction
import com.hotelski.waterme.data.local.entity.ReminderEntity
import com.hotelski.waterme.data.local.entity.TaskStatus
import com.hotelski.waterme.data.local.model.CareHistoryWithPlant
import com.hotelski.waterme.data.local.model.CareTaskWithPlant
import com.hotelski.waterme.data.local.model.PlantWithDetails
import com.hotelski.waterme.model.HealthMood
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun PlantWithDetails.toPlantCardUiModel(
    dueTaskCount: Int = 0,
    clock: Clock = Clock.systemDefaultZone(),
): PlantCardUiModel =
    PlantCardUiModel(
        id = plant.plantId,
        name = plant.name,
        plantType = plant.plantType,
        location = plant.location,
        photoUri = primaryPhotoUri(),
        dueTaskCount = dueTaskCount,
        nextCareLabel = reminders
            .filter { it.isEnabled && it.deletedAt == null }
            .minByOrNull { it.nextDueAt }
            ?.let { "Next: ${it.careType.shortLabel()} ${it.nextDueAt.toDueDateLabel(clock)}" },
        notes = plant.notes,
    )

fun PlantWithDetails.toPlantDetailsUiModel(): PlantDetailsUiModel =
    PlantDetailsUiModel(
        id = plant.plantId,
        name = plant.name,
        plantType = plant.plantType,
        location = plant.location,
        notes = plant.notes,
        primaryPhotoUri = primaryPhotoUri(),
        photoCount = photos.size,
        reminderCount = reminders.count { it.deletedAt == null },
        careHistoryCount = careHistory.count { it.action != HistoryAction.HEALTH_NOTE },
    )

fun CareTaskWithPlant.toCareTaskUiModel(clock: Clock = Clock.systemDefaultZone()): CareTaskUiModel =
    CareTaskUiModel(
        id = taskId,
        plantId = plantId,
        plantName = plantName,
        plantLocation = location,
        careType = careType,
        dueLabel = effectiveDueAt.toDueLabel(clock),
        isOverdue = effectiveDueAt.toLocalDate(clock.zone).isBefore(LocalDate.now(clock)),
        isSnoozed = status == TaskStatus.SNOOZED || snoozedUntil != null,
    )

fun CareTaskEntity.toCareTaskUiModel(
    plant: PlantDetailsUiModel,
    clock: Clock = Clock.systemDefaultZone(),
): CareTaskUiModel =
    CareTaskUiModel(
        id = taskId,
        plantId = plantId,
        plantName = plant.name,
        plantLocation = plant.location,
        careType = careType,
        dueLabel = effectiveDueAt.toDueLabel(clock),
        isOverdue = effectiveDueAt.toLocalDate(clock.zone).isBefore(LocalDate.now(clock)),
        isSnoozed = status == TaskStatus.SNOOZED || snoozedUntil != null,
    )

fun ReminderEntity.toReminderUiModel(clock: Clock = Clock.systemDefaultZone()): ReminderUiModel =
    ReminderUiModel(
        id = reminderId,
        careType = careType,
        frequencyLabel = "Every $frequencyDays day${if (frequencyDays == 1) "" else "s"}",
        nextDueLabel = "Next: ${nextDueAt.toDueDateLabel(clock)}",
        enabled = isEnabled,
    )

fun CareHistoryEntity.toCareHistoryUiModel(
    plantName: String,
    clock: Clock = Clock.systemDefaultZone(),
): CareHistoryUiModel =
    CareHistoryUiModel(
        id = historyId,
        plantId = plantId,
        plantName = plantName,
        careType = careType,
        actionLabel = action.displayLabel(),
        dateLabel = performedAt.toDueDateLabel(clock),
        notes = notes.orEmpty(),
        performedAtMillis = performedAt,
        photoUri = photoUri,
    )

fun CareHistoryWithPlant.toCareHistoryUiModel(clock: Clock = Clock.systemDefaultZone()): CareHistoryUiModel =
    CareHistoryUiModel(
        id = historyId,
        plantId = plantId,
        plantName = plantName,
        careType = careType,
        actionLabel = action.displayLabel(),
        dateLabel = performedAt.toDueDateLabel(clock),
        notes = notes.orEmpty(),
        performedAtMillis = performedAt,
        photoUri = photoUri,
    )

fun CareHistoryEntity.toHealthNoteUiModel(
    plantName: String,
    clock: Clock = Clock.systemDefaultZone(),
): HealthNoteUiModel =
    HealthNoteUiModel(
        id = historyId,
        plantName = plantName,
        mood = healthMood ?: HealthMood.ATTENTION,
        dateLabel = performedAt.toDueDateLabel(clock),
        note = notes.orEmpty(),
    )

fun CareHistoryWithPlant.toHealthNoteUiModel(clock: Clock = Clock.systemDefaultZone()): HealthNoteUiModel =
    HealthNoteUiModel(
        id = historyId,
        plantName = plantName,
        mood = healthMood ?: HealthMood.ATTENTION,
        dateLabel = performedAt.toDueDateLabel(clock),
        note = notes.orEmpty(),
    )

fun calendarDaysFromTasks(
    tasks: List<CareTaskUiModel>,
    taskDates: Map<String, Long>,
    clock: Clock = Clock.systemDefaultZone(),
): List<CalendarDayUiModel> {
    val today = LocalDate.now(clock)
    return tasks
        .groupBy { task -> taskDates.getValue(task.id).toLocalDate(clock.zone) }
        .toSortedMap()
        .map { (date, dayTasks) ->
            CalendarDayUiModel(
                dateLabel = date.toCalendarDayLabel(today),
                isToday = date == today,
                tasks = dayTasks,
            )
        }
}

fun startOfTodayMillis(clock: Clock = Clock.systemDefaultZone()): Long =
    LocalDate.now(clock).atStartOfDay(clock.zone).toInstant().toEpochMilli()

fun endOfTodayMillis(clock: Clock = Clock.systemDefaultZone()): Long =
    LocalDate.now(clock).plusDays(1).atStartOfDay(clock.zone).toInstant().toEpochMilli() - 1

fun daysFromTodayMillis(days: Long, clock: Clock = Clock.systemDefaultZone()): Long =
    LocalDate.now(clock).plusDays(days).atStartOfDay(clock.zone).toInstant().toEpochMilli()

fun calendarWindowEndMillis(daysAhead: Long, clock: Clock = Clock.systemDefaultZone()): Long =
    LocalDate.now(clock).plusDays(daysAhead).plusDays(1).atStartOfDay(clock.zone).toInstant().toEpochMilli()

private fun PlantWithDetails.primaryPhotoUri(): String? =
    photos.firstOrNull { it.isPrimary }?.localUri ?: photos.firstOrNull()?.localUri

private fun Long.toLocalDate(zoneId: ZoneId): LocalDate =
    Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

private fun Long.toDueLabel(clock: Clock): String {
    val today = LocalDate.now(clock)
    val date = toLocalDate(clock.zone)
    return when {
        date.isBefore(today) -> "Overdue"
        date == today -> "Due today"
        date == today.plusDays(1) -> "Tomorrow"
        else -> date.format(shortDateFormatter)
    }
}

private fun Long.toDueDateLabel(clock: Clock): String {
    val today = LocalDate.now(clock)
    val date = toLocalDate(clock.zone)
    return when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> date.format(shortDateFormatter)
    }
}

private fun LocalDate.toCalendarDayLabel(today: LocalDate): String =
    when (this) {
        today -> "Today, ${format(shortDateFormatter)}"
        today.plusDays(1) -> "Tomorrow, ${format(shortDateFormatter)}"
        else -> format(calendarDateFormatter)
    }

private fun HistoryAction.displayLabel(): String =
    when (this) {
        HistoryAction.COMPLETED -> "Completed"
        HistoryAction.SKIPPED -> "Skipped"
        HistoryAction.SNOOZED -> "Snoozed"
        HistoryAction.MANUAL_LOG -> "Manual log"
        HistoryAction.HEALTH_NOTE -> "Health note"
    }

private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d")
private val calendarDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

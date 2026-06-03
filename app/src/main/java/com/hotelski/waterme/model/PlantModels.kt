package com.hotelski.waterme.model

import java.time.LocalDate
import java.util.UUID

enum class CareType(
    val label: String,
    val shortLabel: String,
    val defaultFrequencyDays: Int,
) {
    WATERING("Watering", "Water", 4),
    FERTILIZING("Fertilizing", "Feed", 30),
    REPOTTING("Repotting", "Repot", 180),
    MISTING("Misting", "Mist", 3),
    PRUNING("Pruning", "Prune", 45),
}

enum class HealthMood(val label: String) {
    ATTENTION("Needs attention"),
    HEALTHY("Healthy"),
    GROWTH("New growth"),
}

enum class PlantEnvironment(val label: String) {
    INDOOR("Indoor"),
    OUTDOOR("Outdoor"),
}

data class CareReminder(
    val id: String = UUID.randomUUID().toString(),
    val type: CareType,
    val frequencyDays: Int,
    val nextDueDate: LocalDate,
    val enabled: Boolean = true,
)

data class CareHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val type: CareType,
    val date: LocalDate,
    val note: String,
)

data class HealthNote(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val mood: HealthMood,
    val note: String,
)

data class Plant(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String,
    val location: String,
    val notes: String,
    val photoUri: String? = null,
    val reminders: List<CareReminder> = emptyList(),
    val careHistory: List<CareHistoryEntry> = emptyList(),
    val healthNotes: List<HealthNote> = emptyList(),
) {
    fun dueReminders(today: LocalDate = LocalDate.now()): List<CareReminder> =
        reminders.filter { it.enabled && !it.nextDueDate.isAfter(today) }

    fun upcomingReminders(today: LocalDate = LocalDate.now()): List<CareReminder> =
        reminders.filter { it.enabled && it.nextDueDate.isAfter(today) }
}

package com.hotelski.waterme.data

import android.content.Context
import com.hotelski.waterme.model.CareHistoryEntry
import com.hotelski.waterme.model.CareReminder
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.model.HealthMood
import com.hotelski.waterme.model.HealthNote
import com.hotelski.waterme.model.Plant
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

class PlantRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isOnboardingComplete(): Boolean =
        preferences.getBoolean(KEY_ONBOARDING_COMPLETE, false)

    fun setOnboardingComplete(isComplete: Boolean) {
        preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETE, isComplete).apply()
    }

    fun loadPlants(): List<Plant> {
        val rawPlants = preferences.getString(KEY_PLANTS, null) ?: return samplePlants()
        return runCatching { parsePlants(JSONArray(rawPlants)) }.getOrElse { samplePlants() }
    }

    fun savePlants(plants: List<Plant>) {
        preferences.edit().putString(KEY_PLANTS, plants.toPlantJson().toString()).apply()
    }

    private fun parsePlants(array: JSONArray): List<Plant> =
        List(array.length()) { index -> array.getJSONObject(index).toPlant() }

    private fun JSONObject.toPlant(): Plant =
        Plant(
            id = getString("id"),
            name = getString("name"),
            type = optString("type"),
            location = optString("location"),
            notes = optString("notes"),
            photoUri = optString("photoUri").ifBlank { null },
            reminders = optJSONArray("reminders").toReminderList(),
            careHistory = optJSONArray("careHistory").toCareHistoryList(),
            healthNotes = optJSONArray("healthNotes").toHealthNoteList(),
        )

    private fun JSONArray?.toReminderList(): List<CareReminder> {
        if (this == null) return emptyList()
        return List(length()) { index ->
            val item = getJSONObject(index)
            CareReminder(
                id = item.getString("id"),
                type = CareType.valueOf(item.getString("type")),
                frequencyDays = item.getInt("frequencyDays"),
                nextDueDate = LocalDate.parse(item.getString("nextDueDate")),
                enabled = item.optBoolean("enabled", true),
            )
        }
    }

    private fun JSONArray?.toCareHistoryList(): List<CareHistoryEntry> {
        if (this == null) return emptyList()
        return List(length()) { index ->
            val item = getJSONObject(index)
            CareHistoryEntry(
                id = item.getString("id"),
                type = CareType.valueOf(item.getString("type")),
                date = LocalDate.parse(item.getString("date")),
                note = item.optString("note"),
            )
        }
    }

    private fun JSONArray?.toHealthNoteList(): List<HealthNote> {
        if (this == null) return emptyList()
        return List(length()) { index ->
            val item = getJSONObject(index)
            HealthNote(
                id = item.getString("id"),
                date = LocalDate.parse(item.getString("date")),
                mood = HealthMood.valueOf(item.getString("mood")),
                note = item.optString("note"),
            )
        }
    }

    private fun List<Plant>.toPlantJson(): JSONArray =
        JSONArray().also { array -> forEach { array.put(it.toJson()) } }

    private fun Plant.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("name", name)
            .put("type", type)
            .put("location", location)
            .put("notes", notes)
            .put("photoUri", photoUri.orEmpty())
            .put("reminders", reminders.toReminderJson())
            .put("careHistory", careHistory.toCareHistoryJson())
            .put("healthNotes", healthNotes.toHealthNoteJson())

    private fun List<CareReminder>.toReminderJson(): JSONArray =
        JSONArray().also { array ->
            forEach { reminder ->
                array.put(
                    JSONObject()
                        .put("id", reminder.id)
                        .put("type", reminder.type.name)
                        .put("frequencyDays", reminder.frequencyDays)
                        .put("nextDueDate", reminder.nextDueDate.toString())
                        .put("enabled", reminder.enabled),
                )
            }
        }

    private fun List<CareHistoryEntry>.toCareHistoryJson(): JSONArray =
        JSONArray().also { array ->
            forEach { entry ->
                array.put(
                    JSONObject()
                        .put("id", entry.id)
                        .put("type", entry.type.name)
                        .put("date", entry.date.toString())
                        .put("note", entry.note),
                )
            }
        }

    private fun List<HealthNote>.toHealthNoteJson(): JSONArray =
        JSONArray().also { array ->
            forEach { note ->
                array.put(
                    JSONObject()
                        .put("id", note.id)
                        .put("date", note.date.toString())
                        .put("mood", note.mood.name)
                        .put("note", note.note),
                )
            }
        }

    private fun samplePlants(): List<Plant> {
        val today = LocalDate.now()
        return listOf(
            Plant(
                name = "Monstera Deliciosa",
                type = "Tropical foliage",
                location = "Living room",
                notes = "Bright indirect light. Rotate weekly to keep growth even.",
                reminders = listOf(
                    CareReminder(type = CareType.WATERING, frequencyDays = 5, nextDueDate = today),
                    CareReminder(type = CareType.MISTING, frequencyDays = 3, nextDueDate = today.plusDays(1)),
                    CareReminder(type = CareType.FERTILIZING, frequencyDays = 30, nextDueDate = today.plusDays(10)),
                ),
                careHistory = listOf(
                    CareHistoryEntry(type = CareType.WATERING, date = today.minusDays(5), note = "Soaked until water drained through."),
                    CareHistoryEntry(type = CareType.PRUNING, date = today.minusDays(19), note = "Trimmed one yellowing lower leaf."),
                ),
                healthNotes = listOf(
                    HealthNote(date = today.minusDays(2), mood = HealthMood.GROWTH, note = "New split leaf unfurling."),
                ),
            ),
            Plant(
                name = "Snake Plant",
                type = "Sansevieria",
                location = "Bedroom",
                notes = "Low-water plant. Let soil dry completely between waterings.",
                reminders = listOf(
                    CareReminder(type = CareType.WATERING, frequencyDays = 14, nextDueDate = today.plusDays(2)),
                    CareReminder(type = CareType.PRUNING, frequencyDays = 60, nextDueDate = today.plusDays(20)),
                ),
                careHistory = listOf(
                    CareHistoryEntry(type = CareType.WATERING, date = today.minusDays(12), note = "Light watering after soil check."),
                ),
                healthNotes = listOf(
                    HealthNote(date = today.minusDays(3), mood = HealthMood.HEALTHY, note = "Leaves are upright and firm."),
                ),
            ),
            Plant(
                name = "Golden Pothos",
                type = "Trailing vine",
                location = "Kitchen shelf",
                notes = "Prune vines to encourage fuller growth.",
                reminders = listOf(
                    CareReminder(type = CareType.WATERING, frequencyDays = 6, nextDueDate = today),
                    CareReminder(type = CareType.PRUNING, frequencyDays = 30, nextDueDate = today.plusDays(4)),
                    CareReminder(type = CareType.REPOTTING, frequencyDays = 240, nextDueDate = today.plusDays(45)),
                ),
                careHistory = listOf(
                    CareHistoryEntry(type = CareType.FERTILIZING, date = today.minusDays(21), note = "Half-strength liquid fertilizer."),
                ),
                healthNotes = listOf(
                    HealthNote(date = today.minusDays(1), mood = HealthMood.ATTENTION, note = "Two yellow leaves near the base."),
                ),
            ),
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "waterme_preferences"
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        const val KEY_PLANTS = "plants"
    }
}

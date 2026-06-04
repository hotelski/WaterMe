package com.hotelski.waterme.notifications

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

object ReminderEventStore {
    fun recordCompleted(context: Context, schedule: CareReminderSchedule, completedAtMillis: Long) {
        record(context, schedule, "completed", completedAtMillis)
    }

    fun recordSkipped(context: Context, schedule: CareReminderSchedule, skippedAtMillis: Long) {
        record(context, schedule, "skipped", skippedAtMillis)
    }

    fun recordSnoozed(context: Context, schedule: CareReminderSchedule, snoozedUntilMillis: Long) {
        record(context, schedule, "snoozed", snoozedUntilMillis)
    }

    private fun record(
        context: Context,
        schedule: CareReminderSchedule,
        action: String,
        actionAtMillis: Long,
    ) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val events = JSONArray(preferences.getString(KEY_EVENTS, "[]") ?: "[]")
        events.put(
            JSONObject()
                .put("plantId", schedule.plantId)
                .put("reminderId", schedule.reminderId)
                .put("taskId", schedule.taskId)
                .put("careType", schedule.careType.name)
                .put("action", action)
                .put("actionAtMillis", actionAtMillis),
        )
        preferences.edit { putString(KEY_EVENTS, events.toString()) }
    }

    private const val PREFERENCES_NAME = "waterme_reminder_events"
    private const val KEY_EVENTS = "events"
}

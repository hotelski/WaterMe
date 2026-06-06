package com.hotelski.waterme.notifications

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

object ReminderEventStore {
    fun recordDelivered(context: Context, schedule: CareReminderSchedule, deliveredAtMillis: Long) {
        record(context, schedule, ACTION_DELIVERED, deliveredAtMillis)
    }

    fun recordSuppressedInForeground(
        context: Context,
        schedule: CareReminderSchedule,
        suppressedAtMillis: Long,
    ) {
        record(context, schedule, ACTION_SUPPRESSED_FOREGROUND, suppressedAtMillis)
    }

    fun recordCompleted(context: Context, schedule: CareReminderSchedule, completedAtMillis: Long) {
        record(context, schedule, "completed", completedAtMillis)
    }

    fun recordSkipped(context: Context, schedule: CareReminderSchedule, skippedAtMillis: Long) {
        record(context, schedule, "skipped", skippedAtMillis)
    }

    fun recordSnoozed(context: Context, schedule: CareReminderSchedule, snoozedUntilMillis: Long) {
        record(context, schedule, "snoozed", snoozedUntilMillis)
    }

    fun hasHandledNotification(context: Context, schedule: CareReminderSchedule): Boolean {
        val events = readEvents(context)
        for (index in 0 until events.length()) {
            val event = events.optJSONObject(index) ?: continue
            val action = event.optString("action")
            val dueAtMillis = event.optLong("dueAtMillis", Long.MIN_VALUE)
            if (
                event.optString("taskId") == schedule.taskId &&
                dueAtMillis == schedule.dueAtMillis &&
                action in NOTIFICATION_HANDLED_ACTIONS
            ) {
                return true
            }
        }
        return false
    }

    private fun record(
        context: Context,
        schedule: CareReminderSchedule,
        action: String,
        actionAtMillis: Long,
    ) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val events = readEvents(context)
        events.put(
            JSONObject()
                .put("plantId", schedule.plantId)
                .put("reminderId", schedule.reminderId)
                .put("taskId", schedule.taskId)
                .put("careType", schedule.careType.name)
                .put("dueAtMillis", schedule.dueAtMillis)
                .put("action", action)
                .put("actionAtMillis", actionAtMillis),
        )
        preferences.edit { putString(KEY_EVENTS, events.toString()) }
    }

    private fun readEvents(context: Context): JSONArray {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return JSONArray(preferences.getString(KEY_EVENTS, "[]") ?: "[]")
    }

    private const val PREFERENCES_NAME = "waterme_reminder_events"
    private const val KEY_EVENTS = "events"
    private const val ACTION_DELIVERED = "delivered"
    private const val ACTION_SUPPRESSED_FOREGROUND = "suppressed_foreground"
    private val NOTIFICATION_HANDLED_ACTIONS = setOf(ACTION_DELIVERED, ACTION_SUPPRESSED_FOREGROUND)
}

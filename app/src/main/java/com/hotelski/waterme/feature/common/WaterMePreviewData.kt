package com.hotelski.waterme.feature.common

import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.model.HealthMood

object WaterMePreviewData {
    val plants = listOf(
        PlantCardUiModel(
            id = "plant-monstera",
            name = "Monstera Deliciosa",
            plantType = "Tropical foliage",
            location = "Living room",
            isFavorite = true,
            dueTaskCount = 2,
            nextCareLabel = "Next: Water + Feed Today",
            careRhythms = listOf(
                PlantCareRhythmUiModel(
                    careType = CareType.WATERING,
                    summary = "Watering - Every 5 days - 8:00 AM - starts Today",
                ),
                PlantCareRhythmUiModel(
                    careType = CareType.FERTILIZING,
                    summary = "Fertilizing - Every 30 days - 9:00 AM - starts Today",
                ),
            ),
            notes = "Bright indirect light. Rotate weekly.",
        ),
        PlantCardUiModel(
            id = "plant-snake",
            name = "Snake Plant",
            plantType = "Sansevieria",
            location = "Bedroom",
            dueTaskCount = 0,
            nextCareLabel = "Next: Water Tomorrow",
            careRhythms = listOf(
                PlantCareRhythmUiModel(
                    careType = CareType.WATERING,
                    summary = "Watering - Every 14 days - 8:00 AM - starts Tomorrow",
                ),
            ),
            notes = "Let soil dry completely.",
        ),
        PlantCardUiModel(
            id = "plant-pothos",
            name = "Golden Pothos",
            plantType = "Trailing vine",
            location = "Kitchen shelf",
            dueTaskCount = 1,
            nextCareLabel = "Next: Prune Jun 2",
            careRhythms = listOf(
                PlantCareRhythmUiModel(
                    careType = CareType.PRUNING,
                    summary = "Pruning - Every 45 days - 10:00 AM - starts Jun 2",
                ),
            ),
            notes = "Trim vines for fuller growth.",
        ),
    )

    val tasks = listOf(
        CareTaskUiModel(
            id = "task-water-monstera",
            plantId = "plant-monstera",
            plantName = "Monstera Deliciosa",
            plantLocation = "Living room",
            careType = CareType.WATERING,
            dueLabel = "Due today",
        ),
        CareTaskUiModel(
            id = "task-mist-pothos",
            plantId = "plant-pothos",
            plantName = "Golden Pothos",
            plantLocation = "Kitchen shelf",
            careType = CareType.MISTING,
            dueLabel = "Due today",
            isSnoozed = true,
        ),
        CareTaskUiModel(
            id = "task-feed-snake",
            plantId = "plant-snake",
            plantName = "Snake Plant",
            plantLocation = "Bedroom",
            careType = CareType.FERTILIZING,
            dueLabel = "Overdue",
            isOverdue = true,
        ),
    )

    val reminders = listOf(
        ReminderUiModel(
            id = "reminder-water",
            careType = CareType.WATERING,
            frequencyLabel = "Every 5 days",
            nextDueLabel = "Next: Today",
        ),
        ReminderUiModel(
            id = "reminder-mist",
            careType = CareType.MISTING,
            frequencyLabel = "Every 3 days",
            nextDueLabel = "Next: Tomorrow",
        ),
        ReminderUiModel(
            id = "reminder-feed",
            careType = CareType.FERTILIZING,
            frequencyLabel = "Every 30 days",
            nextDueLabel = "Next: Jun 12",
        ),
    )

    val reminderDrafts = listOf(
        ReminderDraftUiModel(CareType.WATERING, enabled = true, everyDays = "5", startsInDays = "0"),
        ReminderDraftUiModel(CareType.FERTILIZING, enabled = false, everyDays = "30", startsInDays = "30"),
        ReminderDraftUiModel(CareType.REPOTTING, enabled = false, everyDays = "180", startsInDays = "180"),
        ReminderDraftUiModel(CareType.MISTING, enabled = true, everyDays = "3", startsInDays = "1"),
        ReminderDraftUiModel(CareType.PRUNING, enabled = false, everyDays = "45", startsInDays = "45"),
    )

    val plantDetails = PlantDetailsUiModel(
        id = "plant-monstera",
        name = "Monstera Deliciosa",
        plantType = "Tropical foliage",
        location = "Living room",
        notes = "Bright indirect light. Rotate weekly to keep growth even.",
        photoCount = 3,
        reminderCount = 3,
        careHistoryCount = 8,
    )

    val history = listOf(
        CareHistoryUiModel(
            id = "history-water",
            plantId = "plant-monstera",
            plantName = "Monstera Deliciosa",
            careType = CareType.WATERING,
            actionLabel = "Completed",
            dateLabel = "Today",
            notes = "Watered until drainage ran clear.",
        ),
        CareHistoryUiModel(
            id = "history-prune",
            plantId = "plant-pothos",
            plantName = "Golden Pothos",
            careType = CareType.PRUNING,
            actionLabel = "Completed",
            dateLabel = "May 20",
            notes = "Trimmed two long vines and propagated cuttings.",
        ),
        CareHistoryUiModel(
            id = "history-skip",
            plantId = "plant-snake",
            plantName = "Snake Plant",
            careType = CareType.WATERING,
            actionLabel = "Skipped",
            dateLabel = "May 18",
            notes = "Soil still damp.",
        ),
    )

    val healthNotes = listOf(
        HealthNoteUiModel(
            id = "health-growth",
            plantName = "Monstera Deliciosa",
            mood = HealthMood.GROWTH,
            dateLabel = "Today",
            note = "New split leaf started unfurling.",
        ),
        HealthNoteUiModel(
            id = "health-attention",
            plantName = "Golden Pothos",
            mood = HealthMood.ATTENTION,
            dateLabel = "Yesterday",
            note = "Two yellow leaves near the base.",
        ),
    )

    val calendarDays = listOf(
        CalendarDayUiModel("Today, May 24", isToday = true, tasks = tasks.take(2)),
        CalendarDayUiModel("Tomorrow, May 25", tasks = tasks.drop(2)),
        CalendarDayUiModel("Fri, May 29", tasks = tasks.take(1)),
    )
}

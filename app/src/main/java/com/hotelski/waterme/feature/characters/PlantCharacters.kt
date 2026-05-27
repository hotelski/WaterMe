package com.hotelski.waterme.feature.characters

import androidx.annotation.DrawableRes
import com.hotelski.waterme.R
import com.hotelski.waterme.data.local.entity.HistoryAction
import com.hotelski.waterme.data.local.model.CareHistoryWithPlant
import com.hotelski.waterme.model.CareType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

data class PlantCharacterUiModel(
    val id: String,
    @param:DrawableRes val imageResId: Int,
    val name: String,
    val title: String,
    val description: String,
    val unlockLabel: String,
    val progress: Int,
    val target: Int,
    val isUnlocked: Boolean,
    val isSelected: Boolean,
    val accentColor: Long,
    val celebrationTitle: String,
    val celebrationMessage: String,
) {
    val progressPercent: Float
        get() = if (target <= 0) 1f else (progress.toFloat() / target).coerceIn(0f, 1f)

    val progressLabel: String
        get() = if (isUnlocked) "Unlocked" else "$progress/$target"
}

data class CharacterAchievementSummary(
    val totalCareLogs: Int = 0,
    val wateringLogs: Int = 0,
    val fertilizingLogs: Int = 0,
    val activeCareDays: Int = 0,
    val healthNotes: Int = 0,
    val plantsAddedTotal: Int = 0,
    val plantsWateredTotal: Int = 0,
    val morningWateringCount: Int = 0,
    val nightWateringCount: Int = 0,
    val samePlantWateringCount: Int = 0,
    val appOpenDayStreak: Int = 0,
    val careStreakDays: Int = 0,
    val succulentWateringCount: Int = 0,
)

private data class PlantCharacterDefinition(
    val id: String,
    @param:DrawableRes val imageResId: Int,
    val name: String,
    val title: String,
    val description: String,
    val unlockLabel: String,
    val target: Int,
    val accentColor: Long,
    val celebrationTitle: String,
    val celebrationMessage: String,
    val progress: (CharacterAchievementSummary) -> Int,
)

fun buildPlantCharacters(
    careHistory: List<CareHistoryWithPlant>,
    selectedCharacterId: String,
    clock: Clock = Clock.systemDefaultZone(),
    plantsAddedTotal: Int = 0,
    appOpenDayStreak: Int = 0,
): List<PlantCharacterUiModel> {
    val summary = careHistory.toCharacterAchievementSummary(
        clock = clock,
        plantsAddedTotal = plantsAddedTotal,
        appOpenDayStreak = appOpenDayStreak,
    )
    val unlockedIds = characterDefinitions
        .filter { definition -> definition.progress(summary) >= definition.target }
        .map { it.id }
        .toSet()
    val resolvedSelectedId = selectedCharacterId.takeIf { it in unlockedIds } ?: DEFAULT_CHARACTER_ID

    return characterDefinitions.map { definition ->
        val progress = definition.progress(summary).coerceAtMost(definition.target)
        PlantCharacterUiModel(
            id = definition.id,
            imageResId = definition.imageResId,
            name = definition.name,
            title = definition.title,
            description = definition.description,
            unlockLabel = definition.unlockLabel,
            progress = progress,
            target = definition.target,
            isUnlocked = definition.id in unlockedIds,
            isSelected = definition.id == resolvedSelectedId,
            accentColor = definition.accentColor,
            celebrationTitle = definition.celebrationTitle,
            celebrationMessage = definition.celebrationMessage,
        )
    }
}

fun activePlantCharacter(
    careHistory: List<CareHistoryWithPlant>,
    selectedCharacterId: String,
    clock: Clock = Clock.systemDefaultZone(),
    plantsAddedTotal: Int = 0,
    appOpenDayStreak: Int = 0,
): PlantCharacterUiModel =
    buildPlantCharacters(
        careHistory = careHistory,
        selectedCharacterId = selectedCharacterId,
        clock = clock,
        plantsAddedTotal = plantsAddedTotal,
        appOpenDayStreak = appOpenDayStreak,
    ).first { it.isSelected }

fun List<CareHistoryWithPlant>.toCharacterAchievementSummary(
    clock: Clock = Clock.systemDefaultZone(),
    plantsAddedTotal: Int = 0,
    appOpenDayStreak: Int = 0,
): CharacterAchievementSummary {
    val careLogs = filter { it.action.isCompletedCareAction() }
    val wateringLogs = careLogs.filter { it.careType == CareType.WATERING }
    val activeDays = careLogs
        .map { it.performedDate(clock) }
        .distinct()
    val wateringCountsByPlant = wateringLogs.groupingBy { it.plantId }.eachCount()

    return CharacterAchievementSummary(
        totalCareLogs = careLogs.size,
        wateringLogs = wateringLogs.size,
        fertilizingLogs = careLogs.count { it.careType == CareType.FERTILIZING },
        activeCareDays = activeDays.size,
        healthNotes = count { it.action == HistoryAction.HEALTH_NOTE },
        plantsAddedTotal = plantsAddedTotal,
        plantsWateredTotal = wateringLogs.map { it.plantId }.distinct().size,
        morningWateringCount = wateringLogs.count { it.performedHour(clock) in 5..11 },
        nightWateringCount = wateringLogs.count { it.performedHour(clock) >= 21 },
        samePlantWateringCount = wateringCountsByPlant.values.maxOrNull() ?: 0,
        appOpenDayStreak = appOpenDayStreak,
        careStreakDays = activeDays.longestConsecutiveStreak(),
        succulentWateringCount = wateringLogs.count { it.plantType.isSucculentType() },
    )
}

private val characterDefinitions = listOf(
    PlantCharacterDefinition(
        id = DEFAULT_CHARACTER_ID,
        imageResId = R.drawable.sprout,
        name = "Sprout",
        title = "First garden friend",
        description = "A cheerful little sprout companion that celebrates the start of your plant care journey.",
        unlockLabel = "Default character",
        target = 0,
        accentColor = 0xFF5E8F5A,
        celebrationTitle = "Sprout is happy",
        celebrationMessage = "A little leaf stretch for completed care.",
        progress = { 0 },
    ),
    PlantCharacterDefinition(
        id = "MOMO_MOSS",
        imageResId = R.drawable.momo_moss,
        name = "Momo Moss",
        title = "Soft moss buddy",
        description = "A calm fluffy moss companion that loves gentle watering routines.",
        unlockLabel = "Water 3 plants",
        target = 3,
        accentColor = 0xFF7FAE58,
        celebrationTitle = "Momo Moss feels fresh",
        celebrationMessage = "Soft moss wiggles for every cared plant.",
        progress = { it.plantsWateredTotal },
    ),
    PlantCharacterDefinition(
        id = "SUNNY_BLOOM",
        imageResId = R.drawable.sunny_bloom,
        name = "Sunny Bloom",
        title = "Morning sunshine friend",
        description = "A bright sunflower companion that appears when you take care of plants in the morning.",
        unlockLabel = "Complete morning watering",
        target = 1,
        accentColor = 0xFFF2B84B,
        celebrationTitle = "Sunny Bloom shines",
        celebrationMessage = "A warm little sunflower smile for your care.",
        progress = { it.morningWateringCount },
    ),
    PlantCharacterDefinition(
        id = "ROOTY",
        imageResId = R.drawable.rooty,
        name = "Rooty",
        title = "Hardworking garden helper",
        description = "A strong root companion that rewards steady plant care progress.",
        unlockLabel = "Complete 7 waterings",
        target = 7,
        accentColor = 0xFFB98245,
        celebrationTitle = "Rooty gets to work",
        celebrationMessage = "Tiny boots, big care energy.",
        progress = { it.wateringLogs },
    ),
    PlantCharacterDefinition(
        id = "TINY_TULIP",
        imageResId = R.drawable.tiny_tulip,
        name = "Tiny Tulip",
        title = "Shy blooming friend",
        description = "A sweet tulip companion that grows attached to consistent care for the same plant.",
        unlockLabel = "Water the same plant 3 times",
        target = 3,
        accentColor = 0xFFE98BA0,
        celebrationTitle = "Tiny Tulip blooms",
        celebrationMessage = "A shy little bloom for your careful attention.",
        progress = { it.samePlantWateringCount },
    ),
    PlantCharacterDefinition(
        id = "MINTY_MAX",
        imageResId = R.drawable.minty_max,
        name = "Minty Max",
        title = "Energetic mint buddy",
        description = "A sporty mint companion that loves daily app check-ins and active care habits.",
        unlockLabel = "Open app 3 days",
        target = 3,
        accentColor = 0xFF4FAE8A,
        celebrationTitle = "Minty Max is pumped",
        celebrationMessage = "Fresh leaves, fresh energy, fresh progress.",
        progress = { it.appOpenDayStreak },
    ),
    PlantCharacterDefinition(
        id = "CAPTAIN_CACTUS",
        imageResId = R.drawable.captain_cactus,
        name = "Captain Cactus",
        title = "Brave streak captain",
        description = "A cheerful cactus captain that joins when you start building a watering streak.",
        unlockLabel = "Reach a 3-day streak",
        target = 3,
        accentColor = 0xFF4F9B58,
        celebrationTitle = "Captain Cactus salutes",
        celebrationMessage = "The captain approves this watering mission.",
        progress = { it.careStreakDays },
    ),
    PlantCharacterDefinition(
        id = "LUNA_LEAF",
        imageResId = R.drawable.luna_leaf,
        name = "Luna Leaf",
        title = "Night care spirit",
        description = "A mystical leaf companion that appears when you care for plants after sunset.",
        unlockLabel = "Water after 9 PM",
        target = 1,
        accentColor = 0xFF7A6CCF,
        celebrationTitle = "Luna Leaf glows",
        celebrationMessage = "A soft moonlit sparkle for completed care.",
        progress = { it.nightWateringCount },
    ),
    PlantCharacterDefinition(
        id = "PROFESSOR_IVY",
        imageResId = R.drawable.professor_ivy,
        name = "Professor Ivy",
        title = "Plant care scholar",
        description = "A wise ivy companion that rewards expanding and organizing your garden.",
        unlockLabel = "Add 5 plants",
        target = 5,
        accentColor = 0xFF6C9E4F,
        celebrationTitle = "Professor Ivy approves",
        celebrationMessage = "Excellent care habits, scientifically speaking.",
        progress = { it.plantsAddedTotal },
    ),
    PlantCharacterDefinition(
        id = "AQUA_ALOE",
        imageResId = R.drawable.aqua_aloe,
        name = "Aqua Aloe",
        title = "Succulent care healer",
        description = "A gentle aloe companion that appears when you care for a succulent.",
        unlockLabel = "Water a succulent",
        target = 1,
        accentColor = 0xFF4FA7C7,
        celebrationTitle = "Aqua Aloe feels restored",
        celebrationMessage = "Cool water, calm leaves, healthy plant care.",
        progress = { it.succulentWateringCount },
    ),
)

private fun HistoryAction.isCompletedCareAction(): Boolean =
    this == HistoryAction.COMPLETED || this == HistoryAction.MANUAL_LOG

private fun CareHistoryWithPlant.performedDate(clock: Clock): LocalDate =
    Instant.ofEpochMilli(performedAt).atZone(clock.zone).toLocalDate()

private fun CareHistoryWithPlant.performedHour(clock: Clock): Int =
    Instant.ofEpochMilli(performedAt).atZone(clock.zone).hour

private fun List<LocalDate>.longestConsecutiveStreak(): Int {
    val dates = distinct().sorted()
    if (dates.isEmpty()) return 0

    var current = 1
    var longest = 1
    for (index in 1 until dates.size) {
        current = if (ChronoUnit.DAYS.between(dates[index - 1], dates[index]) == 1L) {
            current + 1
        } else {
            1
        }
        longest = maxOf(longest, current)
    }
    return longest
}

private fun String.isSucculentType(): Boolean {
    val normalized = lowercase(Locale.ROOT)
    return succulentKeywords.any { keyword -> normalized.contains(keyword) }
}

private val succulentKeywords = listOf(
    "succulent",
    "aloe",
    "cactus",
    "cacti",
    "sansevieria",
)

private const val DEFAULT_CHARACTER_ID = "SPROUT"

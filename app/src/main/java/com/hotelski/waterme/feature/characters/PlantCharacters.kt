package com.hotelski.waterme.feature.characters

import com.hotelski.waterme.data.local.entity.HistoryAction
import com.hotelski.waterme.data.local.model.CareHistoryWithPlant
import com.hotelski.waterme.model.CareType
import java.time.Clock
import java.time.Instant

data class PlantCharacterUiModel(
    val id: String,
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
)

private data class PlantCharacterDefinition(
    val id: String,
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
): List<PlantCharacterUiModel> {
    val summary = careHistory.toCharacterAchievementSummary(clock)
    val unlockedIds = characterDefinitions
        .filter { definition -> definition.progress(summary) >= definition.target }
        .map { it.id }
        .toSet()
    val resolvedSelectedId = selectedCharacterId.takeIf { it in unlockedIds } ?: DEFAULT_CHARACTER_ID

    return characterDefinitions.map { definition ->
        val progress = definition.progress(summary).coerceAtMost(definition.target)
        PlantCharacterUiModel(
            id = definition.id,
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
): PlantCharacterUiModel =
    buildPlantCharacters(careHistory, selectedCharacterId, clock).first { it.isSelected }

fun List<CareHistoryWithPlant>.toCharacterAchievementSummary(
    clock: Clock = Clock.systemDefaultZone(),
): CharacterAchievementSummary {
    val careLogs = filter { it.action != HistoryAction.HEALTH_NOTE }
    val activeDays = careLogs
        .map { Instant.ofEpochMilli(it.performedAt).atZone(clock.zone).toLocalDate() }
        .distinct()
        .size

    return CharacterAchievementSummary(
        totalCareLogs = careLogs.size,
        wateringLogs = careLogs.count { it.careType == CareType.WATERING },
        fertilizingLogs = careLogs.count { it.careType == CareType.FERTILIZING },
        activeCareDays = activeDays,
        healthNotes = count { it.action == HistoryAction.HEALTH_NOTE },
    )
}

private val characterDefinitions = listOf(
    PlantCharacterDefinition(
        id = DEFAULT_CHARACTER_ID,
        name = "Sprout",
        title = "First garden friend",
        description = "A calm sprout companion that appears when care is completed.",
        unlockLabel = "Default character",
        target = 0,
        accentColor = 0xFF5E8F5A,
        celebrationTitle = "Sprout is happy",
        celebrationMessage = "A little leaf stretch for completed care.",
        progress = { 0 },
    ),
    PlantCharacterDefinition(
        id = "DEW_FERN",
        name = "Dew Fern",
        title = "Watering specialist",
        description = "Unlocks after consistent watering logs and reacts best to watering tasks.",
        unlockLabel = "Complete 3 watering logs",
        target = 3,
        accentColor = 0xFF5FA8A1,
        celebrationTitle = "Dew Fern perked up",
        celebrationMessage = "Fresh water logged. The fern is glowing.",
        progress = { it.wateringLogs },
    ),
    PlantCharacterDefinition(
        id = "MOSS_MINDER",
        name = "Moss Minder",
        title = "Routine keeper",
        description = "A soft moss companion for keepers who build a care rhythm.",
        unlockLabel = "Log care on 3 different days",
        target = 3,
        accentColor = 0xFF7A9F57,
        celebrationTitle = "Moss Minder noticed",
        celebrationMessage = "Another care moment added to your garden rhythm.",
        progress = { it.activeCareDays },
    ),
    PlantCharacterDefinition(
        id = "BLOOM_CACTUS",
        name = "Bloom Cactus",
        title = "Care collector",
        description = "Unlocks when your care history starts to feel established.",
        unlockLabel = "Complete 7 care logs",
        target = 7,
        accentColor = 0xFFD09865,
        celebrationTitle = "Bloom Cactus cheered",
        celebrationMessage = "Care logged. The cactus bloom opens a little more.",
        progress = { it.totalCareLogs },
    ),
    PlantCharacterDefinition(
        id = "MONSTERA_GUARDIAN",
        name = "Monstera Guardian",
        title = "Premium garden guardian",
        description = "A larger companion for mature gardens with deep care history.",
        unlockLabel = "Complete 20 care logs",
        target = 20,
        accentColor = 0xFF2F6F4E,
        celebrationTitle = "Monstera Guardian approves",
        celebrationMessage = "Your garden care streak feels strong today.",
        progress = { it.totalCareLogs },
    ),
)

private const val DEFAULT_CHARACTER_ID = "SPROUT"

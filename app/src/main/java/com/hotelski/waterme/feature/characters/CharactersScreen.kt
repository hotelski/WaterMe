package com.hotelski.waterme.feature.characters

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.R
import com.hotelski.waterme.feature.common.WaterMeCard
import com.hotelski.waterme.feature.common.WaterMeEmptyState
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMePremiumCard
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.ui.theme.Clay
import com.hotelski.waterme.ui.theme.GardenBackground
import com.hotelski.waterme.ui.theme.Ink
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.MutedInk
import com.hotelski.waterme.ui.theme.SoftCream
import com.hotelski.waterme.ui.theme.WaterMeTheme

@Composable
fun CharactersScreen(
    uiState: CharactersUiState,
    onEvent: (CharactersEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GardenBackground,
        topBar = {
            WaterMeTopBar(
                title = "Characters",
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                navigationContentDescription = "Back",
                onNavigationClick = { onEvent(CharactersEvent.BackClicked) },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> WaterMeLoadingState("Growing characters...", Modifier.padding(innerPadding))
            uiState.errorMessage != null && uiState.characters.isEmpty() -> Box(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeErrorState(uiState.errorMessage, onRetryClick = { onEvent(CharactersEvent.RetryClicked) })
            }
            uiState.characters.isEmpty() -> Box(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeEmptyState(
                    title = "No characters",
                    message = "WaterMe could not prepare character rewards.",
                    icon = Icons.Rounded.LocalFlorist,
                )
            }
            else -> CharactersContent(
                uiState = uiState,
                onEvent = onEvent,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun CharactersContent(
    uiState: CharactersUiState,
    onEvent: (CharactersEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GardenBackground),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ActiveCharacterHero(uiState)
        }
        if (uiState.successMessage != null || uiState.errorMessage != null) {
            item {
                CharacterMessageCard(
                    successMessage = uiState.successMessage,
                    errorMessage = uiState.errorMessage,
                )
            }
        }
        item {
            AchievementSummaryCard(
                summary = uiState.achievementSummary,
                unlockedCount = uiState.characters.count { it.isUnlocked },
                totalCount = uiState.characters.size,
            )
        }
        items(uiState.characters, key = { it.id }) { character ->
            CharacterUnlockCard(
                character = character,
                heartBurstKey = uiState.heartBurstKey.takeIf { character.isSelected && it != 0L },
                onSelect = { onEvent(CharactersEvent.CharacterSelected(character.id)) },
            )
        }
    }
}

@Composable
private fun ActiveCharacterHero(
    uiState: CharactersUiState,
    modifier: Modifier = Modifier,
) {
    val activeCharacter = uiState.activeCharacter ?: return
    val activeAccent = Color(activeCharacter.accentColor)
    WaterMePremiumCard(
        modifier = modifier,
        containerColor = activeAccent.copy(alpha = 0.14f),
        accentColor = activeAccent,
        shape = RoundedCornerShape(34.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlantCharacterAvatar(
                character = activeCharacter,
                size = 118.dp,
                animated = true,
                heartBurstKey = uiState.heartBurstKey.takeIf { it != 0L },
            )
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
            ) {
                Text(
                    text = activeCharacter.name,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AchievementSummaryCard(
    summary: CharacterAchievementSummary,
    unlockedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    WaterMeCard(modifier = modifier, containerColor = SoftCream) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Stars, contentDescription = null, tint = LeafGreen)
                Text(
                    text = "Achievement progress",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryMetric("$unlockedCount/$totalCount", "unlocked", Modifier.weight(1f))
                SummaryMetric("${summary.wateringLogs}", "watering", Modifier.weight(1f))
                SummaryMetric("${summary.activeCareDays}", "care days", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = LeafGreen.copy(alpha = 0.10f),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = LeafGreen, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MutedInk, maxLines = 1)
        }
    }
}

@Composable
private fun CharacterUnlockCard(
    character: PlantCharacterUiModel,
    heartBurstKey: Any?,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = Color(character.accentColor)
    val accentText = readableAccentTextColor(accent)
    val buttonContent = readableButtonContentColor(accent)
    WaterMePremiumCard(
        modifier = modifier.animateContentSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        accentColor = accent,
        shape = RoundedCornerShape(30.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlantCharacterAvatar(
                    character = character,
                    size = 78.dp,
                    animated = character.isSelected,
                    heartBurstKey = heartBurstKey,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = character.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            color = Ink,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        CharacterStatePill(character)
                    }
                    Text(
                        text = character.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = accentText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = character.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = character.unlockLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = character.progressLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = accentText,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                LinearProgressIndicator(
                    progress = { character.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = accent,
                    trackColor = accent.copy(alpha = 0.14f),
                )
            }

            if (character.isUnlocked) {
                Button(
                    onClick = if (character.isSelected) {
                        {}
                    } else {
                        onSelect
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = buttonContent,
                    ),
                ) {
                    Icon(
                        imageVector = if (character.isSelected) Icons.Rounded.Check else Icons.Rounded.LocalFlorist,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (character.isSelected) "Selected" else "Select character")
                }
            } else {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Locked")
                }
            }
        }
    }
}

@Composable
private fun CharacterStatePill(character: PlantCharacterUiModel) {
    val accent = Color(character.accentColor)
    val accentText = readableAccentTextColor(accent)
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = when {
            character.isSelected -> accent.copy(alpha = 0.16f)
            character.isUnlocked -> LeafGreen.copy(alpha = 0.12f)
            else -> Clay.copy(alpha = 0.11f)
        },
    ) {
        Text(
            text = when {
                character.isSelected -> "Active"
                character.isUnlocked -> "Unlocked"
                else -> "Locked"
            },
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = when {
                character.isSelected -> accentText
                character.isUnlocked -> LeafGreen
                else -> Clay
            },
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

private fun readableAccentTextColor(accent: Color): Color =
    if (accent.luminance() > 0.42f) Ink else accent

private fun readableButtonContentColor(accent: Color): Color =
    if (accent.luminance() > 0.55f) Ink else Color.White

@Composable
private fun CharacterMessageCard(
    successMessage: String?,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    val message = successMessage ?: errorMessage ?: return
    val color = if (successMessage != null) LeafGreen else Clay
    WaterMeCard(modifier = modifier, containerColor = color.copy(alpha = 0.10f)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (successMessage != null) Icons.Rounded.Check else Icons.Rounded.Lock,
                contentDescription = null,
                tint = color,
            )
            Text(message, color = Ink, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CharactersScreenPreview() {
    val characters = previewCharacters()
    WaterMeTheme {
        CharactersScreen(
            uiState = CharactersUiState(
                activeCharacter = characters.first(),
                characters = characters,
                achievementSummary = CharacterAchievementSummary(
                    totalCareLogs = 8,
                    wateringLogs = 4,
                    activeCareDays = 3,
                ),
            ),
            onEvent = {},
        )
    }
}

private fun previewCharacters(): List<PlantCharacterUiModel> =
    listOf(
        PlantCharacterUiModel(
            id = "SPROUT",
            imageResId = R.drawable.sprout,
            name = "Sprout",
            title = "First garden friend",
            description = "A calm sprout companion that appears when care is completed.",
            unlockLabel = "Default character",
            progress = 0,
            target = 0,
            isUnlocked = true,
            isSelected = true,
            accentColor = 0xFF5E8F5A,
            celebrationTitle = "Sprout is happy",
            celebrationMessage = "A little leaf stretch for completed care.",
        ),
        PlantCharacterUiModel(
            id = "MOMO_MOSS",
            imageResId = R.drawable.momo_moss,
            name = "Momo Moss",
            title = "Soft moss buddy",
            description = "Unlocks after consistent watering logs.",
            unlockLabel = "Complete 3 watering logs",
            progress = 2,
            target = 3,
            isUnlocked = false,
            isSelected = false,
            accentColor = 0xFF7FAE58,
            celebrationTitle = "Momo Moss feels fresh",
            celebrationMessage = "Soft moss wiggles for every cared plant.",
        ),
    )

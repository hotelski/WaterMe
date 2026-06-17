package com.hotelski.waterme.feature.aiplantcare

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Compost
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.characters.PlantCharacterCelebrationCard
import com.hotelski.waterme.feature.common.PlantPhotoTile
import com.hotelski.waterme.feature.common.WaterMeEmptyState
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeIconBadge
import com.hotelski.waterme.feature.common.WaterMePremiumCard
import com.hotelski.waterme.feature.common.WaterMeStatusChip
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.model.PlantCareAdvice
import com.hotelski.waterme.ui.theme.Clay
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.MistBlue
import com.hotelski.waterme.ui.theme.WaterMeTheme

@Composable
fun AiPlantCareScreen(
    uiState: AiPlantCareUiState,
    onEvent: (AiPlantCareEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    uiState.pendingSuggestedAction?.let { pendingAction ->
        AiCareSuggestedActionDialog(
            pendingAction = pendingAction,
            isApplying = uiState.isApplyingSuggestedAction,
            onValueChange = { onEvent(AiPlantCareEvent.SuggestedActionDraftChanged(it)) },
            onDismiss = { onEvent(AiPlantCareEvent.DismissSuggestedActionClicked) },
            onConfirm = { onEvent(AiPlantCareEvent.ConfirmSuggestedActionClicked) },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            WaterMeTopBar(
                title = "AI Care",
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                navigationContentDescription = "Back",
                onNavigationClick = { onEvent(AiPlantCareEvent.BackClicked) },
            )
        },
    ) { innerPadding ->
        AiPlantCareContent(
            uiState = uiState,
            onEvent = onEvent,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun AiPlantCareContent(
    uiState: AiPlantCareUiState,
    onEvent: (AiPlantCareEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            AiCareHeroCard()
        }

        item {
            AiCareModeSelector(
                inputMode = uiState.inputMode,
                onSavedPlantClick = { onEvent(AiPlantCareEvent.SavedPlantModeSelected) },
                onTemporaryPlantClick = { onEvent(AiPlantCareEvent.TemporaryPlantModeSelected) },
            )
        }

        if (uiState.isLoadingPlants) {
            item {
                AiCareLoadingCard(message = "Loading your plants...")
            }
        } else {
            when (uiState.inputMode) {
                AiPlantCareInputMode.SAVED_PLANT -> item {
                    SavedPlantPicker(
                        plants = uiState.savedPlants,
                        selectedPlantId = uiState.selectedPlantId,
                        onPlantSelected = { plantId -> onEvent(AiPlantCareEvent.SavedPlantSelected(plantId)) },
                        onUseTemporaryPlant = { onEvent(AiPlantCareEvent.TemporaryPlantModeSelected) },
                    )
                }

                AiPlantCareInputMode.TEMPORARY_PLANT -> item {
                    TemporaryPlantForm(
                        plantName = uiState.temporaryPlantName,
                        scientificName = uiState.temporaryScientificName,
                        onPlantNameChanged = { onEvent(AiPlantCareEvent.TemporaryPlantNameChanged(it)) },
                        onScientificNameChanged = { onEvent(AiPlantCareEvent.TemporaryScientificNameChanged(it)) },
                    )
                }
            }
        }

        if (uiState.shouldShowAdviceButton) {
            item {
                AiCareQuotaStatus(
                    remainingRequests = uiState.remainingAiCareRequests,
                    requestLimit = uiState.aiCareRequestLimit,
                    isQuotaExhausted = uiState.isAiCareQuotaExhausted,
                    resetCountdown = uiState.aiCareQuotaResetCountdown,
                )
            }
            item {
                GetAiCareAdviceButton(
                    enabled = uiState.canRequestAdvice,
                    isLoading = uiState.isAdviceLoading,
                    label = if (uiState.advice == null) "Get AI care advice" else "Refresh advice",
                    onClick = {
                        onEvent(
                            if (uiState.advice == null) {
                                AiPlantCareEvent.GetAdviceClicked
                            } else {
                                AiPlantCareEvent.RefreshAdviceClicked
                            },
                        )
                    },
                )
            }
        }

        if (uiState.errorMessage != null) {
            item {
                WaterMeErrorState(
                    message = uiState.errorMessage,
                    onRetryClick = null,
                )
            }
        }

        if (!uiState.aiAvailabilityMessage.isNullOrBlank()) {
            item {
                AiCareAvailabilityMessageCard(message = uiState.aiAvailabilityMessage)
            }
        }

        if (uiState.fallbackAdvice != null) {
            item {
                AiCareFallbackAdviceCard(fallbackAdvice = uiState.fallbackAdvice)
            }
        }

        if (uiState.advice != null) {
            item {
                PlantCareAdviceCard(
                    advice = uiState.advice,
                    generatedLabel = uiState.adviceUpdatedLabel,
                )
            }
            item {
                AiCareFollowUpCard(
                    question = uiState.followUpQuestion,
                    isLoading = uiState.isFollowUpLoading,
                    errorMessage = uiState.followUpErrorMessage,
                    items = uiState.followUpItems,
                    isQuotaExhausted = uiState.isAiCareQuotaExhausted,
                    resetCountdown = uiState.aiCareQuotaResetCountdown,
                    onQuestionChanged = { onEvent(AiPlantCareEvent.FollowUpQuestionChanged(it)) },
                    onSend = { onEvent(AiPlantCareEvent.SendFollowUpClicked) },
                )
            }
            if (!uiState.actionMessage.isNullOrBlank()) {
                item {
                    AiCareSuggestedActionMessage(uiState = uiState)
                }
            }
            item {
                AiCareSuggestedActionsCard(
                    advice = uiState.advice,
                    hasSavedPlant = uiState.selectedPlant != null,
                    isApplying = uiState.isApplyingSuggestedAction,
                    onSetWateringReminder = {
                        onEvent(AiPlantCareEvent.SetReminderSuggestedActionClicked(CareType.WATERING))
                    },
                    onSetFertilizingReminder = {
                        onEvent(AiPlantCareEvent.SetReminderSuggestedActionClicked(CareType.FERTILIZING))
                    },
                    onAddNote = { onEvent(AiPlantCareEvent.AddNoteSuggestedActionClicked) },
                    onSaveCareProfile = { onEvent(AiPlantCareEvent.SaveCareProfileSuggestedActionClicked) },
                    onAddTemporaryPlant = { onEvent(AiPlantCareEvent.AddTemporaryPlantClicked) },
                )
            }
        }

        if (uiState.shouldShowEmptyState) {
            item {
                AiCareEmptyState(uiState = uiState)
            }
        }
    }
}

@Composable
private fun AiCareHeroCard(
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        accentColor = LeafGreen,
        shape = RoundedCornerShape(28.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WaterMeIconBadge(
                icon = Icons.Rounded.TipsAndUpdates,
                size = 52.dp,
                color = LeafGreen,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "AI plant care",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Choose a saved plant or type a plant name to get care advice.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AiCareQuotaStatus(
    remainingRequests: Int,
    requestLimit: Int,
    isQuotaExhausted: Boolean,
    resetCountdown: String?,
    modifier: Modifier = Modifier,
) {
    if (isQuotaExhausted) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.34f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.16f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "Daily AI Care limit reached",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Resets in ${resetCountdown ?: "--:--"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            WaterMeStatusChip(
                label = "$remainingRequests/$requestLimit AI requests left today",
                color = LeafGreen,
                icon = Icons.Rounded.TipsAndUpdates,
            )
        }
    }
}

@Composable
private fun AiCareModeSelector(
    inputMode: AiPlantCareInputMode,
    onSavedPlantClick: () -> Unit,
    onTemporaryPlantClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AiCareModeChip(
            selected = inputMode == AiPlantCareInputMode.SAVED_PLANT,
            label = "Saved plant",
            icon = Icons.Rounded.LocalFlorist,
            onClick = onSavedPlantClick,
            modifier = Modifier.weight(1f),
        )
        AiCareModeChip(
            selected = inputMode == AiPlantCareInputMode.TEMPORARY_PLANT,
            label = "New plant",
            icon = Icons.Rounded.Spa,
            onClick = onTemporaryPlantClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AiCareModeChip(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) LeafGreen.copy(alpha = 0.48f) else MaterialTheme.colorScheme.outlineVariant
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) LeafGreen.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) LeafGreen else MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SavedPlantPicker(
    plants: List<AiPlantCarePlantUiModel>,
    selectedPlantId: String?,
    onPlantSelected: (String) -> Unit,
    onUseTemporaryPlant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Choose from saved plants",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (plants.isEmpty()) {
            WaterMeEmptyState(
                title = "No saved plants yet",
                message = "Type a temporary plant name and WaterMe will prepare care advice.",
                icon = Icons.Rounded.LocalFlorist,
                actionLabel = "Use new plant",
                onActionClick = onUseTemporaryPlant,
            )
        } else {
            val selectedPlant = selectedPlantId?.let { selectedId ->
                plants.firstOrNull { it.id == selectedId }
            }
            if (selectedPlant != null) {
                SavedPlantChoiceCard(
                    plant = selectedPlant,
                    selected = true,
                    onClick = { onPlantSelected(selectedPlant.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    plants.chunked(2).forEach { rowPlants ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            rowPlants.forEach { plant ->
                                SavedPlantChoiceCard(
                                    plant = plant,
                                    selected = false,
                                    onClick = { onPlantSelected(plant.id) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (rowPlants.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedPlantChoiceCard(
    plant: AiPlantCarePlantUiModel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = modifier
            .height(if (selected) 206.dp else 168.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (selected) LeafGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) LeafGreen.copy(alpha = 0.62f) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PlantPhotoTile(
                photoUri = plant.photoUri,
                plantName = plant.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                fillContainer = true,
            )
            Text(
                text = plant.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) LeafGreen else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TemporaryPlantForm(
    plantName: String,
    scientificName: String,
    onPlantNameChanged: (String) -> Unit,
    onScientificNameChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        accentColor = LeafGreen,
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Temporary plant",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedTextField(
                value = plantName,
                onValueChange = onPlantNameChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Plant name") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.LocalFlorist, contentDescription = null) },
                shape = RoundedCornerShape(18.dp),
            )
            OutlinedTextField(
                value = scientificName,
                onValueChange = onScientificNameChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Optional scientific name") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Spa, contentDescription = null) },
                shape = RoundedCornerShape(18.dp),
            )
        }
    }
}

@Composable
private fun GetAiCareAdviceButton(
    enabled: Boolean,
    isLoading: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Icon(Icons.Rounded.TipsAndUpdates, contentDescription = null, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (isLoading) "Preparing advice" else label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AiCareLoadingCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(
        modifier = modifier,
        containerColor = MistBlue.copy(alpha = 0.34f),
        accentColor = LeafGreen,
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp,
                color = LeafGreen,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AiCareEmptyState(
    uiState: AiPlantCareUiState,
    modifier: Modifier = Modifier,
) {
    val title = when {
        uiState.inputMode == AiPlantCareInputMode.SAVED_PLANT && uiState.savedPlants.isEmpty() -> "Use a new plant name"
        uiState.inputMode == AiPlantCareInputMode.SAVED_PLANT -> "Select a saved plant"
        else -> "Ready for AI care"
    }
    val message = when {
        uiState.inputMode == AiPlantCareInputMode.SAVED_PLANT && uiState.savedPlants.isEmpty() ->
            "There are no saved plants to choose from yet, but temporary advice is available."
        uiState.inputMode == AiPlantCareInputMode.SAVED_PLANT ->
            "Choose one plant above, then ask WaterMe for care advice."
        else ->
            "Type the plant name, add a scientific name if you know it, then generate care advice."
    }

    WaterMeEmptyState(
        title = title,
        message = message,
        icon = Icons.Rounded.TipsAndUpdates,
        modifier = modifier,
    )
}

@Composable
private fun AiCareAvailabilityMessageCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(
        modifier = modifier,
        containerColor = MistBlue.copy(alpha = 0.28f),
        accentColor = MistBlue,
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AiCareFallbackAdviceCard(
    fallbackAdvice: AiCareFallbackAdviceUiModel,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        accentColor = LeafGreen,
        shape = RoundedCornerShape(30.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WaterMeIconBadge(
                    icon = Icons.Rounded.LocalFlorist,
                    size = 50.dp,
                    color = LeafGreen,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = fallbackAdvice.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = fallbackAdvice.reasonMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = LeafGreen.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, LeafGreen.copy(alpha = 0.18f)),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = fallbackAdvice.plantName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Basic WaterMe suggestion",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (fallbackAdvice.careRows.isEmpty()) {
                Text(
                    text = "No saved reminders are available for this plant yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    fallbackAdvice.careRows.forEach { row ->
                        AiCareFallbackCareRow(row = row)
                    }
                }
            }

            Text(
                text = fallbackAdvice.suggestion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AiCareFallbackCareRow(
    row: AiCareLocalCareUiModel,
    modifier: Modifier = Modifier,
) {
    val statusColor = if (row.isUrgent) MaterialTheme.colorScheme.error else LeafGreen
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WaterMeIconBadge(
                icon = if (row.title == CareType.WATERING.label) Icons.Rounded.WaterDrop else Icons.Rounded.Compost,
                size = 42.dp,
                color = statusColor,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = row.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    WaterMeStatusChip(
                        label = row.statusLabel,
                        color = statusColor,
                    )
                }
                Text(
                    text = row.intervalLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = row.lastCompletedLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun PlantCareAdviceCard(
    advice: PlantCareAdvice,
    generatedLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PlantCareAdviceHeader(
            advice = advice,
            generatedLabel = generatedLabel,
        )
        AiCareSectionCard(
            icon = Icons.Rounded.Straighten,
            title = "Height",
            text = advice.matureHeight,
            color = LeafGreen,
        )
        AiCareSectionCard(
            icon = Icons.Rounded.WaterDrop,
            title = "Water",
            text = advice.watering,
            color = MaterialTheme.colorScheme.primary,
            chipLabel = advice.waterChipLabel(),
        )
        AiCareSectionCard(
            icon = Icons.Rounded.WbSunny,
            title = "Light",
            text = advice.light,
            color = MaterialTheme.colorScheme.tertiary,
            chipLabel = advice.lightChipLabel(),
        )
        AiCareSectionCard(
            icon = Icons.Rounded.DeviceThermostat,
            title = "Temperature",
            text = advice.temperature,
            color = MaterialTheme.colorScheme.error,
        )
        AiCareSectionCard(
            icon = Icons.Rounded.Waves,
            title = "Humidity",
            text = advice.humidity,
            color = MaterialTheme.colorScheme.secondary,
        )
        AiCareSectionCard(
            icon = Icons.Rounded.Eco,
            title = "Fertilizing",
            text = advice.fertilizing,
            color = LeafGreen,
        )
        AiCareSectionCard(
            icon = Icons.Rounded.Compost,
            title = "Repotting",
            text = advice.repotting,
            color = MaterialTheme.colorScheme.tertiary,
        )
        AiCareSectionCard(
            icon = Icons.Rounded.LocalFlorist,
            title = "Flowering",
            text = advice.flowering,
            color = LeafGreen,
        )
        AiCareSectionCard(
            icon = Icons.AutoMirrored.Rounded.TrendingUp,
            title = "Growth",
            text = advice.growth,
            color = MaterialTheme.colorScheme.primary,
        )
        AiCareSectionCard(
            icon = Icons.Rounded.WarningAmber,
            title = "Toxicity",
            text = advice.toxicity,
            color = MaterialTheme.colorScheme.error,
            chipLabel = advice.toxicityChipLabel(),
        )
        AiCareSectionCard(
            icon = Icons.Rounded.Public,
            title = "Origin",
            text = advice.origin,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = advice.disclaimer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AiCareFollowUpCard(
    question: String,
    isLoading: Boolean,
    errorMessage: String?,
    items: List<AiCareFollowUpUiModel>,
    isQuotaExhausted: Boolean,
    resetCountdown: String?,
    onQuestionChanged: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        accentColor = MistBlue,
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WaterMeIconBadge(
                    icon = Icons.Rounded.TipsAndUpdates,
                    size = 46.dp,
                    color = MistBlue,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Ask follow-up",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Ask one quick question without changing the care profile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isQuotaExhausted) {
                AiCareFollowUpMessage(
                    message = "Daily AI Care limit reached. Resets in ${resetCountdown ?: "--:--"}.",
                    isError = true,
                )
            }

            OutlinedTextField(
                value = question,
                onValueChange = onQuestionChanged,
                enabled = !isLoading && !isQuotaExhausted,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ask about this plant...") },
                minLines = 1,
                maxLines = 4,
                shape = RoundedCornerShape(18.dp),
            )

            Button(
                onClick = onSend,
                enabled = question.isNotBlank() && !isLoading && !isQuotaExhausted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isLoading) "Asking AI..." else "Send",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (!errorMessage.isNullOrBlank()) {
                AiCareFollowUpMessage(
                    message = errorMessage,
                    isError = true,
                )
            }

            if (items.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Follow-up Q&A",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    items.forEach { item ->
                        AiCareFollowUpItem(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun AiCareFollowUpItem(
    item: AiCareFollowUpUiModel,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = item.question,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = item.answer,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AiCareFollowUpMessage(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (isError) MaterialTheme.colorScheme.error else LeafGreen
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.22f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isError) Icons.Rounded.WarningAmber else Icons.Rounded.Check,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AiCareSuggestedActionsCard(
    advice: PlantCareAdvice,
    hasSavedPlant: Boolean,
    isApplying: Boolean,
    onSetWateringReminder: () -> Unit,
    onSetFertilizingReminder: () -> Unit,
    onAddNote: () -> Unit,
    onSaveCareProfile: () -> Unit,
    onAddTemporaryPlant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        accentColor = LeafGreen,
        shape = RoundedCornerShape(30.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WaterMeIconBadge(
                    icon = Icons.Rounded.Check,
                    size = 46.dp,
                    color = LeafGreen,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Suggested actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Review and confirm before WaterMe updates anything.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AiCareSuggestedActionButton(
                title = "Set watering reminder",
                subtitle = advice.suggestedWateringIntervalDays?.let { "Prefilled: every $it days" }
                    ?: "Prefilled with WaterMe default",
                icon = Icons.Rounded.WaterDrop,
                color = MistBlue,
                enabled = hasSavedPlant && !isApplying,
                onClick = onSetWateringReminder,
            )
            AiCareSuggestedActionButton(
                title = "Set fertilizing reminder",
                subtitle = advice.suggestedFertilizingIntervalDays?.let { "Prefilled: every $it days" }
                    ?: "Prefilled with WaterMe default",
                icon = Icons.Rounded.Compost,
                color = LeafGreen,
                enabled = hasSavedPlant && !isApplying,
                onClick = onSetFertilizingReminder,
            )
            AiCareSuggestedActionButton(
                title = "Add note",
                subtitle = advice.suggestedNote?.takeIf { it.isNotBlank() } ?: "Prefilled from the AI summary",
                icon = Icons.Rounded.TipsAndUpdates,
                color = MaterialTheme.colorScheme.tertiary,
                enabled = hasSavedPlant && !isApplying,
                onClick = onAddNote,
            )
            AiCareSuggestedActionButton(
                title = "Save care profile",
                subtitle = if (hasSavedPlant) {
                    "Attach this AI profile to plant notes"
                } else {
                    "Already kept in AI Care cache"
                },
                icon = Icons.Rounded.LocalFlorist,
                color = LeafGreen,
                enabled = !isApplying,
                onClick = onSaveCareProfile,
            )
            if (!hasSavedPlant) {
                AiCareSuggestedActionButton(
                    title = "Add to my plants",
                    subtitle = "Open New Plant with this profile prefilled",
                    icon = Icons.Rounded.LocalFlorist,
                    color = LeafGreen,
                    enabled = !isApplying,
                    onClick = onAddTemporaryPlant,
                )
                Text(
                    text = "Add this plant first to apply reminders or notes.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AiCareSuggestedActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, color.copy(alpha = if (enabled) 0.28f else 0.10f)),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AiCareSuggestedActionMessage(
    uiState: AiPlantCareUiState,
    modifier: Modifier = Modifier,
) {
    val message = uiState.actionMessage ?: return
    val activeCharacter = uiState.activeCharacter
    if (!uiState.actionMessageIsError && activeCharacter != null) {
        PlantCharacterCelebrationCard(
            character = activeCharacter,
            message = message,
            heartBurstKey = uiState.actionHeartBurstKey.takeIf { it != 0L },
            modifier = modifier,
        )
    } else {
        AiCareActionMessageCard(
            message = message,
            isError = uiState.actionMessageIsError,
            modifier = modifier,
        )
    }
}

@Composable
private fun AiCareActionMessageCard(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (isError) MaterialTheme.colorScheme.error else LeafGreen
    WaterMePremiumCard(
        modifier = modifier,
        containerColor = color.copy(alpha = 0.11f),
        accentColor = color,
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isError) Icons.Rounded.WarningAmber else Icons.Rounded.Check,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AiCareSuggestedActionDialog(
    pendingAction: AiCarePendingActionUiModel,
    isApplying: Boolean,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (!isApplying) onDismiss()
        },
        title = {
            Text(
                text = pendingAction.title,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = pendingAction.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = pendingAction.inputValue,
                    onValueChange = onValueChange,
                    enabled = !isApplying,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(pendingAction.inputLabel) },
                    suffix = if (pendingAction.numericInput) {
                        { Text("days") }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (pendingAction.numericInput) KeyboardType.Number else KeyboardType.Text,
                    ),
                    minLines = if (pendingAction.numericInput) 1 else 3,
                    maxLines = if (pendingAction.numericInput) 1 else 8,
                    shape = RoundedCornerShape(18.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isApplying,
                shape = RoundedCornerShape(16.dp),
            ) {
                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isApplying) "Saving..." else pendingAction.confirmLabel)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isApplying,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Cancel")
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlantCareAdviceHeader(
    advice: PlantCareAdvice,
    generatedLabel: String?,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
        accentColor = LeafGreen,
        shape = RoundedCornerShape(32.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                WaterMeIconBadge(
                    icon = Icons.Rounded.LocalFlorist,
                    size = 58.dp,
                    color = LeafGreen,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = advice.plantName.ifBlank { "Plant care advice" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    advice.scientificName?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WaterMeStatusChip(
                    label = advice.difficultyChipLabel(),
                    color = advice.difficultyChipColor(),
                    icon = Icons.Rounded.TipsAndUpdates,
                )
                WaterMeStatusChip(
                    label = advice.toxicityChipLabel(),
                    color = advice.toxicityChipColor(),
                    icon = Icons.Rounded.WarningAmber,
                )
                WaterMeStatusChip(
                    label = advice.waterChipLabel(),
                    color = MistBlue,
                    icon = Icons.Rounded.WaterDrop,
                )
                WaterMeStatusChip(
                    label = advice.lightChipLabel(),
                    color = MaterialTheme.colorScheme.tertiary,
                    icon = Icons.Rounded.WbSunny,
                )
            }
            Text(
                text = advice.shortDescription.ifBlank { "WaterMe could not find a short description for this plant." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = generatedLabel ?: "Generated by AI",
                style = MaterialTheme.typography.labelMedium,
                color = LeafGreen,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiCareSectionCard(
    icon: ImageVector,
    title: String,
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    chipLabel: String? = null,
) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 7.dp,
                shape = shape,
                ambientColor = color.copy(alpha = 0.08f),
                spotColor = color.copy(alpha = 0.07f),
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, color.copy(alpha = 0.14f)),
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = color,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!chipLabel.isNullOrBlank()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        AiCareMiniChip(label = chipLabel, color = color)
                    }
                }
                Text(
                    text = text.ifBlank { "No specific guidance available yet." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AiCareMiniChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.11f),
        contentColor = color,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun PlantCareAdvice.difficultyChipLabel(): String =
    when {
        careDifficulty.contains("easy", ignoreCase = true) -> "Easy"
        careDifficulty.contains("difficult", ignoreCase = true) ||
            careDifficulty.contains("hard", ignoreCase = true) -> "Difficult"
        else -> "Medium"
    }

@Composable
private fun PlantCareAdvice.difficultyChipColor(): Color =
    when (difficultyChipLabel()) {
        "Easy" -> LeafGreen
        "Difficult" -> Clay
        else -> MaterialTheme.colorScheme.tertiary
    }

private fun PlantCareAdvice.waterChipLabel(): String {
    val normalizedWatering = watering.lowercase()
    val interval = suggestedWateringIntervalDays
    return when {
        interval != null && interval <= 3 -> "High water"
        interval != null && interval >= 10 -> "Low water"
        normalizedWatering.contains("rare") ||
            normalizedWatering.contains("sparse") ||
            normalizedWatering.contains("dry") ||
            normalizedWatering.contains("low") ||
            normalizedWatering.contains("\u0440\u044F\u0434\u043A") ||
            normalizedWatering.contains("\u0441\u0443\u0445") -> "Low water"
        normalizedWatering.contains("moist") ||
            normalizedWatering.contains("daily") ||
            normalizedWatering.contains("high") ||
            normalizedWatering.contains("\u0432\u043B\u0430\u0436") ||
            normalizedWatering.contains("\u0447\u0435\u0441\u0442") -> "High water"
        else -> "Moderate water"
    }
}

private fun PlantCareAdvice.lightChipLabel(): String {
    val normalizedLight = listOf(light, suggestedLightLevel.orEmpty()).joinToString(" ").lowercase()
    return when {
        normalizedLight.contains("full sun") ||
            normalizedLight.contains("direct sun") ||
            normalizedLight.contains("\u043F\u0440\u044F\u043A\u043E") ||
            normalizedLight.contains("\u043F\u044A\u043B\u043D\u043E \u0441\u043B\u044A\u043D\u0446\u0435") -> "Full sun"
        normalizedLight.contains("low") ||
            normalizedLight.contains("shade") ||
            normalizedLight.contains("\u0441\u044F\u043D\u043A") ||
            normalizedLight.contains("\u0441\u043B\u0430\u0431") ||
            normalizedLight.contains("\u043D\u0438\u0441\u043A") -> "Low light"
        else -> "Bright indirect light"
    }
}

private fun PlantCareAdvice.toxicityChipLabel(): String {
    val normalizedToxicity = toxicity.lowercase()
    return when {
        normalizedToxicity.contains("not toxic") ||
            normalizedToxicity.contains("non-toxic") ||
            normalizedToxicity.contains("pet safe") ||
            normalizedToxicity.contains("safe for pets") ||
            normalizedToxicity.contains("\u0431\u0435\u0437\u043E\u043F\u0430\u0441") ||
            normalizedToxicity.contains("\u043D\u0435\u0442\u043E\u043A\u0441") -> "Pet safe"
        normalizedToxicity.contains("toxic") ||
            normalizedToxicity.contains("poison") ||
            normalizedToxicity.contains("pets") ||
            normalizedToxicity.contains("children") ||
            normalizedToxicity.contains("\u0442\u043E\u043A\u0441") ||
            normalizedToxicity.contains("\u043E\u0442\u0440\u043E\u0432") ||
            normalizedToxicity.contains("\u0434\u0435\u0446\u0430") ||
            normalizedToxicity.contains("\u0434\u043E\u043C\u0430\u0448") -> "Toxic"
        else -> "Unknown"
    }
}

@Composable
private fun PlantCareAdvice.toxicityChipColor(): Color =
    when (toxicityChipLabel()) {
        "Pet safe" -> LeafGreen
        "Toxic" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

@Preview(showBackground = true)
@Composable
private fun AiPlantCareScreenPreview() {
    WaterMeTheme {
        AiPlantCareScreen(
            uiState = AiPlantCareUiState(
                savedPlants = listOf(
                    AiPlantCarePlantUiModel(
                        id = "1",
                        name = "Monstera",
                        scientificName = "Monstera deliciosa",
                        location = "Living room",
                        photoUri = null,
                        notes = "",
                        reminderCount = 2,
                        careLogCount = 5,
                        wateringCare = AiCareLocalCareUiModel(
                            title = "Watering",
                            intervalLabel = "Every 4 days",
                            lastCompletedLabel = "Last watered: Jun 12",
                            statusLabel = "Upcoming: Jun 18",
                        ),
                        fertilizingCare = AiCareLocalCareUiModel(
                            title = "Fertilizing",
                            intervalLabel = "Every 30 days",
                            lastCompletedLabel = "Last fertilized: Jun 1",
                            statusLabel = "Upcoming: Jul 1",
                        ),
                    ),
                ),
                selectedPlantId = "1",
                advice = PlantCareAdvice(
                    plantName = "Monstera",
                    scientificName = "Monstera deliciosa",
                    shortDescription = "A bold tropical foliage plant that prefers steady care and indirect light.",
                    matureHeight = "Often 3 to 8 ft indoors with support.",
                    watering = "Water when the top soil feels partly dry.",
                    light = "Bright, indirect light is best.",
                    temperature = "Keep in a stable warm room.",
                    humidity = "Average humidity works; higher humidity helps leaf edges.",
                    fertilizing = "Feed lightly during active growth.",
                    repotting = "Repot when roots crowd the pot.",
                    flowering = "Indoor flowering is uncommon.",
                    growth = "Grows faster in bright seasons.",
                    toxicity = "Keep away from pets and children.",
                    origin = "Native to tropical forests of Central America.",
                    disclaimer = "AI advice may be inaccurate. Always check your plant’s real condition.",
                ),
            ),
            onEvent = {},
        )
    }
}

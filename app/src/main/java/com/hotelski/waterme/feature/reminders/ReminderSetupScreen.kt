package com.hotelski.waterme.feature.reminders

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.CareTypeBadge
import com.hotelski.waterme.feature.common.ReminderDraftUiModel
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeIconBadge
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMePremiumCard
import com.hotelski.waterme.feature.common.WaterMePreviewData
import com.hotelski.waterme.feature.common.WaterMePrimaryButton
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.feature.common.label
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.ui.theme.WaterMeTheme

data class ReminderSetupUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val plantId: String? = null,
    val plantName: String = "New plant",
    val reminders: List<ReminderDraftUiModel> = WaterMePreviewData.reminderDrafts,
    val errorMessage: String? = null,
)

sealed interface ReminderSetupEvent {
    data object BackClicked : ReminderSetupEvent
    data object SaveClicked : ReminderSetupEvent
    data object RetryClicked : ReminderSetupEvent
    data class ReminderEnabledChanged(val careType: CareType, val enabled: Boolean) : ReminderSetupEvent
    data class EveryDaysChanged(val careType: CareType, val value: String) : ReminderSetupEvent
    data class StartsInDaysChanged(val careType: CareType, val value: String) : ReminderSetupEvent
}

@Composable
fun ReminderSetupScreen(
    uiState: ReminderSetupUiState,
    onEvent: (ReminderSetupEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            WaterMeTopBar(
                title = "Reminder Setup",
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                navigationContentDescription = "Back",
                onNavigationClick = { onEvent(ReminderSetupEvent.BackClicked) },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> WaterMeLoadingState("Loading reminder setup...", Modifier.padding(innerPadding))
            uiState.errorMessage != null -> Column(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeErrorState(uiState.errorMessage, onRetryClick = { onEvent(ReminderSetupEvent.RetryClicked) })
            }

            else -> LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { ReminderSheetHandle() }
                item {
                    WaterMePremiumCard(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.46f),
                        shape = RoundedCornerShape(32.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            WaterMeIconBadge(icon = Icons.Rounded.Notifications, size = 58.dp)
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Care schedule for ${uiState.plantName}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "Choose which reminders WaterMe should track and when they start.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                item { ReminderSectionHeader("Reminder types", "Premium pickers with active states for each care rhythm.") }

                items(uiState.reminders, key = { it.careType.name }) { reminder ->
                    WaterMePremiumCard(shape = RoundedCornerShape(28.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CareTypeBadge(reminder.careType)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        reminder.careType.label(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        "Repeat every ${reminder.everyDays.ifBlank { "?" }} days",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = reminder.enabled,
                                    onCheckedChange = {
                                        onEvent(ReminderSetupEvent.ReminderEnabledChanged(reminder.careType, it))
                                    },
                                )
                            }
                            if (reminder.enabled) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = reminder.everyDays,
                                        onValueChange = {
                                            onEvent(ReminderSetupEvent.EveryDaysChanged(reminder.careType, it))
                                        },
                                        label = { Text("Every") },
                                        suffix = { Text("days") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(18.dp),
                                    )
                                    OutlinedTextField(
                                        value = reminder.startsInDays,
                                        onValueChange = {
                                            onEvent(ReminderSetupEvent.StartsInDaysChanged(reminder.careType, it))
                                        },
                                        label = { Text("Starts in") },
                                        suffix = { Text("days") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    WaterMePrimaryButton(
                        label = if (uiState.isSaving) "Saving..." else "Save reminders",
                        onClick = { onEvent(ReminderSetupEvent.SaveClicked) },
                        icon = Icons.Rounded.Check,
                        enabled = !uiState.isSaving,
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ReminderSheetHandle(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(5.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.28f), RoundedCornerShape(999.dp)),
        )
    }
}

@Composable
private fun ReminderSectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReminderSetupScreenPreview() {
    WaterMeTheme {
        ReminderSetupScreen(
            uiState = ReminderSetupUiState(plantName = "Monstera Deliciosa"),
            onEvent = {},
        )
    }
}

package com.hotelski.waterme.feature.editplant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.CareTypeBadge
import com.hotelski.waterme.feature.common.PlantPhotoTile
import com.hotelski.waterme.feature.common.ReminderDraftUiModel
import com.hotelski.waterme.feature.common.WaterMeCard
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMePreviewData
import com.hotelski.waterme.feature.common.WaterMePrimaryButton
import com.hotelski.waterme.feature.common.WaterMeSectionHeader
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.feature.common.label
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.ui.theme.Clay
import com.hotelski.waterme.ui.theme.GardenBackground
import com.hotelski.waterme.ui.theme.MutedInk
import com.hotelski.waterme.ui.theme.WaterMeTheme

data class EditPlantUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val name: String = "",
    val plantType: String = "",
    val location: String = "",
    val notes: String = "",
    val primaryPhotoUri: String? = null,
    val reminders: List<ReminderDraftUiModel> = emptyList(),
    val errorMessage: String? = null,
) {
    val canSave: Boolean
        get() = name.isNotBlank() && !isSaving
}

sealed interface EditPlantEvent {
    data object BackClicked : EditPlantEvent
    data object SaveClicked : EditPlantEvent
    data object DeleteClicked : EditPlantEvent
    data object ChangePhotoClicked : EditPlantEvent
    data object RetryClicked : EditPlantEvent
    data class NameChanged(val value: String) : EditPlantEvent
    data class PlantTypeChanged(val value: String) : EditPlantEvent
    data class LocationChanged(val value: String) : EditPlantEvent
    data class NotesChanged(val value: String) : EditPlantEvent
    data class ReminderEnabledChanged(val careType: CareType, val enabled: Boolean) : EditPlantEvent
    data class ReminderEveryDaysChanged(val careType: CareType, val value: String) : EditPlantEvent
}

@Composable
fun EditPlantScreen(
    uiState: EditPlantUiState,
    onEvent: (EditPlantEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GardenBackground,
        topBar = {
            WaterMeTopBar(
                title = "Edit Plant",
                navigationIcon = Icons.Rounded.ArrowBack,
                navigationContentDescription = "Back",
                onNavigationClick = { onEvent(EditPlantEvent.BackClicked) },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> WaterMeLoadingState("Loading plant details...", Modifier.padding(innerPadding))
            uiState.errorMessage != null -> Column(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeErrorState(uiState.errorMessage, onRetryClick = { onEvent(EditPlantEvent.RetryClicked) })
            }

            else -> EditPlantContent(
                uiState = uiState,
                onEvent = onEvent,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun EditPlantContent(
    uiState: EditPlantUiState,
    onEvent: (EditPlantEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GardenBackground),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            WaterMeCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        PlantPhotoTile(uiState.primaryPhotoUri, uiState.name, size = 86.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Plant profile", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                            Text("Update the details, room, notes, and care schedule.", color = MutedInk)
                            androidx.compose.material3.TextButton(onClick = { onEvent(EditPlantEvent.ChangePhotoClicked) }) {
                                Text("Change photo")
                            }
                        }
                    }
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { onEvent(EditPlantEvent.NameChanged(it)) },
                        label = { Text("Plant name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = uiState.plantType,
                        onValueChange = { onEvent(EditPlantEvent.PlantTypeChanged(it)) },
                        label = { Text("Plant type") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = uiState.location,
                        onValueChange = { onEvent(EditPlantEvent.LocationChanged(it)) },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = uiState.notes,
                        onValueChange = { onEvent(EditPlantEvent.NotesChanged(it)) },
                        label = { Text("Notes") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(118.dp),
                    )
                }
            }
        }

        item { WaterMeSectionHeader("Reminder Schedule") }

        items(uiState.reminders, key = { it.careType.name }) { reminder ->
            WaterMeCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CareTypeBadge(reminder.careType)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(reminder.careType.label(), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                            Text("Every ${reminder.everyDays.ifBlank { "?" }} days", color = MutedInk)
                        }
                        Switch(
                            checked = reminder.enabled,
                            onCheckedChange = { onEvent(EditPlantEvent.ReminderEnabledChanged(reminder.careType, it)) },
                        )
                    }
                    if (reminder.enabled) {
                        OutlinedTextField(
                            value = reminder.everyDays,
                            onValueChange = { onEvent(EditPlantEvent.ReminderEveryDaysChanged(reminder.careType, it)) },
                            label = { Text("Every") },
                            suffix = { Text("days") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                }
            }
        }

        item {
            WaterMePrimaryButton(
                label = if (uiState.isSaving) "Saving..." else "Save changes",
                onClick = { onEvent(EditPlantEvent.SaveClicked) },
                enabled = uiState.canSave,
                icon = Icons.Rounded.Check,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { onEvent(EditPlantEvent.DeleteClicked) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Clay)
                Spacer(Modifier.width(8.dp))
                Text("Delete plant", color = Clay)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditPlantScreenPreview() {
    WaterMeTheme {
        EditPlantScreen(
            uiState = EditPlantUiState(
                name = WaterMePreviewData.plantDetails.name,
                plantType = WaterMePreviewData.plantDetails.plantType,
                location = WaterMePreviewData.plantDetails.location,
                notes = WaterMePreviewData.plantDetails.notes,
                reminders = WaterMePreviewData.reminderDrafts,
            ),
            onEvent = {},
        )
    }
}

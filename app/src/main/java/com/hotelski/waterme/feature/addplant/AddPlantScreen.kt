package com.hotelski.waterme.feature.addplant

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
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
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
import com.hotelski.waterme.feature.common.WaterMePrimaryButton
import com.hotelski.waterme.feature.common.WaterMeSectionHeader
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.feature.common.label
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.ui.theme.GardenBackground
import com.hotelski.waterme.ui.theme.MutedInk
import com.hotelski.waterme.ui.theme.WaterMeTheme

data class AddPlantFieldErrors(
    val name: String? = null,
    val reminders: Map<CareType, String> = emptyMap(),
)

data class AddPlantUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val name: String = "",
    val plantType: String = "",
    val location: String = "",
    val notes: String = "",
    val selectedPhotoUri: String? = null,
    val reminders: List<ReminderDraftUiModel> = defaultReminderDrafts(),
    val fieldErrors: AddPlantFieldErrors = AddPlantFieldErrors(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val createdPlantId: String? = null,
) {
    val canSave: Boolean
        get() = name.isNotBlank() && !isSaving && !isLoading
}

private fun defaultReminderDrafts(): List<ReminderDraftUiModel> =
    listOf(
        ReminderDraftUiModel(CareType.WATERING, enabled = true, everyDays = "4", startsInDays = "0"),
        ReminderDraftUiModel(CareType.FERTILIZING, enabled = false, everyDays = "30", startsInDays = "30"),
        ReminderDraftUiModel(CareType.REPOTTING, enabled = false, everyDays = "180", startsInDays = "180"),
        ReminderDraftUiModel(CareType.MISTING, enabled = true, everyDays = "3", startsInDays = "1"),
        ReminderDraftUiModel(CareType.PRUNING, enabled = false, everyDays = "45", startsInDays = "45"),
    )

sealed interface AddPlantEvent {
    data object BackClicked : AddPlantEvent
    data object ChoosePhotoClicked : AddPlantEvent
    data object SaveClicked : AddPlantEvent
    data object RetryClicked : AddPlantEvent
    data class NameChanged(val value: String) : AddPlantEvent
    data class PlantTypeChanged(val value: String) : AddPlantEvent
    data class LocationChanged(val value: String) : AddPlantEvent
    data class NotesChanged(val value: String) : AddPlantEvent
    data class ReminderEnabledChanged(val careType: CareType, val enabled: Boolean) : AddPlantEvent
    data class ReminderEveryDaysChanged(val careType: CareType, val value: String) : AddPlantEvent
    data class ReminderStartsInChanged(val careType: CareType, val value: String) : AddPlantEvent
}

@Composable
fun AddPlantScreen(
    uiState: AddPlantUiState,
    onEvent: (AddPlantEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GardenBackground,
        topBar = {
            WaterMeTopBar(
                title = "Add Plant",
                navigationIcon = Icons.Rounded.ArrowBack,
                navigationContentDescription = "Back",
                onNavigationClick = { onEvent(AddPlantEvent.BackClicked) },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> WaterMeLoadingState("Preparing plant form...", Modifier.padding(innerPadding))
            uiState.errorMessage != null -> Column(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeErrorState(uiState.errorMessage, onRetryClick = { onEvent(AddPlantEvent.RetryClicked) })
            }

            else -> AddPlantContent(
                uiState = uiState,
                onEvent = onEvent,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun AddPlantContent(
    uiState: AddPlantUiState,
    onEvent: (AddPlantEvent) -> Unit,
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
            PlantProfileFormCard(
                name = uiState.name,
                plantType = uiState.plantType,
                location = uiState.location,
                notes = uiState.notes,
                photoUri = uiState.selectedPhotoUri,
                nameError = uiState.fieldErrors.name,
                onEvent = onEvent,
            )
        }

        item { WaterMeSectionHeader("Care Reminders") }

        items(uiState.reminders, key = { it.careType.name }) { reminder ->
            ReminderDraftCard(
                reminder = reminder,
                errorMessage = uiState.fieldErrors.reminders[reminder.careType],
                onEvent = onEvent,
            )
        }

        item {
            WaterMePrimaryButton(
                label = if (uiState.isSaving) "Saving..." else "Save plant",
                onClick = { onEvent(AddPlantEvent.SaveClicked) },
                enabled = uiState.canSave,
                icon = Icons.Rounded.Check,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PlantProfileFormCard(
    name: String,
    plantType: String,
    location: String,
    notes: String,
    photoUri: String?,
    nameError: String?,
    onEvent: (AddPlantEvent) -> Unit,
) {
    WaterMeCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlantPhotoTile(photoUri = photoUri, plantName = name.ifBlank { "New plant" }, size = 86.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Plant profile", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Text(
                        "Add the details that help you recognize and care for it.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = MutedInk,
                    )
                    androidx.compose.material3.TextButton(onClick = { onEvent(AddPlantEvent.ChoosePhotoClicked) }) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Choose photo")
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { onEvent(AddPlantEvent.NameChanged(it)) },
                label = { Text("Plant name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = nameError != null,
                supportingText = if (nameError == null) null else {
                    { Text(nameError) }
                },
            )
            OutlinedTextField(
                value = plantType,
                onValueChange = { onEvent(AddPlantEvent.PlantTypeChanged(it)) },
                label = { Text("Plant type") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = location,
                onValueChange = { onEvent(AddPlantEvent.LocationChanged(it)) },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { onEvent(AddPlantEvent.NotesChanged(it)) },
                label = { Text("Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp),
            )
        }
    }
}

@Composable
private fun ReminderDraftCard(
    reminder: ReminderDraftUiModel,
    errorMessage: String?,
    onEvent: (AddPlantEvent) -> Unit,
) {
    WaterMeCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CareTypeBadge(reminder.careType)
                Column(modifier = Modifier.weight(1f)) {
                    Text(reminder.careType.label(), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Text("Suggested every ${reminder.everyDays.ifBlank { "?" }} days", color = MutedInk)
                }
                Switch(
                    checked = reminder.enabled,
                    onCheckedChange = { onEvent(AddPlantEvent.ReminderEnabledChanged(reminder.careType, it)) },
                )
            }
            if (reminder.enabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = reminder.everyDays,
                        onValueChange = { onEvent(AddPlantEvent.ReminderEveryDaysChanged(reminder.careType, it)) },
                        label = { Text("Every") },
                        suffix = { Text("days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        isError = errorMessage != null,
                    )
                    OutlinedTextField(
                        value = reminder.startsInDays,
                        onValueChange = { onEvent(AddPlantEvent.ReminderStartsInChanged(reminder.careType, it)) },
                        label = { Text("Starts in") },
                        suffix = { Text("days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        isError = errorMessage != null,
                    )
                }
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddPlantScreenPreview() {
    WaterMeTheme {
        AddPlantScreen(
            uiState = AddPlantUiState(
                name = "Calathea",
                plantType = "Prayer plant",
                location = "Office",
                notes = "Likes filtered light and consistent moisture.",
            ),
            onEvent = {},
        )
    }
}

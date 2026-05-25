package com.hotelski.waterme.feature.editplant

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.CareTypeBadge
import com.hotelski.waterme.feature.common.PlantPhotoTile
import com.hotelski.waterme.feature.common.ReminderDraftUiModel
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeIconBadge
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMePremiumCard
import com.hotelski.waterme.feature.common.WaterMePreviewData
import com.hotelski.waterme.feature.common.WaterMePrimaryButton
import com.hotelski.waterme.feature.common.WaterMeStatusChip
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.feature.common.label
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.ui.theme.Clay
import com.hotelski.waterme.ui.theme.WaterMeTheme

data class EditPlantFieldErrors(
    val name: String? = null,
    val reminders: Map<CareType, String> = emptyMap(),
)

data class EditPlantUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val name: String = "",
    val plantType: String = "",
    val location: String = "",
    val notes: String = "",
    val primaryPhotoUri: String? = null,
    val reminders: List<ReminderDraftUiModel> = emptyList(),
    val fieldErrors: EditPlantFieldErrors = EditPlantFieldErrors(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val canSave: Boolean
        get() = name.isNotBlank() && !isSaving && !isDeleting && !isLoading
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
    data class ReminderStartsInChanged(val careType: CareType, val value: String) : EditPlantEvent
}

@Composable
fun EditPlantScreen(
    uiState: EditPlantUiState,
    onEvent: (EditPlantEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            WaterMeTopBar(
                title = "Edit Plant",
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
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
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { EditSheetHandle() }
        item {
            PlantProfileCard(
                uiState = uiState,
                onEvent = onEvent,
            )
        }

        item { EditSectionHeader("Reminder schedule", "Turn care types on or off and adjust their cadence.") }

        items(uiState.reminders, key = { it.careType.name }) { reminder ->
            ReminderScheduleCard(
                reminder = reminder,
                errorMessage = uiState.fieldErrors.reminders[reminder.careType],
                onEvent = onEvent,
            )
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
                enabled = !uiState.isDeleting && !uiState.isSaving,
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Clay)
                Spacer(Modifier.width(8.dp))
                Text(if (uiState.isDeleting) "Deleting..." else "Delete plant", color = Clay)
            }
        }
    }
}

@Composable
private fun PlantProfileCard(
    uiState: EditPlantUiState,
    onEvent: (EditPlantEvent) -> Unit,
) {
    val nameSupportingText: @Composable (() -> Unit)? =
        uiState.fieldErrors.name?.let { message -> { Text(message) } }

    WaterMePremiumCard(shape = RoundedCornerShape(32.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                PlantPhotoTile(uiState.primaryPhotoUri, uiState.name, size = 104.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WaterMeStatusChip(
                        label = "Profile",
                        color = MaterialTheme.colorScheme.primary,
                        icon = Icons.Rounded.PhotoCamera,
                    )
                    Text(
                        "Update the room, notes, and care schedule.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { onEvent(EditPlantEvent.ChangePhotoClicked) }) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
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
                isError = uiState.fieldErrors.name != null,
                supportingText = nameSupportingText,
                shape = RoundedCornerShape(18.dp),
            )
            OutlinedTextField(
                value = uiState.plantType,
                onValueChange = { onEvent(EditPlantEvent.PlantTypeChanged(it)) },
                label = { Text("Plant type") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
            )
            OutlinedTextField(
                value = uiState.location,
                onValueChange = { onEvent(EditPlantEvent.LocationChanged(it)) },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
            )
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { onEvent(EditPlantEvent.NotesChanged(it)) },
                label = { Text("Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp),
                shape = RoundedCornerShape(18.dp),
            )
        }
    }
}

@Composable
private fun ReminderScheduleCard(
    reminder: ReminderDraftUiModel,
    errorMessage: String?,
    onEvent: (EditPlantEvent) -> Unit,
) {
    WaterMePremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CareTypeBadge(reminder.careType)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        reminder.careType.label(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Every ${reminder.everyDays.ifBlank { "?" }} days",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = reminder.enabled,
                    onCheckedChange = { onEvent(EditPlantEvent.ReminderEnabledChanged(reminder.careType, it)) },
                )
            }
            if (reminder.enabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = reminder.everyDays,
                        onValueChange = { onEvent(EditPlantEvent.ReminderEveryDaysChanged(reminder.careType, it)) },
                        label = { Text("Every") },
                        suffix = { Text("days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = errorMessage != null,
                        shape = RoundedCornerShape(18.dp),
                    )
                    OutlinedTextField(
                        value = reminder.startsInDays,
                        onValueChange = { onEvent(EditPlantEvent.ReminderStartsInChanged(reminder.careType, it)) },
                        label = { Text("Starts in") },
                        suffix = { Text("days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = errorMessage != null,
                        shape = RoundedCornerShape(18.dp),
                    )
                }
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun EditSheetHandle(modifier: Modifier = Modifier) {
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
private fun EditSectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WaterMeIconBadge(icon = Icons.Rounded.Check, size = 42.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

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
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.CareTypeBadge
import com.hotelski.waterme.feature.common.PlantPhotoTile
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
import com.hotelski.waterme.model.PlantEnvironment
import com.hotelski.waterme.ui.theme.Clay
import com.hotelski.waterme.ui.theme.WaterMeTheme

data class EditPlantFieldErrors(
    val name: String? = null,
    val reminders: Map<CareType, String> = emptyMap(),
)

enum class EditReminderPeriod {
    AM,
    PM,
}

data class EditReminderDraftUiModel(
    val careType: CareType,
    val enabled: Boolean,
    val everyDays: String,
    val startDateMillis: Long,
    val startDateLabel: String,
    val preferredHour: String,
    val preferredMinute: String,
    val preferredPeriod: EditReminderPeriod,
)

data class EditPlantUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val name: String = "",
    val plantType: String = "",
    val location: String = "",
    val environment: PlantEnvironment = PlantEnvironment.INDOOR,
    val notes: String = "",
    val primaryPhotoUri: String? = null,
    val reminders: List<EditReminderDraftUiModel> = emptyList(),
    val startDatePickerCareType: CareType? = null,
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
    data object DismissStartDatePicker : EditPlantEvent
    data class NameChanged(val value: String) : EditPlantEvent
    data class PlantTypeChanged(val value: String) : EditPlantEvent
    data class LocationChanged(val value: String) : EditPlantEvent
    data class EnvironmentSelected(val environment: PlantEnvironment) : EditPlantEvent
    data class NotesChanged(val value: String) : EditPlantEvent
    data class ReminderEnabledChanged(val careType: CareType, val enabled: Boolean) : EditPlantEvent
    data class ReminderEveryDaysChanged(val careType: CareType, val value: String) : EditPlantEvent
    data class ReminderHourChanged(val careType: CareType, val value: String) : EditPlantEvent
    data class ReminderMinuteChanged(val careType: CareType, val value: String) : EditPlantEvent
    data class ReminderPeriodSelected(val careType: CareType, val period: EditReminderPeriod) : EditPlantEvent
    data class ReminderStartDateClicked(val careType: CareType) : EditPlantEvent
    data class ReminderStartDateSelected(val millis: Long?) : EditPlantEvent
}

@Composable
fun EditPlantScreen(
    uiState: EditPlantUiState,
    onEvent: (EditPlantEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.startDatePickerCareType != null) {
        StartDatePickerDialog(
            initialSelectedDateMillis = uiState.reminders
                .firstOrNull { it.careType == uiState.startDatePickerCareType }
                ?.startDateMillis,
            onDismiss = { onEvent(EditPlantEvent.DismissStartDatePicker) },
            onConfirm = { selectedMillis -> onEvent(EditPlantEvent.ReminderStartDateSelected(selectedMillis)) },
        )
    }

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

        item { EditSectionHeader("Reminder schedule", "Adjust watering and fertilizing cadence.") }

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
                        "Update notes, photo, and care schedule.",
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
                value = uiState.notes,
                onValueChange = { onEvent(EditPlantEvent.NotesChanged(it)) },
                label = { Text("Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp),
                shape = RoundedCornerShape(18.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlantEnvironment.entries.forEach { environment ->
                    FilterChip(
                        selected = uiState.environment == environment,
                        onClick = { onEvent(EditPlantEvent.EnvironmentSelected(environment)) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (environment == PlantEnvironment.INDOOR) {
                                    Icons.Rounded.Home
                                } else {
                                    Icons.Rounded.Park
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        label = { Text(environment.label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderScheduleCard(
    reminder: EditReminderDraftUiModel,
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = reminder.everyDays,
                        onValueChange = { onEvent(EditPlantEvent.ReminderEveryDaysChanged(reminder.careType, it)) },
                        label = { Text("Every") },
                        suffix = { Text("days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = errorMessage != null,
                        shape = RoundedCornerShape(18.dp),
                    )
                    StartDateButton(
                        label = reminder.startDateLabel,
                        onClick = { onEvent(EditPlantEvent.ReminderStartDateClicked(reminder.careType)) },
                    )
                    NotificationTimeFields(
                        reminder = reminder,
                        onHourChanged = {
                            onEvent(EditPlantEvent.ReminderHourChanged(reminder.careType, it))
                        },
                        onMinuteChanged = {
                            onEvent(EditPlantEvent.ReminderMinuteChanged(reminder.careType, it))
                        },
                        onPeriodSelected = {
                            onEvent(EditPlantEvent.ReminderPeriodSelected(reminder.careType, it))
                        },
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
private fun NotificationTimeFields(
    reminder: EditReminderDraftUiModel,
    onHourChanged: (String) -> Unit,
    onMinuteChanged: (String) -> Unit,
    onPeriodSelected: (EditReminderPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Notification time",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = reminder.preferredHour,
                onValueChange = onHourChanged,
                label = { Text("Hour") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
            )
            OutlinedTextField(
                value = reminder.preferredMinute,
                onValueChange = onMinuteChanged,
                label = { Text("Minute") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditReminderPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = reminder.preferredPeriod == period,
                        onClick = { onPeriodSelected(period) },
                        label = { Text(period.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StartDateButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(
                        text = "Start date",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            TextButton(onClick = onClick) {
                Text("Choose")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartDatePickerDialog(
    initialSelectedDateMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit,
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialSelectedDateMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = { onConfirm(datePickerState.selectedDateMillis) }) {
                Text("Use date")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(state = datePickerState)
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
                reminders = listOf(
                    EditReminderDraftUiModel(
                        CareType.WATERING,
                        enabled = true,
                        everyDays = "5",
                        startDateMillis = 0L,
                        startDateLabel = "Today",
                        preferredHour = "9",
                        preferredMinute = "0",
                        preferredPeriod = EditReminderPeriod.AM,
                    ),
                    EditReminderDraftUiModel(
                        CareType.FERTILIZING,
                        enabled = false,
                        everyDays = "30",
                        startDateMillis = 0L,
                        startDateLabel = "Jun 25",
                        preferredHour = "9",
                        preferredMinute = "0",
                        preferredPeriod = EditReminderPeriod.AM,
                    ),
                ),
            ),
            onEvent = {},
        )
    }
}

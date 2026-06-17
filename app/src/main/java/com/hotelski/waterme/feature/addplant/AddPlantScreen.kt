package com.hotelski.waterme.feature.addplant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Spa
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.CareTypeBadge
import com.hotelski.waterme.feature.common.PlantPhotoTile
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeIconBadge
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMePremiumCard
import com.hotelski.waterme.feature.common.WaterMePrimaryButton
import com.hotelski.waterme.feature.common.WaterMeStatusChip
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.feature.common.label
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.model.PlantEnvironment
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.WaterMeTheme

data class AddPlantFieldErrors(
    val name: String? = null,
    val reminder: String? = null,
)

enum class AddPlantFrequencyOption(
    val label: String,
    val days: Int,
) {
    EVERYDAY("Every day", 1),
    EVERY_2_DAYS("Every 2 days", 2),
    EVERY_3_DAYS("Every 3 days", 3),
    EVERY_7_DAYS("Every 7 days", 7),
    EVERY_14_DAYS("Every 14 days", 14),
    EVERY_30_DAYS("Every 30 days", 30),
    CUSTOM("Custom", 0),
}

enum class AddPlantReminderTimeOption(
    val label: String,
    val hour: Int,
    val minute: Int,
) {
    MORNING("8:00 AM", 8, 0),
    LATE_MORNING("9:00 AM", 9, 0),
    EVENING("6:00 PM", 18, 0),
    NIGHT("8:00 PM", 20, 0),
    CUSTOM("Custom", -1, -1),
}

data class AddPlantReminderDraftUiModel(
    val careType: CareType,
    val enabled: Boolean,
    val frequency: AddPlantFrequencyOption,
    val customFrequencyDays: String = "",
    val reminderTime: AddPlantReminderTimeOption = AddPlantReminderTimeOption.LATE_MORNING,
    val customReminderHour: String = "",
    val customReminderMinute: String = "",
    val startDateMillis: Long,
    val startDateLabel: String,
)

data class AddPlantUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val name: String = "",
    val plantType: String = "Houseplant",
    val environment: PlantEnvironment = PlantEnvironment.INDOOR,
    val notes: String = "",
    val selectedPhotoUri: String? = null,
    val reminders: List<AddPlantReminderDraftUiModel> = emptyList(),
    val startDatePickerCareType: CareType? = null,
    val fieldErrors: AddPlantFieldErrors = AddPlantFieldErrors(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val createdPlantId: String? = null,
) {
    val canSave: Boolean
        get() = name.isNotBlank() && !isSaving && !isLoading
}

sealed interface AddPlantEvent {
    data object BackClicked : AddPlantEvent
    data object ChoosePhotoClicked : AddPlantEvent
    data object SaveClicked : AddPlantEvent
    data object RetryClicked : AddPlantEvent
    data object DismissStartDatePicker : AddPlantEvent
    data class NameChanged(val value: String) : AddPlantEvent
    data class EnvironmentSelected(val environment: PlantEnvironment) : AddPlantEvent
    data class NotesChanged(val value: String) : AddPlantEvent
    data class ReminderEnabledChanged(val careType: CareType, val enabled: Boolean) : AddPlantEvent
    data class FrequencySelected(val careType: CareType, val frequency: AddPlantFrequencyOption) : AddPlantEvent
    data class CustomFrequencyDaysChanged(val careType: CareType, val value: String) : AddPlantEvent
    data class ReminderTimeSelected(val careType: CareType, val time: AddPlantReminderTimeOption) : AddPlantEvent
    data class CustomReminderHourChanged(val careType: CareType, val value: String) : AddPlantEvent
    data class CustomReminderMinuteChanged(val careType: CareType, val value: String) : AddPlantEvent
    data class StartDateClicked(val careType: CareType) : AddPlantEvent
    data class StartDateSelected(val millis: Long?) : AddPlantEvent
}

@Composable
fun AddPlantScreen(
    uiState: AddPlantUiState,
    onEvent: (AddPlantEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            WaterMeTopBar(
                title = "New Plant",
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
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

    if (uiState.startDatePickerCareType != null) {
        StartDatePickerDialog(
            initialSelectedDateMillis = uiState.reminders
                .firstOrNull { it.careType == uiState.startDatePickerCareType }
                ?.startDateMillis ?: 0L,
            onDismiss = { onEvent(AddPlantEvent.DismissStartDatePicker) },
            onConfirm = { selectedMillis -> onEvent(AddPlantEvent.StartDateSelected(selectedMillis)) },
        )
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
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { BottomSheetHandle() }
        item {
            AddPlantHero(
                name = uiState.name,
                photoUri = uiState.selectedPhotoUri,
                onChoosePhoto = { onEvent(AddPlantEvent.ChoosePhotoClicked) },
            )
        }
        item {
            PlantNameCard(
                name = uiState.name,
                nameError = uiState.fieldErrors.name,
                onNameChanged = { onEvent(AddPlantEvent.NameChanged(it)) },
            )
        }
        item {
            PlantEnvironmentCard(
                selected = uiState.environment,
                onSelected = { onEvent(AddPlantEvent.EnvironmentSelected(it)) },
            )
        }
        item {
            ReminderCard(
                uiState = uiState,
                onEvent = onEvent,
            )
        }
        item {
            NotesCard(
                notes = uiState.notes,
                onNotesChanged = { onEvent(AddPlantEvent.NotesChanged(it)) },
            )
        }
        item {
            WaterMePrimaryButton(
                label = if (uiState.isSaving) "Saving..." else "Save plant",
                onClick = { onEvent(AddPlantEvent.SaveClicked) },
                enabled = uiState.canSave,
                icon = Icons.Rounded.Check,
            )
        }
    }
}

@Composable
private fun AddPlantHero(
    name: String,
    photoUri: String?,
    onChoosePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        shape = RoundedCornerShape(34.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Plant profile",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Create a calm care profile with reminders and notes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ImageUploadPanel(
                name = name.ifBlank { "New plant" },
                photoUri = photoUri,
                onChoosePhoto = onChoosePhoto,
            )
        }
    }
}

@Composable
private fun BottomSheetHandle(modifier: Modifier = Modifier) {
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
private fun ImageUploadPanel(
    name: String,
    photoUri: String?,
    onChoosePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlantPhotoTile(
                photoUri = photoUri,
                plantName = name,
                size = 98.dp,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WaterMeStatusChip(
                    label = if (photoUri == null) "Image upload" else "Photo ready",
                    color = LeafGreen,
                    icon = Icons.Rounded.PhotoCamera,
                )
                Text(
                    text = "Take or choose a photo for the plant card.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onChoosePhoto,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (photoUri == null) "Add photo" else "Change photo")
                }
            }
        }
    }
}

@Composable
private fun PlantNameCard(
    name: String,
    nameError: String?,
    onNameChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(
                icon = Icons.Rounded.LocalFlorist,
                title = "Plant name",
                subtitle = "Houseplant nickname or botanical name.",
            )
            OutlinedTextField(
                value = name,
                onValueChange = onNameChanged,
                label = { Text("Plant name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = nameError != null,
                supportingText = if (nameError == null) null else {
                    { Text(nameError) }
                },
                shape = RoundedCornerShape(18.dp),
            )
        }
    }
}

@Composable
private fun PlantEnvironmentCard(
    selected: PlantEnvironment,
    onSelected: (PlantEnvironment) -> Unit,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(
                icon = Icons.Rounded.Spa,
                title = "Plant category",
                subtitle = "Indoor or outdoor plant.",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlantEnvironment.entries.forEach { environment ->
                    FilterChip(
                        selected = selected == environment,
                        onClick = { onSelected(environment) },
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
private fun ReminderCard(
    uiState: AddPlantUiState,
    onEvent: (AddPlantEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionTitle(
                icon = Icons.Rounded.Notifications,
                title = "Care reminders",
                subtitle = "Set watering, fertilizing, or both.",
            )
            uiState.reminders.forEach { reminder ->
                AddPlantReminderScheduleCard(
                    reminder = reminder,
                    onEvent = onEvent,
                )
            }
            if (uiState.fieldErrors.reminder != null) {
                Text(
                    text = uiState.fieldErrors.reminder,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun AddPlantReminderScheduleCard(
    reminder: AddPlantReminderDraftUiModel,
    onEvent: (AddPlantEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (reminder.enabled) 0.34f else 0.16f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CareTypeBadge(reminder.careType, size = 42.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.careType.label(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (reminder.enabled) {
                            "Every ${reminder.frequency.displayDays(reminder.customFrequencyDays)} days"
                        } else {
                            "Off"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = reminder.enabled,
                    onCheckedChange = {
                        onEvent(AddPlantEvent.ReminderEnabledChanged(reminder.careType, it))
                    },
                )
            }

            if (reminder.enabled) {
                ChipRow(title = "Frequency") {
                    AddPlantFrequencyOption.entries.forEach { frequency ->
                        FilterChip(
                            selected = reminder.frequency == frequency,
                            onClick = { onEvent(AddPlantEvent.FrequencySelected(reminder.careType, frequency)) },
                            label = { Text(frequency.label) },
                        )
                    }
                }
                if (reminder.frequency == AddPlantFrequencyOption.CUSTOM) {
                    OutlinedTextField(
                        value = reminder.customFrequencyDays,
                        onValueChange = {
                            onEvent(AddPlantEvent.CustomFrequencyDaysChanged(reminder.careType, it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Every X days") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(18.dp),
                        suffix = { Text("days") },
                    )
                }
                ChipRow(title = "Notification time") {
                    AddPlantReminderTimeOption.entries.forEach { time ->
                        FilterChip(
                            selected = reminder.reminderTime == time,
                            onClick = { onEvent(AddPlantEvent.ReminderTimeSelected(reminder.careType, time)) },
                            label = { Text(time.label) },
                        )
                    }
                }
                if (reminder.reminderTime == AddPlantReminderTimeOption.CUSTOM) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = reminder.customReminderHour,
                            onValueChange = {
                                onEvent(AddPlantEvent.CustomReminderHourChanged(reminder.careType, it))
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Hour") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(18.dp),
                        )
                        OutlinedTextField(
                            value = reminder.customReminderMinute,
                            onValueChange = {
                                onEvent(AddPlantEvent.CustomReminderMinuteChanged(reminder.careType, it))
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Minute") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(18.dp),
                        )
                    }
                }
                StartDateButton(
                    label = reminder.startDateLabel,
                    onClick = { onEvent(AddPlantEvent.StartDateClicked(reminder.careType)) },
                )
            }
        }
    }
}

private fun AddPlantFrequencyOption.displayDays(customFrequencyDays: String): String =
    if (this == AddPlantFrequencyOption.CUSTOM) {
        customFrequencyDays.ifBlank { "?" }
    } else {
        days.toString()
    }

@Composable
private fun NotesCard(
    notes: String,
    onNotesChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(
                icon = Icons.Rounded.Spa,
                title = "Notes",
                subtitle = "Light, soil, yellow leaves, dry soil, or new growth.",
            )
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChanged,
                label = { Text("Plant notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                shape = RoundedCornerShape(18.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WaterMeIconBadge(icon = icon, size = 44.dp, color = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChipRow(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
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
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.44f),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Start date",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            TextButton(onClick = onClick) {
                Text("Change")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartDatePickerDialog(
    initialSelectedDateMillis: Long,
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

@Preview(showBackground = true)
@Composable
private fun AddPlantScreenPreview() {
    WaterMeTheme {
        AddPlantScreen(
            uiState = AddPlantUiState(
                name = "Calathea",
                notes = "Likes filtered light and consistent moisture.",
                reminders = listOf(
                    AddPlantReminderDraftUiModel(
                        careType = CareType.WATERING,
                        enabled = true,
                        frequency = AddPlantFrequencyOption.EVERY_3_DAYS,
                        startDateMillis = 0L,
                        startDateLabel = "Today",
                    ),
                    AddPlantReminderDraftUiModel(
                        careType = CareType.FERTILIZING,
                        enabled = true,
                        frequency = AddPlantFrequencyOption.EVERY_30_DAYS,
                        startDateMillis = 0L,
                        startDateLabel = "Jun 25",
                    ),
                ),
            ),
            onEvent = {},
        )
    }
}

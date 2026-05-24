package com.hotelski.waterme.feature.addplant

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Notifications
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
import com.hotelski.waterme.feature.common.WaterMeCard
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMePrimaryButton
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.feature.common.label
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.ui.theme.CardWhite
import com.hotelski.waterme.ui.theme.GardenBackground
import com.hotelski.waterme.ui.theme.Ink
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.MutedInk
import com.hotelski.waterme.ui.theme.SoftCream
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

data class AddPlantUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val name: String = "",
    val notes: String = "",
    val selectedPhotoUri: String? = null,
    val reminderCareType: CareType = CareType.WATERING,
    val frequency: AddPlantFrequencyOption = AddPlantFrequencyOption.EVERY_3_DAYS,
    val customFrequencyDays: String = "",
    val reminderTime: AddPlantReminderTimeOption = AddPlantReminderTimeOption.LATE_MORNING,
    val customReminderHour: String = "",
    val customReminderMinute: String = "",
    val startDateMillis: Long = 0L,
    val startDateLabel: String = "Today",
    val showStartDatePicker: Boolean = false,
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
    data object StartDateClicked : AddPlantEvent
    data object DismissStartDatePicker : AddPlantEvent
    data class NameChanged(val value: String) : AddPlantEvent
    data class NotesChanged(val value: String) : AddPlantEvent
    data class ReminderCareTypeSelected(val careType: CareType) : AddPlantEvent
    data class FrequencySelected(val frequency: AddPlantFrequencyOption) : AddPlantEvent
    data class CustomFrequencyDaysChanged(val value: String) : AddPlantEvent
    data class ReminderTimeSelected(val time: AddPlantReminderTimeOption) : AddPlantEvent
    data class CustomReminderHourChanged(val value: String) : AddPlantEvent
    data class CustomReminderMinuteChanged(val value: String) : AddPlantEvent
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
        containerColor = GardenBackground,
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

    if (uiState.showStartDatePicker) {
        StartDatePickerDialog(
            initialSelectedDateMillis = uiState.startDateMillis,
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
            .background(GardenBackground),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = SoftCream,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlantPhotoTile(
                photoUri = photoUri,
                plantName = name.ifBlank { "New plant" },
                size = 104.dp,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Plant profile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Text(
                    text = "A fresh care card for a leafy new companion.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedInk,
                )
                OutlinedButton(
                    onClick = onChoosePhoto,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (photoUri == null) "Choose photo" else "Change photo")
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
    WaterMeCard(modifier = modifier, containerColor = CardWhite) {
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
private fun ReminderCard(
    uiState: AddPlantUiState,
    onEvent: (AddPlantEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    WaterMeCard(modifier = modifier, containerColor = CardWhite) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionTitle(
                icon = Icons.Rounded.Notifications,
                title = "First reminder",
                subtitle = "Watering or fertilizing to start.",
            )
            ChipRow(title = "Care") {
                ReminderCareTypes.forEach { careType ->
                    FilterChip(
                        selected = uiState.reminderCareType == careType,
                        onClick = { onEvent(AddPlantEvent.ReminderCareTypeSelected(careType)) },
                        label = { Text(careType.label()) },
                        leadingIcon = if (uiState.reminderCareType == careType) {
                            { CareTypeBadge(careType, size = 28.dp) }
                        } else {
                            null
                        },
                    )
                }
            }
            ChipRow(title = "Frequency") {
                AddPlantFrequencyOption.entries.forEach { frequency ->
                    FilterChip(
                        selected = uiState.frequency == frequency,
                        onClick = { onEvent(AddPlantEvent.FrequencySelected(frequency)) },
                        label = { Text(frequency.label) },
                    )
                }
            }
            if (uiState.frequency == AddPlantFrequencyOption.CUSTOM) {
                OutlinedTextField(
                    value = uiState.customFrequencyDays,
                    onValueChange = { onEvent(AddPlantEvent.CustomFrequencyDaysChanged(it)) },
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
                        selected = uiState.reminderTime == time,
                        onClick = { onEvent(AddPlantEvent.ReminderTimeSelected(time)) },
                        label = { Text(time.label) },
                    )
                }
            }
            if (uiState.reminderTime == AddPlantReminderTimeOption.CUSTOM) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.customReminderHour,
                        onValueChange = { onEvent(AddPlantEvent.CustomReminderHourChanged(it)) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Hour") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(18.dp),
                    )
                    OutlinedTextField(
                        value = uiState.customReminderMinute,
                        onValueChange = { onEvent(AddPlantEvent.CustomReminderMinuteChanged(it)) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Minute") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(18.dp),
                    )
                }
            }
            StartDateButton(
                label = uiState.startDateLabel,
                onClick = { onEvent(AddPlantEvent.StartDateClicked) },
            )
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
private fun NotesCard(
    notes: String,
    onNotesChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    WaterMeCard(modifier = modifier, containerColor = CardWhite) {
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
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(LeafGreen.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = LeafGreen)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedInk,
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
            color = MutedInk,
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
        color = LeafGreen.copy(alpha = 0.10f),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Event, contentDescription = null, tint = LeafGreen)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Start date", style = MaterialTheme.typography.labelLarge, color = MutedInk)
                    Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
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

private val ReminderCareTypes = listOf(CareType.WATERING, CareType.FERTILIZING)

@Preview(showBackground = true)
@Composable
private fun AddPlantScreenPreview() {
    WaterMeTheme {
        AddPlantScreen(
            uiState = AddPlantUiState(
                name = "Calathea",
                notes = "Likes filtered light and consistent moisture.",
                startDateLabel = "Today",
            ),
            onEvent = {},
        )
    }
}

package com.hotelski.waterme.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.WaterMeCard
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.ui.theme.CardWhite
import com.hotelski.waterme.ui.theme.Clay
import com.hotelski.waterme.ui.theme.GardenBackground
import com.hotelski.waterme.ui.theme.Ink
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.MutedInk
import com.hotelski.waterme.ui.theme.SoftCream
import com.hotelski.waterme.ui.theme.WaterMeTheme

enum class SettingsThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class SettingsMeasurementUnits {
    METRIC,
    IMPERIAL,
}

data class SettingsUiState(
    val isLoading: Boolean = false,
    val profileName: String = "Plant keeper",
    val notificationsEnabled: Boolean = true,
    val notificationPermissionLabel: String = "Not requested",
    val defaultReminderHour: Int = 9,
    val defaultReminderMinute: Int = 0,
    val themePreference: SettingsThemePreference = SettingsThemePreference.SYSTEM,
    val darkModeEnabled: Boolean = false,
    val measurementUnits: SettingsMeasurementUnits = SettingsMeasurementUnits.METRIC,
    val backupSyncEnabled: Boolean = false,
    val backupProviderLabel: String = "Local only",
    val lastBackupLabel: String = "Never backed up",
    val lastRestoreLabel: String = "Never restored",
    val localOnlyMode: Boolean = true,
    val analyticsEnabled: Boolean = false,
    val diagnosticsEnabled: Boolean = false,
    val appVersion: String = "1.0 (1)",
    val plantCount: Int = 0,
    val activeReminderCount: Int = 0,
    val careHistoryCount: Int = 0,
    val healthNoteCount: Int = 0,
    val showDeleteAllDataConfirmation: Boolean = false,
    val isDeletingAllData: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

sealed interface SettingsEvent {
    data object RetryClicked : SettingsEvent
    data object ShowOnboardingClicked : SettingsEvent
    data object CharactersClicked : SettingsEvent
    data object RequestNotificationPermissionClicked : SettingsEvent
    data object BackupNowClicked : SettingsEvent
    data object RestoreBackupClicked : SettingsEvent
    data object DeleteAllDataClicked : SettingsEvent
    data object ConfirmDeleteAllDataClicked : SettingsEvent
    data object DismissDeleteAllDataClicked : SettingsEvent
    data class ProfileNameChanged(val value: String) : SettingsEvent
    data class NotificationsChanged(val enabled: Boolean) : SettingsEvent
    data class DefaultReminderTimeChanged(val hour: Int, val minute: Int) : SettingsEvent
    data class ThemePreferenceChanged(val preference: SettingsThemePreference) : SettingsEvent
    data class DarkModeChanged(val enabled: Boolean) : SettingsEvent
    data class MeasurementUnitsChanged(val units: SettingsMeasurementUnits) : SettingsEvent
    data class BackupSyncChanged(val enabled: Boolean) : SettingsEvent
    data class LocalOnlyModeChanged(val enabled: Boolean) : SettingsEvent
    data class AnalyticsChanged(val enabled: Boolean) : SettingsEvent
    data class DiagnosticsChanged(val enabled: Boolean) : SettingsEvent
}

private data class ReminderTimeOption(
    val label: String,
    val hour: Int,
    val minute: Int,
)

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.showDeleteAllDataConfirmation) {
        DeleteAllDataDialog(
            isDeleting = uiState.isDeletingAllData,
            onDismiss = { onEvent(SettingsEvent.DismissDeleteAllDataClicked) },
            onConfirm = { onEvent(SettingsEvent.ConfirmDeleteAllDataClicked) },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GardenBackground,
        topBar = { WaterMeTopBar(title = "Settings") },
    ) { innerPadding ->
        val blockingError = uiState.errorMessage
        when {
            uiState.isLoading -> WaterMeLoadingState("Loading settings...", Modifier.padding(innerPadding))
            blockingError != null && uiState.plantCount == 0 -> Box(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeErrorState(blockingError, onRetryClick = { onEvent(SettingsEvent.RetryClicked) })
            }

            else -> SettingsContent(
                uiState = uiState,
                onEvent = onEvent,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GardenBackground),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (uiState.errorMessage != null) {
            item {
                SettingsMessageCard(
                    title = "Settings need attention",
                    message = uiState.errorMessage,
                    color = Clay,
                    icon = Icons.Rounded.Info,
                )
            }
        }
        if (uiState.successMessage != null) {
            item {
                SettingsMessageCard(
                    title = "Saved",
                    message = uiState.successMessage,
                    color = LeafGreen,
                    icon = Icons.Rounded.Check,
                )
            }
        }

        item { ProfileSettingsCard(uiState, onEvent) }
        item { CharacterSettingsCard(onEvent) }
        item { NotificationSettingsCard(uiState, onEvent) }
        item { DefaultReminderTimesCard(uiState, onEvent) }
        item { AppearanceSettingsCard(uiState, onEvent) }
        item { MeasurementUnitsCard(uiState, onEvent) }
        item { BackupRestoreCard(uiState, onEvent) }
        item { PrivacySettingsCard(uiState, onEvent) }
        item { GardenStatsCard(uiState) }
        item { AboutAppCard(uiState, onEvent) }
        item { DeleteAllDataCard(uiState, onEvent) }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun ProfileSettingsCard(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    SettingsSectionCard(
        title = "Profile",
        subtitle = "Personalize your local plant care workspace.",
        icon = Icons.Rounded.AccountCircle,
    ) {
        OutlinedTextField(
            value = uiState.profileName,
            onValueChange = { onEvent(SettingsEvent.ProfileNameChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Profile name") },
            singleLine = true,
        )
        SettingsInfoRow("Profile type", "Local WaterMe profile")
    }
}

@Composable
private fun CharacterSettingsCard(
    onEvent: (SettingsEvent) -> Unit,
) {
    SettingsSectionCard(
        title = "Characters",
        subtitle = "Choose a plant companion and unlock new styles through care achievements.",
        icon = Icons.Rounded.LocalFlorist,
    ) {
        SettingsInfoRow("Unlocks", "Dynamic achievements")
        OutlinedButton(
            onClick = { onEvent(SettingsEvent.CharactersClicked) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(Icons.Rounded.LocalFlorist, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Open character garden")
        }
    }
}

@Composable
private fun NotificationSettingsCard(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    SettingsSectionCard(
        title = "Notifications",
        subtitle = "Control plant care reminders and permission status.",
        icon = Icons.Rounded.Notifications,
    ) {
        ToggleSettingRow(
            title = "Plant care reminders",
            description = "Send local alerts when care is due.",
            checked = uiState.notificationsEnabled,
            onCheckedChange = { onEvent(SettingsEvent.NotificationsChanged(it)) },
        )
        SettingsInfoRow("Permission", uiState.notificationPermissionLabel)
        OutlinedButton(
            onClick = { onEvent(SettingsEvent.RequestNotificationPermissionClicked) },
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(Icons.Rounded.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Review permission")
        }
    }
}

@Composable
private fun DefaultReminderTimesCard(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    SettingsSectionCard(
        title = "Default reminder time",
        subtitle = "Choose when new plant reminders start by default.",
        icon = Icons.Rounded.Schedule,
    ) {
        SettingsInfoRow("Current default", formatReminderTime(uiState.defaultReminderHour, uiState.defaultReminderMinute))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            reminderTimeOptions.forEach { option ->
                FilterChip(
                    selected = uiState.defaultReminderHour == option.hour && uiState.defaultReminderMinute == option.minute,
                    onClick = { onEvent(SettingsEvent.DefaultReminderTimeChanged(option.hour, option.minute)) },
                    label = { Text(option.label) },
                )
            }
        }
    }
}

@Composable
private fun AppearanceSettingsCard(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    SettingsSectionCard(
        title = "Appearance",
        subtitle = "Use system colors or keep WaterMe dark.",
        icon = Icons.Rounded.DarkMode,
    ) {
        ToggleSettingRow(
            title = "Dark mode",
            description = "Force a calm dark theme preference.",
            checked = uiState.darkModeEnabled,
            onCheckedChange = { onEvent(SettingsEvent.DarkModeChanged(it)) },
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsThemePreference.entries.forEach { preference ->
                FilterChip(
                    selected = uiState.themePreference == preference,
                    onClick = { onEvent(SettingsEvent.ThemePreferenceChanged(preference)) },
                    label = { Text(preference.displayLabel()) },
                )
            }
        }
    }
}

@Composable
private fun MeasurementUnitsCard(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    SettingsSectionCard(
        title = "Measurement units",
        subtitle = "Set units for future plant measurements.",
        icon = Icons.Rounded.Straighten,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsMeasurementUnits.entries.forEach { units ->
                FilterChip(
                    selected = uiState.measurementUnits == units,
                    onClick = { onEvent(SettingsEvent.MeasurementUnitsChanged(units)) },
                    label = { Text(units.displayLabel()) },
                )
            }
        }
    }
}

@Composable
private fun BackupRestoreCard(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    SettingsSectionCard(
        title = "Backup and restore",
        subtitle = "Prepare your local garden for backup workflows.",
        icon = Icons.Rounded.Backup,
    ) {
        ToggleSettingRow(
            title = "Backup sync",
            description = "Keep a backup preference ready for cloud sync.",
            checked = uiState.backupSyncEnabled,
            onCheckedChange = { onEvent(SettingsEvent.BackupSyncChanged(it)) },
        )
        SettingsInfoRow("Provider", uiState.backupProviderLabel)
        SettingsInfoRow("Last backup", uiState.lastBackupLabel)
        SettingsInfoRow("Last restore", uiState.lastRestoreLabel)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { onEvent(SettingsEvent.BackupNowClicked) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("Back up")
            }
            OutlinedButton(
                onClick = { onEvent(SettingsEvent.RestoreBackupClicked) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Restore")
            }
        }
    }
}

@Composable
private fun PrivacySettingsCard(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    SettingsSectionCard(
        title = "Privacy",
        subtitle = "Choose what stays local and what can help improve WaterMe.",
        icon = Icons.Rounded.PrivacyTip,
    ) {
        ToggleSettingRow(
            title = "Local-only mode",
            description = "Keep plant data on this device.",
            checked = uiState.localOnlyMode,
            onCheckedChange = { onEvent(SettingsEvent.LocalOnlyModeChanged(it)) },
        )
        ToggleSettingRow(
            title = "Usage analytics",
            description = "Allow anonymous app usage signals.",
            checked = uiState.analyticsEnabled,
            onCheckedChange = { onEvent(SettingsEvent.AnalyticsChanged(it)) },
        )
        ToggleSettingRow(
            title = "Diagnostics",
            description = "Share crash and reliability diagnostics.",
            checked = uiState.diagnosticsEnabled,
            onCheckedChange = { onEvent(SettingsEvent.DiagnosticsChanged(it)) },
        )
    }
}

@Composable
private fun GardenStatsCard(uiState: SettingsUiState) {
    SettingsSectionCard(
        title = "Garden stats",
        subtitle = "A quick look at local WaterMe data.",
        icon = Icons.Rounded.Spa,
    ) {
        if (uiState.plantCount == 0 && uiState.activeReminderCount == 0 && uiState.careHistoryCount == 0) {
            InlineEmptyState(
                title = "No garden data",
                message = "Add plants and care tasks to see local stats here.",
                icon = Icons.Rounded.Spa,
            )
        } else {
            SettingsInfoRow("Plants tracked", uiState.plantCount.toString())
            SettingsInfoRow("Active reminders", uiState.activeReminderCount.toString())
            SettingsInfoRow("Care history entries", uiState.careHistoryCount.toString())
            SettingsInfoRow("Health notes", uiState.healthNoteCount.toString())
        }
    }
}

@Composable
private fun AboutAppCard(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    SettingsSectionCard(
        title = "About WaterMe",
        subtitle = "App details and onboarding.",
        icon = Icons.Rounded.Info,
    ) {
        SettingsInfoRow("App version", uiState.appVersion)
        SettingsInfoRow("Storage", "Room database + Preferences DataStore")
        OutlinedButton(
            onClick = { onEvent(SettingsEvent.ShowOnboardingClicked) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("Show onboarding again")
        }
    }
}

@Composable
private fun DeleteAllDataCard(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    WaterMeCard(containerColor = Color(0xFFFFF4ED)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = Clay)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Delete all data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Remove plants, reminders, care history, photos, and saved settings from this device.", color = MutedInk)
                }
            }
            OutlinedButton(
                onClick = { onEvent(SettingsEvent.DeleteAllDataClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isDeletingAllData,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Clay),
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (uiState.isDeletingAllData) "Deleting..." else "Delete all data")
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    WaterMeCard(containerColor = CardWhite) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(LeafGreen.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = LeafGreen)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MutedInk)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
private fun InlineEmptyState(
    title: String,
    message: String,
    icon: ImageVector,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SoftCream, RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = LeafGreen)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Ink, fontWeight = FontWeight.Bold)
            Text(message, color = MutedInk, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Ink, fontWeight = FontWeight.SemiBold)
            Text(description, color = MutedInk, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SettingsInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = MutedInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            color = Ink,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SettingsMessageCard(
    title: String,
    message: String,
    color: Color,
    icon: ImageVector,
) {
    WaterMeCard(containerColor = color.copy(alpha = 0.1f)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Ink)
                Text(message, style = MaterialTheme.typography.bodySmall, color = MutedInk)
            }
        }
    }
}

@Composable
private fun DeleteAllDataDialog(
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = Clay) },
        title = { Text("Delete all WaterMe data?") },
        text = { Text("This removes local plants, reminders, care history, plant photos, and saved preferences from this device.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = Clay),
            ) {
                Text(if (isDeleting) "Deleting..." else "Delete all data")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting,
            ) {
                Text("Cancel")
            }
        },
    )
}

private fun SettingsThemePreference.displayLabel(): String =
    when (this) {
        SettingsThemePreference.SYSTEM -> "System"
        SettingsThemePreference.LIGHT -> "Light"
        SettingsThemePreference.DARK -> "Dark"
    }

private fun SettingsMeasurementUnits.displayLabel(): String =
    when (this) {
        SettingsMeasurementUnits.METRIC -> "Metric"
        SettingsMeasurementUnits.IMPERIAL -> "Imperial"
    }

private fun formatReminderTime(hour: Int, minute: Int): String {
    val suffix = if (hour < 12) "AM" else "PM"
    val hour12 = when (val normalizedHour = hour % 12) {
        0 -> 12
        else -> normalizedHour
    }
    return "$hour12:${minute.toString().padStart(2, '0')} $suffix"
}

private val reminderTimeOptions = listOf(
    ReminderTimeOption("8:00 AM", 8, 0),
    ReminderTimeOption("9:00 AM", 9, 0),
    ReminderTimeOption("10:00 AM", 10, 0),
    ReminderTimeOption("6:00 PM", 18, 0),
)

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    WaterMeTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                profileName = "Ivan",
                notificationsEnabled = true,
                notificationPermissionLabel = "Granted",
                defaultReminderHour = 9,
                defaultReminderMinute = 0,
                backupSyncEnabled = true,
                backupProviderLabel = "Google Drive",
                lastBackupLabel = "May 24, 9:00 AM",
                localOnlyMode = true,
                plantCount = 12,
                activeReminderCount = 31,
                careHistoryCount = 94,
                healthNoteCount = 18,
                appVersion = "1.0 (1)",
            ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsLoadingPreview() {
    WaterMeTheme {
        SettingsScreen(
            uiState = SettingsUiState(isLoading = true),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsEmptyPreview() {
    WaterMeTheme {
        SettingsScreen(
            uiState = SettingsUiState(profileName = "Plant keeper"),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsDeleteDialogPreview() {
    WaterMeTheme {
        SettingsScreen(
            uiState = SettingsUiState(showDeleteAllDataConfirmation = true),
            onEvent = {},
        )
    }
}

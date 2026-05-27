package com.hotelski.waterme.feature.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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


data class SettingsUiState(
    val isLoading: Boolean = false,
    val profileName: String = "Plant keeper",
    val selectedCharacterName: String = "Sprout",
    val notificationsEnabled: Boolean = true,
    val notificationPermissionLabel: String = "Not requested",
    val defaultReminderHour: Int = 9,
    val defaultReminderMinute: Int = 0,
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
    data object DeleteAllDataClicked : SettingsEvent
    data object ConfirmDeleteAllDataClicked : SettingsEvent
    data object DismissDeleteAllDataClicked : SettingsEvent
    data class NotificationsChanged(val enabled: Boolean) : SettingsEvent
}


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
        item { CharacterSettingsCard(uiState, onEvent) }
        item { NotificationSettingsCard(uiState, onEvent) }
        item { GardenStatsCard(uiState) }
        item { AboutAppCard(uiState, onEvent) }
        item { DeleteAllDataCard(uiState, onEvent) }
        item { Spacer(Modifier.height(72.dp)) }
    }
}


@Composable
private fun CharacterSettingsCard(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    SettingsSectionCard(
        title = "Characters",
        subtitle = "Choose a plant companion and unlock new styles through care achievements.",
        icon = Icons.Rounded.LocalFlorist,
    ) {
        SettingsInfoRow("Selected character", uiState.selectedCharacterName)
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
        // SettingsInfoRow("Permission", uiState.notificationPermissionLabel)
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

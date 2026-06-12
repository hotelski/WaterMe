package com.hotelski.waterme.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.hotelski.waterme.data.local.entity.ThemePreference
import com.hotelski.waterme.data.preferences.TextColorPreference
import com.hotelski.waterme.feature.characters.PlantCharacterCelebrationCard
import com.hotelski.waterme.feature.characters.PlantCharacterUiModel
import com.hotelski.waterme.feature.common.WaterMeCard
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeLeafRefreshBox
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.feature.legal.LegalDocument
import com.hotelski.waterme.ui.theme.Clay
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.WaterMeTheme


data class SettingsUiState(
    val isLoading: Boolean = false,
    val profileName: String = "Plant keeper",
    val selectedCharacterName: String = "Sprout",
    val activeCharacter: PlantCharacterUiModel? = null,
    val notificationsEnabled: Boolean = true,
    val notificationPermissionLabel: String = "Not requested",
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val textColorPreference: TextColorPreference = TextColorPreference.FOREST,
    val defaultReminderHour: Int = 9,
    val defaultReminderMinute: Int = 0,
    val appVersion: String = "1.0 (1)",
    val plantCount: Int = 0,
    val showDeleteAllDataConfirmation: Boolean = false,
    val isDeletingAllData: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

sealed interface SettingsEvent {
    data object RetryClicked : SettingsEvent
    data object RefreshPulled : SettingsEvent
    data object FeedbackClicked : SettingsEvent
    data object CharactersClicked : SettingsEvent
    data object RequestNotificationPermissionClicked : SettingsEvent
    data object DeleteAllDataClicked : SettingsEvent
    data object ConfirmDeleteAllDataClicked : SettingsEvent
    data object DismissDeleteAllDataClicked : SettingsEvent
    data class LegalDocumentClicked(val document: LegalDocument) : SettingsEvent
    data class NotificationsChanged(val enabled: Boolean) : SettingsEvent
    data object ColorSchemeResetClicked : SettingsEvent
    data class ThemePreferenceChanged(val value: ThemePreference) : SettingsEvent
    data class TextColorPreferenceChanged(val value: TextColorPreference) : SettingsEvent
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { WaterMeTopBar(title = "Settings") },
    ) { innerPadding ->
        val blockingError = uiState.errorMessage
        when {
            uiState.isLoading -> WaterMeLoadingState("Loading settings...", Modifier.padding(innerPadding))
            blockingError != null && uiState.plantCount == 0 -> Box(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeErrorState(blockingError, onRetryClick = { onEvent(SettingsEvent.RetryClicked) })
            }

            else -> WaterMeLeafRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { onEvent(SettingsEvent.RefreshPulled) },
                modifier = Modifier.padding(innerPadding),
            ) {
                SettingsContent(
                    uiState = uiState,
                    onEvent = onEvent,
                )
            }
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
            .background(MaterialTheme.colorScheme.background),
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
                if (uiState.activeCharacter != null) {
                    PlantCharacterCelebrationCard(
                        character = uiState.activeCharacter,
                        message = uiState.successMessage,
                    )
                } else {
                    SettingsMessageCard(
                        title = "Saved",
                        message = uiState.successMessage,
                        color = LeafGreen,
                        icon = Icons.Rounded.Check,
                    )
                }
            }
        }
        item { CharacterSettingsCard(uiState, onEvent) }
        item { ColorSchemeSettingsCard(uiState, onEvent) }
        item { NotificationSettingsCard(uiState, onEvent) }
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
private fun ColorSchemeSettingsCard(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    SettingsSectionCard(
        title = "Color Scheme",
        subtitle = "Switch between day and night mode, then tune the text tone.",
        icon = Icons.Rounded.Palette,
    ) {
        Text("Mode", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ModeChoiceChip(
                label = "Reset",
                icon = Icons.Rounded.RestartAlt,
                selected = uiState.themePreference == ThemePreference.SYSTEM &&
                    uiState.textColorPreference == TextColorPreference.FOREST,
                onClick = { onEvent(SettingsEvent.ColorSchemeResetClicked) },
                modifier = Modifier.weight(1f),
            )
            ModeChoiceChip(
                label = "Day",
                icon = Icons.Rounded.LightMode,
                selected = uiState.themePreference == ThemePreference.LIGHT,
                onClick = { onEvent(SettingsEvent.ThemePreferenceChanged(ThemePreference.LIGHT)) },
                modifier = Modifier.weight(1f),
            )
            ModeChoiceChip(
                label = "Night",
                icon = Icons.Rounded.DarkMode,
                selected = uiState.themePreference == ThemePreference.DARK,
                onClick = { onEvent(SettingsEvent.ThemePreferenceChanged(ThemePreference.DARK)) },
                modifier = Modifier.weight(1f),
            )
        }

        Text("Text color", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextColorPreference.entries.forEach { preference ->
                TextColorCircleButton(
                    preference = preference,
                    selected = uiState.textColorPreference == preference,
                    onClick = { onEvent(SettingsEvent.TextColorPreferenceChanged(preference)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ModeChoiceChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun TextColorCircleButton(
    preference: TextColorPreference,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(26.dp),
            shape = CircleShape,
            color = preference.previewColor(),
            border = BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = preference.settingsLabel(),
                        modifier = Modifier.size(14.dp),
                        tint = preference.selectionTint(),
                    )
                }
            }
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
private fun AboutAppCard(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    SettingsSectionCard(
        title = "About WaterMe",
        subtitle = "App details and ways to share feedback.",
        icon = Icons.Rounded.Info,
    ) {
        SettingsInfoRow("App version", uiState.appVersion)
        SettingsInfoRow("Data", "Stored locally on this device")
        SettingsInfoRow("Tracking", "No ads or selling data")
        SettingsInfoRow("Notifications", "Optional local reminders")
        LegalDocumentButton(
            title = "Terms of use",
            subtitle = "Use, responsibility, and limitations",
            icon = Icons.Rounded.Gavel,
            onClick = { onEvent(SettingsEvent.LegalDocumentClicked(LegalDocument.Terms)) },
        )
        LegalDocumentButton(
            title = "Privacy Policy",
            subtitle = "Local data, photos, feedback, and deletion",
            icon = Icons.Rounded.Policy,
            onClick = { onEvent(SettingsEvent.LegalDocumentClicked(LegalDocument.Privacy)) },
        )
        OutlinedButton(
            onClick = { onEvent(SettingsEvent.FeedbackClicked) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(Icons.Rounded.RateReview, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Send feedback")
        }
    }
}

@Composable
private fun LegalDocumentButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = LeafGreen)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DeleteAllDataCard(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    WaterMeCard(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.42f)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Delete all data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        "Remove plants, reminders, care history, photos, and saved settings from this device.",
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.78f),
                    )
                }
            }
            OutlinedButton(
                onClick = { onEvent(SettingsEvent.DeleteAllDataClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isDeletingAllData,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                    disabledContentColor = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.58f),
                ),
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
    WaterMeCard(containerColor = MaterialTheme.colorScheme.surface) {
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
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
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
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(0.42f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            modifier = Modifier.weight(0.58f),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
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
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun TextColorPreference.settingsLabel(): String =
    when (this) {
        TextColorPreference.FOREST -> "Forest"
        TextColorPreference.MINT -> "Mint"
        TextColorPreference.BLUE -> "Blue"
        TextColorPreference.SKY -> "Sky"
        TextColorPreference.CLAY -> "Clay"
        TextColorPreference.AMBER -> "Amber"
        TextColorPreference.ROSE -> "Rose"
        TextColorPreference.LAVENDER -> "Lavender"
        TextColorPreference.SLATE -> "Slate"
        TextColorPreference.HIGH_CONTRAST -> "Contrast"
    }

private fun TextColorPreference.previewColor(): Color =
    when (this) {
        TextColorPreference.FOREST -> LeafGreen
        TextColorPreference.MINT -> Color(0xFF56A86D)
        TextColorPreference.BLUE -> Color(0xFF1F5E78)
        TextColorPreference.SKY -> Color(0xFF3D89B4)
        TextColorPreference.CLAY -> Color(0xFF6B4A16)
        TextColorPreference.AMBER -> Color(0xFFA06A08)
        TextColorPreference.ROSE -> Color(0xFF9C4357)
        TextColorPreference.LAVENDER -> Color(0xFF7158A6)
        TextColorPreference.SLATE -> Color(0xFF33424B)
        TextColorPreference.HIGH_CONTRAST -> Color.Black
    }

private fun TextColorPreference.selectionTint(): Color =
    when (this) {
        TextColorPreference.FOREST,
        TextColorPreference.MINT,
        TextColorPreference.SKY,
        TextColorPreference.AMBER -> Color(0xFF102816)
        else -> Color.White
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

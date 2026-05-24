package com.hotelski.waterme.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.WaterMeCard
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMeSectionHeader
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.ui.theme.GardenBackground
import com.hotelski.waterme.ui.theme.Ink
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.MutedInk
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
    val notificationsEnabled: Boolean = true,
    val notificationPermissionLabel: String = "Granted",
    val themePreference: SettingsThemePreference = SettingsThemePreference.SYSTEM,
    val measurementUnits: SettingsMeasurementUnits = SettingsMeasurementUnits.METRIC,
    val backupSyncEnabled: Boolean = false,
    val plantCount: Int = 0,
    val activeReminderCount: Int = 0,
    val careHistoryCount: Int = 0,
    val healthNoteCount: Int = 0,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

sealed interface SettingsEvent {
    data object RetryClicked : SettingsEvent
    data object ShowOnboardingClicked : SettingsEvent
    data object RequestNotificationPermissionClicked : SettingsEvent
    data class NotificationsChanged(val enabled: Boolean) : SettingsEvent
    data class ThemePreferenceChanged(val preference: SettingsThemePreference) : SettingsEvent
    data class MeasurementUnitsChanged(val units: SettingsMeasurementUnits) : SettingsEvent
    data class BackupSyncChanged(val enabled: Boolean) : SettingsEvent
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GardenBackground,
        topBar = { WaterMeTopBar(title = "Settings") },
    ) { innerPadding ->
        when {
            uiState.isLoading -> WaterMeLoadingState("Loading settings...", Modifier.padding(innerPadding))
            uiState.errorMessage != null -> Box(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeErrorState(uiState.errorMessage, onRetryClick = { onEvent(SettingsEvent.RetryClicked) })
            }

            else -> LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(GardenBackground),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    WaterMeCard {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Notifications, contentDescription = null, tint = LeafGreen)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Plant care reminders", fontWeight = FontWeight.Bold, color = Ink)
                                    Text("WaterMe schedules local notifications around 9:00 AM.", color = MutedInk)
                                }
                                Switch(
                                    checked = uiState.notificationsEnabled,
                                    onCheckedChange = { onEvent(SettingsEvent.NotificationsChanged(it)) },
                                )
                            }
                            SettingRow("Permission", uiState.notificationPermissionLabel)
                            OutlinedButton(onClick = { onEvent(SettingsEvent.RequestNotificationPermissionClicked) }) {
                                Text("Review notification permission")
                            }
                        }
                    }
                }

                item {
                    WaterMeCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            WaterMeSectionHeader("Appearance")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                }

                item {
                    WaterMeCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            WaterMeSectionHeader("Units")
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
                }

                item {
                    WaterMeCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Settings, contentDescription = null, tint = LeafGreen)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Backup and sync", fontWeight = FontWeight.Bold, color = Ink)
                                    Text("Keep plant data ready for a future cloud backup.", color = MutedInk)
                                }
                                Switch(
                                    checked = uiState.backupSyncEnabled,
                                    onCheckedChange = { onEvent(SettingsEvent.BackupSyncChanged(it)) },
                                )
                            }
                        }
                    }
                }

                item {
                    WaterMeCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            WaterMeSectionHeader("Garden stats")
                            SettingRow("Plants tracked", uiState.plantCount.toString())
                            SettingRow("Active reminders", uiState.activeReminderCount.toString())
                            SettingRow("Care history entries", uiState.careHistoryCount.toString())
                            SettingRow("Health notes", uiState.healthNoteCount.toString())
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { onEvent(SettingsEvent.ShowOnboardingClicked) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Show onboarding again")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MutedInk)
        Text(value, color = Ink, fontWeight = FontWeight.SemiBold)
    }
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

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    WaterMeTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                plantCount = 12,
                activeReminderCount = 31,
                careHistoryCount = 94,
                healthNoteCount = 18,
            ),
            onEvent = {},
        )
    }
}

package com.hotelski.waterme.feature.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.WaterMeCard
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.WaterMeTheme

enum class LegalDocument(
    val routeValue: String,
    val title: String,
    val icon: ImageVector,
) {
    Terms("terms", "Terms of use", Icons.Rounded.Gavel),
    Privacy("privacy", "Privacy Policy", Icons.Rounded.Policy);

    companion object {
        fun fromRouteValue(value: String?): LegalDocument =
            entries.firstOrNull { it.routeValue == value } ?: Terms
    }
}

@Composable
fun LegalScreen(
    document: LegalDocument,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = document.content()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            WaterMeTopBar(
                title = document.title,
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                navigationContentDescription = "Back",
                onNavigationClick = onBack,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                WaterMeCard(containerColor = MaterialTheme.colorScheme.surface) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(document.icon, contentDescription = null, tint = LeafGreen)
                        Text(
                            text = document.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Last updated: June 3, 2026",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            content.sections.forEach { section ->
                item {
                    LegalSectionCard(section)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun LegalSectionCard(section: LegalSection) {
    WaterMeCard(containerColor = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            section.body.forEach { paragraph ->
                Text(
                    text = paragraph,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class LegalContent(
    val sections: List<LegalSection>,
)

private data class LegalSection(
    val title: String,
    val body: List<String>,
)

private fun LegalDocument.content(): LegalContent =
    when (this) {
        LegalDocument.Terms -> termsContent()
        LegalDocument.Privacy -> privacyContent()
    }

private fun termsContent(): LegalContent =
    LegalContent(
        sections = listOf(
            LegalSection(
                title = "Use of WaterMe",
                body = listOf(
                    "WaterMe helps you track plants, reminders, photos, notes, and care history. The app is intended for personal plant-care organization.",
                    "Plant care suggestions and reminders are informational only. You are responsible for checking each plant's actual needs, environment, and safety.",
                ),
            ),
            LegalSection(
                title = "Your content",
                body = listOf(
                    "You control the plant names, photos, notes, reminder schedules, and care logs you add to the app.",
                    "Do not add content you do not have the right to store or share through external apps.",
                ),
            ),
            LegalSection(
                title = "Notifications and reminders",
                body = listOf(
                    "Reminders depend on your device settings, notification permissions, time zone, and operating system behavior.",
                    "WaterMe cannot guarantee that a reminder will always arrive at the exact expected time.",
                ),
            ),
            LegalSection(
                title = "Feedback and support",
                body = listOf(
                    "If you send feedback, your device may open an email or sharing app. Anything sent through that external app is handled by that app and service.",
                    "Optional creator support or donation features, when available, are voluntary and do not change your right to use the app features already available.",
                ),
            ),
            LegalSection(
                title = "No warranty",
                body = listOf(
                    "WaterMe is provided as is, without guarantees that it will be uninterrupted, error-free, or suitable for every plant-care situation.",
                    "To the extent allowed by law, WaterMe is not liable for plant damage, missed care, data loss, or indirect losses from using the app.",
                ),
            ),
            LegalSection(
                title = "Changes and contact",
                body = listOf(
                    "These terms may be updated when the app changes. Continued use of WaterMe means you accept the updated terms.",
                    "Questions can be sent through Settings > About WaterMe > Send feedback.",
                ),
            ),
        ),
    )

private fun privacyContent(): LegalContent =
    LegalContent(
        sections = listOf(
            LegalSection(
                title = "Data stored by the app",
                body = listOf(
                    "WaterMe stores plant names, environments, photos you attach, notes, reminders, care tasks, care history, preferences, and character progress.",
                    "This data is stored locally on your device using the app database and preferences storage.",
                ),
            ),
            LegalSection(
                title = "No account, ads, or sale of data",
                body = listOf(
                    "WaterMe does not require an account for local use.",
                    "The app does not sell your data and does not use advertising identifiers for ads.",
                ),
            ),
            LegalSection(
                title = "Photos and files",
                body = listOf(
                    "When you choose a plant photo, WaterMe keeps a local reference that lets the app show that image.",
                    "Only select photos you are comfortable storing on this device inside the app experience.",
                ),
            ),
            LegalSection(
                title = "Notifications",
                body = listOf(
                    "If you enable notifications, WaterMe uses local notification permissions to remind you about care tasks.",
                    "You can disable notifications in Settings or through your device settings.",
                ),
            ),
            LegalSection(
                title = "Feedback messages",
                body = listOf(
                    "When you send feedback, the message may include the name, email, topic, and text you entered.",
                    "Feedback is sent through the email or sharing app you choose, so that external service may process the message under its own policy.",
                ),
            ),
            LegalSection(
                title = "Delete data",
                body = listOf(
                    "You can remove local app data from Settings > Delete all data.",
                    "Deleting the app or clearing app storage through the operating system may also remove locally stored WaterMe data.",
                ),
            ),
            LegalSection(
                title = "Children and changes",
                body = listOf(
                    "WaterMe is not designed to collect personal data from children.",
                    "This policy may be updated when features change. The latest version is shown in this screen.",
                ),
            ),
        ),
    )

@Preview(showBackground = true)
@Composable
private fun LegalScreenPreview() {
    WaterMeTheme {
        LegalScreen(
            document = LegalDocument.Privacy,
            onBack = {},
        )
    }
}

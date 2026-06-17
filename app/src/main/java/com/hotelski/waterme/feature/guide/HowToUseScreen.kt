package com.hotelski.waterme.feature.guide

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.WaterMeIconBadge
import com.hotelski.waterme.feature.common.WaterMePremiumCard
import com.hotelski.waterme.feature.common.WaterMeStatusChip
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.ui.theme.Clay
import com.hotelski.waterme.ui.theme.FreshGreen
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.MistBlue
import com.hotelski.waterme.ui.theme.WaterMeTheme

@Composable
fun HowToUseScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            WaterMeTopBar(
                title = "How it works",
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                navigationContentDescription = "Back",
                onNavigationClick = onBack,
            )
        },
    ) { innerPadding ->
        HowToUseContent(
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun HowToUseContent(
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { GuideHeroCard() }
        item { SmartToolsCard() }
        item { DailyRhythmCard() }
        item { RecordsCard() }
        item { QuickTipsCard() }
    }
}

@Composable
private fun GuideHeroCard() {
    WaterMePremiumCard(
        containerColor = MaterialTheme.colorScheme.surface,
        accentColor = LeafGreen,
        shape = RoundedCornerShape(32.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WaterMeStatusChip(
                    label = "Care hub",
                    color = LeafGreen,
                    icon = Icons.Rounded.LocalFlorist,
                )
                Text(
                    text = "WaterMe keeps plant care simple",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Add plants, scan unknown leaves, get AI care guidance, and keep reminders in one quiet daily flow.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box(
                modifier = Modifier
                    .size(108.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                LeafGreen.copy(alpha = 0.18f),
                                MistBlue.copy(alpha = 0.18f),
                                Clay.copy(alpha = 0.12f),
                            ),
                        ),
                        shape = RoundedCornerShape(30.dp),
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                SmartGuideIllustration(modifier = Modifier.size(88.dp))
            }
        }
    }
}

@Composable
private fun SmartToolsCard() {
    WaterMePremiumCard(
        containerColor = MaterialTheme.colorScheme.surface,
        accentColor = MistBlue,
        shape = RoundedCornerShape(30.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GuideSectionHeader(
                icon = Icons.Rounded.AutoAwesome,
                color = MistBlue,
                title = "Smart tools",
                subtitle = "Use these when you need help identifying or caring for a plant.",
            )

            GuideFeatureCard(
                icon = Icons.Rounded.PhotoCamera,
                color = MistBlue,
                title = "Plant Scanner",
                description = "Take a photo or choose one from the gallery, crop the exact area, and WaterMe shows the most likely plant match.",
                bullets = listOf(
                    "Great for unknown plants",
                    "Uses the selected crop for analysis",
                    "Save the result to My Plants",
                ),
            )

            GuideFeatureCard(
                icon = Icons.Rounded.TipsAndUpdates,
                color = LeafGreen,
                title = "AI Care",
                description = "Choose a saved plant or type a temporary name to get a care profile with water, light, humidity, toxicity, and origin info.",
                bullets = listOf(
                    "Works with saved or temporary plants",
                    "Can suggest reminders and notes",
                    "Supports short follow-up questions",
                ),
            )
        }
    }
}

@Composable
private fun DailyRhythmCard() {
    val steps = rememberDailyGuideSteps()
    WaterMePremiumCard(
        containerColor = MaterialTheme.colorScheme.surface,
        accentColor = LeafGreen,
        shape = RoundedCornerShape(30.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GuideSectionHeader(
                icon = Icons.Rounded.WaterDrop,
                color = LeafGreen,
                title = "Daily rhythm",
                subtitle = "A small routine keeps the app useful without feeling busy.",
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                steps.forEachIndexed { index, step ->
                    GuideStepRow(
                        number = index + 1,
                        step = step,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordsCard() {
    WaterMePremiumCard(
        containerColor = MaterialTheme.colorScheme.surface,
        accentColor = Clay,
        shape = RoundedCornerShape(30.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GuideSectionHeader(
                icon = Icons.Rounded.History,
                color = Clay,
                title = "Plant memory",
                subtitle = "WaterMe becomes more useful as you keep small records.",
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GuideMiniCard(
                    title = "Photos",
                    text = "Keep visual progress.",
                    icon = Icons.Rounded.PhotoCamera,
                    color = MistBlue,
                    modifier = Modifier.weight(1f),
                )
                GuideMiniCard(
                    title = "History",
                    text = "Review care actions.",
                    icon = Icons.Rounded.History,
                    color = Clay,
                    modifier = Modifier.weight(1f),
                )
            }
            GuideMiniCard(
                title = "Notes",
                text = "Save observations from AI Care, repotting, pests, growth, or anything you notice.",
                icon = Icons.Rounded.Check,
                color = LeafGreen,
            )
        }
    }
}

@Composable
private fun QuickTipsCard() {
    WaterMePremiumCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
        accentColor = FreshGreen,
        shape = RoundedCornerShape(30.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GuideSectionHeader(
                icon = Icons.Rounded.LocalFlorist,
                color = FreshGreen,
                title = "Best results",
                subtitle = "Small habits make scanner matches and AI advice more useful.",
            )
            GuideTipRow("Use clear photos with one plant filling most of the frame.")
            GuideTipRow("Crop the leaf, flower, or stem area before scanning.")
            GuideTipRow("For AI Care, use the common name or scientific name when you know it.")
            GuideTipRow("Always check the real plant condition before following any AI suggestion.")
        }
    }
}

@Composable
private fun GuideSectionHeader(
    icon: ImageVector,
    color: Color,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WaterMeIconBadge(
            icon = icon,
            color = color,
            size = 46.dp,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GuideFeatureCard(
    icon: ImageVector,
    color: Color,
    title: String,
    description: String,
    bullets: List<String>,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        color.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f),
                    ),
                ),
                shape,
            )
            .border(1.dp, color.copy(alpha = 0.18f), shape)
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WaterMeIconBadge(icon = icon, color = color, size = 48.dp)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                bullets.forEach { bullet ->
                    GuideBullet(text = bullet, color = color)
                }
            }
        }
    }
}

@Composable
private fun GuideStepRow(
    number: Int,
    step: GuideStep,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(step.color.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
            .border(1.dp, step.color.copy(alpha = 0.13f), RoundedCornerShape(22.dp))
            .padding(13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f), RoundedCornerShape(16.dp))
                .border(1.dp, step.color.copy(alpha = 0.16f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = step.color,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = step.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = step.icon,
            contentDescription = null,
            tint = step.color,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun GuideMiniCard(
    title: String,
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.09f), RoundedCornerShape(22.dp))
            .border(1.dp, color.copy(alpha = 0.14f), RoundedCornerShape(22.dp))
            .padding(13.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            WaterMeIconBadge(icon = icon, color = color, size = 38.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GuideBullet(
    text: String,
    color: Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .background(color, RoundedCornerShape(999.dp)),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GuideTipRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = LeafGreen,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SmartGuideIllustration(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "GuideSmartMotion")
    val orbit by transition.animateFloat(
        initialValue = -18f,
        targetValue = 342f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "GuideOrbit",
    )
    val scanSweep by transition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.84f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "GuideScanSweep",
    )
    val sparkle by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "GuideSparkle",
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width * 0.50f, size.height * 0.50f)
        val badgeTopLeft = Offset(size.width * 0.12f, size.height * 0.12f)
        val badgeSize = Size(size.width * 0.76f, size.height * 0.76f)
        val badgeRadius = 26.dp.toPx()

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.44f),
                    LeafGreen.copy(alpha = 0.12f),
                    MistBlue.copy(alpha = 0.18f),
                ),
            ),
            topLeft = badgeTopLeft,
            size = badgeSize,
            cornerRadius = CornerRadius(badgeRadius, badgeRadius),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.72f),
            topLeft = Offset(size.width * 0.23f, size.height * 0.24f),
            size = Size(size.width * 0.54f, size.height * 0.52f),
            cornerRadius = CornerRadius(21.dp.toPx(), 21.dp.toPx()),
        )
        drawArc(
            color = LeafGreen.copy(alpha = 0.44f),
            startAngle = orbit,
            sweepAngle = 118f,
            useCenter = false,
            topLeft = Offset(size.width * 0.18f, size.height * 0.18f),
            size = Size(size.width * 0.64f, size.height * 0.64f),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
            color = MistBlue.copy(alpha = 0.32f),
            startAngle = orbit + 170f,
            sweepAngle = 72f,
            useCenter = false,
            topLeft = Offset(size.width * 0.26f, size.height * 0.26f),
            size = Size(size.width * 0.48f, size.height * 0.48f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )

        val scanY = size.height * scanSweep
        drawLine(
            color = MistBlue.copy(alpha = 0.72f),
            start = Offset(size.width * 0.29f, scanY),
            end = Offset(size.width * 0.71f, scanY),
            strokeWidth = 2.4.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = MistBlue.copy(alpha = 0.15f),
            radius = 10.dp.toPx() * sparkle,
            center = Offset(size.width * 0.72f, scanY),
        )

        val stemBase = Offset(size.width * 0.50f, size.height * 0.68f)
        val stemTop = Offset(size.width * 0.50f, size.height * 0.43f)
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(
                    Clay.copy(alpha = 0.92f),
                    Clay.copy(alpha = 0.62f),
                ),
            ),
            topLeft = Offset(size.width * 0.36f, size.height * 0.66f),
            size = Size(size.width * 0.28f, size.height * 0.14f),
            cornerRadius = CornerRadius(11.dp.toPx(), 11.dp.toPx()),
        )
        drawLine(
            color = LeafGreen.copy(alpha = 0.86f),
            start = stemBase,
            end = stemTop,
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
        rotate(degrees = -22f, pivot = stemTop) {
            drawOval(
                color = FreshGreen,
                topLeft = Offset(stemTop.x - size.width * 0.32f, stemTop.y - size.height * 0.05f),
                size = Size(size.width * 0.34f, size.height * 0.15f),
            )
        }
        rotate(degrees = 20f, pivot = stemTop) {
            drawOval(
                color = LeafGreen,
                topLeft = Offset(stemTop.x - size.width * 0.02f, stemTop.y - size.height * 0.10f),
                size = Size(size.width * 0.36f, size.height * 0.17f),
            )
        }
        drawCircle(
            color = Color.White.copy(alpha = 0.95f),
            radius = 3.2.dp.toPx(),
            center = stemTop,
        )

        rotate(degrees = orbit * 0.25f, pivot = center) {
            drawCircle(
                color = LeafGreen.copy(alpha = 0.88f),
                radius = 3.dp.toPx(),
                center = Offset(size.width * 0.28f, size.height * 0.30f),
            )
            drawCircle(
                color = MistBlue.copy(alpha = 0.88f),
                radius = 2.6.dp.toPx(),
                center = Offset(size.width * 0.74f, size.height * 0.36f),
            )
        }
        drawCircle(
            color = Clay.copy(alpha = 0.18f + sparkle * 0.18f),
            radius = 8.dp.toPx() * sparkle,
            center = Offset(size.width * 0.68f, size.height * 0.24f),
        )
        drawCircle(
            color = Clay.copy(alpha = 0.88f),
            radius = 2.3.dp.toPx(),
            center = Offset(size.width * 0.68f, size.height * 0.24f),
        )
    }
}

@Composable
private fun rememberDailyGuideSteps(): List<GuideStep> =
    remember {
        listOf(
            GuideStep(
                title = "Add your plant",
                text = "Start with a name, photo, location, and optional notes.",
                icon = Icons.Rounded.LocalFlorist,
                color = LeafGreen,
            ),
            GuideStep(
                title = "Set care reminders",
                text = "Choose watering and fertilizing intervals that match the plant.",
                icon = Icons.Rounded.Notifications,
                color = MistBlue,
            ),
            GuideStep(
                title = "Check Today",
                text = "Mark care as done, snooze it, or skip when the plant is not ready.",
                icon = Icons.Rounded.WaterDrop,
                color = FreshGreen,
            ),
            GuideStep(
                title = "Track what changed",
                text = "Use history, notes, and photos to remember what worked.",
                icon = Icons.Rounded.History,
                color = Clay,
            ),
        )
    }

private data class GuideStep(
    val title: String,
    val text: String,
    val icon: ImageVector,
    val color: Color,
)

@Preview(showBackground = true)
@Composable
private fun HowToUseScreenPreview() {
    WaterMeTheme {
        HowToUseScreen(onBack = {})
    }
}

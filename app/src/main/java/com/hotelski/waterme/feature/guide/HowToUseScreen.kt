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
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.WaterMeCard
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
    val summarySteps = rememberGuideSummarySteps()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            GuideHeroCard()
        }
        item {
            GuideSummaryCard(steps = summarySteps)
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun GuideHeroCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f),
                    ),
                ),
                shape = RoundedCornerShape(30.dp),
            )
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Quick guide",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = "Add plants, set reminders, care today, and track changes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                )
            }

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                LeafGreen.copy(alpha = 0.18f),
                                MistBlue.copy(alpha = 0.18f),
                            ),
                        ),
                        RoundedCornerShape(28.dp),
                    )
                    .padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedPlantGuide(
                    modifier = Modifier.size(84.dp),
                )
            }
        }
    }
}

@Composable
private fun GuideSummaryCard(
    steps: List<GuideSummaryStep>,
) {
    val transition = rememberInfiniteTransition(label = "GuideFlowMotion")
    val activePosition by transition.animateFloat(
        initialValue = 0f,
        targetValue = steps.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "GuideFlowActivePosition",
    )

    WaterMeCard(containerColor = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(LeafGreen.copy(alpha = 0.11f), RoundedCornerShape(17.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WaterDrop,
                        contentDescription = null,
                        tint = LeafGreen,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Care flow",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "A simple daily loop.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                steps.forEachIndexed { index, step ->
                    GuideFlowStepRow(
                        number = index + 1,
                        step = step,
                        shade = flowShade(activePosition = activePosition, stepIndex = index),
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideFlowStepRow(
    number: Int,
    step: GuideSummaryStep,
    shade: Float,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        step.color.copy(alpha = 0.07f + shade * 0.15f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f + shade * 0.05f),
                    ),
                ),
                shape,
            )
            .border(
                width = 1.dp,
                color = step.color.copy(alpha = 0.07f + shade * 0.18f),
                shape = shape,
            )
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.84f + shade * 0.08f), RoundedCornerShape(17.dp))
                    .border(1.dp, step.color.copy(alpha = 0.12f + shade * 0.18f), RoundedCornerShape(17.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = step.color,
                    modifier = Modifier.size(23.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "STEP ${number.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = step.color,
                    maxLines = 1,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = step.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedPlantGuide(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "GuidePlantMotion")
    val growth by transition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "GuidePlantGrowth",
    )
    val dropY by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.50f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "GuideWaterDrop",
    )

    Canvas(modifier = modifier) {
        val potWidth = size.width * 0.48f
        val potHeight = size.height * 0.24f
        val potLeft = (size.width - potWidth) / 2f
        val potTop = size.height - potHeight - 4.dp.toPx()
        val stemBottom = Offset(size.width / 2f, potTop + 4.dp.toPx())
        val leafNode = Offset(size.width / 2f, size.height * (0.45f - (growth - 0.78f) * 0.32f))

        drawRoundRect(
            color = Color.White.copy(alpha = 0.20f),
            topLeft = Offset(size.width * 0.05f, size.height * 0.07f),
            size = Size(size.width * 0.90f, size.height * 0.86f),
            cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx()),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.10f),
            topLeft = Offset(size.width * 0.10f, size.height * 0.12f),
            size = Size(size.width * 0.72f, size.height * 0.58f),
            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
        )
        drawRoundRect(
            color = Clay.copy(alpha = 0.95f),
            topLeft = Offset(potLeft, potTop),
            size = Size(potWidth, potHeight),
            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
        )
        drawLine(
            color = Color.White.copy(alpha = 0.94f),
            start = stemBottom,
            end = leafNode,
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round,
        )
        rotate(degrees = -13f, pivot = leafNode) {
            drawOval(
                color = FreshGreen,
                topLeft = Offset(leafNode.x - size.width * 0.36f, leafNode.y - size.height * 0.08f),
                size = Size(size.width * 0.40f * growth, size.height * 0.17f * growth),
            )
        }
        rotate(degrees = 14f, pivot = leafNode) {
            drawOval(
                color = LeafGreen,
                topLeft = Offset(leafNode.x - size.width * 0.02f, leafNode.y - size.height * 0.13f),
                size = Size(size.width * 0.42f * growth, size.height * 0.19f * growth),
            )
        }
        drawCircle(
            color = Color.White.copy(alpha = 0.92f),
            radius = 3.3.dp.toPx(),
            center = leafNode,
        )
        drawCircle(
            color = MistBlue.copy(alpha = 0.86f),
            radius = 6.dp.toPx(),
            center = Offset(size.width * 0.77f, size.height * dropY),
        )
    }
}

@Composable
private fun rememberGuideSummarySteps(): List<GuideSummaryStep> =
    remember {
        listOf(
            GuideSummaryStep(
                title = "Add plants",
                summary = "Photo, name, place, notes.",
                icon = Icons.Rounded.LocalFlorist,
                color = LeafGreen,
            ),
            GuideSummaryStep(
                title = "Set rhythm",
                summary = "Pick care intervals.",
                icon = Icons.Rounded.Notifications,
                color = MistBlue,
            ),
            GuideSummaryStep(
                title = "Care today",
                summary = "Done, snooze, or skip.",
                icon = Icons.Rounded.WaterDrop,
                color = FreshGreen,
            ),
            GuideSummaryStep(
                title = "Track changes",
                summary = "Notes, photos, history.",
                icon = Icons.Rounded.PhotoCamera,
                color = Clay,
            ),
        )
    }

private data class GuideSummaryStep(
    val title: String,
    val summary: String,
    val icon: ImageVector,
    val color: Color,
)

private fun flowShade(
    activePosition: Float,
    stepIndex: Int,
): Float {
    val localProgress = activePosition - stepIndex
    if (localProgress < 0f || localProgress >= 1f) return 0f
    return 1f - localProgress * 0.78f
}

@Preview(showBackground = true)
@Composable
private fun HowToUseScreenPreview() {
    WaterMeTheme {
        HowToUseScreen(onBack = {})
    }
}

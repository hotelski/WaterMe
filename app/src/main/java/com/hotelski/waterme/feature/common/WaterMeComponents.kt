package com.hotelski.waterme.feature.common

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.model.HealthMood
import com.hotelski.waterme.ui.theme.CardWhite
import com.hotelski.waterme.ui.theme.Clay
import com.hotelski.waterme.ui.theme.FreshGreen
import com.hotelski.waterme.ui.theme.GardenBackground
import com.hotelski.waterme.ui.theme.Ink
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.MistBlue
import com.hotelski.waterme.ui.theme.MutedInk
import com.hotelski.waterme.ui.theme.SoftCream
import com.hotelski.waterme.ui.theme.WaterMeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterMeTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    navigationContentDescription: String? = null,
    onNavigationClick: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    actionContentDescription: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (navigationIcon != null && onNavigationClick != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(navigationIcon, contentDescription = navigationContentDescription)
                }
            }
        },
        actions = {
            if (actionIcon != null && onActionClick != null) {
                IconButton(onClick = onActionClick) {
                    Icon(actionIcon, contentDescription = actionContentDescription)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@Composable
fun WaterMeCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    content: @Composable () -> Unit,
) {
    WaterMePremiumCard(
        modifier = modifier,
        containerColor = containerColor,
        content = content,
    )
}

@Composable
fun WaterMePremiumCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    accentColor: Color = Color.Unspecified,
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    content: @Composable () -> Unit,
) {
    val darkTheme = isSystemInDarkTheme()
    val resolvedContainerColor = if (containerColor == Color.Unspecified) {
        MaterialTheme.colorScheme.surface
    } else {
        containerColor
    }
    val resolvedAccentColor = if (accentColor == Color.Unspecified) {
        MaterialTheme.colorScheme.primary
    } else {
        accentColor
    }
    val outlineColor = if (darkTheme) {
        Color.White.copy(alpha = 0.07f)
    } else {
        Color.White.copy(alpha = 0.78f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (darkTheme) 0.dp else 12.dp,
                shape = shape,
                ambientColor = resolvedAccentColor.copy(alpha = 0.10f),
                spotColor = resolvedAccentColor.copy(alpha = 0.08f),
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        resolvedContainerColor.copy(alpha = if (darkTheme) 0.92f else 0.98f),
                        resolvedContainerColor.copy(alpha = if (darkTheme) 0.86f else 0.92f),
                    ),
                ),
            )
            .border(1.dp, outlineColor, shape)
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
fun WaterMePrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            ),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun WaterMeSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun WaterMeLoadingState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = LeafGreen)
        Spacer(Modifier.height(16.dp))
        Text(message, color = MutedInk, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun WaterMeEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.LocalFlorist,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    WaterMePremiumCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
        accentColor = MaterialTheme.colorScheme.primary,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WaterMeIconBadge(icon = icon, size = 58.dp)
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (actionLabel != null && onActionClick != null) {
                OutlinedButton(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun WaterMeErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetryClick: (() -> Unit)? = null,
) {
    WaterMeCard(
        modifier = modifier,
        containerColor = Color(0xFFFFF4ED),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = Clay)
            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MutedInk)
            if (onRetryClick != null) {
                OutlinedButton(
                    onClick = onRetryClick,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Try again")
                }
            }
        }
    }
}

@Composable
fun PlantPhotoTile(
    photoUri: String?,
    plantName: String,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    val imageBitmap = rememberPlantImageBitmap(photoUri)
    val shape = RoundedCornerShape(size * 0.28f)
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = LeafGreen.copy(alpha = 0.16f),
                spotColor = LeafGreen.copy(alpha = 0.12f),
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.66f), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "$plantName photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Rounded.LocalFlorist,
                contentDescription = "$plantName photo",
                modifier = Modifier.size(size * 0.46f),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun WaterMeIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    color: Color = Color.Unspecified,
    contentDescription: String? = null,
) {
    val resolvedColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.34f))
            .background(resolvedColor.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(size * 0.48f),
            tint = resolvedColor,
        )
    }
}

@Composable
fun WaterMeStatusChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.13f),
        contentColor = color,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun WaterMeFloatingActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.shadow(
            elevation = 14.dp,
            shape = RoundedCornerShape(24.dp),
            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
        ),
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
private fun rememberPlantImageBitmap(photoUri: String?): ImageBitmap? {
    val context = LocalContext.current
    val imageState = produceState<ImageBitmap?>(initialValue = null, photoUri) {
        value = if (photoUri.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver
                        .openInputStream(Uri.parse(photoUri))
                        ?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                }.getOrNull()
            }
        }
    }
    return imageState.value
}

@Composable
fun CareTypeBadge(
    careType: CareType,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val color = careType.accentColor()
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = careType.shortLabel().take(1),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = color,
        )
    }
}

@Composable
fun PlantCard(
    plant: PlantCardUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WaterMeCard(
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlantPhotoTile(
                photoUri = plant.photoUri,
                plantName = plant.name,
                size = 78.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = plant.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (plant.dueTaskCount > 0) {
                        CountPill("${plant.dueTaskCount} due")
                    }
                }
                if (plant.nextCareLabel != null) {
                    Text(
                        text = plant.nextCareLabel,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = LeafGreen,
                    )
                }
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MutedInk)
        }
    }
}

@Composable
fun CareTaskCard(
    task: CareTaskUiModel,
    onOpenPlant: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    completeEnabled: Boolean = true,
    onSkip: (() -> Unit)? = null,
    onSnooze: (() -> Unit)? = null,
) {
    WaterMeCard(
        modifier = modifier.clickable(onClick = onOpenPlant),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CareTypeBadge(task.careType)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.careType.label(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                    )
                    Text(
                        text = task.plantName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = task.plantLocation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedInk,
                    )
                }
                StatusPill(task)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onComplete,
                    enabled = completeEnabled,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Done")
                }
                if (onSnooze != null) {
                    OutlinedButton(onClick = onSnooze, shape = RoundedCornerShape(16.dp)) {
                        Text("Snooze")
                    }
                }
                if (onSkip != null) {
                    TextButton(onClick = onSkip) {
                        Text("Skip")
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderRow(
    reminder: ReminderUiModel,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    WaterMeCard(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CareTypeBadge(reminder.careType)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.careType.label(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Text(reminder.frequencyLabel, style = MaterialTheme.typography.bodyMedium, color = MutedInk)
                Text(reminder.nextDueLabel, style = MaterialTheme.typography.labelLarge, color = LeafGreen)
            }
            CountPill(if (reminder.enabled) "On" else "Off")
        }
    }
}

@Composable
fun CareHistoryRow(
    entry: CareHistoryUiModel,
    modifier: Modifier = Modifier,
) {
    WaterMeCard(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CareTypeBadge(entry.careType, size = 42.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${entry.actionLabel} ${entry.careType.shortLabel().lowercase()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Text(entry.plantName, style = MaterialTheme.typography.bodyMedium, color = MutedInk)
                if (entry.notes.isNotBlank()) {
                    Text(
                        text = entry.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(entry.dateLabel, style = MaterialTheme.typography.labelMedium, color = LeafGreen)
        }
    }
}

@Composable
fun HealthNoteRow(
    note: HealthNoteUiModel,
    modifier: Modifier = Modifier,
) {
    WaterMeCard(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(note.mood.accentColor().copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = note.mood.label().first().toString(),
                    color = note.mood.accentColor(),
                    fontWeight = FontWeight.Black,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(note.plantName, style = MaterialTheme.typography.labelLarge, color = LeafGreen)
                Text(note.note, style = MaterialTheme.typography.bodyMedium, color = Ink)
                Text("${note.mood.label()} - ${note.dateLabel}", style = MaterialTheme.typography.bodySmall, color = MutedInk)
            }
        }
    }
}

@Composable
private fun StatusPill(task: CareTaskUiModel) {
    val label = when {
        task.isOverdue -> "Overdue"
        task.isSnoozed -> "Snoozed"
        else -> task.dueLabel
    }
    val color = when {
        task.isOverdue -> Clay
        task.isSnoozed -> MistBlue
        else -> LeafGreen
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun CountPill(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = LeafGreen.copy(alpha = 0.12f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = LeafGreen,
            maxLines = 1,
        )
    }
}

@Composable
fun CareType.accentColor(): Color =
    when (this) {
        CareType.WATERING -> MistBlue
        CareType.FERTILIZING -> FreshGreen
        CareType.REPOTTING -> Clay
        CareType.MISTING -> Color(0xFF6AA9A5)
        CareType.PRUNING -> LeafGreen
    }

@Composable
fun HealthMood.accentColor(): Color =
    when (this) {
        HealthMood.ATTENTION -> Clay
        HealthMood.HEALTHY -> LeafGreen
        HealthMood.GROWTH -> FreshGreen
    }

@Preview(showBackground = true)
@Composable
private fun WaterMeComponentsPreview() {
    WaterMeTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GardenBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlantCard(plant = WaterMePreviewData.plants.first(), onClick = {})
            CareTaskCard(task = WaterMePreviewData.tasks.first(), onOpenPlant = {}, onComplete = {})
            WaterMeEmptyState(title = "No plants yet", message = "Add your first plant to start a care schedule.")
        }
    }
}

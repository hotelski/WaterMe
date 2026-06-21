package com.hotelski.waterme.feature.plantscanner

import android.graphics.BitmapFactory
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.PlantPhotoTile
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeIconBadge
import com.hotelski.waterme.feature.common.WaterMePremiumCard
import com.hotelski.waterme.feature.common.WaterMeStatusChip
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.MistBlue
import com.hotelski.waterme.ui.theme.WaterMeTheme
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private val PlantScannerImageClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .build()

@Composable
fun PlantScannerScreen(
    uiState: PlantScannerUiState,
    onEvent: (PlantScannerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            WaterMeTopBar(
                title = "Plant Scanner",
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                navigationContentDescription = "Back",
                onNavigationClick = { onEvent(PlantScannerEvent.BackClicked) },
            )
        },
    ) { innerPadding ->
        PlantScannerContent(
            uiState = uiState,
            onEvent = onEvent,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun PlantScannerContent(
    uiState: PlantScannerUiState,
    onEvent: (PlantScannerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScannerHeroCard(
                onTakePhoto = { onEvent(PlantScannerEvent.TakePhotoClicked) },
                onChooseGallery = { onEvent(PlantScannerEvent.ChooseFromGalleryClicked) },
                remainingScans = uiState.remainingScans,
                scanLimit = uiState.scanLimit,
                isQuotaExhausted = uiState.isScanQuotaExhausted,
                resetCountdown = uiState.scanQuotaResetCountdown,
            )
        }

        if (uiState.selectedImageUri != null) {
            item {
                SelectedImageCard(photoUri = uiState.selectedImageUri)
            }
        }

        if (uiState.actionMessage != null) {
            item {
                ScannerInlineMessage(message = uiState.actionMessage)
            }
        }

        when {
            uiState.errorMessage != null -> item {
                val retryAction = if (uiState.selectedImageUri != null) {
                    { onEvent(PlantScannerEvent.RetryClicked) }
                } else {
                    null
                }
                WaterMeErrorState(
                    message = uiState.errorMessage,
                    onRetryClick = retryAction,
                )
            }

            uiState.isLoading -> item {
                ScannerLoadingCard()
            }

            uiState.results.isNotEmpty() -> item {
                PlantScannerResultCard(
                    result = uiState.results.first(),
                    onSaveToPlants = { onEvent(PlantScannerEvent.SaveToPlantsClicked) },
                )
            }

            uiState.isEmpty -> item {
                ScannerEmptyState()
            }
        }

        if (uiState.recentScans.isNotEmpty()) {
            item {
                RecentScansCard(recentScans = uiState.recentScans)
            }
        }
    }
}

@Composable
private fun ScannerHeroCard(
    onTakePhoto: () -> Unit,
    onChooseGallery: () -> Unit,
    remainingScans: Int,
    scanLimit: Int,
    isQuotaExhausted: Boolean,
    resetCountdown: String?,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        accentColor = LeafGreen,
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WaterMeIconBadge(
                    icon = Icons.Rounded.AutoAwesome,
                    size = 48.dp,
                    color = LeafGreen,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Scan a plant",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Take or choose a photo, then select the plant area.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onTakePhoto,
                    enabled = !isQuotaExhausted,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Take photo", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onChooseGallery,
                    enabled = !isQuotaExhausted,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Gallery", fontWeight = FontWeight.SemiBold)
                }
            }

            ScannerQuotaStatus(
                remainingScans = remainingScans,
                scanLimit = scanLimit,
                isQuotaExhausted = isQuotaExhausted,
                resetCountdown = resetCountdown,
            )
        }
    }
}

@Composable
private fun ScannerQuotaStatus(
    remainingScans: Int,
    scanLimit: Int,
    isQuotaExhausted: Boolean,
    resetCountdown: String?,
    modifier: Modifier = Modifier,
) {
    if (isQuotaExhausted) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.34f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(20.dp),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = "Daily scan limit reached",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Resets in ${resetCountdown ?: "--:--"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            WaterMeStatusChip(
                label = "$remainingScans/$scanLimit scans left today",
                color = LeafGreen,
                icon = Icons.Rounded.AutoAwesome,
            )
        }
    }
}

@Composable
private fun ScannerEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(238.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                shape = RoundedCornerShape(28.dp),
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScannerAnimatedFrame()
            Text(
                text = "Ready to scan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Use a clear photo of leaves or flowers for a better match.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ScannerAnimatedFrame(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scannerEmptyTransition")
    val scanProgress = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scannerLineProgress",
    )
    val scanTravelPx = with(LocalDensity.current) { 74.dp.toPx() }

    Box(
        modifier = modifier
            .size(108.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        LeafGreen.copy(alpha = 0.13f),
                        MistBlue.copy(alpha = 0.08f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = LeafGreen.copy(alpha = 0.20f),
                shape = RoundedCornerShape(30.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.LocalFlorist,
            contentDescription = null,
            modifier = Modifier.size(42.dp),
            tint = LeafGreen.copy(alpha = 0.92f),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .graphicsLayer { translationY = scanProgress.value * scanTravelPx }
                .fillMaxWidth(0.70f)
                .height(3.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            LeafGreen.copy(alpha = 0.92f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun SelectedImageCard(
    photoUri: String,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Photo for scanning",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)),
            ) {
                PlantPhotoTile(
                    photoUri = photoUri,
                    plantName = "Selected plant",
                    fillContainer = true,
                )
            }
        }
    }
}

@Composable
private fun ScannerLoadingCard(modifier: Modifier = Modifier) {
    WaterMePremiumCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(34.dp),
                strokeWidth = 3.dp,
                color = LeafGreen,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Scanning plant photo...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Checking the cropped area against known plant matches.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PlantScannerResultCard(
    result: PlantScannerResultUiModel,
    onSaveToPlants: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(
        modifier = modifier,
        accentColor = LeafGreen,
        shape = RoundedCornerShape(30.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Top match",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Review the match, then save it as a new plant profile.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                WaterMeStatusChip(
                    label = "${result.confidencePercent}%",
                    color = LeafGreen,
                    icon = Icons.Rounded.AutoAwesome,
                )
            }

            Box {
                PlantMatchImage(
                    imageUrl = result.relatedImageUrl,
                    plantName = result.commonName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(238.dp),
                )
                PlantImageOverlayLabel(
                    label = "Reference photo",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = result.commonName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = result.scientificName,
                    style = MaterialTheme.typography.titleMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                result.relatedImageAttribution?.let { attribution ->
                    Text(
                        text = attribution,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            OutlinedButton(
                onClick = onSaveToPlants,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save to my plants", fontWeight = FontWeight.SemiBold)
            }

            Text(
                text = "Identification powered by Pl@ntNet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun RecentScansCard(
    recentScans: List<PlantScannerRecentScanUiModel>,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        accentColor = MistBlue,
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WaterMeIconBadge(
                    icon = Icons.Rounded.History,
                    size = 38.dp,
                    color = MistBlue,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Recent scans",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Last ${recentScans.size.coerceAtMost(3)} identified plants",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                recentScans.take(3).forEach { scan ->
                    RecentScanRow(scan = scan)
                }
            }
        }
    }
}

@Composable
private fun RecentScanRow(
    scan: PlantScannerRecentScanUiModel,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MistBlue.copy(alpha = 0.08f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.26f)),
        ) {
            PlantPhotoTile(
                photoUri = scan.photoUri,
                plantName = scan.commonName,
                fillContainer = true,
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = scan.commonName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (scan.scientificName.isNotBlank()) {
                Text(
                    text = scan.scientificName,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        WaterMeStatusChip(
            label = "${scan.confidencePercent}%",
            color = LeafGreen,
            icon = Icons.Rounded.AutoAwesome,
        )
    }
}

@Composable
private fun PlantImageOverlayLabel(
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.68f))
            .padding(horizontal = 11.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Image,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = Color.White,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlantMatchImage(
    imageUrl: String?,
    plantName: String,
    modifier: Modifier = Modifier,
) {
    val imageBitmap = rememberRemotePlantImageBitmap(imageUrl)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.70f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
                shape = RoundedCornerShape(26.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (imageBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = imageBitmap,
                contentDescription = "$plantName reference photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Rounded.LocalFlorist,
                    contentDescription = "$plantName reference photo",
                    modifier = Modifier.size(42.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Reference image unavailable",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun rememberRemotePlantImageBitmap(imageUrl: String?): ImageBitmap? {
    val imageState = produceState<ImageBitmap?>(initialValue = null, imageUrl) {
        value = if (imageUrl.isNullOrBlank()) {
            null
        } else {
            loadRemotePlantImageBitmap(imageUrl)
        }
    }
    return imageState.value
}

private suspend fun loadRemotePlantImageBitmap(imageUrl: String): ImageBitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(imageUrl)
                .build()
            PlantScannerImageClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@runCatching null
                }
                val bytes = response.body?.bytes() ?: return@runCatching null
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        }.getOrNull()
    }

@Composable
private fun ScannerInlineMessage(
    message: String,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(
        modifier = modifier,
        containerColor = LeafGreen.copy(alpha = 0.08f),
        accentColor = LeafGreen,
        shape = RoundedCornerShape(22.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlantScannerEmptyPreview() {
    WaterMeTheme {
        PlantScannerScreen(
            uiState = PlantScannerUiState(
                recentScans = scannerPreviewRecentScans(),
            ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlantScannerLoadingPreview() {
    WaterMeTheme {
        PlantScannerScreen(
            uiState = PlantScannerUiState(
                selectedImageUri = "",
                isLoading = true,
            ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlantScannerResultPreview() {
    WaterMeTheme {
        PlantScannerScreen(
            uiState = PlantScannerUiState(
                selectedImageUri = "",
                results = listOf(
                    PlantScannerResultUiModel(
                        commonName = "Monstera",
                        scientificName = "Monstera deliciosa",
                        confidenceScore = 0.92,
                    ),
                    PlantScannerResultUiModel(
                        commonName = "Split-leaf philodendron",
                        scientificName = "Thaumatophyllum bipinnatifidum",
                        confidenceScore = 0.71,
                    ),
                    PlantScannerResultUiModel(
                        commonName = "Swiss cheese plant",
                        scientificName = "Monstera adansonii",
                        confidenceScore = 0.64,
                    ),
                ),
                recentScans = scannerPreviewRecentScans(),
            ),
            onEvent = {},
        )
    }
}

private fun scannerPreviewRecentScans(): List<PlantScannerRecentScanUiModel> =
    listOf(
        PlantScannerRecentScanUiModel(
            commonName = "Monstera",
            scientificName = "Monstera deliciosa",
            confidencePercent = 92,
            photoUri = null,
        ),
        PlantScannerRecentScanUiModel(
            commonName = "Prayer plant",
            scientificName = "Maranta leuconeura",
            confidencePercent = 86,
            photoUri = null,
        ),
        PlantScannerRecentScanUiModel(
            commonName = "Snake plant",
            scientificName = "Dracaena trifasciata",
            confidencePercent = 81,
            photoUri = null,
        ),
    )

@Preview(showBackground = true)
@Composable
private fun PlantScannerErrorPreview() {
    WaterMeTheme {
        PlantScannerScreen(
            uiState = PlantScannerUiState(
                errorMessage = "WaterMe could not open the photo picker.",
            ),
            onEvent = {},
        )
    }
}

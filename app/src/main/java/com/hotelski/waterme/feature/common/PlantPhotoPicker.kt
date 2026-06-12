package com.hotelski.waterme.feature.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlinx.coroutines.launch

@Composable
fun PlantPhotoSourceDialog(
    onTakePhoto: () -> Unit,
    onChooseImage: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plant photo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PlantPhotoSourceButton(
                    label = "Take photo",
                    supportingText = "Use the phone camera.",
                    icon = Icons.Rounded.PhotoCamera,
                    onClick = onTakePhoto,
                )
                PlantPhotoSourceButton(
                    label = "Choose image",
                    supportingText = "Pick any image from this device.",
                    icon = Icons.Rounded.Image,
                    onClick = onChooseImage,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun PlantPhotoSourceButton(
    label: String,
    supportingText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontWeight = FontWeight.SemiBold)
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun PlantPhotoCropDialog(
    sourceUri: Uri,
    onPhotoCropped: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loadError by remember(sourceUri) { mutableStateOf<String?>(null) }
    val sourceBitmap by produceState<Bitmap?>(initialValue = null, sourceUri) {
        loadError = null
        value = loadPlantPhotoBitmap(context, sourceUri, SOURCE_BITMAP_MAX_DIMENSION)
        if (value == null) {
            loadError = "WaterMe could not open this image."
        }
    }
    var cropBoxSize by remember(sourceUri) { mutableStateOf(IntSize.Zero) }
    var userScale by remember(sourceUri) { mutableStateOf(1f) }
    var offset by remember(sourceUri) { mutableStateOf(Offset.Zero) }
    var isSaving by remember(sourceUri) { mutableStateOf(false) }
    var saveError by remember(sourceUri) { mutableStateOf<String?>(null) }
    val bitmap = sourceBitmap

    LaunchedEffect(bitmap, cropBoxSize, userScale) {
        if (bitmap != null && cropBoxSize.hasArea()) {
            offset = offset.coerceCropOffset(bitmap.width, bitmap.height, cropBoxSize, userScale)
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isSaving) {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(18.dp),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Select visible area",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Drag or zoom until the plant fits the square.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.Black)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(26.dp),
                        )
                        .onSizeChanged { cropBoxSize = it }
                        .then(
                            if (bitmap != null && cropBoxSize.hasArea()) {
                                Modifier.pointerInput(bitmap, cropBoxSize, userScale) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val nextScale = (userScale * zoom).coerceIn(MIN_CROP_SCALE, MAX_CROP_SCALE)
                                        val scaleRatio = if (userScale == 0f) 1f else nextScale / userScale
                                        val nextOffset = Offset(
                                            x = offset.x * scaleRatio + pan.x,
                                            y = offset.y * scaleRatio + pan.y,
                                        )
                                        userScale = nextScale
                                        offset = nextOffset.coerceCropOffset(
                                            bitmap.width,
                                            bitmap.height,
                                            cropBoxSize,
                                            userScale,
                                        )
                                    }
                                }
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        bitmap != null -> PlantPhotoCropCanvas(
                            bitmap = bitmap,
                            userScale = userScale,
                            offset = offset,
                            modifier = Modifier.fillMaxSize(),
                        )

                        loadError != null -> Text(
                            text = loadError.orEmpty(),
                            modifier = Modifier.padding(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        else -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(26.dp),
                            ),
                    )
                }

                saveError?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                    ) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            userScale = 1f
                            offset = Offset.Zero
                            saveError = null
                        },
                        enabled = bitmap != null && !isSaving,
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reset")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val currentBitmap = bitmap ?: return@Button
                            val currentSize = cropBoxSize
                            if (!currentSize.hasArea()) return@Button

                            scope.launch {
                                isSaving = true
                                saveError = null
                                runCatching {
                                    saveCroppedPlantPhoto(
                                        context = context,
                                        source = currentBitmap,
                                        viewportSize = currentSize,
                                        userScale = userScale,
                                        offset = offset,
                                    )
                                }
                                    .onSuccess(onPhotoCropped)
                                    .onFailure {
                                        isSaving = false
                                        saveError = "WaterMe could not save the cropped photo."
                                    }
                            }
                        },
                        enabled = bitmap != null && cropBoxSize.hasArea() && !isSaving,
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(if (isSaving) "Saving..." else "Use area")
                    }
                }
            }
        }
    }
}

@Composable
private fun PlantPhotoCropCanvas(
    bitmap: Bitmap,
    userScale: Float,
    offset: Offset,
    modifier: Modifier = Modifier,
) {
    val bitmapPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    Canvas(modifier = modifier) {
        val baseScale = max(size.width / bitmap.width.toFloat(), size.height / bitmap.height.toFloat())
        val totalScale = baseScale * userScale
        val scaledWidth = bitmap.width * totalScale
        val scaledHeight = bitmap.height * totalScale
        val left = (size.width - scaledWidth) / 2f + offset.x
        val top = (size.height - scaledHeight) / 2f + offset.y
        drawContext.canvas.nativeCanvas.drawBitmap(
            bitmap,
            null,
            RectF(left, top, left + scaledWidth, top + scaledHeight),
            bitmapPaint,
        )
    }
}

fun createPlantCameraImageUri(context: Context): Uri {
    val directory = File(context.cacheDir, CAMERA_PHOTO_DIRECTORY).apply { mkdirs() }
    val file = File.createTempFile("plant_photo_", ".jpg", directory)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
}

suspend fun loadPlantPhotoBitmap(
    context: Context,
    uri: Uri,
    maxDimension: Int = DISPLAY_BITMAP_MAX_DIMENSION,
): Bitmap? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    runCatching {
        val orientation = readExifOrientation(context, uri)
        decodeSampledBitmap(context, uri, maxDimension)?.applyExifOrientation(orientation)
    }.getOrNull()
}

private suspend fun saveCroppedPlantPhoto(
    context: Context,
    source: Bitmap,
    viewportSize: IntSize,
    userScale: Float,
    offset: Offset,
): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    val outputBitmap = Bitmap.createBitmap(CROPPED_PHOTO_SIZE_PX, CROPPED_PHOTO_SIZE_PX, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(outputBitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val baseScale = max(
        viewportSize.width / source.width.toFloat(),
        viewportSize.height / source.height.toFloat(),
    )
    val totalScale = baseScale * userScale
    val outputScale = CROPPED_PHOTO_SIZE_PX / viewportSize.width.toFloat()
    val scaledWidth = source.width * totalScale * outputScale
    val scaledHeight = source.height * totalScale * outputScale
    val left = ((viewportSize.width - source.width * totalScale) / 2f + offset.x) * outputScale
    val top = ((viewportSize.height - source.height * totalScale) / 2f + offset.y) * outputScale

    canvas.drawBitmap(
        source,
        null,
        RectF(left, top, left + scaledWidth, top + scaledHeight),
        paint,
    )

    val directory = File(context.filesDir, SAVED_PHOTO_DIRECTORY).apply { mkdirs() }
    val file = File(directory, "plant_photo_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { output ->
        outputBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
    }
    outputBitmap.recycle()
    Uri.fromFile(file).toString()
}

private fun decodeSampledBitmap(
    context: Context,
    uri: Uri,
    maxDimension: Int,
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return null
    }

    val options = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
        inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
    }
    return context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, options)
    }
}

private fun calculateSampleSize(
    width: Int,
    height: Int,
    maxDimension: Int,
): Int {
    var sampleSize = 1
    while ((width / sampleSize) > maxDimension || (height / sampleSize) > maxDimension) {
        sampleSize *= 2
    }
    return sampleSize
}

private fun readExifOrientation(context: Context, uri: Uri): Int =
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

private fun Bitmap.applyExifOrientation(orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
        else -> return this
    }

    return runCatching {
        Bitmap.createBitmap(this, 0, 0, width, height, matrix, true).also { rotated ->
            if (rotated != this) {
                recycle()
            }
        }
    }.getOrElse { this }
}

private fun Offset.coerceCropOffset(
    bitmapWidth: Int,
    bitmapHeight: Int,
    viewportSize: IntSize,
    userScale: Float,
): Offset {
    val baseScale = max(
        viewportSize.width / bitmapWidth.toFloat(),
        viewportSize.height / bitmapHeight.toFloat(),
    )
    val totalScale = baseScale * userScale
    val maxX = ((bitmapWidth * totalScale - viewportSize.width) / 2f).coerceAtLeast(0f)
    val maxY = ((bitmapHeight * totalScale - viewportSize.height) / 2f).coerceAtLeast(0f)
    return Offset(
        x = x.coerceIn(-maxX, maxX),
        y = y.coerceIn(-maxY, maxY),
    )
}

private fun IntSize.hasArea(): Boolean = width > 0 && height > 0

private const val CAMERA_PHOTO_DIRECTORY = "camera_photos"
private const val SAVED_PHOTO_DIRECTORY = "plant_photos"
private const val SOURCE_BITMAP_MAX_DIMENSION = 2400
private const val DISPLAY_BITMAP_MAX_DIMENSION = 1200
private const val CROPPED_PHOTO_SIZE_PX = 1024
private const val JPEG_QUALITY = 92
private const val MIN_CROP_SCALE = 1f
private const val MAX_CROP_SCALE = 4f

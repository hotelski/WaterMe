package com.hotelski.waterme.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val LeafGreen = Color(0xFF2F7D4B)
val FreshGreen = Color(0xFF79B987)
val MistBlue = Color(0xFF7FB9D7)
val Clay = Color(0xFFB88C4A)
val SoftCream = Color(0xFFFFFBF3)
val GardenBackground = Color(0xFFF6FBF7)
val Ink = Color(0xFF173326)
val MutedInk = Color(0xFF5E7267)
val CardWhite = Color(0xFFFFFFFF)

private val WaterMeColorScheme = lightColorScheme(
    primary = LeafGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDFF2E6),
    onPrimaryContainer = Color(0xFF0E2A19),
    secondary = FreshGreen,
    onSecondary = Color(0xFF102816),
    secondaryContainer = Color(0xFFEAF6EC),
    onSecondaryContainer = Color(0xFF193821),
    tertiary = Clay,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE8C2),
    onTertiaryContainer = Color(0xFF422C07),
    background = GardenBackground,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE7F1EA),
    onSurfaceVariant = MutedInk,
    outline = Color(0xFFB7C9BE),
)

private val WaterMeShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
)

@Composable
fun WaterMeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = GardenBackground.toArgb()
            window.navigationBarColor = GardenBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = WaterMeColorScheme,
        typography = Typography(),
        shapes = WaterMeShapes,
        content = content,
    )
}

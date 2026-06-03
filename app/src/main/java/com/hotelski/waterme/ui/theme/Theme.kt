package com.hotelski.waterme.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.hotelski.waterme.data.preferences.TextColorPreference

val LeafGreen = Color(0xFF2F7D4B)
val DeepLeaf = Color(0xFF163D2A)
val FreshGreen = Color(0xFF79B987)
val SageGreen = Color(0xFFA8CDB2)
val MistBlue = Color(0xFF7FB9D7)
val Clay = Color(0xFFB88C4A)
val SoftCream = Color(0xFFFFFBF3)
val GardenBackground = Color(0xFFF6FBF7)
val Ink = Color(0xFF173326)
val MutedInk = Color(0xFF5E7267)
val CardWhite = Color(0xFFFFFFFF)
val Linen = Color(0xFFF1F6EF)
val MossDark = Color(0xFF0F1F17)
val ForestSurface = Color(0xFF172A20)
val ForestSurfaceHigh = Color(0xFF21362B)
val PaleMint = Color(0xFFE8F3EA)

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

private val DarkWaterMeColorScheme = darkColorScheme(
    primary = SageGreen,
    onPrimary = Color(0xFF0E2518),
    primaryContainer = Color(0xFF214D34),
    onPrimaryContainer = Color(0xFFDDEFE3),
    secondary = FreshGreen,
    onSecondary = Color(0xFF102816),
    secondaryContainer = Color(0xFF264A2D),
    onSecondaryContainer = Color(0xFFE5F3E8),
    tertiary = Color(0xFFE6C17F),
    onTertiary = Color(0xFF372506),
    tertiaryContainer = Color(0xFF5B431D),
    onTertiaryContainer = Color(0xFFFFE7BD),
    background = MossDark,
    onBackground = Color(0xFFE9F1EB),
    surface = ForestSurface,
    onSurface = Color(0xFFE9F1EB),
    surfaceVariant = ForestSurfaceHigh,
    onSurfaceVariant = Color(0xFFBACBC0),
    outline = Color(0xFF5E7468),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val WaterMeShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
)

private val WaterMeTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp,
    ),
)

object WaterMeSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 40.dp
}

@Composable
fun WaterMeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    textColorPreference: TextColorPreference = TextColorPreference.FOREST,
    content: @Composable () -> Unit,
) {
    val colorScheme = (if (darkTheme) DarkWaterMeColorScheme else WaterMeColorScheme)
        .withTextColor(textColorPreference, darkTheme)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WaterMeTypography,
        shapes = WaterMeShapes,
        content = content,
    )
}

private fun androidx.compose.material3.ColorScheme.withTextColor(
    preference: TextColorPreference,
    darkTheme: Boolean,
): androidx.compose.material3.ColorScheme {
    val textColor = preference.textColor(darkTheme)
    val mutedTextColor = textColor.copy(alpha = if (darkTheme) 0.76f else 0.70f)
    return copy(
        onBackground = textColor,
        onSurface = textColor,
        onSurfaceVariant = mutedTextColor,
        onPrimaryContainer = textColor,
        onSecondaryContainer = textColor,
    )
}

private fun TextColorPreference.textColor(darkTheme: Boolean): Color =
    when (this) {
        TextColorPreference.FOREST -> if (darkTheme) Color(0xFFE9F1EB) else Ink
        TextColorPreference.MINT -> if (darkTheme) Color(0xFFD2F5DC) else Color(0xFF1F6A43)
        TextColorPreference.BLUE -> if (darkTheme) Color(0xFFD7F0FF) else Color(0xFF1F5E78)
        TextColorPreference.SKY -> if (darkTheme) Color(0xFFCBEAFF) else Color(0xFF2A6F95)
        TextColorPreference.CLAY -> if (darkTheme) Color(0xFFFFE2AC) else Color(0xFF6B4A16)
        TextColorPreference.AMBER -> if (darkTheme) Color(0xFFFFD978) else Color(0xFF7A5300)
        TextColorPreference.ROSE -> if (darkTheme) Color(0xFFFFD7DF) else Color(0xFF8A2F45)
        TextColorPreference.LAVENDER -> if (darkTheme) Color(0xFFE8DCFF) else Color(0xFF5A438A)
        TextColorPreference.SLATE -> if (darkTheme) Color(0xFFE5EAF0) else Color(0xFF26343D)
        TextColorPreference.HIGH_CONTRAST -> if (darkTheme) Color.White else Color.Black
    }

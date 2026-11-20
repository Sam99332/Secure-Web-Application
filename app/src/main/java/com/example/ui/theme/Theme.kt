package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SecureAuthDarkColorScheme = darkColorScheme(
    primary = CyanPulse,
    onPrimary = MidnightNavy,
    primaryContainer = DeepIndigo,
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = EmeraldSoft,
    onSecondary = MidnightNavy,
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = AmberWarning,
    onTertiary = MidnightNavy,
    background = MidnightNavy,
    onBackground = TextPrimaryDark,
    surface = DeepSlate,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateCardDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = SlateBorderDark,
    error = CrimsonAlert,
    onError = Color.White,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

private val SecureAuthLightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = DeepIndigo,
    secondary = EmeraldVerified,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = AmberWarning,
    onTertiary = Color.White,
    background = IceCanvas,
    onBackground = TextPrimaryLight,
    surface = CrispWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = SoftSlateSurface,
    onSurfaceVariant = TextSecondaryLight,
    outline = SlateBorderLight,
    error = CrimsonDark,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

val SecureAuthShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SecureAuthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SecureAuthDarkColorScheme else SecureAuthLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SecureAuthTypography,
        shapes = SecureAuthShapes,
        content = content
    )
}

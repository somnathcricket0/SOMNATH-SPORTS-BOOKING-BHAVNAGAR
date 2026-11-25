package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SomnathLightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = CrispWhite,
    primaryContainer = RoyalBlueLight,
    onPrimaryContainer = RoyalBlueDark,
    secondary = RoyalBlueMid,
    onSecondary = CrispWhite,
    secondaryContainer = SurfaceMuted,
    onSecondaryContainer = RoyalBlueDark,
    tertiary = GoldAccent,
    onTertiary = CrispWhite,
    tertiaryContainer = GoldLight,
    onTertiaryContainer = TextPrimary,
    background = SoftWhite,
    onBackground = TextPrimary,
    surface = CrispWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSecondary,
    outline = BorderBlue,
    outlineVariant = RoyalBlueLight,
    error = RejectedRed,
    onError = CrispWhite,
    errorContainer = RejectedRedBg,
    onErrorContainer = RejectedRed
)

private val SomnathShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SomnathSportsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SomnathLightColorScheme,
        typography = SomnathTypography,
        shapes = SomnathShapes,
        content = content
    )
}

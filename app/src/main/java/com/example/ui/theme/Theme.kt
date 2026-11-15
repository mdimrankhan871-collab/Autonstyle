package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AutonstyleLightColorScheme = lightColorScheme(
    primary = AutonNavy,
    onPrimary = AutonWhite,
    primaryContainer = AutonNavySoft,
    onPrimaryContainer = AutonNavy,
    secondary = AutonLime,
    onSecondary = AutonNavy,
    secondaryContainer = AutonLimeSoft,
    onSecondaryContainer = AutonNavy,
    tertiary = AutonRed,
    onTertiary = AutonWhite,
    tertiaryContainer = AutonRedSoft,
    onTertiaryContainer = AutonRed,
    background = AutonWhite,
    onBackground = AutonTextPrimary,
    surface = AutonWhite,
    onSurface = AutonTextPrimary,
    surfaceVariant = AutonCanvas,
    onSurfaceVariant = AutonTextSecondary,
    surfaceContainerLowest = AutonWhite,
    surfaceContainerLow = AutonCanvas,
    surfaceContainer = AutonSurfaceAlt,
    outline = AutonBorder,
    outlineVariant = AutonBorder,
    error = AutonRed,
    onError = AutonWhite,
    errorContainer = AutonRedSoft,
    onErrorContainer = AutonRed
)

val AutonstyleShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun AutonstyleTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AutonstyleLightColorScheme,
        typography = AutonstyleTypography,
        shapes = AutonstyleShapes,
        content = content
    )
}

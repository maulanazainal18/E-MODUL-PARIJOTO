package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DarkSagePrimary,
    onPrimary = DarkForestBg,
    primaryContainer = ForestGreenDark,
    onPrimaryContainer = ForestGreenContainer,
    secondary = GoldAccent,
    onSecondary = DarkForestBg,
    secondaryContainer = DarkForestSurfaceVariant,
    onSecondaryContainer = GoldContainer,
    tertiary = DarkParijotoTertiary,
    onTertiary = DarkForestBg,
    tertiaryContainer = ParijotoPurple,
    onTertiaryContainer = ParijotoPurpleContainer,
    background = DarkForestBg,
    onBackground = DarkCreamText,
    surface = DarkForestSurface,
    onSurface = DarkCreamText,
    surfaceVariant = DarkForestSurfaceVariant,
    onSurfaceVariant = LightSageGreen,
    outline = OutlineBotanical
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = PureWhite,
    primaryContainer = ForestGreenContainer,
    onPrimaryContainer = ForestGreenDark,
    secondary = LightSageGreen,
    onSecondary = InkDark,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = InkDark,
    tertiary = ParijotoPurple,
    onTertiary = PureWhite,
    tertiaryContainer = ParijotoPurpleContainer,
    onTertiaryContainer = ParijotoPurple,
    background = CreamBackground,
    onBackground = InkDark,
    surface = PureWhite,
    onSurface = InkDark,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = OutlineBotanical,
    error = ErrorRed,
    onError = PureWhite
)

val ParijotoShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun ParijotoLearningTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ParijotoShapes,
        content = content
    )
}

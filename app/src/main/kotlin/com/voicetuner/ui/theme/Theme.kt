package com.voicetuner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryLight,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryLight,
    tertiary = Tertiary,
    tertiaryContainer = TertiaryLight,
    background = Background,
    surface = SurfaceLight,
    surfaceVariant = Background,
    onBackground = OnBackground,
    onSurface = OnSurfaceLight,
    outline = TextSubtle
)

@Composable
fun VoiceTunerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

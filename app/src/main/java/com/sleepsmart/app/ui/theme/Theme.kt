package com.sleepsmart.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SleepSmartDarkColors = darkColorScheme(
    primary = Purple400,
    onPrimary = Navy950,
    primaryContainer = Navy700,
    onPrimaryContainer = TextPrimary,
    secondary = Purple300,
    onSecondary = Navy950,
    background = Navy950,
    onBackground = TextPrimary,
    surface = Navy900,
    onSurface = TextPrimary,
    surfaceVariant = Navy800,
    onSurfaceVariant = TextMuted,
    outline = Navy700,
    error = StageAwake,
    onError = Navy950
)

@Composable
fun SleepSmartTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SleepSmartDarkColors,
        typography = SleepSmartTypography,
        content = content
    )
}

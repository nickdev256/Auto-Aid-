package com.project.auto_aid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AutoAidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    val LightColors = lightColorScheme(
        primary = BluePrimary,
        onPrimary = Color.White,

        secondary = BlueSecondary,
        onSecondary = Color.White,

        background = BackgroundLight,
        onBackground = TextDark,

        surface = BackgroundLight,
        onSurface = TextDark,

        surfaceVariant = Color(0xFFFFFFFF),
        onSurfaceVariant = TextDark,

        outline = Color(0xFFDADFFF)
    )

    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography(),
        content = content
    )
}

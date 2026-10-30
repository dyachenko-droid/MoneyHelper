package ru.denpom.finance.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun MoneyTheme(dark: Boolean, content: @Composable () -> Unit) {
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.pri, onPrimary = c.onPri, primaryContainer = c.priC, onPrimaryContainer = c.onPriC,
            secondary = c.amb, secondaryContainer = c.ambC, onSecondaryContainer = c.onAmbC,
            background = c.bg, onBackground = c.on, surface = c.s1, onSurface = c.on,
            surfaceVariant = c.s2, onSurfaceVariant = c.on2, surfaceContainer = c.s1, surfaceContainerHigh = c.s1,
            surfaceContainerHighest = c.s2, surfaceContainerLow = c.s1, surfaceContainerLowest = c.bg,
            outline = c.s3, outlineVariant = c.s2, error = c.exp,
        )
    } else {
        lightColorScheme(
            primary = c.pri, onPrimary = c.onPri, primaryContainer = c.priC, onPrimaryContainer = c.onPriC,
            secondary = c.amb, secondaryContainer = c.ambC, onSecondaryContainer = c.onAmbC,
            background = c.bg, onBackground = c.on, surface = c.s1, onSurface = c.on,
            surfaceVariant = c.s2, onSurfaceVariant = c.on2, surfaceContainer = c.s1, surfaceContainerHigh = c.s1,
            surfaceContainerHighest = c.s2, surfaceContainerLow = c.s1, surfaceContainerLowest = c.bg,
            outline = c.s3, outlineVariant = c.s2, error = c.exp,
        )
    }
    CompositionLocalProvider(LocalAppColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = MaterialTypography, content = content)
    }
}

object App {
    val colors: AppColors
        @Composable get() = LocalAppColors.current
}

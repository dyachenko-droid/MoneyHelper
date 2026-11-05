package ru.denpom.finance.app.util

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/** Follow device orientation on startup/web screens, restoring the app orientation on exit. */
@Composable
fun RotatingScreen(content: @Composable (isLandscape: Boolean) -> Unit) {
    val context = LocalContext.current
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val previous =
            activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        onDispose { activity?.requestedOrientation = previous }
    }

    content(isLandscape)
}

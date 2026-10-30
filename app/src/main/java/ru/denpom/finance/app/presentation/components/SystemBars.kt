package ru.denpom.finance.app.presentation.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.Window
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun Window.hideSystemBars() {
    WindowCompat.getInsetsController(this, decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.systemBars())
    }
}

fun Window.showSystemBars() {
    WindowCompat.getInsetsController(this, decorView).show(WindowInsetsCompat.Type.systemBars())
}

private fun Context.activityWindow(): Window? =
    generateSequence(this) { (it as? ContextWrapper)?.baseContext }
        .filterIsInstance<Activity>()
        .firstOrNull()
        ?.window

@Composable
fun HideSystemBars() {
    val view = LocalView.current
    LaunchedEffect(view) {
        generateSequence<Any>(view) { (it as? View)?.parent }
            .filterIsInstance<DialogWindowProvider>()
            .firstOrNull()
            ?.window
            ?.hideSystemBars()
    }
}

@Composable
fun rememberPermissionRequest(onResult: (Boolean) -> Unit): (String) -> Unit {
    val view = LocalView.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        view.context.activityWindow()?.hideSystemBars()
        onResult(granted)
    }
    return remember(launcher, view) {
        { permission ->
            view.context.activityWindow()?.showSystemBars()
            launcher.launch(permission)
        }
    }
}

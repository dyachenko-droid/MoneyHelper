package ru.denpom.finance.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.denpom.finance.app.domain.model.ThemeMode
import ru.denpom.finance.app.presentation.theme.MoneyTheme
import ru.denpom.finance.app.presentation.launch.StartupGate
import ru.denpom.finance.app.presentation.launch.StartupViewModel
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.AndroidEntryPoint
import ru.denpom.finance.app.presentation.MainViewModel
import ru.denpom.finance.app.presentation.MoneyRoot
import ru.denpom.finance.app.presentation.components.hideSystemBars
import ru.denpom.finance.app.widget.ACTION_ADD
import ru.denpom.finance.app.widget.ACTION_SCAN

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val startup: StartupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handle(intent)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = viewModel.onForeground()
            override fun onStop(owner: LifecycleOwner) = viewModel.onBackground()
        })
        setContent {
            val startupState by startup.state.collectAsStateWithLifecycle()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val dark = when (settings?.theme) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> systemDark
            }
            LaunchedEffect(startupState) { startup.onScreenVisible() }
            MoneyTheme(dark) {
                StartupGate(startupState, startup::acceptPrivacy) {
                    MoneyRoot(systemDark = systemDark, viewModel = viewModel)
                }
            }
        }
        window.hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) window.hideSystemBars()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        when (intent?.action) {
            ACTION_SCAN -> viewModel.requestLaunch(LaunchAction.SCAN)
            ACTION_ADD -> viewModel.requestLaunch(LaunchAction.ADD)
        }
    }
}

enum class LaunchAction { SCAN, ADD }

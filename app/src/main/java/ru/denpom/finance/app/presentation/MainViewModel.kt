package ru.denpom.finance.app.presentation

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.denpom.finance.app.LaunchAction
import ru.denpom.finance.app.domain.model.AppSettings
import ru.denpom.finance.app.domain.repository.SettingsRepository
import ru.denpom.finance.app.domain.usecase.StartupUseCase
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val startup: StartupUseCase,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settingsRepo.settings.map<AppSettings, AppSettings?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _locked = MutableStateFlow(true)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private val _launch = MutableStateFlow<LaunchAction?>(null)
    val launch: StateFlow<LaunchAction?> = _launch.asStateFlow()

    private var backgroundAt = 0L

    init {
        viewModelScope.launch {
            _locked.value = settingsRepo.current().pinEnabled
            startup.run()
        }
    }

    fun onBackground() {
        backgroundAt = SystemClock.elapsedRealtime()
    }

    fun onForeground() {
        if (backgroundAt == 0L) return
        val away = SystemClock.elapsedRealtime() - backgroundAt
        backgroundAt = 0L
        if (away > LOCK_AFTER_MS && settings.value?.pinEnabled == true) _locked.value = true
    }

    fun unlock() {
        _locked.value = false
    }

    suspend fun checkPin(pin: String): Boolean = settingsRepo.checkPin(pin)

    fun requestLaunch(action: LaunchAction) {
        _launch.value = action
    }

    fun consumeLaunch() {
        _launch.value = null
    }

    private companion object {
        const val LOCK_AFTER_MS = 30_000L
    }
}

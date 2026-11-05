package ru.denpom.finance.app.presentation.launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.denpom.finance.app.domain.launch.AcceptPrivacyUseCase
import ru.denpom.finance.app.domain.launch.LaunchDestination
import ru.denpom.finance.app.domain.launch.LaunchPreferences
import ru.denpom.finance.app.domain.launch.RequestNotificationPermissionUseCase
import ru.denpom.finance.app.domain.launch.ResolveLaunchUseCase

sealed interface StartupState {
    data object Loading : StartupState
    data class Ready(val destination: LaunchDestination) : StartupState
}

@HiltViewModel
class StartupViewModel @Inject constructor(
    resolveLaunch: ResolveLaunchUseCase,
    preferences: LaunchPreferences,
    private val acceptDefaultPrivacy: AcceptPrivacyUseCase,
    private val requestNotifications: RequestNotificationPermissionUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow<StartupState>(StartupState.Loading)
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val firstLaunch = preferences.isFirstLaunch
            val destination = resolveLaunch()
            if (firstLaunch) delay(650)
            mutableState.value = StartupState.Ready(destination)
        }
    }

    fun acceptPrivacy() {
        val current = (mutableState.value as? StartupState.Ready)?.destination as? LaunchDestination.Web ?: return
        if (!current.requiresAcceptance) return
        acceptDefaultPrivacy()
        mutableState.value = StartupState.Ready(LaunchDestination.Native)
    }

    suspend fun onScreenVisible() {
        val destination = (mutableState.value as? StartupState.Ready)?.destination ?: return
        requestNotifications(destination)
    }
}

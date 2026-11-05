package ru.denpom.finance.app.domain.launch

import kotlinx.coroutines.flow.MutableStateFlow

class FakeLaunchPreferences(
    override var isFirstLaunch: Boolean = true,
    override var privacyUrl: String = DEFAULT_PRIVACY_URL,
    override var hasRemotePrivacyUrl: Boolean = false,
) : LaunchPreferences {
    override var notificationPermissionRequested = false
    var writes = 0
    override fun saveDefaultPrivacy() { writes++; privacyUrl = DEFAULT_PRIVACY_URL; hasRemotePrivacyUrl = false }
    override fun completeFirstLaunch(url: String, fromFirebase: Boolean) {
        writes++; isFirstLaunch = false; privacyUrl = url; hasRemotePrivacyUrl = fromFirebase
    }
}

class FakeAttributionRepository(initial: InstallAttribution? = null) : AttributionRepository {
    override val updates = MutableStateFlow(initial)
    override var appsFlyerId = "af-123"
    var refreshes = 0
    var refresh: suspend () -> InstallAttribution? = { null }
    override suspend fun refresh(): InstallAttribution? { refreshes++; return refresh.invoke() }
}

class FakeNotifications : NotificationRepository {
    override var available = true
    var requests = 0
    val logins = mutableListOf<String>()
    var loginError = false
    var requestError = false
    override fun identify(externalId: String) {
        if (loginError) error("not ready")
        logins += externalId
    }
    override suspend fun requestPermission() {
        if (requestError) error("not ready")
        requests++
    }
}

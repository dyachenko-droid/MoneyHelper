package ru.denpom.finance.app.domain.launch

import kotlinx.coroutines.CancellationException

class SyncNotificationIdentityUseCase(
    private val attribution: AttributionRepository,
    private val notifications: NotificationRepository,
) {
    private var linkedId: String? = null
    operator fun invoke() {
        if (!notifications.available) return
        val id = runCatching { attribution.appsFlyerId }.getOrDefault("")
        if (id.isNotBlank() && id != linkedId) {
            runCatching { notifications.identify(id) }.onSuccess { linkedId = id }
        }
    }
}

class RequestNotificationPermissionUseCase(
    private val preferences: LaunchPreferences,
    private val notifications: NotificationRepository,
) {
    suspend operator fun invoke(destination: LaunchDestination) {
        val allowed = destination == LaunchDestination.Native ||
            (destination is LaunchDestination.Web && !destination.requiresAcceptance)
        if (!allowed || !notifications.available || preferences.notificationPermissionRequested) return
        preferences.notificationPermissionRequested = true
        try { notifications.requestPermission() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { preferences.notificationPermissionRequested = false }
    }
}

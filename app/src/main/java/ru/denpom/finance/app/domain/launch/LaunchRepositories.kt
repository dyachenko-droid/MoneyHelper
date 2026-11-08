package ru.denpom.finance.app.domain.launch

import kotlinx.coroutines.flow.StateFlow

interface LaunchPreferences {
    val isFirstLaunch: Boolean
    val privacyUrl: String
    val hasRemotePrivacyUrl: Boolean
    var notificationPermissionRequested: Boolean
    fun saveDefaultPrivacy()
    fun completeFirstLaunch(url: String, fromFirebase: Boolean)
}

interface AttributionRepository {
    val updates: StateFlow<InstallAttribution?>
    val appsFlyerId: String
    suspend fun deviceAdvertisingId(): String?
    suspend fun refresh(): InstallAttribution?
}

fun interface PrivacyLinkRepository {
    suspend fun read(field: String): String?
}

interface NotificationRepository {
    val available: Boolean
    fun identify(externalId: String)
    suspend fun requestPermission()
}

package ru.denpom.finance.app.domain.launch

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class NotificationUseCasesTest {
    @Test fun externalIdIsTheAppsFlyerUidAndRepeatedSignalsDoNotDuplicateLogin() {
        val repo = FakeAttributionRepository()
        val notifications = FakeNotifications()
        val sync = SyncNotificationIdentityUseCase(repo, notifications)
        sync(); sync()
        assertEquals(listOf(repo.appsFlyerId), notifications.logins)
    }

    @Test fun blankUidCanBeBoundWhenItBecomesAvailable() {
        val repo = FakeAttributionRepository().apply { appsFlyerId = "" }
        val notifications = FakeNotifications()
        val sync = SyncNotificationIdentityUseCase(repo, notifications)
        sync()
        assertTrue(notifications.logins.isEmpty())
        repo.appsFlyerId = "ready-uid"
        sync()
        assertEquals(listOf("ready-uid"), notifications.logins)
    }

    @Test fun failedIdentityBindingCanBeRetried() {
        val repo = FakeAttributionRepository()
        val notifications = FakeNotifications().apply { loginError = true }
        val sync = SyncNotificationIdentityUseCase(repo, notifications)
        sync()
        notifications.loginError = false
        sync()
        assertEquals(listOf(repo.appsFlyerId), notifications.logins)
    }

    @Test fun defaultPrivacyDoesNotPromptBeforeAcceptance() = runTest {
        val prefs = FakeLaunchPreferences()
        val notifications = FakeNotifications()
        val request = RequestNotificationPermissionUseCase(prefs, notifications)
        request(LaunchDestination.Web(DEFAULT_PRIVACY_URL, true))
        assertEquals(0, notifications.requests)
        assertFalse(prefs.notificationPermissionRequested)
        request(LaunchDestination.Native)
        assertEquals(1, notifications.requests)
        assertTrue(prefs.notificationPermissionRequested)
    }

    @Test fun remoteWebPromptsOnlyOnceAcrossRouteChanges() = runTest {
        val prefs = FakeLaunchPreferences()
        val notifications = FakeNotifications()
        val request = RequestNotificationPermissionUseCase(prefs, notifications)
        request(LaunchDestination.Web("12ук", false))
        request(LaunchDestination.Native)
        assertEquals(1, notifications.requests)
    }

    @Test fun missingNotificationConfigurationDoesNotPromptOrBind() = runTest {
        val prefs = FakeLaunchPreferences()
        val notifications = FakeNotifications().apply { available = false }
        RequestNotificationPermissionUseCase(prefs, notifications)(LaunchDestination.Native)
        SyncNotificationIdentityUseCase(FakeAttributionRepository(), notifications)()
        assertEquals(0, notifications.requests)
        assertFalse(prefs.notificationPermissionRequested)
        assertTrue(notifications.logins.isEmpty())
    }

    @Test fun failedPermissionRequestDoesNotPreventRetry() = runTest {
        val prefs = FakeLaunchPreferences()
        val notifications = FakeNotifications().apply { requestError = true }
        val request = RequestNotificationPermissionUseCase(prefs, notifications)
        request(LaunchDestination.Native)
        assertFalse(prefs.notificationPermissionRequested)
        notifications.requestError = false
        request(LaunchDestination.Native)
        assertEquals(1, notifications.requests)
    }
}

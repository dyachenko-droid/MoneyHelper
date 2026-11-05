package ru.denpom.finance.app.data.launch

import com.onesignal.OneSignal
import ru.denpom.finance.app.domain.launch.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OneSignalNotificationRepository @Inject constructor() : NotificationRepository {
    override val available get() = IntegrationConfig.oneSignalConfigured
    override fun identify(externalId: String) { if (available) OneSignal.login(externalId) }
    override suspend fun requestPermission() { if (available) OneSignal.Notifications.requestPermission(false) }
}

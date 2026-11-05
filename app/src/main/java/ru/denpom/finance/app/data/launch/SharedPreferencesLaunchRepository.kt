package ru.denpom.finance.app.data.launch

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.denpom.finance.app.domain.launch.DEFAULT_PRIVACY_URL
import ru.denpom.finance.app.domain.launch.LaunchPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesLaunchRepository @Inject constructor(@ApplicationContext context: Context) : LaunchPreferences {
    private val prefs = context.getSharedPreferences("startup", Context.MODE_PRIVATE)
    override val isFirstLaunch get() = prefs.getBoolean("first_launch", true)
    override val privacyUrl get() = prefs.getString("privacy_url", DEFAULT_PRIVACY_URL) ?: DEFAULT_PRIVACY_URL
    override val hasRemotePrivacyUrl get() = prefs.getBoolean("remote_privacy", false)
    override var notificationPermissionRequested: Boolean
        get() = prefs.getBoolean("notification_permission_requested", false)
        set(value) { prefs.edit().putBoolean("notification_permission_requested", value).apply() }

    override fun saveDefaultPrivacy() {
        prefs.edit().putString("privacy_url", DEFAULT_PRIVACY_URL).putBoolean("remote_privacy", false).apply()
    }

    override fun completeFirstLaunch(url: String, fromFirebase: Boolean) {
        prefs.edit().putString("privacy_url", url).putBoolean("first_launch", false)
            .putBoolean("remote_privacy", fromFirebase).apply()
    }
}

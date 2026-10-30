package ru.denpom.finance.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.denpom.finance.app.domain.model.AppSettings
import ru.denpom.finance.app.domain.model.Currency
import ru.denpom.finance.app.domain.model.ThemeMode
import ru.denpom.finance.app.domain.repository.SettingsRepository
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.store: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private object Keys {
        val onboarding = booleanPreferencesKey("onboarding_done")
        val theme = stringPreferencesKey("theme")
        val hide = booleanPreferencesKey("hide_amounts")
        val currency = stringPreferencesKey("currency")
        val name = stringPreferencesKey("user_name")
        val pin = stringPreferencesKey("pin_hash")
        val biometric = booleanPreferencesKey("biometric")
        val daily = booleanPreferencesKey("daily_reminder")
        val minutes = intPreferencesKey("reminder_minutes")
        val payments = booleanPreferencesKey("payment_reminders")
    }

    override val settings: Flow<AppSettings> = context.store.data.map { p ->
        AppSettings(
            onboardingDone = p[Keys.onboarding] ?: false,
            theme = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            hideAmounts = p[Keys.hide] ?: false,
            currency = p[Keys.currency]?.let { runCatching { Currency.valueOf(it) }.getOrNull() } ?: Currency.RUB,
            userName = p[Keys.name].orEmpty(),
            pinHash = p[Keys.pin].orEmpty(),
            biometric = p[Keys.biometric] ?: false,
            dailyReminder = p[Keys.daily] ?: false,
            reminderMinutes = p[Keys.minutes] ?: 21 * 60,
            paymentReminders = p[Keys.payments] ?: false,
        )
    }

    override suspend fun current(): AppSettings = settings.first()

    override suspend fun setOnboardingDone(done: Boolean) {
        context.store.edit { it[Keys.onboarding] = done }
    }

    override suspend fun setTheme(mode: ThemeMode) {
        context.store.edit { it[Keys.theme] = mode.name }
    }

    override suspend fun setHideAmounts(hide: Boolean) {
        context.store.edit { it[Keys.hide] = hide }
    }

    override suspend fun setCurrency(currency: Currency) {
        context.store.edit { it[Keys.currency] = currency.name }
    }

    override suspend fun setUserName(name: String) {
        context.store.edit { it[Keys.name] = name.trim() }
    }

    override suspend fun setPin(pin: String?) {
        context.store.edit {
            if (pin.isNullOrBlank()) {
                it.remove(Keys.pin)
                it[Keys.biometric] = false
            } else {
                it[Keys.pin] = hash(pin)
            }
        }
    }

    override suspend fun checkPin(pin: String): Boolean = current().pinHash == hash(pin)

    override suspend fun setBiometric(enabled: Boolean) {
        context.store.edit { it[Keys.biometric] = enabled }
    }

    override suspend fun setDailyReminder(enabled: Boolean, minutes: Int) {
        context.store.edit {
            it[Keys.daily] = enabled
            it[Keys.minutes] = minutes
        }
    }

    override suspend fun setPaymentReminders(enabled: Boolean) {
        context.store.edit { it[Keys.payments] = enabled }
    }

    private fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256").digest("denpom:$pin".toByteArray()).joinToString("") { "%02x".format(it) }
}

package ru.denpom.finance.app.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.denpom.finance.app.domain.model.AppSettings
import ru.denpom.finance.app.domain.model.Currency
import ru.denpom.finance.app.domain.model.ThemeMode

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun current(): AppSettings
    suspend fun setOnboardingDone(done: Boolean)
    suspend fun setTheme(mode: ThemeMode)
    suspend fun setHideAmounts(hide: Boolean)
    suspend fun setCurrency(currency: Currency)
    suspend fun setUserName(name: String)
    suspend fun setPin(pin: String?)
    suspend fun checkPin(pin: String): Boolean
    suspend fun setBiometric(enabled: Boolean)
    suspend fun setDailyReminder(enabled: Boolean, minutes: Int)
    suspend fun setPaymentReminders(enabled: Boolean)
}

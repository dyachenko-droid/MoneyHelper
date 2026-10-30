package ru.denpom.finance.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import ru.denpom.finance.app.data.reminders.ReminderScheduler
import ru.denpom.finance.app.domain.repository.AccountRepository
import ru.denpom.finance.app.domain.repository.BudgetRepository
import ru.denpom.finance.app.domain.repository.OperationRepository
import ru.denpom.finance.app.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val operations: OperationRepository,
    private val accounts: AccountRepository,
    private val budgets: BudgetRepository,
    private val settings: SettingsRepository,
    private val reminders: ReminderScheduler,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(FlowPreview::class)
    fun start() {
        scope.launch { reminders.apply(settings.current()) }
        scope.launch {
            combine(operations.observeOperations(), accounts.observeAccounts(), budgets.observeBudgets(), settings.settings) { a, b, c, d -> listOf(a, b, c, d.currency, d.hideAmounts, d.pinHash) }
                .drop(1)
                .debounce(800)
                .collect { refreshWidgets() }
        }
        scope.launch {
            settings.settings.distinctUntilChangedBy { it.dailyReminder to it.paymentReminders to it.reminderMinutes }.drop(1).collect { reminders.apply(it) }
        }
    }

    suspend fun refreshWidgets() {
        runCatching {
            BalanceWidget().updateAll(context)
            BudgetWidget().updateAll(context)
        }
    }

}

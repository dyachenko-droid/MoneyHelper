package ru.denpom.finance.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.denpom.finance.app.domain.model.Account
import ru.denpom.finance.app.domain.model.Category
import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.data.repository.PaymentCalendarRepository
import ru.denpom.finance.app.domain.model.PaymentOccurrence
import ru.denpom.finance.app.domain.usecase.PaymentSchedule
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.domain.model.Recurring
import ru.denpom.finance.app.domain.repository.AccountRepository
import ru.denpom.finance.app.domain.repository.BudgetRepository
import ru.denpom.finance.app.domain.repository.CategoryRepository
import ru.denpom.finance.app.domain.repository.OperationRepository
import ru.denpom.finance.app.domain.repository.RecurringRepository
import ru.denpom.finance.app.domain.repository.SettingsRepository
import ru.denpom.finance.app.domain.usecase.SpendingCalculator
import ru.denpom.finance.app.domain.usecase.BudgetPlanner
import ru.denpom.finance.app.domain.usecase.BudgetPlan
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class HomeState(
    val loaded: Boolean = false,
    val userName: String = "",
    val total: Long = 0,
    val accounts: List<Account> = emptyList(),
    val month: YearMonth = YearMonth.now(),
    val limit: Long? = null,
    val spent: Long = 0,
    val daysLeft: Int = 0,
    val monthProgress: Float = 0f,
    val forecast: Long = 0,
    val upcoming: List<Recurring> = emptyList(),
    val upcomingTotal: Long = 0,
    val plan: BudgetPlan = BudgetPlan(0, 0, null, null),
    val insight: String? = null,
    val recent: List<Operation> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val accountNames: Map<Long, String> = emptyMap(),
) {
    val left: Long get() = (limit ?: 0) - spent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    accounts: AccountRepository,
    operations: OperationRepository,
    categories: CategoryRepository,
    budgets: BudgetRepository,
    recurring: RecurringRepository,
    calendar: PaymentCalendarRepository,
    private val settings: SettingsRepository,
    private val calc: SpendingCalculator,
) : ViewModel() {

    private val base = combine(
        accounts.observeAccounts(),
        operations.observeOperations(),
        categories.observeCategories(),
        budgets.observeBudgets(),
        recurring.observeRecurring(),
    ) { acc, ops, cats, buds, rec -> Inputs(acc, ops, cats, buds.firstOrNull { it.categoryId == null }?.limit, rec) }

    val state: StateFlow<HomeState> = combine(base, settings.settings, calendar.occurrences) { input, s, overrides -> build(input, s.userName, overrides) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun toggleHide() {
        viewModelScope.launch { settings.setHideAmounts(!settings.current().hideAmounts) }
    }

    private data class Inputs(
        val accounts: List<Account>,
        val operations: List<Operation>,
        val categories: List<Category>,
        val limit: Long?,
        val recurring: List<Recurring>,
    )

    private fun build(input: Inputs, name: String, overrides: List<PaymentOccurrence>): HomeState {
        val today = LocalDate.now()
        val month = YearMonth.from(today)
        val spent = calc.totalSpent(input.operations, month)
        val cats = input.categories.associateBy { it.id }
        val upcoming = PaymentSchedule.pendingExpenses(input.recurring, overrides, input.operations, today, today.plusDays(29))
            .map { it.rule.copy(nextDate = Period.startOfDay(it.occurrence.due)) }
        val plan = BudgetPlanner.calculate(input.limit, spent, input.recurring, input.operations, today, overrides)
        return HomeState(
            loaded = true,
            userName = name,
            total = input.accounts.sumOf { it.balance },
            accounts = input.accounts,
            month = month,
            limit = input.limit,
            spent = spent,
            daysLeft = month.lengthOfMonth() - today.dayOfMonth + 1,
            monthProgress = today.dayOfMonth.toFloat() / month.lengthOfMonth(),
            forecast = calc.forecast(spent, month, today),
            upcoming = upcoming.take(3),
            upcomingTotal = plan.reserved,
            plan = plan,
            insight = insight(input.operations, cats, month, today),
            recent = input.operations.take(5),
            categories = cats,
            accountNames = input.accounts.associate { it.id to it.name },
        )
    }

    private fun insight(ops: List<Operation>, cats: Map<Long, Category>, month: YearMonth, today: LocalDate): String? {
        val now = calc.byCategory(ops, month, today.dayOfMonth)
        val prev = calc.byCategory(ops, month.minusMonths(1), today.dayOfMonth)
        val best = now.mapNotNull { (id, sum) ->
            val before = prev[id] ?: return@mapNotNull null
            if (before < 50_000 || sum <= before) null else Triple(id, sum, ((sum - before) * 100 / before).toInt())
        }.filter { it.third >= 10 }.maxByOrNull { it.second - (prev[it.first] ?: 0) }
        if (best != null) {
            val name = cats[best.first]?.name?.lowercase() ?: return null
            return "На $name в этом месяце на ${best.third}% больше, чем в прошлом"
        }
        val saved = prev.mapNotNull { (id, before) ->
            val sum = now[id] ?: 0
            if (before < 50_000 || sum >= before) null else Triple(id, sum, ((before - sum) * 100 / before).toInt())
        }.filter { it.third >= 15 }.maxByOrNull { it.third } ?: return null
        val name = cats[saved.first]?.name?.lowercase() ?: return null
        return "На $name вы тратите на ${saved.third}% меньше, чем месяц назад"
    }
}

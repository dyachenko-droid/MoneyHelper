package ru.denpom.finance.app.presentation.subscriptions

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
import ru.denpom.finance.app.domain.model.CategoryKind
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.model.RecurrencePeriod
import ru.denpom.finance.app.domain.model.Recurring
import ru.denpom.finance.app.domain.repository.AccountRepository
import ru.denpom.finance.app.domain.repository.CategoryRepository
import ru.denpom.finance.app.domain.repository.RecurringRepository
import ru.denpom.finance.app.domain.usecase.StartupUseCase
import javax.inject.Inject

data class SubscriptionsState(
    val items: List<Recurring> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
) {
    val monthly: Long
        get() = items.filter { it.type == OperationType.EXPENSE }.sumOf {
            when (it.period) {
                RecurrencePeriod.WEEKLY -> it.amount * 52 / 12
                RecurrencePeriod.MONTHLY -> it.amount
                RecurrencePeriod.YEARLY -> it.amount / 12
            }
        }
}

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val recurring: RecurringRepository,
    accounts: AccountRepository,
    categories: CategoryRepository,
    private val startup: StartupUseCase,
) : ViewModel() {

    val state: StateFlow<SubscriptionsState> = combine(
        recurring.observeRecurring(),
        accounts.observeAccounts(),
        categories.observeCategories(),
    ) { r, a, c -> SubscriptionsState(r, a, c.filter { it.kind == CategoryKind.EXPENSE }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubscriptionsState())

    fun save(item: Recurring) {
        viewModelScope.launch {
            recurring.save(item)
            startup.materializeRecurring()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { recurring.delete(id) }
    }
}

package ru.denpom.finance.app.presentation.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.denpom.finance.app.domain.model.Account
import ru.denpom.finance.app.domain.model.Debt
import ru.denpom.finance.app.domain.model.Loan
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.domain.model.RecurrencePeriod
import ru.denpom.finance.app.domain.model.Recurring
import ru.denpom.finance.app.domain.repository.AccountRepository
import ru.denpom.finance.app.domain.repository.DebtRepository
import ru.denpom.finance.app.domain.repository.RecurringRepository
import java.time.LocalDate
import javax.inject.Inject

data class DebtsState(
    val debts: List<Debt> = emptyList(),
    val loans: List<Loan> = emptyList(),
    val accounts: List<Account> = emptyList(),
)

@HiltViewModel
class DebtsViewModel @Inject constructor(
    private val repo: DebtRepository,
    private val recurring: RecurringRepository,
    accounts: AccountRepository,
) : ViewModel() {

    val state: StateFlow<DebtsState> = combine(repo.observeDebts(), repo.observeLoans(), accounts.observeAccounts()) { d, l, a -> DebtsState(d, l, a) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DebtsState())

    fun save(debt: Debt) = viewModelScope.launch { repo.save(debt) }
    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }
    fun pay(id: Long, amount: Long) = viewModelScope.launch { repo.pay(id, amount) }

    fun saveLoan(loan: Loan, addRecurring: Boolean, accountId: Long?) = viewModelScope.launch {
        var recurringId = loan.recurringId
        if (addRecurring && accountId != null) {
            val today = LocalDate.now()
            val next = loan.schedule().map { Period.dateOf(it.date) }.firstOrNull { !it.isBefore(today) }
            if (next != null) {
                recurringId = recurring.save(
                    Recurring(
                        id = loan.recurringId ?: 0,
                        title = "Платёж: ${loan.name}",
                        amount = loan.monthlyPayment,
                        type = OperationType.EXPENSE,
                        accountId = accountId,
                        categoryId = null,
                        period = RecurrencePeriod.MONTHLY,
                        nextDate = Period.startOfDay(next),
                        isSubscription = false,
                        autoCreate = false,
                    )
                )
            }
        }
        repo.saveLoan(loan.copy(recurringId = recurringId))
    }

    fun deleteLoan(loan: Loan) = viewModelScope.launch {
        loan.recurringId?.let { recurring.delete(it) }
        repo.deleteLoan(loan.id)
    }
}

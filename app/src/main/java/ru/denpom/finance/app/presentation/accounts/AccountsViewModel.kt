package ru.denpom.finance.app.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.denpom.finance.app.domain.model.Account
import ru.denpom.finance.app.domain.model.AccountType
import ru.denpom.finance.app.domain.repository.AccountRepository
import ru.denpom.finance.app.domain.repository.OperationRepository
import javax.inject.Inject

data class AccountsState(
    val accounts: List<Account> = emptyList(),
    val used: Set<Long> = emptySet(),
) {
    val total: Long get() = accounts.sumOf { it.balance }
    val debt: Long get() = accounts.filter { it.balance < 0 }.sumOf { it.balance }
}

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accounts: AccountRepository,
    operations: OperationRepository,
) : ViewModel() {

    val state: StateFlow<AccountsState> = combine(accounts.observeAccounts(), operations.observeOperations()) { accs, ops ->
        AccountsState(accs, ops.flatMap { listOfNotNull(it.accountId, it.toAccountId) }.toSet())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsState())

    fun save(id: Long, name: String, type: AccountType, subtitle: String, balance: Long, creditLimit: Long?) {
        viewModelScope.launch {
            val existing = state.value.accounts.firstOrNull { it.id == id }
            val opening = if (existing == null) balance else existing.openingBalance + (balance - existing.balance)
            accounts.save(
                Account(
                    id = id,
                    name = name.trim(),
                    type = type,
                    subtitle = subtitle.trim(),
                    openingBalance = opening,
                    creditLimit = if (type == AccountType.CREDIT) creditLimit else null,
                )
            )
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { accounts.delete(id) }
    }
}

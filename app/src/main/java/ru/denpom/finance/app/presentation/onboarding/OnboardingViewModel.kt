package ru.denpom.finance.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.denpom.finance.app.domain.model.Account
import ru.denpom.finance.app.domain.model.AccountType
import ru.denpom.finance.app.domain.model.Currency
import ru.denpom.finance.app.domain.repository.AccountRepository
import ru.denpom.finance.app.domain.repository.CategoryRepository
import ru.denpom.finance.app.domain.repository.SettingsRepository
import ru.denpom.finance.app.presentation.format.parseAmount
import javax.inject.Inject

data class OnboardingState(
    val page: Int = 0,
    val currency: Currency = Currency.RUB,
    val type: AccountType = AccountType.CARD,
    val name: String = "Основная карта",
    val balance: String = "",
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun next() = _state.update { it.copy(page = (it.page + 1).coerceAtMost(3)) }
    fun back() = _state.update { it.copy(page = (it.page - 1).coerceAtLeast(0)) }
    fun skip() = _state.update { it.copy(page = 3) }
    fun setPage(page: Int) = _state.update { it.copy(page = page) }
    fun setCurrency(c: Currency) = _state.update { it.copy(currency = c) }
    fun setName(n: String) = _state.update { it.copy(name = n) }
    fun setBalance(b: String) = _state.update { it.copy(balance = b.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' || ch == '-' }) }

    fun setType(t: AccountType) = _state.update {
        it.copy(
            type = t,
            name = when (t) {
                AccountType.CARD -> "Основная карта"
                AccountType.CASH -> "Кошелёк"
                AccountType.DEPOSIT -> "Вклад"
                AccountType.CREDIT -> "Кредитка"
            },
        )
    }

    fun finish(onDone: (String) -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            categories.ensureDefaults()
            settings.setCurrency(s.currency)
            val name = s.name.trim().ifBlank { "Новый счёт" }
            if (accounts.count() == 0) {
                accounts.save(
                    Account(
                        id = 0,
                        name = name,
                        type = s.type,
                        subtitle = "",
                        openingBalance = parseAmount(s.balance),
                        creditLimit = null,
                    )
                )
            }
            settings.setOnboardingDone(true)
            onDone(name)
        }
    }
}

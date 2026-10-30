package ru.denpom.finance.app.presentation.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.denpom.finance.app.domain.model.AppSettings
import ru.denpom.finance.app.domain.model.Currency
import ru.denpom.finance.app.domain.model.ThemeMode
import ru.denpom.finance.app.domain.repository.AccountRepository
import ru.denpom.finance.app.domain.repository.CategoryRepository
import ru.denpom.finance.app.domain.repository.RecurringRepository
import ru.denpom.finance.app.domain.repository.SettingsRepository
import javax.inject.Inject

data class MoreState(
    val settings: AppSettings = AppSettings(),
    val accounts: Int = 0,
    val rules: Int = 0,
    val recurring: Int = 0,
)

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val settings: SettingsRepository,
    accounts: AccountRepository,
    categories: CategoryRepository,
    recurring: RecurringRepository,
) : ViewModel() {

    val state: StateFlow<MoreState> = combine(
        settings.settings,
        accounts.observeAccounts(),
        categories.observeRules(),
        recurring.observeRecurring(),
    ) { s, a, r, rec -> MoreState(s, a.size, r.size, rec.size) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoreState())

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settings.setTheme(mode) }
    fun setCurrency(c: Currency) = viewModelScope.launch { settings.setCurrency(c) }
    fun setName(name: String) = viewModelScope.launch { settings.setUserName(name) }
}

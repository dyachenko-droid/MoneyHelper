package ru.denpom.finance.app.presentation.planning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.denpom.finance.app.data.repository.BalanceCheckRepository
import ru.denpom.finance.app.domain.model.Account
import ru.denpom.finance.app.domain.model.BalanceCheck
import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.domain.repository.AccountRepository
import ru.denpom.finance.app.domain.repository.OperationRepository
import ru.denpom.finance.app.domain.usecase.OperationQuery
import ru.denpom.finance.app.presentation.LocalAppActions
import ru.denpom.finance.app.presentation.components.PrimaryButton
import ru.denpom.finance.app.presentation.components.TextAction
import ru.denpom.finance.app.presentation.format.Dates
import ru.denpom.finance.app.presentation.format.Money
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import javax.inject.Inject

data class BalanceCheckState(
    val accounts: List<Account> = emptyList(),
    val operations: List<Operation> = emptyList(),
    val checks: List<BalanceCheck> = emptyList(),
)

@HiltViewModel
class BalanceCheckViewModel @Inject constructor(
    private val repository: BalanceCheckRepository,
    accounts: AccountRepository,
    operations: OperationRepository,
) : ViewModel() {
    val state = combine(accounts.observeAccounts(), operations.observeOperations(), repository.checks, ::BalanceCheckState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BalanceCheckState())

    fun save(account: Account, actual: Long, adjust: Boolean, onResult: (String?) -> Unit) = viewModelScope.launch {
        try {
            repository.save(account.id, account.balance, actual, adjust)
            onResult(null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onResult(e.message ?: "Не удалось сохранить сверку")
        }
    }
}

@Composable
fun BalanceCheckScreen(onBack: () -> Unit, onOperation: (Operation) -> Unit, viewModel: BalanceCheckViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val fmt = Money.fmt
    val actions = LocalAppActions.current
    var accountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var actualInput by rememberSaveable { mutableStateOf("") }
    var showOperations by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val account = if (accountId == null) state.accounts.firstOrNull() else state.accounts.firstOrNull { it.id == accountId }
    val history = state.checks.filter { it.accountId == account?.id }
    val last = history.firstOrNull()
    val actual = parseBalance(actualInput)
    val difference = if (actual == null || account == null) null else runCatching { Math.subtractExact(actual, account.balance) }.getOrNull()

    fun save(adjust: Boolean) {
        if (busy || account == null || actual == null) return
        busy = true
        viewModel.save(account, actual, adjust) { error ->
            busy = false
            confirm = false
            if (error == null) actualInput = ""
            actions.snack(error ?: if (adjust) "Сверка и корректировка сохранены" else "Сверка сохранена")
        }
    }

    PlanningScreen("Сверка остатков", onBack) {
        if (account == null) {
            item {
                if (state.accounts.isEmpty()) Caption("Добавьте счёт, чтобы сверить его остаток.")
                else ChoiceChip("Выберите счёт", state.accounts, { it.name }) {
                    accountId = it.id
                    actualInput = ""
                    confirm = false
                }
            }
        } else {
            item {
                PlanningCard {
                    ChoiceChip(account.name, state.accounts, { it.name }) {
                        accountId = it.id
                        actualInput = ""
                        showOperations = false
                        showHistory = false
                    }
                    Caption("По записям ${fmt.format(account.balance)}")
                    Caption(last?.let { "Последняя сверка · ${Dates.full(it.timestamp)}" } ?: "Первая сверка по этому счёту")
                    OutlinedTextField(
                        value = actualInput,
                        onValueChange = { actualInput = it.take(18) },
                        label = { Text("Фактический остаток, ${fmt.symbol}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = actualInput.isNotBlank() && actual == null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (actual != null && actual != 0L) TextAction("Сменить знак остатка") {
                        actualInput = if (actual < 0) actualInput.trim().drop(1) else "-" + actualInput.trim().removePrefix("+")
                    }
                    if (difference != null) {
                        Caption(if (difference == 0L) "Остатки совпадают" else "Расхождение ${fmt.signed(difference)}")
                    }
                    TextAction(if (showOperations) "Скрыть операции" else "Операции после последней сверки") {
                        showOperations = !showOperations
                    }
                    PrimaryButton(
                        if (difference != null && difference != 0L) "Скорректировать остаток" else "Сохранить сверку",
                        Modifier.fillMaxWidth(), enabled = difference != null && !busy,
                    ) {
                        if (difference == 0L) save(false) else confirm = true
                    }
                    if (history.isNotEmpty()) TextAction(if (showHistory) "Скрыть историю" else "История сверок") { showHistory = !showHistory }
                }
            }
            if (showOperations) {
                val since = last?.timestamp ?: Long.MIN_VALUE
                val rows = state.operations.filter { it.timestamp > since && (it.accountId == account.id || it.toAccountId == account.id) }
                if (rows.isEmpty()) item { Caption("Операций после сверки нет") }
                items(rows, key = { "operation-${it.id}" }) { op ->
                    PlanningCard {
                        Text(
                            "${op.title} · ${fmt.signed(OperationQuery.accountDelta(op, account.id))}",
                            modifier = Modifier.fillMaxWidth().clickable { onOperation(op) },
                            style = AppText.bodyMedium, color = App.colors.on,
                        )
                        Caption(Dates.full(op.timestamp))
                    }
                }
            }
            if (showHistory) items(history, key = { "check-${it.id}" }) { check ->
                PlanningCard {
                    Text(Dates.full(check.timestamp), style = AppText.bodyMedium, color = App.colors.on)
                    Caption("По записям ${fmt.format(check.recordedBalance)} · фактически ${fmt.format(check.actualBalance)}")
                    Caption(if (check.adjustment == 0L) "Остатки совпали" else "Корректировка ${fmt.signed(check.adjustment)}")
                }
            }
        }
    }

    if (confirm && difference != null) AlertDialog(
        onDismissRequest = { if (!busy) confirm = false },
        title = { Text("Скорректировать остаток?") },
        text = { Text("Остаток счёта изменится на ${fmt.signed(difference)}. Корректировка будет видна в истории сверок.") },
        confirmButton = { TextButton({ save(true) }, enabled = !busy) { Text("Скорректировать") } },
        dismissButton = { TextButton({ confirm = false }, enabled = !busy) { Text("Отмена") } },
    )
}

private fun parseBalance(raw: String): Long? {
    val value = raw.trim().replace(',', '.').replace('−', '-')
    if (!value.matches(Regex("[+-]?\\d{1,13}(\\.\\d{1,2})?"))) return null
    return runCatching { value.toBigDecimal().movePointRight(2).longValueExact() }.getOrNull()
}

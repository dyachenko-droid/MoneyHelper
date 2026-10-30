package ru.denpom.finance.app.presentation.imports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.denpom.finance.app.domain.model.Account
import ru.denpom.finance.app.domain.model.ImportPreview
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.domain.repository.AccountRepository
import ru.denpom.finance.app.domain.usecase.ImportStatementUseCase
import ru.denpom.finance.app.presentation.LocalAppActions
import ru.denpom.finance.app.presentation.components.BackTopBar
import ru.denpom.finance.app.presentation.components.EmptyState
import ru.denpom.finance.app.presentation.components.FilterChip
import ru.denpom.finance.app.presentation.components.PrimaryButton
import ru.denpom.finance.app.presentation.components.card
import ru.denpom.finance.app.presentation.format.Dates
import ru.denpom.finance.app.presentation.format.Money
import ru.denpom.finance.app.presentation.subscriptions.ToggleRow
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import javax.inject.Inject

data class ImportState(
    val loading: Boolean = false,
    val preview: ImportPreview? = null,
    val accountId: Long? = null,
    val skipTransfers: Boolean = true,
    val failed: Boolean = false,
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importer: ImportStatementUseCase,
    accounts: AccountRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ImportState())
    val state: StateFlow<ImportState> = _state.asStateFlow()
    val accounts: StateFlow<List<Account>> = accounts.observeAccounts().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun load(uri: String) = viewModelScope.launch {
        _state.update { it.copy(loading = true, failed = false) }
        val preview = runCatching { importer.preview(uri) }.getOrNull()
        _state.update {
            it.copy(
                loading = false,
                preview = preview?.takeIf { p -> p.rows.isNotEmpty() },
                failed = preview == null || preview.rows.isEmpty(),
                accountId = it.accountId ?: accounts.value.firstOrNull()?.id,
            )
        }
    }

    fun setAccount(id: Long) = _state.update { it.copy(accountId = id) }
    fun setSkip(skip: Boolean) = _state.update { it.copy(skipTransfers = skip) }

    fun commit(onDone: (Int) -> Unit) {
        val s = _state.value
        val preview = s.preview ?: return
        val accountId = s.accountId ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val n = importer.commit(preview.rows, accountId, s.skipTransfers)
            _state.value = ImportState(accountId = accountId)
            onDone(n)
        }
    }
}

@Composable
fun ImportScreen(onBack: () -> Unit, viewModel: ImportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val c = App.colors
    val fmt = Money.fmt
    val actions = LocalAppActions.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { viewModel.load(it.toString()) } }

    Column(Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding()) {
        BackTopBar("Импорт выписки", onBack)
        val preview = state.preview
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.loading) {
                item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = c.pri) } }
            } else if (preview == null) {
                item {
                    EmptyState(
                        Icons.Rounded.UploadFile,
                        if (state.failed) "Не удалось прочитать файл" else "Загрузите выписку CSV",
                        if (state.failed) "Нужен CSV с колонками «Дата» и «Сумма». В Т-Банке: Операции → Выписка → CSV."
                        else "Выгрузите операции из банка в CSV — мы разложим их по категориям и пропустим дубли",
                        action = "Выбрать файл",
                        onAction = { picker.launch(arrayOf("text/*", "application/vnd.ms-excel", "application/octet-stream")) },
                    )
                }
            } else {
                val rows = preview.rows.filter { !state.skipTransfers || !it.isTransfer }
                val expenses = rows.filter { it.amount < 0 }
                val incomes = rows.filter { it.amount > 0 }
                item {
                    Column(Modifier.card().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${rows.size} ${Dates.plural(rows.size.toLong(), "операция", "операции", "операций")}", style = AppText.title.copy(fontSize = 20.sp), color = c.on)
                        val from = preview.rows.minOf { it.timestamp }
                        val to = preview.rows.maxOf { it.timestamp }
                        Text("${Dates.dayMonth(Period.dateOf(from))} – ${Dates.dayMonth(Period.dateOf(to))}", style = AppText.caption, color = c.on2)
                        Text("Расходы ${fmt.format(-expenses.sumOf { it.amount }, false)} · доходы ${fmt.format(incomes.sumOf { it.amount }, false)}", style = AppText.caption, color = c.on)
                        if (preview.duplicates > 0) Text("Уже есть в приложении и будут пропущены: ${preview.duplicates}", style = AppText.caption, color = c.warn)
                    }
                }
                item {
                    var menu by remember { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Записать на счёт", style = AppText.body, color = c.on2)
                        Box {
                            FilterChip(accounts.firstOrNull { it.id == state.accountId }?.name ?: "Выберите") { menu = true }
                            DropdownMenu(menu, { menu = false }) { accounts.forEach { a -> DropdownMenuItem({ Text(a.name) }, { viewModel.setAccount(a.id); menu = false }) } }
                        }
                    }
                }
                item { ToggleRow("Пропускать переводы и пополнения", state.skipTransfers, viewModel::setSkip) }
                item {
                    Column(Modifier.card(RoundedCornerShape(20.dp)).padding(vertical = 4.dp)) {
                        rows.take(30).forEach { row ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(row.description, style = AppText.body, color = c.on, maxLines = 1)
                                    Text(listOf(Dates.dayMonth(Period.dateOf(row.timestamp)), row.bankCategory).filter { it.isNotBlank() }.joinToString(" · "), style = AppText.caption, color = c.on2, maxLines = 1)
                                }
                                Text(fmt.signed(row.amount), style = AppText.amount, color = if (row.amount > 0) c.inc else c.exp)
                            }
                        }
                        if (rows.size > 30) Text("и ещё ${rows.size - 30}", style = AppText.caption, color = c.on2, modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
        if (preview != null && !state.loading) {
            PrimaryButton("Импортировать", Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), enabled = state.accountId != null) {
                viewModel.commit { n ->
                    actions.snack(if (n > 0) "Добавлено $n ${Dates.plural(n.toLong(), "операция", "операции", "операций")}" else "Новых операций не нашлось")
                    onBack()
                }
            }
        }
    }
}

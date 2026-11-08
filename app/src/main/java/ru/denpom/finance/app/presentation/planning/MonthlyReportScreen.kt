package ru.denpom.finance.app.presentation.planning

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import ru.denpom.finance.app.domain.model.Category
import ru.denpom.finance.app.domain.model.Goal
import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.repository.BudgetRepository
import ru.denpom.finance.app.domain.repository.CategoryRepository
import ru.denpom.finance.app.domain.repository.GoalRepository
import ru.denpom.finance.app.domain.repository.OperationRepository
import ru.denpom.finance.app.domain.usecase.MonthlyReport
import ru.denpom.finance.app.domain.usecase.MonthlyReports
import ru.denpom.finance.app.presentation.format.Dates
import ru.denpom.finance.app.presentation.format.Money
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class MonthlyReportState(
    val month: YearMonth = YearMonth.now(),
    val report: MonthlyReport? = null,
    val categories: Map<Long, Category> = emptyMap(),
    val goals: Map<Long, Goal> = emptyMap(),
    val operations: Map<Long, Operation> = emptyMap(),
)

@HiltViewModel
class MonthlyReportViewModel @Inject constructor(
    operations: OperationRepository,
    categories: CategoryRepository,
    budgets: BudgetRepository,
    goals: GoalRepository,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    private val savings = combine(goals.observeGoals(), goals.observeEntries(), ::Pair)
    val state = combine(operations.observeOperations(), categories.observeCategories(), budgets.observeBudgets(), savings, month) {
        ops, cats, limits, (goalList, entries), selected ->
        MonthlyReportState(
            selected, MonthlyReports.calculate(selected, LocalDate.now(), ops, limits, entries),
            cats.associateBy { it.id }, goalList.associateBy { it.id }, ops.associateBy { it.id },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthlyReportState())

    fun shift(amount: Long) = month.update { it.plusMonths(amount).coerceAtMost(YearMonth.now()) }
}

private enum class ReportSection { INCOME, EXPENSES, NET, SAVINGS, GROWTH, PREVIOUS, OVERAGE }
private data class ReportDetail(val title: String, val section: ReportSection, val categoryId: Long? = null)

@Composable
fun MonthlyReportScreen(onBack: () -> Unit, onOperation: (Operation) -> Unit, viewModel: MonthlyReportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val report = state.report
    val fmt = Money.fmt
    var detail by remember { mutableStateOf<ReportDetail?>(null) }
    val current = detail

    if (current != null) {
        BackHandler { detail = null }
        PlanningScreen(current.title, { detail = null }) {
            if (current.section == ReportSection.SAVINGS) {
                val entries = report?.savings.orEmpty()
                if (entries.isEmpty()) item { Caption("Движений по копилкам нет") }
                items(entries, key = { it.id }) { entry ->
                    val op = entry.operationId?.let { state.operations[it] }
                    PlanningCard {
                        Text(
                            "${state.goals[entry.goalId]?.name ?: "Копилка"} · ${fmt.signed(entry.amount)}",
                            Modifier.fillMaxWidth().clickable(enabled = op != null) { op?.let(onOperation) },
                            style = AppText.bodyMedium, color = App.colors.on,
                        )
                        Caption(Dates.full(entry.timestamp))
                    }
                }
            } else {
                val lines = report?.let {
                    when (current.section) {
                        ReportSection.INCOME -> it.income
                        ReportSection.EXPENSES -> it.expenses
                        ReportSection.NET -> (it.income + it.expenses).sortedByDescending { line -> line.operation.timestamp }
                        ReportSection.GROWTH -> it.growth.firstOrNull { change -> change.categoryId == current.categoryId }?.current.orEmpty()
                        ReportSection.PREVIOUS -> it.growth.firstOrNull { change -> change.categoryId == current.categoryId }?.previous.orEmpty()
                        ReportSection.OVERAGE -> it.overages.firstOrNull { overage -> overage.categoryId == current.categoryId }?.lines.orEmpty()
                        ReportSection.SAVINGS -> emptyList()
                    }
                }.orEmpty()
                if (lines.isEmpty()) item { Caption("Операций за период нет") }
                items(lines, key = { it.operation.id }) { line ->
                    val op = line.operation
                    PlanningCard {
                        Row(Modifier.fillMaxWidth().clickable { onOperation(op) }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(op.title, Modifier.weight(1f), style = AppText.bodyMedium, color = App.colors.on)
                            val signed = if (op.type == OperationType.EXPENSE) -line.amount else line.amount
                            Text(fmt.signed(signed), style = AppText.amount, color = if (signed < 0) App.colors.exp else App.colors.inc)
                        }
                        Caption(Dates.full(op.timestamp))
                    }
                }
            }
        }
        return
    }

    PlanningScreen("Отчёт за месяц", onBack) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton({ viewModel.shift(-1) }) { Text("‹ Пред.") }
                Text(Dates.monthTitle(state.month), Modifier.weight(1f), textAlign = TextAlign.Center, style = AppText.section, color = App.colors.on)
                TextButton({ viewModel.shift(1) }, enabled = state.month < YearMonth.now()) { Text("След. ›") }
            }
        }
        if (report == null) return@PlanningScreen
        item {
            PlanningCard {
                if (report.partial) Caption("Данные по ${Dates.dayMonth(LocalDate.now())} включительно")
                ReportAmount("Доходы", fmt.format(report.incomeTotal)) { detail = ReportDetail("Доходы", ReportSection.INCOME) }
                ReportAmount("Расходы", fmt.format(report.expenseTotal)) { detail = ReportDetail("Расходы", ReportSection.EXPENSES) }
                ReportAmount("Доходы минус расходы", fmt.signed(report.net)) {
                    detail = ReportDetail("Доходы и расходы", ReportSection.NET)
                }
                ReportAmount("Чистое пополнение копилок", fmt.signed(report.savings.sumOf { it.amount })) {
                    detail = ReportDetail("Движения по копилкам", ReportSection.SAVINGS)
                }
            }
        }
        item {
            PlanningCard {
                Text("Где выросли расходы", style = AppText.section, color = App.colors.on)
                Caption(if (report.partial) "Сравнение с теми же днями прошлого месяца" else "Сравнение с прошлым месяцем")
                if (report.growth.isEmpty()) Caption("Рост расходов по категориям отсутствует")
                report.growth.take(5).forEach { change ->
                    val name = state.categories[change.categoryId]?.name ?: "Без категории"
                    ReportAmount(name, fmt.signed(change.increase)) { detail = ReportDetail(name, ReportSection.GROWTH, change.categoryId) }
                    ReportAmount("В прошлом месяце", fmt.format(change.previous.sumOf { it.amount })) {
                        detail = ReportDetail("$name · прошлый месяц", ReportSection.PREVIOUS, change.categoryId)
                    }
                }
            }
        }
        item {
            PlanningCard {
                Text("Превышение бюджета", style = AppText.section, color = App.colors.on)
                when {
                    state.month < YearMonth.now() -> Caption("Лимиты показываются для текущего месяца")
                    report.overages.isEmpty() -> Caption("Превышений заданных лимитов нет")
                }
                report.overages.forEach { overage ->
                    val name = if (overage.categoryId == null) "Общий бюджет" else state.categories[overage.categoryId]?.name ?: "Удалённая категория"
                    ReportAmount(name, fmt.signed(overage.spent - overage.limit)) {
                        detail = ReportDetail(name, ReportSection.OVERAGE, overage.categoryId)
                    }
                    Caption("Потрачено ${fmt.format(overage.spent)} из ${fmt.format(overage.limit)}")
                }
            }
        }
    }
}

@Composable
private fun ReportAmount(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = AppText.bodyMedium, color = App.colors.on)
        Text(value, style = AppText.amount, color = App.colors.pri)
    }
}

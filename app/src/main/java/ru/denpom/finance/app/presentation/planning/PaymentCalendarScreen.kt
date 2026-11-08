package ru.denpom.finance.app.presentation.planning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import ru.denpom.finance.app.data.repository.PaymentCalendarRepository
import ru.denpom.finance.app.domain.model.Debt
import ru.denpom.finance.app.domain.model.DebtDirection
import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.model.PaymentStatus
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.domain.model.ScheduledPayment
import ru.denpom.finance.app.domain.repository.DebtRepository
import ru.denpom.finance.app.domain.repository.OperationRepository
import ru.denpom.finance.app.domain.repository.RecurringRepository
import ru.denpom.finance.app.domain.usecase.PaymentSchedule
import ru.denpom.finance.app.presentation.LocalAppActions
import ru.denpom.finance.app.presentation.components.FilterChip
import ru.denpom.finance.app.presentation.components.HideSystemBars
import ru.denpom.finance.app.presentation.format.Dates
import ru.denpom.finance.app.presentation.format.Money
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.abs

sealed interface CalendarEntry {
    val key: String
    val title: String
    val date: LocalDate
    val amount: Long
    val status: PaymentStatus
    val outgoing: Boolean

    data class Recurring(val payment: ScheduledPayment, val recorded: Operation?) : CalendarEntry {
        override val key = "recurring-${payment.rule.id}-${payment.occurrence.scheduled}"
        override val title = payment.rule.title
        override val date = payment.occurrence.due
        override val amount = recorded?.amount ?: payment.rule.amount
        override val status = payment.occurrence.status
        override val outgoing = payment.rule.type == OperationType.EXPENSE
    }

    data class DebtDue(val debt: Debt, override val date: LocalDate) : CalendarEntry {
        override val key = "debt-${debt.id}"
        override val title = if (debt.direction == DebtDirection.FROM_ME) "Вернуть · ${debt.person}" else "Получить · ${debt.person}"
        override val amount = debt.left
        override val status = PaymentStatus.PENDING
        override val outgoing = debt.direction == DebtDirection.FROM_ME
    }
}

data class CalendarState(
    val entries: List<CalendarEntry> = emptyList(),
    val operations: List<Operation> = emptyList(),
    val linked: Set<Long> = emptySet(),
)

@HiltViewModel
class PaymentCalendarViewModel @Inject constructor(
    private val repository: PaymentCalendarRepository,
    recurring: RecurringRepository,
    debts: DebtRepository,
    operations: OperationRepository,
) : ViewModel() {
    val state = combine(recurring.observeRecurring(), repository.occurrences, debts.observeDebts(), operations.observeOperations()) { rules, overrides, debtRows, ops ->
        val today = LocalDate.now()
        val through = today.plusDays(29)
        val from = today.minusDays(30)
        val opsById = ops.associateBy { it.id }
        val entries = PaymentSchedule.between(rules, overrides, from, through)
            .map { CalendarEntry.Recurring(it, it.occurrence.operationId?.let(opsById::get)) }
        val due = debtRows.mapNotNull { debt ->
            val date = debt.dueDate?.let(Period::dateOf) ?: return@mapNotNull null
            if (debt.closed || date > through) null else CalendarEntry.DebtDue(debt, date)
        }
        CalendarState((entries + due).sortedBy { it.date }, ops, overrides.mapNotNull { it.operationId }.toSet())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarState())

    fun update(entry: CalendarEntry, status: PaymentStatus, date: LocalDate = entry.date, linkId: Long? = null, onResult: (String?) -> Unit) =
        viewModelScope.launch {
            try {
                when (entry) {
                    is CalendarEntry.Recurring -> repository.resolve(entry.payment, status, date, linkId)
                    is CalendarEntry.DebtDue -> if (status == PaymentStatus.PAID) repository.settleDebt(entry.debt.id, entry.amount)
                    else repository.moveDebt(entry.debt.id, entry.debt.dueDate, date)
                }
                onResult(null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onResult(e.message ?: "Не удалось сохранить платёж")
            }
        }
}

private enum class PaymentAction { DETAILS, LINK, PAY, MOVE, SKIP }

@Composable
fun PaymentCalendarScreen(
    onBack: () -> Unit,
    onSchedules: () -> Unit,
    onOperation: (Operation) -> Unit,
    viewModel: PaymentCalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val fmt = Money.fmt
    val today = LocalDate.now()
    var showHistory by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<CalendarEntry?>(null) }
    var action by remember { mutableStateOf(PaymentAction.DETAILS) }
    var busy by remember { mutableStateOf(false) }

    val pending = state.entries.filter { it.status == PaymentStatus.PENDING }
    val visible = if (showHistory) state.entries.filter { it.status != PaymentStatus.PENDING } else pending

    fun save(entry: CalendarEntry, status: PaymentStatus, date: LocalDate = entry.date, operationId: Long? = null) {
        if (busy) return
        busy = true
        viewModel.update(entry, status, date, operationId) { error ->
            busy = false
            selected = null
            actions.snack(error ?: when (status) {
                PaymentStatus.PAID -> if (operationId == null) "Платёж отмечен как выполненный" else "Операция связана с платежом"
                PaymentStatus.SKIPPED -> "Платёж пропущен"
                PaymentStatus.PENDING -> "Дата платежа перенесена"
            })
        }
    }

    PlanningScreen("Календарь платежей", onBack) {
        item {
            PlanningCard {
                Text("Ближайшие 30 дней", style = AppText.section, color = App.colors.on)
                Caption("К оплате ${fmt.format(pending.filter { it.outgoing && it.date >= today }.sumOf { it.amount })}")
                Caption("К получению ${fmt.format(pending.filter { !it.outgoing && it.date >= today }.sumOf { it.amount })}")
                val overdue = pending.filter { it.date < today }
                if (overdue.isNotEmpty()) Caption("Просрочено ${overdue.size} · к оплате ${fmt.format(overdue.filter { it.outgoing }.sumOf { it.amount })}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(if (showHistory) "К плану" else "История", selected = showHistory) { showHistory = !showHistory }
                    FilterChip("Расписания", onClick = onSchedules)
                }
            }
        }
        if (visible.isEmpty()) item { Caption(if (showHistory) "За этот период нет завершённых платежей" else "Запланированных платежей нет") }
        items(visible, key = { it.key }) { entry ->
            PlanningCard {
                Row(
                    Modifier.fillMaxWidth().clickable {
                        action = PaymentAction.DETAILS
                        selected = entry
                    }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(entry.title, style = AppText.bodyMedium, color = App.colors.on)
                        val label = when (entry.status) {
                            PaymentStatus.PAID -> "Выполнен"
                            PaymentStatus.SKIPPED -> "Пропущен"
                            PaymentStatus.PENDING -> if (entry.date < today) "Просрочен" else "Запланирован"
                        }
                        Caption("${Dates.relative(entry.date)} · $label")
                    }
                    Text(fmt.format(entry.amount), style = AppText.amount, color = if (entry.outgoing) App.colors.exp else App.colors.inc)
                }
            }
        }
    }

    val entry = selected ?: return
    val recurring = entry as? CalendarEntry.Recurring
    val existing = recurring?.recorded
    val dismiss = { if (!busy) selected = null }
    when (action) {
        PaymentAction.MOVE -> PaymentDatePicker(entry.date.coerceAtLeast(today), busy, dismiss) { save(entry, PaymentStatus.PENDING, it) }
        PaymentAction.PAY, PaymentAction.SKIP -> {
            val skip = action == PaymentAction.SKIP
            AlertDialog(
                onDismissRequest = dismiss,
                title = { Text(if (skip) "Пропустить этот платёж?" else "Отметить платёж выполненным?") },
                text = { Text(when {
                    skip -> "Этот платёж исчезнет из плана. Следующие даты останутся в расписании."
                    recurring == null -> "Приложение погасит остаток долга в разделе «Долги»."
                    else -> "Будет создана операция на сумму ${fmt.format(entry.amount)} сегодняшней датой."
                }) },
                confirmButton = {
                    TextButton({ save(entry, if (skip) PaymentStatus.SKIPPED else PaymentStatus.PAID) }, enabled = !busy) { Text("Подтвердить") }
                },
                dismissButton = { TextButton(dismiss, enabled = !busy) { Text("Отмена") } },
            )
        }
        PaymentAction.LINK -> {
            val candidates = remember(state, recurring) {
                state.operations.asSequence().filter { it.id !in state.linked && recurring?.payment?.canLink(it) == true }
                    .sortedBy { abs(ChronoUnit.DAYS.between(entry.date, Period.dateOf(it.timestamp))) }
                    .toList()
            }
            AlertDialog(
                onDismissRequest = dismiss,
                title = { Text("Связать с операцией") },
                text = {
                    if (candidates.isEmpty()) Text("Подойдут операции того же типа, на том же счёте и с такой же суммой.")
                    else LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(candidates, key = { it.id }) { op ->
                            Column(Modifier.fillMaxWidth().clickable(enabled = !busy) { save(entry, PaymentStatus.PAID, operationId = op.id) }.padding(vertical = 12.dp)) {
                                Text(op.title, style = AppText.bodyMedium)
                                Caption(Dates.full(op.timestamp))
                            }
                        }
                    }
                },
                confirmButton = { TextButton(dismiss, enabled = !busy) { Text("Закрыть") } },
            )
        }
        PaymentAction.DETAILS -> AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(entry.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Caption("${Dates.relative(entry.date)} · ${fmt.format(entry.amount)}")
                    if (entry.status == PaymentStatus.PENDING) {
                        if (recurring != null && state.operations.any { it.id !in state.linked && recurring.payment.isRecordedAsScheduled(it) }) {
                            Caption("Есть похожая операция. Свяжите её с платежом, если она относится к этому списанию.")
                        }
                        TextButton({ action = PaymentAction.PAY }) { Text("Отметить выполненным") }
                        if (recurring != null) TextButton({ action = PaymentAction.LINK }) { Text("Связать с операцией") }
                        TextButton({ action = PaymentAction.MOVE }) { Text("Перенести") }
                        if (recurring != null) TextButton({ action = PaymentAction.SKIP }) { Text("Пропустить") }
                    } else if (existing != null) {
                        TextButton({ selected = null; onOperation(existing) }) { Text("Открыть операцию") }
                    } else {
                        Caption(if (entry.status == PaymentStatus.SKIPPED) "Платёж пропущен" else "Платёж выполнен")
                    }
                }
            },
            confirmButton = { TextButton(dismiss) { Text("Закрыть") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentDatePicker(initial: LocalDate, busy: Boolean, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val picker = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
    val date = picker.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton({ date?.let(onPick) }, enabled = !busy && date != null && date >= LocalDate.now()) { Text("Перенести") } },
        dismissButton = { TextButton(onDismiss, enabled = !busy) { Text("Отмена") } },
    ) {
        HideSystemBars()
        DatePicker(picker)
    }
}

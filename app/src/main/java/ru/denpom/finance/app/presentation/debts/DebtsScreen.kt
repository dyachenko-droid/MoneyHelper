package ru.denpom.finance.app.presentation.debts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CreditScore
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.denpom.finance.app.domain.model.Debt
import ru.denpom.finance.app.domain.model.DebtDirection
import ru.denpom.finance.app.domain.model.Loan
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.presentation.LocalAppActions
import ru.denpom.finance.app.presentation.components.BackTopBar
import ru.denpom.finance.app.presentation.components.EmptyState
import ru.denpom.finance.app.presentation.components.FilledField
import ru.denpom.finance.app.presentation.components.FilterChip
import ru.denpom.finance.app.presentation.components.InitialsAvatar
import ru.denpom.finance.app.presentation.components.PrimaryButton
import ru.denpom.finance.app.presentation.components.ProgressLine
import ru.denpom.finance.app.presentation.components.SectionHeader
import ru.denpom.finance.app.presentation.components.Segmented
import ru.denpom.finance.app.presentation.components.SheetHandle
import ru.denpom.finance.app.presentation.components.TextAction
import ru.denpom.finance.app.presentation.components.card
import ru.denpom.finance.app.presentation.format.Dates
import ru.denpom.finance.app.presentation.format.Money
import ru.denpom.finance.app.presentation.format.amountInput
import ru.denpom.finance.app.presentation.format.parseAmount
import ru.denpom.finance.app.presentation.home.initialsOf
import ru.denpom.finance.app.presentation.subscriptions.ToggleRow
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import ru.denpom.finance.app.presentation.components.HideSystemBars

private sealed interface DebtDialog {
    data class EditDebt(val debt: Debt?, val direction: DebtDirection) : DebtDialog
    data class Pay(val debt: Debt) : DebtDialog
    data class EditLoan(val loan: Loan?) : DebtDialog
    data class Schedule(val loan: Loan) : DebtDialog
}

@Composable
fun DebtsScreen(onBack: () -> Unit, viewModel: DebtsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = App.colors
    val fmt = Money.fmt
    val actions = LocalAppActions.current
    var tab by remember { mutableStateOf(0) }
    var dialog by remember { mutableStateOf<DebtDialog?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding()) {
        BackTopBar("Долги и кредиты", onBack)
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Segmented(listOf("Мне должны", "Я должен", "Кредиты"), tab, { tab = it }) }
            if (tab < 2) {
                val direction = if (tab == 0) DebtDirection.TO_ME else DebtDirection.FROM_ME
                val list = state.debts.filter { it.direction == direction }
                val open = list.filterNot { it.closed }
                val closed = list.filter { it.closed }
                item {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(if (tab == 0) c.priC else c.ambC).padding(20.dp)) {
                        val fg = if (tab == 0) c.onPriC else c.onAmbC
                        Text(if (tab == 0) "Вам должны" else "Вы должны", style = AppText.label.copy(fontWeight = FontWeight.Normal), color = fg.copy(alpha = 0.8f))
                        Text(fmt.format(open.sumOf { it.left }, false), style = AppText.amountL.copy(fontSize = 30.sp), color = fg)
                        Text("${open.size} ${Dates.plural(open.size.toLong(), "открытый долг", "открытых долга", "открытых долгов")}", style = AppText.caption.copy(fontSize = 14.sp), color = fg.copy(alpha = 0.85f))
                    }
                }
                items(open, key = { it.id }) { debt -> DebtCard(debt, { dialog = DebtDialog.EditDebt(debt, debt.direction) }) { dialog = DebtDialog.Pay(debt) } }
                if (list.isEmpty()) {
                    item {
                        EmptyState(
                            Icons.Rounded.Handshake,
                            if (tab == 0) "Никто не должен" else "Долгов нет",
                            if (tab == 0) "Запишите, кому вы одолжили, или разделите чек с друзьями" else "Запишите, у кого заняли — напомним о сроке",
                        )
                    }
                }
                if (closed.isNotEmpty()) {
                    item { SectionHeader("Закрытые", Modifier.padding(top = 4.dp)) }
                    items(closed, key = { it.id }) { debt -> DebtCard(debt, { dialog = DebtDialog.EditDebt(debt, debt.direction) }, null) }
                }
            } else {
                item {
                    val monthly = state.loans.sumOf { it.monthlyPayment }
                    val remaining = state.loans.sumOf { loan -> remainingOf(loan) }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.hero).padding(20.dp)) {
                        Text("Платежи по кредитам", style = AppText.label.copy(fontWeight = FontWeight.Normal), color = c.onHero.copy(alpha = 0.8f))
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(fmt.format(monthly, false), style = AppText.amountL.copy(fontSize = 30.sp), color = c.onHero)
                            Text("в месяц", style = AppText.body, color = c.onHero.copy(alpha = 0.75f), modifier = Modifier.padding(bottom = 5.dp))
                        }
                        Text("Остаток долга ${fmt.format(remaining, false)}", style = AppText.caption.copy(fontSize = 14.sp), color = c.onHero.copy(alpha = 0.85f))
                    }
                }
                items(state.loans, key = { it.id }) { loan -> LoanCard(loan) { dialog = DebtDialog.Schedule(loan) } }
                if (state.loans.isEmpty()) {
                    item { EmptyState(Icons.Rounded.CreditScore, "Кредитов нет", "Добавьте кредит или ипотеку — покажем график, остаток и переплату") }
                }
            }
        }
        PrimaryButton(
            when (tab) { 0 -> "Мне должны"; 1 -> "Я занял"; else -> "Добавить кредит" },
            Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            leading = Icons.Rounded.Add,
        ) {
            dialog = if (tab == 2) DebtDialog.EditLoan(null) else DebtDialog.EditDebt(null, if (tab == 0) DebtDirection.TO_ME else DebtDirection.FROM_ME)
        }
    }

    when (val d = dialog) {
        is DebtDialog.EditDebt -> DebtSheet(
            debt = d.debt,
            direction = d.direction,
            onDismiss = { dialog = null },
            onSave = { viewModel.save(it); actions.snack("Сохранено"); dialog = null },
            onDelete = d.debt?.let { debt -> { viewModel.delete(debt.id); actions.snack("Долг удалён"); dialog = null } },
        )
        is DebtDialog.Pay -> PaySheet(d.debt, { dialog = null }) { amount ->
            viewModel.pay(d.debt.id, amount)
            actions.snack(if (amount >= d.debt.left) "Долг закрыт" else "Платёж записан")
            dialog = null
        }
        is DebtDialog.EditLoan -> LoanSheet(
            loan = d.loan,
            accounts = state.accounts,
            onDismiss = { dialog = null },
            onSave = { loan, add, acc -> viewModel.saveLoan(loan, add, acc); actions.snack("Кредит сохранён"); dialog = null },
            onDelete = d.loan?.let { loan -> { viewModel.deleteLoan(loan); actions.snack("Кредит удалён"); dialog = null } },
        )
        is DebtDialog.Schedule -> ScheduleSheet(d.loan, { dialog = null }) { dialog = DebtDialog.EditLoan(d.loan) }
        null -> Unit
    }
}

private fun remainingOf(loan: Loan): Long {
    val now = System.currentTimeMillis()
    return loan.schedule().lastOrNull { it.date <= now }?.balanceAfter ?: loan.principal
}

@Composable
private fun DebtCard(debt: Debt, onEdit: () -> Unit, onPay: (() -> Unit)?) {
    val c = App.colors
    val fmt = Money.fmt
    val overdue = debt.dueDate != null && !debt.closed && debt.dueDate < Period.startOfDay(LocalDate.now())
    Column(Modifier.fillMaxWidth().card(RoundedCornerShape(20.dp)).clickable(onClick = onEdit).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InitialsAvatar(initialsOf(debt.person), if (debt.direction == DebtDirection.TO_ME) 155f else 65f)
            Column(Modifier.weight(1f)) {
                Text(debt.person, style = AppText.bodyMedium, color = c.on)
                val sub = listOfNotNull(
                    debt.note.takeIf { it.isNotBlank() },
                    debt.dueDate?.let { "до ${Dates.dayMonth(Period.dateOf(it))}" },
                ).joinToString(" · ")
                if (sub.isNotEmpty()) Text(sub, style = AppText.caption, color = if (overdue) c.exp else c.on2, maxLines = 2)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(fmt.format(debt.left), style = AppText.amount, color = if (debt.closed) c.on2 else c.on)
                if (debt.paid > 0 && !debt.closed) Text("из ${fmt.format(debt.amount, false)}", style = AppText.small.copy(fontWeight = FontWeight.Normal), color = c.on2)
            }
        }
        if (debt.paid > 0 && !debt.closed) ProgressLine(debt.paid.toFloat() / debt.amount, c.pri)
        if (onPay != null) {
            Row {
                Spacer(Modifier.weight(1f))
                PrimaryButton(if (debt.direction == DebtDirection.TO_ME) "Вернули" else "Вернул", height = 40.dp, container = c.priC, content = c.onPriC, onClick = onPay)
            }
        }
    }
}

@Composable
private fun LoanCard(loan: Loan, onClick: () -> Unit) {
    val c = App.colors
    val fmt = Money.fmt
    val schedule = loan.schedule()
    val now = System.currentTimeMillis()
    val paidCount = schedule.count { it.date <= now }
    val next = schedule.firstOrNull { it.date > now }
    Column(Modifier.fillMaxWidth().card(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(loan.name, style = AppText.section.copy(fontSize = 16.sp), color = c.on)
                Text("${loan.ratePercent.toString().replace('.', ',').removeSuffix(",0")}% · ${loan.termMonths} мес · ${fmt.format(loan.principal, false)}", style = AppText.caption, color = c.on2)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(fmt.format(loan.monthlyPayment, false), style = AppText.amount, color = c.on)
                Text("в месяц", style = AppText.small.copy(fontWeight = FontWeight.Normal), color = c.on2)
            }
        }
        ProgressLine(paidCount.toFloat() / loan.termMonths, c.pri)
        Row {
            Text("Выплачено $paidCount из ${loan.termMonths}", style = AppText.caption, color = c.on2, modifier = Modifier.weight(1f))
            next?.let { Text("Следующий ${Dates.dayMonth(Period.dateOf(it.date))}", style = AppText.caption, color = c.on2) }
        }
        Text("Переплата за весь срок ${fmt.format(loan.overpayment, false)}", style = AppText.caption.copy(fontWeight = FontWeight.Medium), color = c.exp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetFrame(content: @Composable () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = App.colors.s1,
        dragHandle = { SheetHandle(Modifier.padding(top = 8.dp)) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        HideSystemBars()
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateChip(label: String, date: LocalDate?, onPick: (LocalDate) -> Unit) {
    var show by remember { mutableStateOf(false) }
    FilterChip(label, leading = Icons.Rounded.CalendarToday, trailing = null) { show = true }
    if (show) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = (date ?: LocalDate.now()).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton({
                    picker.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    show = false
                }) { Text("Готово") }
            },
            dismissButton = { TextButton({ show = false }) { Text("Отмена") } },
        ) {
            HideSystemBars()
            DatePicker(picker)
        }
    }
}

@Composable
private fun DebtSheet(debt: Debt?, direction: DebtDirection, onDismiss: () -> Unit, onSave: (Debt) -> Unit, onDelete: (() -> Unit)?) {
    val c = App.colors
    var person by remember { mutableStateOf(debt?.person.orEmpty()) }
    var amount by remember { mutableStateOf(debt?.amount?.let { amountInput(it) }.orEmpty()) }
    var note by remember { mutableStateOf(debt?.note.orEmpty()) }
    var due by remember { mutableStateOf(debt?.dueDate?.let { Period.dateOf(it) }) }
    SheetFrame(onDismiss = onDismiss, content = {
        Text(if (direction == DebtDirection.TO_ME) "Мне должны" else "Я должен", style = AppText.section.copy(fontSize = 16.sp), color = c.on)
        FilledField(if (direction == DebtDirection.TO_ME) "Кто" else "Кому", person, { person = it }, placeholder = "Имя")
        FilledField("Сумма, ${Money.fmt.symbol}", amount, { v -> amount = v.filter { it.isDigit() || it == ',' || it == '.' } }, keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        FilledField("За что", note, { note = it }, placeholder = "Ужин, билеты…")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateChip(due?.let { "Вернуть до ${Dates.dayMonth(it)}" } ?: "Без срока", due) { due = it }
            if (due != null) FilterChip("Убрать срок", trailing = null) { due = null }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onDelete != null) TextAction("Удалить", color = c.exp, onClick = onDelete)
            Spacer(Modifier.weight(1f))
            PrimaryButton("Сохранить", height = 48.dp, enabled = person.isNotBlank() && parseAmount(amount) > 0) {
                onSave(
                    Debt(
                        id = debt?.id ?: 0,
                        person = person.trim(),
                        direction = direction,
                        amount = parseAmount(amount),
                        note = note.trim(),
                        createdAt = debt?.createdAt ?: System.currentTimeMillis(),
                        dueDate = due?.let { Period.startOfDay(it) },
                    )
                )
            }
        }
    })
}

@Composable
private fun PaySheet(debt: Debt, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    val c = App.colors
    var amount by remember { mutableStateOf(amountInput(debt.left)) }
    SheetFrame(onDismiss = onDismiss, content = {
        Text("${debt.person}: возврат", style = AppText.section.copy(fontSize = 16.sp), color = c.on)
        FilledField("Сумма, ${Money.fmt.symbol}", amount, { v -> amount = v.filter { it.isDigit() || it == ',' || it == '.' } }, keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        Text("Осталось ${Money.fmt.format(debt.left)}", style = AppText.caption, color = c.on2)
        val value = parseAmount(amount)
        PrimaryButton("Записать", Modifier.fillMaxWidth(), enabled = value > 0) { onSave(value.coerceAtMost(debt.left)) }
    })
}

@Composable
private fun LoanSheet(
    loan: Loan?,
    accounts: List<ru.denpom.finance.app.domain.model.Account>,
    onDismiss: () -> Unit,
    onSave: (Loan, Boolean, Long?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val c = App.colors
    var name by remember { mutableStateOf(loan?.name.orEmpty()) }
    var principal by remember { mutableStateOf(loan?.principal?.let { amountInput(it) }.orEmpty()) }
    var rate by remember { mutableStateOf(loan?.ratePercent?.toString()?.replace('.', ',')?.removeSuffix(",0").orEmpty()) }
    var term by remember { mutableStateOf(loan?.termMonths?.toString().orEmpty()) }
    var start by remember { mutableStateOf(loan?.let { Period.dateOf(it.startDate) } ?: LocalDate.now()) }
    var addRecurring by remember { mutableStateOf(loan?.recurringId != null || loan == null) }
    var accountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    val draft = Loan(
        id = loan?.id ?: 0,
        name = name.trim(),
        principal = parseAmount(principal),
        ratePercent = rate.replace(',', '.').toDoubleOrNull() ?: 0.0,
        termMonths = term.toIntOrNull() ?: 0,
        startDate = Period.startOfDay(start),
        recurringId = loan?.recurringId,
    )
    val valid = name.isNotBlank() && draft.principal > 0 && draft.termMonths in 1..600
    SheetFrame(onDismiss = onDismiss, content = {
        Text(if (loan == null) "Новый кредит" else "Кредит", style = AppText.section.copy(fontSize = 16.sp), color = c.on)
        FilledField("Название", name, { name = it }, placeholder = "Ипотека, автокредит, рассрочка")
        FilledField("Сумма кредита, ${Money.fmt.symbol}", principal, { v -> principal = v.filter { it.isDigit() || it == ',' || it == '.' } }, keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledField("Ставка, % годовых", rate, { v -> rate = v.filter { it.isDigit() || it == ',' || it == '.' } }, Modifier.weight(1f), keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            FilledField("Срок, мес", term, { v -> term = v.filter { it.isDigit() }.take(3) }, Modifier.weight(1f), keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        DateChip("Выдан ${Dates.dayMonth(start)} ${start.year}", start) { start = it }
        if (valid) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.s2).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Ежемесячный платёж ${Money.fmt.format(draft.monthlyPayment, false)}", style = AppText.bodyMedium, color = c.on)
                Text("Переплата ${Money.fmt.format(draft.overpayment, false)} · всего ${Money.fmt.format(draft.principal + draft.overpayment, false)}", style = AppText.caption, color = c.on2)
            }
        }
        ToggleRow("Показывать платёж в «Скоро спишется»", addRecurring) { addRecurring = it }
        if (addRecurring) {
            var menu by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Счёт списания", style = AppText.body, color = c.on2)
                androidx.compose.foundation.layout.Box {
                    FilterChip(accounts.firstOrNull { it.id == accountId }?.name ?: "Выберите") { menu = true }
                    DropdownMenu(menu, { menu = false }) { accounts.forEach { a -> DropdownMenuItem({ Text(a.name) }, { accountId = a.id; menu = false }) } }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onDelete != null) TextAction("Удалить", color = c.exp, onClick = onDelete)
            Spacer(Modifier.weight(1f))
            PrimaryButton("Сохранить", height = 48.dp, enabled = valid) { onSave(draft, addRecurring, accountId) }
        }
    })
}

@Composable
private fun ScheduleSheet(loan: Loan, onDismiss: () -> Unit, onEdit: () -> Unit) {
    val c = App.colors
    val fmt = Money.fmt
    val now = System.currentTimeMillis()
    SheetFrame(onDismiss = onDismiss, content = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(loan.name, style = AppText.section.copy(fontSize = 16.sp), color = c.on, modifier = Modifier.weight(1f))
            TextAction("Изменить", onClick = onEdit)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text("Дата", style = AppText.small, color = c.on2, modifier = Modifier.weight(1.2f))
            Text("Платёж", style = AppText.small, color = c.on2, modifier = Modifier.weight(1f))
            Text("Проценты", style = AppText.small, color = c.on2, modifier = Modifier.weight(1f))
            Text("Остаток", style = AppText.small, color = c.on2, modifier = Modifier.weight(1.1f))
        }
        Column(Modifier.card(RoundedCornerShape(16.dp))) {
            loan.schedule().forEach { p ->
                val past = p.date <= now
                Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val color = if (past) c.on2 else c.on
                    Text(Dates.shortDate(Period.dateOf(p.date)) + "." + Period.dateOf(p.date).year % 100, style = AppText.caption, color = color, modifier = Modifier.weight(1.2f))
                    Text(fmt.number(p.total), style = AppText.caption, color = color, modifier = Modifier.weight(1f))
                    Text(fmt.number(p.interest), style = AppText.caption, color = if (past) c.on2 else c.exp, modifier = Modifier.weight(1f))
                    Text(fmt.number(p.balanceAfter), style = AppText.caption, color = color, modifier = Modifier.weight(1.1f))
                }
            }
        }
        Spacer(Modifier.fillMaxHeight(0f))
    })
}

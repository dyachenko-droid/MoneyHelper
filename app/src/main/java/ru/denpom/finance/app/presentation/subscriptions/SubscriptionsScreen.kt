package ru.denpom.finance.app.presentation.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.domain.model.RecurrencePeriod
import ru.denpom.finance.app.domain.model.Recurring
import ru.denpom.finance.app.presentation.LocalAppActions
import ru.denpom.finance.app.presentation.components.BackTopBar
import ru.denpom.finance.app.presentation.components.CategoryChip
import ru.denpom.finance.app.presentation.components.EmptyState
import ru.denpom.finance.app.presentation.components.FilledField
import ru.denpom.finance.app.presentation.components.FilterChip
import ru.denpom.finance.app.presentation.components.InitialsAvatar
import ru.denpom.finance.app.presentation.components.PrimaryButton
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
import ru.denpom.finance.app.presentation.receipt.CategoryPickerSheet
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import ru.denpom.finance.app.presentation.components.HideSystemBars

private data class SubEdit(val item: Recurring?)

@Composable
fun SubscriptionsScreen(onBack: () -> Unit, viewModel: SubscriptionsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = App.colors
    val fmt = Money.fmt
    val actions = LocalAppActions.current
    var edit by remember { mutableStateOf<SubEdit?>(null) }
    val cats = state.categories.associateBy { it.id }
    val accNames = state.accounts.associate { it.id to it.name }
    val today = Period.startOfDay(LocalDate.now())
    val soon = state.items.filter { it.nextDate < Period.startOfDay(LocalDate.now().plusDays(30)) && it.nextDate >= today }

    Column(Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding()) {
        BackTopBar("Подписки и платежи", onBack)
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.hero).padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Регулярные расходы", style = AppText.label.copy(fontWeight = FontWeight.Normal), color = c.onHero.copy(alpha = 0.8f))
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(fmt.format(state.monthly, false), style = AppText.amountL.copy(fontSize = 30.sp), color = c.onHero)
                        Text("в месяц", style = AppText.body, color = c.onHero.copy(alpha = 0.75f), modifier = Modifier.padding(bottom = 5.dp))
                    }
                    Text("≈ ${fmt.format(state.monthly * 12, false)} в год · ${state.items.count { it.isSubscription }} ${Dates.plural(state.items.count { it.isSubscription }.toLong(), "подписка", "подписки", "подписок")}", style = AppText.caption.copy(fontSize = 14.sp), color = c.onHero.copy(alpha = 0.85f))
                }
            }
            if (state.items.isEmpty()) {
                item {
                    EmptyState(Icons.Rounded.Autorenew, "Пока ничего нет", "Добавьте подписки, аренду, кредит или зарплату — покажем, что и когда спишется")
                }
            }
            if (soon.isNotEmpty()) {
                item { SectionHeader("Ближайшие 30 дней") }
                item {
                    Column(Modifier.card(RoundedCornerShape(20.dp)).padding(vertical = 4.dp)) {
                        soon.forEach { r -> RecurringRow(r, cats[r.categoryId]?.hue, accNames[r.accountId].orEmpty()) { edit = SubEdit(r) } }
                    }
                }
            }
            listOf(true to "Подписки", false to "Регулярные платежи").forEach { (subs, title) ->
                val list = state.items.filter { it.isSubscription == subs }
                if (list.isNotEmpty()) {
                    item { SectionHeader(title, Modifier.padding(top = 4.dp)) }
                    item {
                        Column(Modifier.card(RoundedCornerShape(20.dp)).padding(vertical = 4.dp)) {
                            list.forEach { r -> RecurringRow(r, cats[r.categoryId]?.hue, accNames[r.accountId].orEmpty()) { edit = SubEdit(r) } }
                        }
                    }
                }
            }
        }
        PrimaryButton("Добавить", Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), leading = Icons.Rounded.Add) { edit = SubEdit(null) }
    }

    edit?.let { e ->
        RecurringSheet(
            item = e.item,
            state = state,
            onDismiss = { edit = null },
            onSave = {
                viewModel.save(it)
                actions.snack(if (e.item == null) "Добавлено" else "Сохранено")
                edit = null
            },
            onDelete = e.item?.let { item -> { viewModel.delete(item.id); actions.snack("Удалено"); edit = null } },
        )
    }
}

@Composable
private fun RecurringRow(r: Recurring, hue: Float?, account: String, onClick: () -> Unit) {
    val c = App.colors
    val fmt = Money.fmt
    val period = when (r.period) {
        RecurrencePeriod.WEEKLY -> "каждую неделю"
        RecurrencePeriod.MONTHLY -> "каждый месяц"
        RecurrencePeriod.YEARLY -> "раз в год"
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InitialsAvatar(initialsOf(r.title), hue ?: 200f)
        Column(Modifier.weight(1f)) {
            Text(r.title, style = AppText.bodyMedium, color = c.on, maxLines = 1)
            Text("${Dates.relative(Period.dateOf(r.nextDate))} · $period" + if (account.isNotBlank()) " · $account" else "", style = AppText.caption, color = c.on2, maxLines = 1)
        }
        Text(
            (if (r.type == OperationType.INCOME) "+" else "") + fmt.format(r.amount, r.amount % 100 != 0L),
            style = AppText.amount,
            color = if (r.type == OperationType.INCOME) c.inc else c.on,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringSheet(
    item: Recurring?,
    state: SubscriptionsState,
    onDismiss: () -> Unit,
    onSave: (Recurring) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val c = App.colors
    var title by remember { mutableStateOf(item?.title.orEmpty()) }
    var amount by remember { mutableStateOf(item?.amount?.let { amountInput(it) }.orEmpty()) }
    var period by remember { mutableStateOf(item?.period ?: RecurrencePeriod.MONTHLY) }
    var income by remember { mutableStateOf(item?.type == OperationType.INCOME) }
    var date by remember { mutableStateOf(item?.let { Period.dateOf(it.nextDate) } ?: LocalDate.now().plusDays(1)) }
    var accountId by remember { mutableStateOf(item?.accountId ?: state.accounts.firstOrNull()?.id) }
    var categoryId by remember { mutableStateOf(item?.categoryId ?: state.categories.firstOrNull { it.icon == "autorenew" }?.id) }
    var subscription by remember { mutableStateOf(item?.isSubscription ?: true) }
    var auto by remember { mutableStateOf(item?.autoCreate ?: true) }
    var showDate by remember { mutableStateOf(false) }
    var showCats by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.s1,
        dragHandle = { SheetHandle(Modifier.padding(top = 8.dp)) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        HideSystemBars()
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (item == null) "Новый регулярный платёж" else "Регулярный платёж", style = AppText.section.copy(fontSize = 16.sp), color = c.on)
            Segmented(listOf("Расход", "Доход"), if (income) 1 else 0, { income = it == 1; if (income) subscription = false })
            FilledField("Название", title, { title = it }, placeholder = "Яндекс Плюс, аренда, зарплата…")
            FilledField("Сумма, ${Money.fmt.symbol}", amount, { v -> amount = v.filter { it.isDigit() || it == ',' || it == '.' } }, keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            Segmented(listOf("Неделя", "Месяц", "Год"), period.ordinal, { period = RecurrencePeriod.entries[it] })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip("Следующее: ${Dates.chipDate(date)}", leading = Icons.Rounded.CalendarToday, trailing = null) { showDate = true }
                var accMenu by remember { mutableStateOf(false) }
                Box {
                    FilterChip(state.accounts.firstOrNull { it.id == accountId }?.name ?: "Счёт", leading = Icons.Rounded.AccountBalanceWallet, trailing = null) { accMenu = true }
                    DropdownMenu(accMenu, { accMenu = false }) {
                        state.accounts.forEach { a -> DropdownMenuItem({ Text(a.name) }, { accountId = a.id; accMenu = false }) }
                    }
                }
            }
            if (!income) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Категория", style = AppText.body, color = c.on2)
                    CategoryChip(state.categories.firstOrNull { it.id == categoryId }) { showCats = true }
                }
                ToggleRow("Это подписка", subscription) { subscription = it }
            }
            ToggleRow("Создавать операцию в день списания", auto) { auto = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) TextAction("Удалить", color = c.exp, onClick = onDelete)
                Spacer(Modifier.weight(1f))
                PrimaryButton("Сохранить", height = 48.dp, enabled = title.isNotBlank() && parseAmount(amount) > 0 && accountId != null) {
                    onSave(
                        Recurring(
                            id = item?.id ?: 0,
                            title = title.trim(),
                            amount = parseAmount(amount),
                            type = if (income) OperationType.INCOME else OperationType.EXPENSE,
                            accountId = accountId ?: return@PrimaryButton,
                            categoryId = if (income) null else categoryId,
                            period = period,
                            nextDate = Period.startOfDay(date),
                            isSubscription = !income && subscription,
                            autoCreate = auto,
                        )
                    )
                }
            }
        }
    }

    if (showDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton({
                    picker.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    showDate = false
                }) { Text("Готово") }
            },
            dismissButton = { TextButton({ showDate = false }) { Text("Отмена") } },
        ) {
            HideSystemBars()
            DatePicker(picker)
        }
    }
    if (showCats) {
        CategoryPickerSheet("", state.categories, categoryId, { categoryId = it; showCats = false }, { showCats = false }, title = "Категория", showRuleNote = false)
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = App.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.s2).clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = AppText.body, color = c.on, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = c.pri, checkedThumbColor = c.onPri, uncheckedTrackColor = c.s3, uncheckedBorderColor = c.s3, uncheckedThumbColor = c.on2),
        )
    }
}


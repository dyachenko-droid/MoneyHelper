package ru.denpom.finance.app.presentation.goals

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.denpom.finance.app.domain.model.Goal
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.presentation.LocalAppActions
import ru.denpom.finance.app.presentation.components.BackTopBar
import ru.denpom.finance.app.presentation.components.CategoryAvatar
import ru.denpom.finance.app.presentation.components.CategoryIcons
import ru.denpom.finance.app.presentation.components.EmptyState
import ru.denpom.finance.app.presentation.components.FilledField
import ru.denpom.finance.app.presentation.components.FilterChip
import ru.denpom.finance.app.presentation.components.PrimaryButton
import ru.denpom.finance.app.presentation.components.Segmented
import ru.denpom.finance.app.presentation.components.SelectRing
import ru.denpom.finance.app.presentation.components.SheetHandle
import ru.denpom.finance.app.presentation.components.TextAction
import ru.denpom.finance.app.presentation.components.card
import ru.denpom.finance.app.presentation.format.Dates
import ru.denpom.finance.app.presentation.format.Money
import ru.denpom.finance.app.presentation.format.amountInput
import ru.denpom.finance.app.presentation.format.parseAmount
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import ru.denpom.finance.app.presentation.components.HideSystemBars

private val goalIcons = listOf("savings", "flight", "directions_car", "home", "school", "smartphone", "redeem", "child_care", "fitness_center", "pets")
private val goalHues = listOf(150f, 65f, 245f, 300f, 20f, 180f, 345f, 85f)

private sealed interface GoalDialog {
    data class Edit(val goal: Goal?) : GoalDialog
    data class Deposit(val goal: Goal) : GoalDialog
}

@Composable
fun GoalsScreen(onBack: () -> Unit, viewModel: GoalsViewModel = hiltViewModel()) {
    val goals by viewModel.state.collectAsStateWithLifecycle()
    val c = App.colors
    val fmt = Money.fmt
    val actions = LocalAppActions.current
    var dialog by remember { mutableStateOf<GoalDialog?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding()) {
        BackTopBar("Цели-копилки", onBack)
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (goals.isNotEmpty()) {
                item {
                    val saved = goals.sumOf { it.saved }
                    val target = goals.sumOf { it.target }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.ambC).padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Накоплено на цели", style = AppText.label.copy(fontWeight = FontWeight.Normal), color = c.onAmbC.copy(alpha = 0.8f))
                        Text(fmt.format(saved, false), style = AppText.amountL.copy(fontSize = 30.sp), color = c.onAmbC)
                        Text("из ${fmt.format(target, false)} · ${goals.size} ${Dates.plural(goals.size.toLong(), "цель", "цели", "целей")}", style = AppText.caption.copy(fontSize = 14.sp), color = c.onAmbC.copy(alpha = 0.85f))
                    }
                }
            }
            items(goals, key = { it.id }) { goal ->
                GoalCard(goal, onEdit = { dialog = GoalDialog.Edit(goal) }, onDeposit = { dialog = GoalDialog.Deposit(goal) })
            }
            if (goals.isEmpty()) {
                item { EmptyState(Icons.Rounded.Savings, "Копите на мечту", "Поставьте цель и срок — посчитаем, сколько откладывать каждую неделю") }
            }
        }
        PrimaryButton("Новая цель", Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), leading = Icons.Rounded.Add) { dialog = GoalDialog.Edit(null) }
    }

    when (val d = dialog) {
        is GoalDialog.Edit -> GoalSheet(
            goal = d.goal,
            onDismiss = { dialog = null },
            onSave = { viewModel.save(it); actions.snack(if (d.goal == null) "Цель создана" else "Цель сохранена"); dialog = null },
            onDelete = d.goal?.let { g -> { viewModel.delete(g.id); actions.snack("Цель удалена"); dialog = null } },
        )
        is GoalDialog.Deposit -> DepositSheet(
            goal = d.goal,
            onDismiss = { dialog = null },
            onSave = { amount ->
                viewModel.deposit(d.goal.id, amount)
                val reached = d.goal.saved + amount >= d.goal.target && d.goal.saved < d.goal.target
                actions.snack(if (reached) "Цель «${d.goal.name}» достигнута!" else if (amount > 0) "Копилка пополнена" else "Снято из копилки")
                dialog = null
            },
        )
        null -> Unit
    }
}

@Composable
private fun GoalCard(goal: Goal, onEdit: () -> Unit, onDeposit: () -> Unit) {
    val c = App.colors
    val fmt = Money.fmt
    val done = goal.saved >= goal.target
    Column(Modifier.fillMaxWidth().card().clickable(onClick = onEdit).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(contentAlignment = Alignment.Center) {
                ProgressRing(goal.ratio, c.chart(goal.hue), 64.dp)
                CategoryAvatar(goal.hue, CategoryIcons.of(goal.icon), size = 44.dp, iconSize = 22.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(goal.name, style = AppText.section.copy(fontSize = 16.sp), color = c.on)
                Text("${fmt.format(goal.saved, false)} из ${fmt.format(goal.target, false)}", style = AppText.caption, color = c.on2)
            }
            Text("${(goal.ratio * 100).toInt().coerceAtMost(999)}%", style = AppText.amount, color = if (done) c.inc else c.on)
        }
        val hint = when {
            done -> "Цель достигнута"
            goal.deadline != null -> {
                val days = ChronoUnit.DAYS.between(LocalDate.now(), Period.dateOf(goal.deadline))
                if (days <= 0) "Срок прошёл · осталось ${fmt.format(goal.left, false)}"
                else {
                    val weeks = ((days + 6) / 7).coerceAtLeast(1)
                    "К ${Dates.dayMonth(Period.dateOf(goal.deadline))}: откладывайте ${fmt.format(goal.left / weeks, false)} в неделю"
                }
            }
            else -> "Осталось ${fmt.format(goal.left, false)}"
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(hint, style = AppText.caption, color = if (done) c.inc else c.on2, modifier = Modifier.weight(1f))
            PrimaryButton("Пополнить", height = 40.dp, container = c.priC, content = c.onPriC, onClick = onDeposit)
        }
    }
}

@Composable
fun ProgressRing(ratio: Float, color: androidx.compose.ui.graphics.Color, size: Dp, stroke: Dp = 5.dp) {
    val c = App.colors
    Canvas(Modifier.size(size)) {
        val s = stroke.toPx()
        val arc = Size(this.size.width - s, this.size.height - s)
        drawArc(c.s2, 0f, 360f, false, Offset(s / 2, s / 2), arc, style = Stroke(s))
        drawArc(color, -90f, 360f * ratio.coerceIn(0f, 1f), false, Offset(s / 2, s / 2), arc, style = Stroke(s, cap = StrokeCap.Round))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalSheet(goal: Goal?, onDismiss: () -> Unit, onSave: (Goal) -> Unit, onDelete: (() -> Unit)?) {
    val c = App.colors
    var name by remember { mutableStateOf(goal?.name.orEmpty()) }
    var target by remember { mutableStateOf(goal?.target?.let { amountInput(it) }.orEmpty()) }
    var deadline by remember { mutableStateOf(goal?.deadline?.let { Period.dateOf(it) }) }
    var icon by remember { mutableStateOf(goal?.icon ?: "savings") }
    var hue by remember { mutableStateOf(goal?.hue ?: 65f) }
    var showDate by remember { mutableStateOf(false) }
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
            Text(if (goal == null) "Новая цель" else "Цель", style = AppText.section.copy(fontSize = 16.sp), color = c.on)
            FilledField("Название", name, { name = it }, placeholder = "Ноутбук, отпуск, подушка безопасности")
            FilledField("Сумма цели, ${Money.fmt.symbol}", target, { v -> target = v.filter { it.isDigit() || it == ',' || it == '.' } }, keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(deadline?.let { "К ${Dates.dayMonth(it)} ${it.year}" } ?: "Без срока", leading = Icons.Rounded.CalendarToday, trailing = null) { showDate = true }
                if (deadline != null) FilterChip("Убрать срок", trailing = null) { deadline = null }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                goalHues.forEach { h ->
                    SelectRing(h == hue, 28.dp) { Box(Modifier.size(28.dp).clip(CircleShape).background(c.chart(h)).clickable { hue = h }) }
                }
            }
            goalIcons.chunked(5).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { key ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            SelectRing(key == icon, 40.dp) { Box(Modifier.clip(CircleShape).clickable { icon = key }) { CategoryAvatar(hue, CategoryIcons.of(key)) } }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) TextAction("Удалить", color = c.exp, onClick = onDelete)
                Spacer(Modifier.weight(1f))
                PrimaryButton("Сохранить", height = 48.dp, enabled = name.isNotBlank() && parseAmount(target) > 0) {
                    onSave(
                        Goal(
                            id = goal?.id ?: 0,
                            name = name.trim(),
                            target = parseAmount(target),
                            deadline = deadline?.let { Period.startOfDay(it) },
                            icon = icon,
                            hue = hue,
                            createdAt = goal?.createdAt ?: System.currentTimeMillis(),
                        )
                    )
                }
            }
        }
    }
    if (showDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = (deadline ?: LocalDate.now().plusMonths(6)).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton({
                    picker.selectedDateMillis?.let { deadline = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    showDate = false
                }) { Text("Готово") }
            },
            dismissButton = { TextButton({ showDate = false }) { Text("Отмена") } },
        ) {
            HideSystemBars()
            DatePicker(picker)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepositSheet(goal: Goal, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    val c = App.colors
    var mode by remember { mutableStateOf(0) }
    var amount by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.s1,
        dragHandle = { SheetHandle(Modifier.padding(top = 8.dp)) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        HideSystemBars()
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(goal.name, style = AppText.section.copy(fontSize = 16.sp), color = c.on)
            Segmented(listOf("Пополнить", "Снять"), mode, { mode = it })
            FilledField("Сумма, ${Money.fmt.symbol}", amount, { v -> amount = v.filter { it.isDigit() || it == ',' || it == '.' } }, keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(100_000L, 500_000L, 1_000_000L).forEach { preset ->
                    FilterChip(Money.fmt.plain(preset, false), trailing = null) { amount = amountInput(preset) }
                }
            }
            Text("Копилка — это отметка для себя: баланс счетов не меняется.", style = AppText.caption, color = c.on2)
            val value = parseAmount(amount)
            PrimaryButton(if (mode == 0) "Пополнить" else "Снять", Modifier.fillMaxWidth(), enabled = value > 0 && (mode == 0 || value <= goal.saved)) {
                onSave(if (mode == 0) value else -value)
            }
        }
    }
}

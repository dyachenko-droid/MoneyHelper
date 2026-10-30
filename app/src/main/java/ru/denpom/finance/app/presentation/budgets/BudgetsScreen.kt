package ru.denpom.finance.app.presentation.budgets

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.denpom.finance.app.domain.model.Category
import ru.denpom.finance.app.presentation.components.CategoryAvatar
import ru.denpom.finance.app.presentation.components.CategoryIcons
import ru.denpom.finance.app.presentation.components.FilledField
import ru.denpom.finance.app.presentation.components.FilterChip
import ru.denpom.finance.app.presentation.components.PrimaryButton
import ru.denpom.finance.app.presentation.components.ProgressLine
import ru.denpom.finance.app.presentation.components.ScreenTitle
import ru.denpom.finance.app.presentation.components.SectionHeader
import ru.denpom.finance.app.presentation.components.SheetHandle
import ru.denpom.finance.app.presentation.components.TextAction
import ru.denpom.finance.app.presentation.components.budgetColor
import ru.denpom.finance.app.presentation.components.card
import ru.denpom.finance.app.presentation.format.Dates
import ru.denpom.finance.app.presentation.format.Money
import ru.denpom.finance.app.presentation.format.amountInput
import ru.denpom.finance.app.presentation.format.parseAmount
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import ru.denpom.finance.app.presentation.theme.BudgetBarOnHero
import ru.denpom.finance.app.presentation.components.HideSystemBars

private data class LimitEdit(val category: Category?, val current: Long?)

@Composable
fun BudgetsScreen(viewModel: BudgetsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = App.colors
    val fmt = Money.fmt
    var edit by remember { mutableStateOf<LimitEdit?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            ScreenTitle("Бюджеты") {
                var menu by remember { mutableStateOf(false) }
                Box {
                    FilterChip(Dates.monthTitle(state.month)) { menu = true }
                    DropdownMenu(menu, { menu = false }) {
                        state.months.forEach { m -> DropdownMenuItem({ Text(Dates.monthTitle(m)) }, { viewModel.setMonth(m); menu = false }) }
                    }
                }
            }
        }
        item {
            val limit = state.limit
            Column(
                Modifier.padding(bottom = 8.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.hero)
                    .clickable { edit = LimitEdit(null, limit) }.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Общий лимит месяца", style = AppText.label.copy(fontWeight = FontWeight.Normal), color = c.onHero.copy(alpha = 0.8f), modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.Edit, "Изменить", tint = c.onHero.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(fmt.format(state.spent, false), style = AppText.amountL.copy(fontSize = 30.sp), color = c.onHero)
                    Text(if (limit != null) "из ${fmt.format(limit, false)}" else "потрачено", style = AppText.body, color = c.onHero.copy(alpha = 0.75f), modifier = Modifier.padding(bottom = 5.dp))
                }
                if (limit != null) {
                    ProgressLine(state.spent.toFloat() / limit, BudgetBarOnHero, height = 10.dp, track = Color.White.copy(alpha = 0.16f))
                    val left = limit - state.spent
                    val text = when {
                        left < 0 -> "Лимит превышен на ${fmt.format(-left, false)}"
                        state.daysLeft > 0 -> "Осталось ${fmt.format(left, false)} на ${Dates.days(state.daysLeft.toLong())} · ≈ ${fmt.format(left / state.daysLeft, false)} в день"
                        else -> "Сэкономлено ${fmt.format(left, false)}"
                    }
                    Text(text, style = AppText.caption.copy(fontSize = 14.sp), color = c.onHero.copy(alpha = 0.85f))
                } else {
                    Text("Нажмите, чтобы задать лимит на месяц", style = AppText.caption.copy(fontSize = 14.sp), color = c.onHero.copy(alpha = 0.85f))
                }
            }
        }
        items(state.items, key = { it.category.id }) { b ->
            val color = budgetColor(b.ratio)
            Column(
                Modifier.fillMaxWidth().card(RoundedCornerShape(20.dp)).clickable { edit = LimitEdit(b.category, b.limit) }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategoryAvatar(b.category.hue, CategoryIcons.of(b.category.icon))
                    Column(Modifier.weight(1f)) {
                        Text(b.category.name, style = AppText.bodyMedium, color = c.on)
                        val over = b.spent - b.limit
                        Text(
                            if (over > 0) "Превышен на ${fmt.format(over, false)}" else "${fmt.format(b.spent, false)} из ${fmt.format(b.limit, false)}",
                            style = AppText.caption,
                            color = if (over > 0) c.exp else c.on2,
                        )
                    }
                    Text("${(b.ratio * 100).toInt()}%", style = AppText.amount, color = color)
                }
                ProgressLine(b.ratio, color)
            }
        }
        if (state.unbudgeted.isNotEmpty()) {
            item { SectionHeader("Без лимита", Modifier.padding(top = 12.dp)) }
            item {
                Column(Modifier.card(RoundedCornerShape(20.dp)).padding(vertical = 4.dp)) {
                    state.unbudgeted.forEach { (cat, spent) ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable { edit = LimitEdit(cat, null) }.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CategoryAvatar(cat.hue, CategoryIcons.of(cat.icon))
                            Column(Modifier.weight(1f)) {
                                Text(cat.name, style = AppText.bodyMedium, color = c.on)
                                if (spent > 0) Text("Потрачено ${fmt.format(spent, false)}", style = AppText.caption, color = c.on2)
                            }
                            Icon(Icons.Rounded.Add, "Задать лимит", tint = c.pri, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }

    edit?.let { e ->
        LimitSheet(
            title = e.category?.name ?: "Общий лимит месяца",
            initial = e.current,
            onDismiss = { edit = null },
            onSave = { viewModel.save(e.category?.id, it); edit = null },
            onDelete = if (e.current != null) ({ viewModel.delete(e.category?.id); edit = null }) else null,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitSheet(title: String, initial: Long?, onDismiss: () -> Unit, onSave: (Long) -> Unit, onDelete: (() -> Unit)?) {
    val c = App.colors
    var text by remember { mutableStateOf(initial?.let { amountInput(it) }.orEmpty()) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.s1,
        dragHandle = { SheetHandle(Modifier.padding(top = 8.dp)) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        HideSystemBars()
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = AppText.section.copy(fontSize = 16.sp), color = c.on)
            FilledField(
                label = "Лимит, ${Money.fmt.symbol}",
                value = text,
                onValueChange = { v -> text = v.filter { it.isDigit() || it == ',' || it == '.' } },
                placeholder = "0",
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) TextAction("Убрать лимит", color = c.exp, onClick = onDelete)
                Spacer(Modifier.weight(1f))
                PrimaryButton("Сохранить", height = 48.dp, enabled = parseAmount(text) > 0) { onSave(parseAmount(text)) }
            }
        }
    }
}

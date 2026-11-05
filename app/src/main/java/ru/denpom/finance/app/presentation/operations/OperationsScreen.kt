package ru.denpom.finance.app.presentation.operations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.denpom.finance.app.domain.model.TypeFilter
import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.presentation.LocalAppActions
import ru.denpom.finance.app.presentation.components.CategoryAvatar
import ru.denpom.finance.app.presentation.components.EmptyState
import ru.denpom.finance.app.presentation.components.FilterChip
import ru.denpom.finance.app.presentation.components.IconField
import ru.denpom.finance.app.presentation.components.card
import ru.denpom.finance.app.presentation.format.Dates
import ru.denpom.finance.app.presentation.format.Money
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import kotlin.math.roundToInt

@Composable
fun OperationsScreen(
    onOpen: (Operation) -> Unit,
    viewModel: OperationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = App.colors
    val fmt = Money.fmt
    val actions = LocalAppActions.current
    var openId by remember { mutableStateOf<Long?>(null) }
    val accNames = state.accounts.associate { it.id to it.name }

    Column(Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding()) {
        Column(Modifier.background(c.bg).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Операции", style = AppText.title, color = c.on, modifier = Modifier.height(40.dp).padding(top = 4.dp))
            IconField(
                icon = Icons.Rounded.Search,
                value = state.filter.query,
                onValueChange = viewModel::setQuery,
                placeholder = "Магазин, позиция, заметка, #тег",
                height = 52.dp,
                radius = 26.dp,
                trailing = {
                    if (state.filter.query.isNotEmpty()) {
                        Box(Modifier.size(32.dp).clip(CircleShape).clickable { viewModel.setQuery("") }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, "Очистить", tint = c.on2, modifier = Modifier.size(20.dp))
                        }
                    }
                },
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                var monthMenu by remember { mutableStateOf(false) }
                Box {
                    FilterChip(state.filter.month?.let { Dates.monthTitle(it) } ?: "Всё время", selected = true) { monthMenu = true }
                    DropdownMenu(monthMenu, { monthMenu = false }) {
                        DropdownMenuItem({ Text("Всё время") }, { viewModel.setMonth(null); monthMenu = false })
                        state.months.forEach { m ->
                            DropdownMenuItem({ Text(Dates.monthTitle(m)) }, { viewModel.setMonth(m); monthMenu = false })
                        }
                    }
                }
                FilterChip(
                    state.filter.type.label,
                    selected = state.filter.type != TypeFilter.ALL,
                    trailing = Icons.Rounded.SwapVert,
                ) { viewModel.cycleType() }
                var accMenu by remember { mutableStateOf(false) }
                Box {
                    FilterChip(state.filter.accountId?.let { accNames[it] } ?: "Все счета", selected = state.filter.accountId != null) { accMenu = true }
                    DropdownMenu(accMenu, { accMenu = false }) {
                        DropdownMenuItem({ Text("Все счета") }, { viewModel.setAccount(null); accMenu = false })
                        state.accounts.forEach { a -> DropdownMenuItem({ Text(a.name) }, { viewModel.setAccount(a.id); accMenu = false }) }
                    }
                }
                var catMenu by remember { mutableStateOf(false) }
                Box {
                    FilterChip(state.filter.categoryId?.let { state.categories[it]?.name } ?: "Категории", selected = state.filter.categoryId != null) { catMenu = true }
                    DropdownMenu(catMenu, { catMenu = false }) {
                        DropdownMenuItem({ Text("Все категории") }, { viewModel.setCategory(null); catMenu = false })
                        state.categories.values.forEach { cat ->
                            DropdownMenuItem({ Text(cat.name) }, { viewModel.setCategory(cat.id); catMenu = false })
                        }
                    }
                }
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.loaded && state.hasAny) item {
                val totals = state.summary
                Column(Modifier.fillMaxWidth().card().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Найдено операций: ${totals.count}", style = AppText.caption, color = c.on2)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Расходы", style = AppText.caption, color = c.on2)
                            Text(fmt.format(totals.expenses, false), style = AppText.amount, color = c.exp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Доходы", style = AppText.caption, color = c.on2)
                            Text(fmt.format(totals.income, false), style = AppText.amount, color = c.inc)
                        }
                    }
                    if (totals.transferCount > 0) {
                        Text("Переводы: ${totals.transferCount} · ${fmt.format(totals.transferAmount, false)}", style = AppText.caption, color = c.on2)
                        if (state.filter.accountId != null) Text("На счёт ${fmt.format(totals.transferIn, false)} · со счёта ${fmt.format(totals.transferOut, false)}", style = AppText.caption, color = c.on2)
                    }
                    Text("Изменение по выборке: ${fmt.signed(totals.net)}", style = AppText.bodyMedium, color = c.on)
                }
            }
            items(state.groups, key = { it.date.toEpochDay() }) { group ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.padding(horizontal = 4.dp)) {
                        Text(Dates.dayHeader(group.date), style = AppText.caption.copy(fontWeight = FontWeight.SemiBold), color = c.on2, modifier = Modifier.weight(1f))
                        Text(fmt.signed(group.total), style = AppText.caption, color = c.on2)
                    }
                    Column(Modifier.card(RoundedCornerShape(20.dp))) {
                        group.items.forEach { op ->
                            SwipeRow(
                                open = openId == op.id,
                                onOpenChange = { openId = if (it) op.id else null },
                                onDuplicate = {
                                    openId = null
                                    viewModel.duplicate(op)
                                    actions.snack("Операция продублирована")
                                },
                                onDelete = {
                                    openId = null
                                    viewModel.delete(op) { undo -> actions.snack("Операция удалена", "Отменить", undo) }
                                },
                                onClick = { onOpen(op) },
                            ) {
                                val v = operationVisuals(op, state.categories, accNames, fmt)
                                Row(
                                    Modifier.fillMaxWidth().height(64.dp).background(c.s1).padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    CategoryAvatar(v.hue, v.icon)
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(v.title, style = AppText.bodyMedium, color = c.on, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                                            if (op.receiptId != null) Icon(Icons.Rounded.ReceiptLong, null, tint = c.on2, modifier = Modifier.size(16.dp))
                                        }
                                        if (v.subtitle.isNotEmpty()) Text(v.subtitle, style = AppText.caption, color = c.on2, maxLines = 1)
                                    }
                                    Text(v.amount, style = AppText.amount, color = v.color, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
            if (state.loaded && state.groups.isEmpty()) {
                item {
                    if (state.hasAny) {
                        EmptyState(Icons.Rounded.SearchOff, "Ничего не нашлось", "Измените запрос или покажите операции за всё время", action = "Показать все операции", onAction = viewModel::reset)
                    } else {
                        EmptyState(Icons.Rounded.ReceiptLong, "Операций пока нет", "Отсканируйте чек или добавьте расход — он появится здесь")
                    }
                }
            }
            if (state.groups.isNotEmpty()) {
                item {
                    Text(
                        "Смахните строку влево, чтобы удалить или дублировать",
                        style = AppText.small.copy(fontWeight = FontWeight.Normal),
                        color = c.on2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeRow(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val c = App.colors
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val revealPx = with(density) { 144.dp.toPx() }
    val maxPx = with(density) { 170.dp.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var dragging by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(open) {
        if (!dragging) offset.animateTo(if (open) -revealPx else 0f, tween(250))
    }

    Box(Modifier.fillMaxWidth().height(64.dp)) {
        Row(Modifier.matchParentSize(), horizontalArrangement = Arrangement.End) {
            ActionCell(Icons.Rounded.ContentCopy, "Дублировать", c.ambC, c.onAmbC, onDuplicate)
            ActionCell(Icons.Rounded.Delete, "Удалить", c.exp, c.s1, onDelete)
        }
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = {
                            dragging = false
                            val shouldOpen = offset.value < -revealPx / 2
                            if (shouldOpen) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            scope.launch { offset.animateTo(if (shouldOpen) -revealPx else 0f, tween(250)) }
                            onOpenChange(shouldOpen)
                        },
                        onDragCancel = {
                            dragging = false
                            scope.launch { offset.animateTo(if (open) -revealPx else 0f) }
                        },
                    ) { change, amount ->
                        change.consume()
                        scope.launch { offset.snapTo((offset.value + amount).coerceIn(-maxPx, 0f)) }
                    }
                }
                .clickable {
                    if (open) onOpenChange(false) else onClick()
                },
        ) { content() }
    }
}

@Composable
private fun ActionCell(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Column(
        Modifier.width(72.dp).fillMaxHeight().background(bg).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
        Text(label, style = AppText.tiny.copy(fontWeight = FontWeight.Medium), color = fg)
    }
}

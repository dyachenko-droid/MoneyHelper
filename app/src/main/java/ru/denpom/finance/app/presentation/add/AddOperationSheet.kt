package ru.denpom.finance.app.presentation.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.denpom.finance.app.domain.model.CategoryKind
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.presentation.AddRequest
import ru.denpom.finance.app.presentation.LocalAppActions
import ru.denpom.finance.app.presentation.components.CategoryAvatar
import ru.denpom.finance.app.presentation.components.CategoryIcons
import ru.denpom.finance.app.presentation.components.FilterChip
import ru.denpom.finance.app.presentation.components.IconField
import ru.denpom.finance.app.presentation.components.PrimaryButton
import ru.denpom.finance.app.presentation.components.Segmented
import ru.denpom.finance.app.presentation.components.SelectRing
import ru.denpom.finance.app.presentation.components.SheetHandle
import ru.denpom.finance.app.presentation.format.Dates
import ru.denpom.finance.app.presentation.format.Money
import ru.denpom.finance.app.presentation.format.NBSP
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import java.time.Instant
import java.time.ZoneOffset
import ru.denpom.finance.app.presentation.components.HideSystemBars

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOperationSheet(
    request: AddRequest,
    onDismiss: () -> Unit,
    onOpenReceipt: (Long) -> Unit,
    viewModel: AddOperationViewModel = hiltViewModel(),
) {
    val c = App.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val haptics = LocalHapticFeedback.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showDate by remember { mutableStateOf(false) }

    LaunchedEffect(request) { viewModel.start(request) }

    fun close() {
        scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = c.s1,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        HideSystemBars()
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.94f).navigationBarsPadding().imePadding().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SheetHandle()
            if (state.editId == null) {
                Segmented(
                    listOf("Расход", "Доход", "Перевод"),
                    state.type.ordinal,
                    { viewModel.setType(OperationType.entries[it]) },
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Редактирование", style = AppText.section.copy(fontSize = 16.sp), color = c.on, modifier = Modifier.weight(1f))
                    if (state.receiptId != null) {
                        FilterChip("Чек", leading = Icons.Rounded.ReceiptLong, trailing = null) {
                            val id = state.receiptId ?: return@FilterChip
                            close()
                            onOpenReceipt(id)
                        }
                    }
                }
            }
            val display = state.amount.ifEmpty { "0" }.let { a ->
                val parts = a.split(',')
                val grouped = parts[0].reversed().chunked(3).joinToString(NBSP.toString()).reversed()
                if (parts.size > 1) "$grouped,${parts[1]}" else grouped
            }
            Row(
                Modifier.fillMaxWidth().height(56.dp).clickable { viewModel.clearAmount() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    display,
                    style = AppText.amountXL.copy(fontSize = 40.sp, letterSpacing = (-0.02).sp * 40),
                    color = when {
                        state.amount.isEmpty() -> c.on2
                        state.type == OperationType.INCOME -> c.inc
                        state.type == OperationType.EXPENSE -> c.exp
                        else -> c.on
                    },
                    maxLines = 1,
                )
                Text(" ${Money.fmt.symbol}", style = AppText.amountL.copy(fontSize = 28.sp, fontWeight = FontWeight.Medium), color = c.on2, modifier = Modifier.padding(bottom = 3.dp))
            }

            if (state.type == OperationType.TRANSFER) {
                Row(Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    AccountPicker(accounts.firstOrNull { it.id == state.accountId }?.name ?: "Откуда", accounts, viewModel::setAccount)
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = c.on2, modifier = Modifier.size(22.dp))
                    AccountPicker(accounts.firstOrNull { it.id == state.toAccountId }?.name ?: "Куда", accounts.filter { it.id != state.accountId }, viewModel::setToAccount)
                }
            } else {
                val kind = if (state.type == OperationType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
                val list = categories.filter { it.kind == kind }
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val cell = maxWidth / 4
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(if (list.size > 4) 2 else 1),
                        modifier = Modifier.fillMaxWidth().height(if (list.size > 4) 150.dp else 76.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(list, key = { it.id }) { cat ->
                            val selected = cat.id == state.categoryId
                            Column(
                                Modifier.width(cell).clip(RoundedCornerShape(12.dp)).clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    viewModel.setCategory(cat.id)
                                },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                SelectRing(selected, 44.dp) { CategoryAvatar(cat.hue, CategoryIcons.of(cat.icon), size = 44.dp) }
                                Text(
                                    cat.name,
                                    style = AppText.tiny.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                                    color = c.on,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.type != OperationType.TRANSFER) {
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            accounts.firstOrNull { it.id == state.accountId }?.name ?: "Счёт",
                            leading = Icons.Rounded.AccountBalanceWallet,
                            trailing = null,
                        ) { menu = true }
                        DropdownMenu(menu, { menu = false }) {
                            accounts.forEach { a -> DropdownMenuItem({ Text(a.name) }, { viewModel.setAccount(a.id); menu = false }) }
                        }
                    }
                }
                FilterChip(Dates.chipDate(state.date), leading = Icons.Rounded.CalendarToday, trailing = null) { showDate = true }
                if (state.editId == null && state.type != OperationType.TRANSFER) {
                    FilterChip(
                        if (state.repeat) "Каждый месяц" else "Повторять",
                        selected = state.repeat,
                        leading = Icons.Rounded.Repeat,
                        trailing = null,
                    ) { viewModel.toggleRepeat() }
                }
            }
            IconField(Icons.Rounded.EditNote, state.note, viewModel::setNote, "Заметка или #тег")

            Column(Modifier.fillMaxWidth().weight(1f).heightIn(min = 200.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(",", "0", "⌫")).forEach { row ->
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { key ->
                            Box(
                                Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(16.dp)).background(c.s2).clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                                    viewModel.press(key)
                                },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(key, style = AppText.amountM.copy(fontSize = 22.sp, fontWeight = FontWeight.Medium), color = c.on)
                            }
                        }
                    }
                }
            }
            PrimaryButton("Сохранить", Modifier.fillMaxWidth(), enabled = state.value > 0) {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                viewModel.save(
                    onDone = { msg ->
                        actions.snack(msg)
                        close()
                    },
                    onError = { actions.snack(it) },
                )
            }
        }
    }

    if (showDate) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton({
                    picker.selectedDateMillis?.let { viewModel.setDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
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

@Composable
private fun AccountPicker(label: String, accounts: List<ru.denpom.finance.app.domain.model.Account>, onPick: (Long) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.height(40.dp).clip(RoundedCornerShape(12.dp)).background(App.colors.s2).clickable { menu = true }.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val account = accounts.firstOrNull { it.name == label }
            Icon(account?.let { CategoryIcons.account(it.type) } ?: Icons.Rounded.AccountBalanceWallet, null, tint = App.colors.on, modifier = Modifier.size(18.dp))
            Text(label, style = AppText.label.copy(fontWeight = FontWeight.Normal), color = App.colors.on, maxLines = 1)
        }
        DropdownMenu(menu, { menu = false }) {
            accounts.forEach { a -> DropdownMenuItem({ Text(a.name) }, { onPick(a.id); menu = false }) }
        }
    }
}

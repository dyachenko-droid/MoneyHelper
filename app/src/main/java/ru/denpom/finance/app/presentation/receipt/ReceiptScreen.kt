package ru.denpom.finance.app.presentation.receipt

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.denpom.finance.app.data.receipt.ImageLoader
import ru.denpom.finance.app.domain.model.Category
import ru.denpom.finance.app.domain.model.ReceiptItem
import ru.denpom.finance.app.domain.model.ReceiptStatus
import ru.denpom.finance.app.presentation.LocalAppActions
import ru.denpom.finance.app.presentation.components.BackTopBar
import ru.denpom.finance.app.presentation.components.CategoryAvatar
import ru.denpom.finance.app.presentation.components.CategoryChip
import ru.denpom.finance.app.presentation.components.CategoryIcons
import ru.denpom.finance.app.presentation.components.EmptyState
import ru.denpom.finance.app.presentation.components.FilledField
import ru.denpom.finance.app.presentation.components.PrimaryButton
import ru.denpom.finance.app.presentation.components.RoundIconButton
import ru.denpom.finance.app.presentation.components.SelectRing
import ru.denpom.finance.app.presentation.components.SheetHandle
import ru.denpom.finance.app.presentation.components.TonalButton
import ru.denpom.finance.app.presentation.components.card
import ru.denpom.finance.app.presentation.format.Dates
import ru.denpom.finance.app.presentation.format.Money
import ru.denpom.finance.app.presentation.format.amountInput
import ru.denpom.finance.app.presentation.format.parseAmount
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import ru.denpom.finance.app.presentation.components.HideSystemBars
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import ru.denpom.finance.app.domain.model.Period

@Composable
fun ReceiptScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ReceiptViewModel = hiltViewModel(),
) {
    val c = App.colors
    val fmt = Money.fmt
    val actions = LocalAppActions.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var picker by remember { mutableStateOf<Int?>(null) }
    var itemEdit by remember { mutableStateOf<Int?>(null) }
    var addItem by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var split by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }

    if (pickDate) {
        state.receipt?.let { r ->
            ReceiptDatePicker(Period.dateOf(r.timestamp), { pickDate = false }) {
                pickDate = false
                viewModel.setDate(it)
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding()) {
        BackTopBar("Чек", onBack) {
            if (state.saved) {
                var menu by remember { mutableStateOf(false) }
                Box {
                    RoundIconButton(Icons.Rounded.MoreVert) { menu = true }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem({ Text("Переименовать") }, { menu = false; rename = true })
                        DropdownMenuItem({ Text("Удалить чек", color = c.exp) }, { menu = false; confirmDelete = true })
                    }
                }
            }
        }
        if (state.missing) {
            EmptyState(Icons.Rounded.ReceiptLong, "Чек не найден", "Возможно, он был удалён")
            return@Column
        }
        val receipt = state.receipt ?: return@Column
        val catMap = categories.associateBy { it.id }
        val editable = receipt.status == ReceiptStatus.RECOGNIZED

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(Modifier.card().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(receipt.shop, style = AppText.bar, color = c.on, modifier = Modifier.clickable { rename = true })
                            if (receipt.address.isNotBlank()) Text(receipt.address, style = AppText.caption, color = c.on2)
                            Text(Dates.full(receipt.timestamp), style = AppText.caption, color = c.on2, modifier = Modifier.clickable { pickDate = true })
                        }
                        ReceiptThumb(receipt.photoPath)
                    }
                    Text(fmt.format(receipt.total), style = AppText.amountL, color = c.on)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        var accMenu by remember { mutableStateOf(false) }
                        Box {
                            InfoChip(Icons.Rounded.CreditCard, accounts.firstOrNull { it.id == state.accountId }?.name ?: "Выберите счёт", c.s2, c.on) { accMenu = true }
                            DropdownMenu(accMenu, { accMenu = false }) {
                                accounts.forEach { a -> DropdownMenuItem({ Text(a.name) }, { viewModel.setAccount(a.id); accMenu = false }) }
                            }
                        }
                        when (receipt.status) {
                            ReceiptStatus.VERIFIED -> InfoChip(Icons.Rounded.Verified, "Данные кассы", c.priC, c.onPriC)
                            ReceiptStatus.PENDING -> InfoChip(Icons.Rounded.Schedule, "Только сумма", c.warnC, c.onWarnC)
                            ReceiptStatus.RECOGNIZED -> InfoChip(Icons.Rounded.DocumentScanner, "Распознан по фото", c.warnC, c.onWarnC)
                        }
                        receipt.warrantyUntil?.let {
                            InfoChip(Icons.Rounded.VerifiedUser, "Гарантия до ${Dates.dayMonth(ru.denpom.finance.app.domain.model.Period.dateOf(it))}", c.s2, c.on)
                        }
                    }
                }
            }
            if (receipt.items.isNotEmpty()) {
                item {
                    val segs = receipt.items.groupBy { it.categoryId }.map { (id, list) -> catMap[id] to list.sumOf { it.sum } }.sortedByDescending { it.second }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            segs.forEach { (cat, sum) ->
                                Box(Modifier.weight(sum.coerceAtLeast(1).toFloat()).fillMaxHeight().background(c.chart(cat?.hue ?: 165f)))
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            segs.forEach { (cat, sum) ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(c.chart(cat?.hue ?: 165f)))
                                    Text(cat?.name ?: "Без категории", style = AppText.caption, color = c.on2)
                                    Text(fmt.format(sum), style = AppText.caption.copy(fontWeight = FontWeight.Medium), color = c.on)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Column(Modifier.card().padding(vertical = 4.dp)) {
                    val count = receipt.items.size.toLong()
                    Text(
                        if (count == 0L) "Позиций нет" else "$count ${Dates.plural(count, "позиция", "позиции", "позиций")}",
                        style = AppText.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = c.on2,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                    )
                    if (receipt.status == ReceiptStatus.PENDING) {
                        Text(
                            "Для этого чека сохранена только сумма.",
                            style = AppText.caption,
                            color = c.on2,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    receipt.items.forEachIndexed { index, item ->
                        ItemRow(
                            item = item,
                            category = catMap[item.categoryId],
                            last = index == receipt.items.lastIndex && !editable,
                            onPick = { picker = index },
                            onEdit = if (editable) ({ itemEdit = index }) else null,
                        )
                    }
                    if (editable) {
                        Row(
                            Modifier.fillMaxWidth().height(52.dp).clickable { addItem = true }.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Rounded.Add, null, tint = c.pri, modifier = Modifier.size(20.dp))
                            Text("Добавить позицию", style = AppText.label.copy(fontWeight = FontWeight.SemiBold), color = c.pri)
                        }
                    }
                }
            }
            if (editable) {
                item {
                    Text(
                        "Распознавание по фото может ошибаться — проверьте названия и суммы. Нажмите на позицию, чтобы исправить.",
                        style = AppText.caption,
                        color = c.on2,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(c.bg)
                .drawBehind { drawLine(c.s2, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TonalButton("Разделить", leading = Icons.Rounded.CallSplit) { split = true }
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(if (receipt.warrantyUntil != null) c.priC else c.s2).clickable {
                    val on = viewModel.toggleWarranty()
                    actions.snack(if (on) "Гарантия отмечена на год" else "Метка гарантии снята")
                },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.VerifiedUser, "Гарантийный чек", tint = if (receipt.warrantyUntil != null) c.onPriC else c.on, modifier = Modifier.size(20.dp))
            }
            PrimaryButton(if (state.saved) "Готово" else "Сохранить", Modifier.weight(1f), height = 48.dp) {
                if (state.saved) onBack() else viewModel.save(
                    onDone = { n ->
                        actions.snack(if (n > 0) "Чек сохранён · $n ${Dates.plural(n.toLong(), "позиция", "позиции", "позиций")}" else "Чек сохранён")
                        onSaved()
                    },
                    onError = { actions.snack(it) },
                )
            }
        }

        picker?.let { index ->
            val item = receipt.items.getOrNull(index)
            if (item != null) {
                CategoryPickerSheet(
                    itemName = item.name,
                    categories = viewModel.expenseCategories(),
                    selected = item.categoryId,
                    onPick = { viewModel.setItemCategory(index, it); picker = null },
                    onDismiss = { picker = null },
                )
            }
        }
        if (itemEdit != null || addItem) {
            val index = itemEdit
            ItemEditDialog(
                item = index?.let { receipt.items.getOrNull(it) },
                onDismiss = { itemEdit = null; addItem = false },
                onSave = { name, qty, price -> viewModel.upsertItem(index, name, qty, price); itemEdit = null; addItem = false },
                onDelete = index?.let { { viewModel.deleteItem(it); itemEdit = null } },
            )
        }
        if (split) {
            SplitSheet(receipt, { split = false }) { shares ->
                split = false
                viewModel.recordSplit(shares) { n -> actions.snack("Записано в «Мне должны» · $n ${Dates.plural(n.toLong(), "человек", "человека", "человек")}") }
            }
        }
        if (rename) {
            TextDialog("Название магазина", receipt.shop, { rename = false }) { viewModel.rename(it); rename = false }
        }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = {
                    HideSystemBars()
                    Text("Удалить чек?")
                },
                text = { Text("Операция и все позиции чека будут удалены.") },
                confirmButton = { TextButton({ confirmDelete = false; viewModel.delete { actions.snack("Чек удалён"); onBack() } }) { Text("Удалить", color = c.exp) } },
                dismissButton = { TextButton({ confirmDelete = false }) { Text("Отмена") } },
                containerColor = c.s1,
            )
        }
    }
}

@Composable
private fun ItemRow(item: ReceiptItem, category: Category?, last: Boolean, onPick: () -> Unit, onEdit: (() -> Unit)?) {
    val c = App.colors
    val fmt = Money.fmt
    val qty = if (item.unit == "кг") "${item.quantity.toString().replace('.', ',').trimEnd('0').trimEnd(',')} кг" else "${item.quantity.toLong()} шт"
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (onEdit != null) Modifier.clickable(onClick = onEdit) else Modifier)
            .drawBehind { if (!last) drawLine(c.s2, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.name, style = AppText.body.copy(lineHeight = 20.sp), color = c.on)
                Text("$qty × ${fmt.format(item.price)}", style = AppText.caption, color = c.on2)
            }
            Text(fmt.format(item.sum), style = AppText.amount, color = c.on, maxLines = 1)
        }
        CategoryChip(category, onClick = onPick)
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String, bg: Color, fg: Color, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.height(32.dp).clip(RoundedCornerShape(10.dp)).background(bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
        Text(text, style = AppText.caption, color = fg, maxLines = 1)
    }
}

@Composable
private fun ReceiptThumb(path: String?) {
    val c = App.colors
    val bitmap by produceState<Bitmap?>(null, path) {
        value = path?.let { withContext(Dispatchers.IO) { ImageLoader.load(it, 300) } }
    }
    Box(Modifier.width(56.dp).height(76.dp).clip(RoundedCornerShape(12.dp)).background(c.s2), contentAlignment = Alignment.Center) {
        val b = bitmap
        if (b != null) Image(b.asImageBitmap(), "Фото чека", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Icon(Icons.Rounded.ReceiptLong, null, tint = c.on2, modifier = Modifier.size(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    itemName: String,
    categories: List<Category>,
    selected: Long?,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Категория позиции",
    showRuleNote: Boolean = true,
) {
    val c = App.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.s1,
        dragHandle = { SheetHandle(Modifier.padding(top = 8.dp)) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        HideSystemBars()
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = AppText.section.copy(fontSize = 16.sp), color = c.on)
                if (itemName.isNotBlank()) Text(itemName, style = AppText.caption, color = c.on2)
            }
            categories.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { cat ->
                        val active = cat.id == selected
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { onPick(cat.id) }.padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            SelectRing(active, 48.dp) { CategoryAvatar(cat.hue, CategoryIcons.of(cat.icon), size = 48.dp, iconSize = 22.dp) }
                            Text(cat.name, style = AppText.small.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal), color = c.on, maxLines = 1)
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (showRuleNote) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.s2).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = c.on2, modifier = Modifier.size(18.dp))
                    Text("Запомним правило для этого товара", style = AppText.caption, color = c.on2)
                }
            }
        }
    }
}

@Composable
private fun ItemEditDialog(item: ReceiptItem?, onDismiss: () -> Unit, onSave: (String, Double, Long) -> Unit, onDelete: (() -> Unit)?) {
    val c = App.colors
    var name by remember { mutableStateOf(item?.name.orEmpty()) }
    var qty by remember { mutableStateOf(item?.quantity?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString().replace('.', ',') } ?: "1") }
    var price by remember { mutableStateOf(item?.price?.let { amountInput(it) }.orEmpty()) }
    val q = qty.replace(',', '.').toDoubleOrNull() ?: 0.0
    val p = parseAmount(price)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.s1,
        title = {
            HideSystemBars()
            Text(if (item == null) "Новая позиция" else "Позиция")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledField("Название", name, { name = it }, singleLine = false)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledField("Количество", qty, { v -> qty = v.filter { it.isDigit() || it == ',' || it == '.' } }, Modifier.weight(1f), keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    FilledField("Цена, ${Money.fmt.symbol}", price, { v -> price = v.filter { it.isDigit() || it == ',' || it == '.' } }, Modifier.weight(1f), keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            }
        },
        confirmButton = { TextButton({ onSave(name.trim(), q, p) }, enabled = name.isNotBlank() && q > 0 && p > 0) { Text("Сохранить") } },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onDelete) { Text("Удалить", color = c.exp) }
                TextButton(onDismiss) { Text("Отмена") }
            }
        },
    )
}

@Composable
fun TextDialog(title: String, initial: String, onDismiss: () -> Unit, label: String = "Название", onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = App.colors.s1,
        title = {
            HideSystemBars()
            Text(title)
        },
        text = { FilledField(label, text, { text = it }) },
        confirmButton = { TextButton({ onSave(text) }, enabled = text.isNotBlank()) { Text("Сохранить") } },
        dismissButton = { TextButton(onDismiss) { Text("Отмена") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptDatePicker(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val picker = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton({
                picker.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } ?: onDismiss()
            }) { Text("Готово") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Отмена") } },
    ) {
        HideSystemBars()
        DatePicker(picker)
    }
}

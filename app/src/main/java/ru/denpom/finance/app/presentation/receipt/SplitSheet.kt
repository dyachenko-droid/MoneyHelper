package ru.denpom.finance.app.presentation.receipt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.denpom.finance.app.domain.model.Receipt
import ru.denpom.finance.app.presentation.components.IconField
import ru.denpom.finance.app.presentation.components.PrimaryButton
import ru.denpom.finance.app.presentation.components.Segmented
import ru.denpom.finance.app.presentation.components.SheetHandle
import ru.denpom.finance.app.presentation.components.card
import ru.denpom.finance.app.presentation.format.Money
import ru.denpom.finance.app.presentation.home.initialsOf
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import ru.denpom.finance.app.presentation.components.HideSystemBars

private const val ME = "Я"
private val personHues = listOf(155f, 245f, 345f, 65f, 300f, 20f, 180f, 120f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitSheet(receipt: Receipt, onDismiss: () -> Unit, onConfirm: (Map<String, Long>) -> Unit) {
    val c = App.colors
    val fmt = Money.fmt
    val people = remember { mutableStateListOf(ME) }
    var newName by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(if (receipt.items.isEmpty()) 0 else 1) }
    val assigned = remember { mutableStateMapOf<Int, Set<String>>() }

    val shares: Map<String, Long> = if (mode == 0 || receipt.items.isEmpty()) {
        val each = receipt.total / people.size
        val rest = receipt.total - each * people.size
        people.associateWith { if (it == ME) each + rest else each }
    } else {
        val totals = people.associateWith { 0L }.toMutableMap()
        receipt.items.forEachIndexed { i, item ->
            val who = assigned[i]?.filter { it in people }?.ifEmpty { null } ?: listOf(ME)
            val part = item.sum / who.size
            who.forEachIndexed { k, p -> totals[p] = (totals[p] ?: 0) + part + if (k == 0) item.sum - part * who.size else 0 }
        }
        totals
    }

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
            Text("Разделить чек", style = AppText.section.copy(fontSize = 16.sp), color = c.on)
            IconField(Icons.Rounded.PersonAdd, newName, { newName = it }, "Добавить участника", trailing = {
                if (newName.isNotBlank()) {
                    Text(
                        "Добавить",
                        style = AppText.label.copy(fontWeight = FontWeight.SemiBold),
                        color = c.pri,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                            val name = newName.trim()
                            if (name.isNotEmpty() && name !in people) people += name
                            newName = ""
                        }.padding(8.dp),
                    )
                }
            })
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                people.forEachIndexed { i, p ->
                    Row(
                        Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(c.catBg(personHues[i % personHues.size]))
                            .clickable(enabled = p != ME) { people.remove(p) }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (p == ME) "Я" else "$p  ×", style = AppText.label, color = c.catFg(personHues[i % personHues.size]))
                    }
                }
            }
            if (receipt.items.isNotEmpty()) Segmented(listOf("Поровну", "По позициям"), mode, { mode = it })
            if (mode == 1 && receipt.items.isNotEmpty()) {
                Text("Отметьте, кто что брал. Без отметки позиция ваша.", style = AppText.caption, color = c.on2)
                Column(Modifier.card(RoundedCornerShape(16.dp))) {
                    receipt.items.forEachIndexed { index, item ->
                        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row {
                                Text(item.name, style = AppText.caption, color = c.on, modifier = Modifier.weight(1f), maxLines = 1)
                                Text(fmt.format(item.sum), style = AppText.caption.copy(fontWeight = FontWeight.SemiBold), color = c.on)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                people.forEachIndexed { pi, p ->
                                    val on = p in (assigned[index] ?: emptySet())
                                    val hue = personHues[pi % personHues.size]
                                    Box(
                                        Modifier.size(32.dp).clip(CircleShape).background(if (on) c.catFg(hue) else c.catBg(hue)).clickable {
                                            val cur = assigned[index] ?: emptySet()
                                            assigned[index] = if (on) cur - p else cur + p
                                        },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (on) Icon(Icons.Rounded.Check, null, tint = c.catBg(hue), modifier = Modifier.size(16.dp))
                                        else Text(initialsOf(p), style = AppText.small, color = c.catFg(hue))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Column(Modifier.card(RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                people.forEach { p ->
                    Row {
                        Text(if (p == ME) "Моя часть" else p, style = AppText.body, color = c.on, modifier = Modifier.weight(1f))
                        Text(fmt.format(shares[p] ?: 0), style = AppText.amount, color = if (p == ME) c.on else c.inc)
                    }
                }
            }
            val others = shares.filterKeys { it != ME }
            PrimaryButton(
                if (others.isEmpty()) "Добавьте участника" else "Записать в «Мне должны»",
                Modifier.fillMaxWidth(),
                enabled = others.values.any { it > 0 },
            ) { onConfirm(others) }
        }
    }
}

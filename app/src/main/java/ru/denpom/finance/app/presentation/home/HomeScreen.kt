package ru.denpom.finance.app.presentation.home

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCard
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.denpom.finance.app.domain.model.Account
import ru.denpom.finance.app.domain.model.AccountType
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.domain.model.Recurring
import ru.denpom.finance.app.presentation.AddRequest
import ru.denpom.finance.app.presentation.LocalAppActions
import ru.denpom.finance.app.presentation.components.CategoryIcons
import ru.denpom.finance.app.presentation.components.InitialsAvatar
import ru.denpom.finance.app.presentation.components.OperationRow
import ru.denpom.finance.app.presentation.components.SectionHeader
import ru.denpom.finance.app.presentation.components.budgetColor
import ru.denpom.finance.app.presentation.components.card
import ru.denpom.finance.app.presentation.format.Dates
import ru.denpom.finance.app.presentation.format.Money
import ru.denpom.finance.app.presentation.operations.operationVisuals
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    onScan: () -> Unit,
    onAllOperations: () -> Unit,
    onBudgets: () -> Unit,
    onAccounts: () -> Unit,
    onSubscriptions: () -> Unit,
    onMore: () -> Unit,
    onOperation: (ru.denpom.finance.app.domain.model.Operation) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = App.colors
    val fmt = Money.fmt
    val actions = LocalAppActions.current
    if (!state.loaded) return

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Баланс · ${state.accounts.size} ${Dates.plural(state.accounts.size.toLong(), "счёт", "счёта", "счетов")}", style = AppText.caption, color = c.on2)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(fmt.format(state.total), style = AppText.amountM, color = c.on)
                        Box(Modifier.size(32.dp).clip(CircleShape).clickable { viewModel.toggleHide() }, contentAlignment = Alignment.Center) {
                            Icon(if (fmt.hidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, "Скрыть суммы", tint = c.on2, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(c.priC).clickable(onClick = onMore),
                    contentAlignment = Alignment.Center,
                ) {
                    val initials = state.userName.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
                    if (initials.isNotEmpty()) Text(initials, style = AppText.section, color = c.onPriC)
                    else Icon(Icons.Rounded.Person, null, tint = c.onPriC, modifier = Modifier.size(22.dp))
                }
            }
        }
        item { BudgetRing(state, onBudgets) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(c.pri).clickable(onClick = onScan),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Rounded.QrCodeScanner, null, tint = c.onPri, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Скан", style = AppText.label.copy(fontWeight = FontWeight.SemiBold), color = c.onPri)
                }
                QuickAction(Icons.Rounded.Remove, c.exp, "Расход") { actions.openAdd(AddRequest(OperationType.EXPENSE)) }
                QuickAction(Icons.Rounded.Add, c.inc, "Доход") { actions.openAdd(AddRequest(OperationType.INCOME)) }
                QuickAction(Icons.Rounded.SwapHoriz, c.on, "Перевод") { actions.openAdd(AddRequest(OperationType.TRANSFER)) }
            }
        }
        item {
            LazyRow(
                Modifier.padding(horizontal = 0.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.accounts, key = { it.id }) { AccountPill(it, onAccounts) }
                item { AddAccountPill(onAccounts) }
            }
        }
        if (state.upcoming.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Скоро спишется", style = AppText.section, color = c.on, modifier = Modifier.weight(1f))
                        if (state.upcomingTotal > 0 && !fmt.hidden) {
                            Text("${fmt.format(state.upcomingTotal, false)} до конца месяца", style = AppText.caption, color = c.on2)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.upcoming.forEach { UpcomingTile(it, state, Modifier.weight(1f), onSubscriptions) }
                        repeat(3 - state.upcoming.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        state.insight?.let { text ->
            item {
                Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(c.amb))
                    Text(text, style = AppText.body.copy(fontSize = 14.sp), color = c.on)
                }
            }
        }
        item {
            Column {
                SectionHeader("Последние операции", action = "Все", onAction = onAllOperations)
                if (state.recent.isEmpty()) {
                    Text(
                        "Здесь появятся ваши траты и доходы. Отсканируйте чек или добавьте операцию вручную.",
                        style = AppText.caption,
                        color = c.on2,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
                state.recent.forEach { op ->
                    val v = operationVisuals(op, state.categories, state.accountNames, fmt)
                    OperationRow(
                        title = v.title,
                        subtitle = v.subtitle,
                        amount = v.amount,
                        amountColor = v.color,
                        hue = v.hue,
                        icon = v.icon,
                        hasReceipt = op.receiptId != null,
                        modifier = Modifier.padding(horizontal = 0.dp),
                        onClick = { onOperation(op) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, tint: Color, label: String, onClick: () -> Unit) {
    val c = App.colors
    Box(Modifier.size(48.dp).clip(CircleShape).background(c.s2).clickable(onClickLabel = label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun BudgetRing(state: HomeState, onBudgets: () -> Unit) {
    val c = App.colors
    val fmt = Money.fmt
    val limit = state.limit
    val ratio = if (limit != null && limit > 0) state.spent.toFloat() / limit else 0f
    val arcColor = if (limit == null) c.pri else budgetColor(ratio)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(240.dp)) {
            Box(Modifier.matchParentSize().clip(CircleShape).clickable(onClick = onBudgets), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 16.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(c.s3, 135f, 270f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                    if (ratio > 0f) drawArc(arcColor, 135f, 270f * ratio.coerceAtMost(1f), false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                    val angle = Math.toRadians((135f + 270f * state.monthProgress).toDouble())
                    val r = size.width / 2 - stroke / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    drawCircle(c.on.copy(alpha = 0.4f), 3.dp.toPx(), center + Offset((r * cos(angle)).toFloat(), (r * sin(angle)).toFloat()))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (limit == null) {
                        Text("Потрачено в ${Dates.monthPrepositional(state.month)}", style = AppText.caption, color = c.on2, textAlign = TextAlign.Center)
                        Text(fmt.format(state.spent, false), style = AppText.ring, color = c.on)
                        Text("Лимит не задан", style = AppText.caption, color = c.pri)
                    } else if (state.left >= 0) {
                        Text("Осталось на ${Dates.monthName(state.month).lowercase()}", style = AppText.caption, color = c.on2)
                        Text(fmt.format(state.left, false), style = AppText.ring, color = c.on)
                        val perDay = state.left / state.daysLeft.coerceAtLeast(1)
                        Text("на ${Dates.days(state.daysLeft.toLong())} · ≈ ${fmt.format(perDay, false)} в день", style = AppText.caption, color = c.on2)
                    } else {
                        Text("Лимит превышен на", style = AppText.caption, color = c.on2)
                        Text(fmt.format(-state.left, false), style = AppText.ring, color = c.exp)
                        Text("до конца месяца ${Dates.days(state.daysLeft.toLong())}", style = AppText.caption, color = c.on2)
                    }
                }
            }
            if (limit != null) {
                Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 28.dp, end = 28.dp, bottom = 16.dp)) {
                    Text("0", style = AppText.small.copy(fontWeight = FontWeight.Normal), color = c.on2, modifier = Modifier.weight(1f))
                    Text(if (fmt.hidden) "•••" else fmt.number(limit), style = AppText.small.copy(fontWeight = FontWeight.Normal), color = c.on2)
                }
            }
        }
        val chipText = when {
            limit == null -> "Задайте лимит — покажем остаток на день"
            fmt.hidden -> "Прогноз на конец месяца скрыт"
            state.forecast > limit -> "Прогноз ${fmt.format(state.forecast, false)} · +${fmt.plain(state.forecast - limit, false)} к лимиту"
            else -> "Прогноз ${fmt.format(state.forecast, false)} · в пределах лимита"
        }
        val over = limit != null && !fmt.hidden && state.forecast > limit
        Row(
            Modifier.heightIn(min = 32.dp).clip(RoundedCornerShape(16.dp)).background(if (over) c.warnC else c.ambC).clickable(onClick = onBudgets).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.TrendingUp, null, tint = if (over) c.onWarnC else c.onAmbC, modifier = Modifier.size(16.dp))
            Text(chipText, style = AppText.caption.copy(fontWeight = FontWeight.Medium), color = if (over) c.onWarnC else c.onAmbC)
        }
    }
}

@Composable
private fun AccountPill(account: Account, onClick: () -> Unit) {
    val c = App.colors
    val fmt = Money.fmt
    Row(
        Modifier.padding(vertical = 2.dp).card(RoundedCornerShape(28.dp)).clickable(onClick = onClick).height(56.dp).padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(c.s2), contentAlignment = Alignment.Center) {
            Icon(CategoryIcons.account(account.type), null, tint = c.on2, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(account.name, style = AppText.small.copy(fontWeight = FontWeight.Normal), color = c.on2, maxLines = 1)
            Text(
                fmt.format(account.balance),
                style = AppText.amount,
                color = if (account.balance < 0 || account.type == AccountType.CREDIT && account.balance < 0) c.exp else c.on,
            )
        }
    }
}

@Composable
private fun AddAccountPill(onClick: () -> Unit) {
    val c = App.colors
    Row(
        Modifier.padding(vertical = 2.dp).height(56.dp).clip(RoundedCornerShape(28.dp)).background(c.s2).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.AddCard, null, tint = c.on2, modifier = Modifier.size(20.dp))
        Text("Счёт", style = AppText.label, color = c.on2)
    }
}

@Composable
private fun UpcomingTile(r: Recurring, state: HomeState, modifier: Modifier, onClick: () -> Unit) {
    val c = App.colors
    val fmt = Money.fmt
    val hue = r.categoryId?.let { state.categories[it]?.hue } ?: 200f
    Column(
        modifier.card(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            InitialsAvatar(initialsOf(r.title), hue, size = 32.dp)
            Spacer(Modifier.weight(1f))
            Text(Dates.shortDate(Period.dateOf(r.nextDate)), style = AppText.small.copy(fontWeight = FontWeight.SemiBold), color = c.on2)
        }
        Column {
            Text(r.title, style = AppText.small.copy(fontWeight = FontWeight.Normal), color = c.on2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(fmt.format(r.amount, r.amount % 100 != 0L), style = AppText.amount.copy(fontSize = 14.sp), color = c.on, maxLines = 1)
        }
    }
}

fun initialsOf(title: String): String {
    val word = title.trim().split(' ').firstOrNull().orEmpty()
    if (word.isEmpty()) return "•"
    return if (word.length > 1 && word[0].isLowerCase() && word[1].isUpperCase()) word.take(2) else word.take(1).uppercase()
}

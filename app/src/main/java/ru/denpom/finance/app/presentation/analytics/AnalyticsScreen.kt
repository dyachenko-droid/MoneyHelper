package ru.denpom.finance.app.presentation.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.denpom.finance.app.presentation.components.BackTopBar
import ru.denpom.finance.app.presentation.components.CategoryAvatar
import ru.denpom.finance.app.presentation.components.CategoryIcons
import ru.denpom.finance.app.presentation.components.EmptyState
import ru.denpom.finance.app.presentation.components.InitialsAvatar
import ru.denpom.finance.app.presentation.components.RoundIconButton
import ru.denpom.finance.app.presentation.components.SectionHeader
import ru.denpom.finance.app.presentation.components.Segmented
import ru.denpom.finance.app.presentation.components.card
import ru.denpom.finance.app.presentation.format.Dates
import ru.denpom.finance.app.presentation.format.Money
import ru.denpom.finance.app.presentation.home.initialsOf
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText

@Composable
fun AnalyticsScreen(onBack: () -> Unit, onMonthlyReport: () -> Unit, viewModel: AnalyticsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = App.colors
    val fmt = Money.fmt

    Column(Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding()) {
        BackTopBar("Аналитика", onBack) {
            TextButton(onMonthlyReport) { Text("Отчёт за месяц") }
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Segmented(listOf("Неделя", "Месяц", "Год"), state.range.ordinal, { viewModel.setRange(Range.entries[it]) }) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft) { viewModel.shift(-1) }
                    Text(state.title, style = AppText.section, color = c.on, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    RoundIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, tint = if (state.offset < 0) c.on else c.s3) { viewModel.shift(1) }
                }
            }
            item {
                Column(Modifier.card(RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Donut(state, Modifier.size(132.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Stat("Доходы", fmt.format(state.income, false), c.inc)
                            Stat("Расходы", fmt.format(state.expense, false), c.exp)
                            if (state.previousExpense > 0) {
                                val diff = ((state.expense - state.previousExpense) * 100 / state.previousExpense).toInt()
                                Text(
                                    if (diff >= 0) "+$diff% к прошлому периоду" else "$diff% к прошлому периоду",
                                    style = AppText.small.copy(fontWeight = FontWeight.Medium),
                                    color = if (diff > 0) c.exp else c.inc,
                                )
                            }
                        }
                    }
                    if (state.shares.isEmpty()) {
                        Text("За этот период расходов нет", style = AppText.caption, color = c.on2)
                    }
                    state.shares.take(8).forEach { share ->
                        val hue = share.category?.hue ?: 165f
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CategoryAvatar(hue, CategoryIcons.of(share.category?.icon), size = 32.dp, iconSize = 16.dp)
                            Text(share.category?.name ?: "Без категории", style = AppText.body, color = c.on, modifier = Modifier.weight(1f), maxLines = 1)
                            Text(
                                "${if (state.expense > 0) share.amount * 100 / state.expense else 0}%",
                                style = AppText.caption,
                                color = c.on2,
                            )
                            Text(fmt.format(share.amount, false), style = AppText.amount, color = c.on)
                        }
                    }
                }
            }
            item {
                Column(Modifier.card(RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Доходы и расходы", style = AppText.section, color = c.on)
                    val max = state.bars.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1) ?: 1
                    Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                        state.bars.forEach { bar ->
                            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)) {
                                Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(Modifier.width(10.dp).fillMaxHeight(bar.income.toFloat() / max).clip(RoundedCornerShape(4.dp)).background(c.inc))
                                    Box(Modifier.width(10.dp).fillMaxHeight(bar.expense.toFloat() / max).clip(RoundedCornerShape(4.dp)).background(c.exp))
                                }
                                Text(Dates.monthShort(bar.month), style = AppText.tiny, color = c.on2)
                            }
                        }
                    }
                    if (state.income > 0) {
                        Row {
                            Text("Сбережения за период: ", style = AppText.small.copy(fontWeight = FontWeight.Normal), color = c.on2)
                            Text("${state.savingsRate}%", style = AppText.small.copy(fontWeight = FontWeight.SemiBold), color = if (state.savingsRate >= 0) c.on else c.exp)
                        }
                    }
                }
            }
            if (state.shops.isNotEmpty()) {
                item { SectionHeader("Где тратите больше всего", Modifier.padding(top = 4.dp)) }
                item {
                    Column(Modifier.card(RoundedCornerShape(20.dp)).padding(vertical = 4.dp)) {
                        state.shops.forEach { shop ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                InitialsAvatar(initialsOf(shop.name), null)
                                Column(Modifier.weight(1f)) {
                                    Text(shop.name, style = AppText.bodyMedium, color = c.on, maxLines = 1)
                                    Text("${shop.count} ${Dates.plural(shop.count.toLong(), "покупка", "покупки", "покупок")}", style = AppText.caption, color = c.on2)
                                }
                                Text(fmt.format(shop.amount, false), style = AppText.amount, color = c.on)
                            }
                        }
                        Text(
                            "Средний чек ${fmt.format(state.averageCheck, false)}",
                            style = AppText.caption,
                            color = c.on2,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                }
            }
            item { SectionHeader("Моя инфляция", Modifier.padding(top = 4.dp)) }
            if (state.inflation.isEmpty()) {
                item {
                    Text(
                        "Сканируйте чеки — когда один и тот же товар встретится в разных чеках, покажем, как менялась его цена.",
                        style = AppText.caption,
                        color = c.on2,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            state.inflation.forEach { p ->
                item {
                    val color = if (p.change > 0) c.exp else c.inc
                    Column(Modifier.card(RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(p.name, style = AppText.caption.copy(fontWeight = FontWeight.Medium), color = c.on, maxLines = 1)
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(fmt.format(p.last), style = AppText.amountM.copy(fontSize = 18.sp), color = c.on)
                            Text((if (p.change > 0) "+" else "") + "${p.change}%", style = AppText.small.copy(fontWeight = FontWeight.SemiBold), color = color, modifier = Modifier.padding(bottom = 2.dp))
                            Text("было ${fmt.format(p.first)}", style = AppText.small.copy(fontWeight = FontWeight.Normal), color = c.on2, modifier = Modifier.padding(bottom = 2.dp))
                        }
                        Sparkline(p.points, color, Modifier.fillMaxWidth().height(48.dp))
                        Text("${Dates.dayMonth(p.from)} → ${Dates.dayMonth(p.to)}", style = AppText.tiny, color = c.on2)
                    }
                }
            }
            if (state.expense == 0L && state.income == 0L && state.shops.isEmpty()) {
                item { EmptyState(Icons.Rounded.Insights, "Пока нечего анализировать", "Добавьте несколько операций — здесь появятся графики и подсказки") }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Text(label, style = AppText.small.copy(fontWeight = FontWeight.Normal), color = App.colors.on2)
        }
        Text(value, style = AppText.amount.copy(fontSize = 17.sp), color = App.colors.on)
    }
}

@Composable
private fun Donut(state: AnalyticsState, modifier: Modifier) {
    val c = App.colors
    val fmt = Money.fmt
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2
            val arc = Size(size.width - stroke, size.height - stroke)
            if (state.expense <= 0) {
                drawArc(c.s3, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            } else {
                var start = -90f
                val top = state.shares.take(5)
                val rest = state.expense - top.sumOf { it.amount }
                (top.map { c.chart(it.category?.hue ?: 165f) to it.amount } + listOf(c.s3 to rest)).forEach { (color, amount) ->
                    if (amount <= 0) return@forEach
                    val sweep = 360f * amount / state.expense
                    drawArc(color, start, sweep, false, Offset(inset, inset), arc, style = Stroke(stroke))
                    start += sweep
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Расходы", style = AppText.tiny, color = c.on2)
            Text(fmt.format(state.expense, false), style = AppText.amount.copy(fontSize = 15.sp), color = c.on, maxLines = 1)
        }
    }
}

@Composable
private fun Sparkline(points: List<Long>, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val min = points.min().toFloat()
        val max = points.max().toFloat()
        val span = (max - min).takeIf { it > 0f } ?: 1f
        val pad = 6.dp.toPx()
        val path = Path()
        points.forEachIndexed { i, v ->
            val x = size.width * i / (points.size - 1)
            val y = pad + (size.height - 2 * pad) * (1 - (v - min) / span)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val lastY = pad + (size.height - 2 * pad) * (1 - (points.last() - min) / span)
        drawCircle(color, 4.dp.toPx(), Offset(size.width, lastY))
    }
}

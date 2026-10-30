package ru.denpom.finance.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import ru.denpom.finance.app.MainActivity
import ru.denpom.finance.app.R
import ru.denpom.finance.app.presentation.format.Dates

private object WColors {
    val bg = ColorProvider(Color(0xFFFCFEFF), Color(0xFF12181E))
    val on = ColorProvider(Color(0xFF131B26), Color(0xFFE5EAF0))
    val on2 = ColorProvider(Color(0xFF535F6E), Color(0xFFA3ACB8))
    val pri = ColorProvider(Color(0xFF1364CE), Color(0xFF80B3FD))
    val onPri = ColorProvider(Color(0xFFF8FAFD), Color(0xFF031D44))
    val s2 = ColorProvider(Color(0xFFE6EEF8), Color(0xFF1A212A))
    val exp = ColorProvider(Color(0xFFBB584A), Color(0xFFEE9685))
    val inc = ColorProvider(Color(0xFF198044), Color(0xFF78D694))
    val warn = ColorProvider(Color(0xFFE29936), Color(0xFFEEB154))
}

const val ACTION_SCAN = "ru.denpom.finance.app.SCAN"
const val ACTION_ADD = "ru.denpom.finance.app.ADD"

private fun launch(context: Context, action: String?) =
    actionStartActivity(Intent(context, MainActivity::class.java).setAction(action ?: Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))

class BalanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetSnapshot(context)
        provideContent { GlanceTheme { BalanceContent(context, data) } }
    }
}

class BudgetWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetSnapshot(context)
        provideContent { GlanceTheme { BudgetContent(context, data) } }
    }
}

class BalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BalanceWidget()
}

class BudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetWidget()
}

@Composable
private fun ScanButton(context: Context, size: Int = 44) {
    Box(
        GlanceModifier.size(size.dp).cornerRadius(16.dp).background(WColors.pri).clickable(launch(context, ACTION_SCAN)),
        contentAlignment = Alignment.Center,
    ) {
        Image(ImageProvider(R.drawable.ic_widget_scan), "Сканировать", GlanceModifier.size(22.dp), colorFilter = androidx.glance.ColorFilter.tint(WColors.onPri))
    }
}

@Composable
private fun BalanceContent(context: Context, data: WidgetSnapshot) {
    Row(
        GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(WColors.bg).padding(horizontal = 16.dp, vertical = 12.dp).clickable(launch(context, null)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(GlanceModifier.defaultWeight()) {
            Text("Баланс", style = TextStyle(color = WColors.on2, fontSize = 12.sp))
            Text(data.fmt.format(data.total, false), style = TextStyle(color = WColors.on, fontSize = 20.sp, fontWeight = FontWeight.Bold), maxLines = 1)
        }
        ScanButton(context)
    }
}

@Composable
private fun BudgetContent(context: Context, data: WidgetSnapshot) {
    val fmt = data.fmt
    Column(GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(WColors.bg).padding(16.dp).clickable(launch(context, null))) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight()) {
                val limit = data.limit
                if (limit != null) {
                    val left = limit - data.spent
                    Text("Осталось на ${Dates.monthName(data.month).lowercase()}", style = TextStyle(color = WColors.on2, fontSize = 12.sp))
                    Text(fmt.format(left, false), style = TextStyle(color = if (left < 0) WColors.exp else WColors.on, fontSize = 24.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                    if (left > 0 && !fmt.hidden) Text("≈ ${fmt.format(left / data.daysLeft.coerceAtLeast(1), false)} в день", style = TextStyle(color = WColors.on2, fontSize = 12.sp))
                } else {
                    Text("Потрачено за ${Dates.monthName(data.month).lowercase()}", style = TextStyle(color = WColors.on2, fontSize = 12.sp))
                    Text(fmt.format(data.spent, false), style = TextStyle(color = WColors.on, fontSize = 24.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                }
            }
            Box(
                GlanceModifier.size(44.dp).cornerRadius(16.dp).background(WColors.s2).clickable(launch(context, ACTION_ADD)),
                contentAlignment = Alignment.Center,
            ) { Text("+", style = TextStyle(color = WColors.on, fontSize = 22.sp, fontWeight = FontWeight.Bold)) }
            Spacer(GlanceModifier.width(8.dp))
            ScanButton(context)
        }
        val limit = data.limit
        if (limit != null && limit > 0) {
            Spacer(GlanceModifier.height(10.dp))
            val ratio = (data.spent.toFloat() / limit).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = ratio,
                modifier = GlanceModifier.fillMaxWidth().height(8.dp).cornerRadius(4.dp),
                color = if (ratio >= 1f) WColors.exp else if (ratio >= 0.8f) WColors.warn else WColors.pri,
                backgroundColor = WColors.s2,
            )
        }
        Spacer(GlanceModifier.height(10.dp))
        if (data.locked) {
            Text("Операции скрыты — включён PIN", style = TextStyle(color = WColors.on2, fontSize = 12.sp))
        } else if (data.recent.isEmpty()) {
            Text("Пока нет операций", style = TextStyle(color = WColors.on2, fontSize = 12.sp))
        }
        if (!data.locked) data.recent.forEach { op ->
            Row(GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(op.title, style = TextStyle(color = WColors.on, fontSize = 13.sp), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                Text(op.amount, style = TextStyle(color = if (op.income) WColors.inc else WColors.exp, fontSize = 13.sp, fontWeight = FontWeight.Medium))
            }
        }
    }
}

package ru.denpom.finance.app.presentation.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import ru.denpom.finance.app.presentation.components.CategoryAvatar
import ru.denpom.finance.app.presentation.components.CategoryIcons
import ru.denpom.finance.app.presentation.components.ProgressLine
import ru.denpom.finance.app.presentation.components.card
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText

@Composable
private fun floatAnim(duration: Int = 2400): Float {
    val t = rememberInfiniteTransition(label = "float")
    val v by t.animateFloat(0f, 1f, infiniteRepeatable(tween(duration, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "v")
    return v
}

@Composable
fun ReceiptIllustration() {
    val c = App.colors
    val f = floatAnim()
    BoxWithConstraints(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).background(c.s2)) {
        Column(
            Modifier.align(Alignment.Center).offset(x = (-48).dp, y = (f * 6).dp).rotate(-4f).width(140.dp).card(RoundedCornerShape(14.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.width(70.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.on.copy(alpha = 0.7f)))
            listOf(150f, 150f, 205f, 345f, 150f).forEach { hue ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(c.chart(hue)))
                    Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.s3))
                    Box(Modifier.width(22.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.s3))
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.s3))
            Row {
                Text("ИТОГ", style = AppText.tiny, color = c.on2, modifier = Modifier.weight(1f))
                Text("1 426,35", style = AppText.tiny, color = c.on)
            }
        }
        val items = listOf(
            Triple("shopping_basket", 150f, Pair(84, -70)),
            Triple("chair", 205f, Pair(96, 0)),
            Triple("spa", 345f, Pair(80, 72)),
        )
        items.forEachIndexed { i, (icon, hue, pos) ->
            CategoryAvatar(
                hue,
                CategoryIcons.of(icon),
                size = 56.dp,
                iconSize = 26.dp,
                modifier = Modifier.align(Alignment.Center).offset(x = pos.first.dp, y = (pos.second + (if (i % 2 == 0) f else 1 - f) * 8).dp),
            )
        }
    }
}

@Composable
fun BudgetIllustration() {
    val c = App.colors
    val f = floatAnim(3000)
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).background(c.s2), contentAlignment = Alignment.Center) {
        Column(Modifier.width(220.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(Triple("local_taxi", 245f, 0.55f + f * 0.1f), Triple("shopping_basket", 150f, 0.84f), Triple("local_cafe", 65f, 1f)).forEach { (icon, hue, ratio) ->
                Row(Modifier.card(RoundedCornerShape(16.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CategoryAvatar(hue, CategoryIcons.of(icon), size = 32.dp, iconSize = 16.dp)
                    ProgressLine(ratio, if (ratio >= 1f) c.exp else if (ratio >= 0.8f) c.warn else c.pri, Modifier.weight(1f))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("24", "27", "30").forEachIndexed { i, d ->
                    Column(
                        Modifier.weight(1f).card(RoundedCornerShape(12.dp)).padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(d, style = AppText.section, color = if (i == 0) c.pri else c.on)
                        Box(Modifier.padding(top = 4.dp).width(24.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (i == 0) c.amb else c.s3))
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyIllustration() {
    val c = App.colors
    val f = floatAnim(2600)
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).background(c.s2), contentAlignment = Alignment.Center) {
        Box(Modifier.width(130.dp).height(230.dp).clip(RoundedCornerShape(26.dp)).background(c.hero).padding(8.dp)) {
            Column(
                Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(c.s1).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Spacer(Modifier.height(18.dp))
                Box(Modifier.size(56.dp).clip(CircleShape).background(c.priC), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Lock, null, tint = c.onPriC, modifier = Modifier.size(28.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    repeat(4) { i -> Box(Modifier.size(10.dp).clip(CircleShape).background(if (i < 3) c.pri else c.s3)) }
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.Fingerprint, null, tint = c.pri.copy(alpha = 0.6f + f * 0.4f), modifier = Modifier.size(34.dp))
            }
        }
        listOf(Pair(-104, -60), Pair(100, -34), Pair(-96, 62), Pair(104, 80)).forEachIndexed { i, (x, y) ->
            Box(
                Modifier.offset(x = x.dp, y = (y + (if (i % 2 == 0) f else 1 - f) * 6).dp).size(if (i % 2 == 0) 14.dp else 10.dp).clip(CircleShape)
                    .background(if (i % 2 == 0) c.amb.copy(alpha = 0.7f) else c.pri.copy(alpha = 0.5f))
            )
        }
    }
}


package ru.denpom.finance.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DonutSmall
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.denpom.finance.app.presentation.navigation.Routes
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText

@Composable
fun BottomNav(current: String?, onSelect: (String) -> Unit) {
    val c = App.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.s1)
            .drawBehind { drawLine(c.s2, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 4.dp)) {
            NavItem(Icons.Rounded.Home, "Главная", current == Routes.HOME) { onSelect(Routes.HOME) }
            NavItem(Icons.Rounded.ReceiptLong, "Операции", current == Routes.OPS) { onSelect(Routes.OPS) }
            Column(
                Modifier.weight(1f).fillMaxHeight().noRippleClick { onSelect(Routes.SCAN) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                Box(
                    Modifier
                        .offset(y = (-3).dp)
                        .width(56.dp)
                        .height(44.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x4D104A97), spotColor = Color(0x4D104A97))
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.pri),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.QrCodeScanner, null, tint = c.onPri, modifier = Modifier.size(26.dp)) }
                Text("Сканер", style = AppText.small, color = c.on)
            }
            NavItem(Icons.Rounded.DonutSmall, "Бюджеты", current == Routes.BUDGETS) { onSelect(Routes.BUDGETS) }
            NavItem(Icons.Rounded.Menu, "Ещё", current == Routes.MORE) { onSelect(Routes.MORE) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val c = App.colors
    Column(
        Modifier.weight(1f).fillMaxHeight().noRippleClick(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Box(
            Modifier.width(56.dp).height(32.dp).clip(RoundedCornerShape(16.dp)).background(if (active) c.priC else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (active) c.onPriC else c.on2, modifier = Modifier.size(22.dp)) }
        Text(label, style = AppText.small.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium), color = if (active) c.on else c.on2)
    }
}

@Composable
fun Modifier.noRippleClick(onClick: () -> Unit): Modifier =
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)

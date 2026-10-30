package ru.denpom.finance.app.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.denpom.finance.app.domain.model.Category
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText

@Composable
fun CategoryAvatar(hue: Float, icon: ImageVector, size: Dp = 40.dp, iconSize: Dp = 20.dp, modifier: Modifier = Modifier) {
    val c = App.colors
    Box(
        modifier.size(size).clip(CircleShape).background(c.catBg(hue)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = c.catFg(hue), modifier = Modifier.size(iconSize))
    }
}

@Composable
fun InitialsAvatar(text: String, hue: Float?, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val c = App.colors
    Box(
        modifier.size(size).clip(CircleShape).background(if (hue == null) c.s2 else c.catBg(hue)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = AppText.small.copy(fontWeight = FontWeight.Bold, fontSize = if (text.length > 1) 13.sp else 15.sp),
            color = if (hue == null) c.on2 else c.catFg(hue),
        )
    }
}

@Composable
fun OperationRow(
    title: String,
    subtitle: String,
    amount: String,
    amountColor: Color,
    hue: Float,
    icon: ImageVector,
    hasReceipt: Boolean,
    modifier: Modifier = Modifier,
    minHeight: Dp = 60.dp,
    onClick: (() -> Unit)? = null,
) {
    val c = App.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryAvatar(hue, icon)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = AppText.bodyMedium, color = c.on, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (hasReceipt) Icon(Icons.Rounded.ReceiptLong, null, tint = c.on2, modifier = Modifier.size(16.dp))
            }
            if (subtitle.isNotEmpty()) Text(subtitle, style = AppText.caption, color = c.on2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(amount, style = AppText.amount, color = amountColor, maxLines = 1)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    val c = App.colors
    Row(modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = AppText.section, color = c.on, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                style = AppText.label.copy(fontWeight = FontWeight.SemiBold),
                color = c.pri,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onAction?.invoke() }.padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
fun FilterChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leading: ImageVector? = null,
    trailing: ImageVector? = Icons.Rounded.ExpandMore,
    onClick: () -> Unit,
) {
    val c = App.colors
    Row(
        modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) c.priC else c.s2)
            .clickable(onClick = onClick)
            .padding(start = if (leading != null) 12.dp else 14.dp, end = if (trailing != null) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val fg = if (selected) c.onPriC else c.on
        if (leading != null) Icon(leading, null, tint = fg, modifier = Modifier.size(18.dp))
        Text(label, style = AppText.caption.copy(fontWeight = FontWeight.Medium), color = fg, maxLines = 1)
        if (trailing != null) Icon(trailing, null, tint = fg, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun CategoryChip(category: Category?, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val c = App.colors
    val hue = category?.hue ?: 165f
    Row(
        modifier
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(c.catBg(hue))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 8.dp, end = if (onClick != null) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(CategoryIcons.of(category?.icon), null, tint = c.catFg(hue), modifier = Modifier.size(15.dp))
        Text(category?.name ?: "Без категории", style = AppText.small, color = c.catFg(hue))
        if (onClick != null) Icon(Icons.Rounded.ExpandMore, null, tint = c.catFg(hue), modifier = Modifier.size(16.dp))
    }
}

@Composable
fun Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = App.colors
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.s2).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val active = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) c.s1 else Color.Transparent)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = AppText.label, color = if (active) c.on else c.on2, maxLines = 1)
            }
        }
    }
}

@Composable
fun ProgressLine(ratio: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 8.dp, track: Color = App.colors.s2, marker: Float? = null) {
    val animated by animateFloatAsState(ratio.coerceIn(0f, 1f), label = "progress")
    val c = App.colors
    Box(modifier.fillMaxWidth().height(height)) {
        Box(Modifier.fillMaxWidth().fillMaxHeight().clip(RoundedCornerShape(height / 2)).background(track)) {
            Box(Modifier.fillMaxWidth(animated).fillMaxHeight().clip(RoundedCornerShape(height / 2)).background(color))
        }
        if (marker != null) {
            Box(Modifier.fillMaxWidth(marker.coerceIn(0f, 1f)).fillMaxHeight(), contentAlignment = Alignment.CenterEnd) {
                Box(Modifier.width(2.dp).height(height + 6.dp).clip(RoundedCornerShape(1.dp)).background(c.on.copy(alpha = 0.35f)))
            }
        }
    }
}

@Composable
fun budgetColor(ratio: Float): Color {
    val c = App.colors
    return when {
        ratio >= 1f -> c.exp
        ratio >= 0.8f -> c.warn
        else -> c.pri
    }
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    leading: ImageVector? = null,
    container: Color = App.colors.pri,
    content: Color = App.colors.onPri,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(if (enabled) container else container.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leading != null) {
            Icon(leading, null, tint = content, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = AppText.section.copy(fontSize = if (height >= 52.dp) 16.sp else 15.sp), color = content)
    }
}

@Composable
fun TonalButton(text: String, modifier: Modifier = Modifier, leading: ImageVector? = null, onClick: () -> Unit) {
    val c = App.colors
    Row(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(c.s2)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leading != null) Icon(leading, null, tint = c.on, modifier = Modifier.size(18.dp))
        Text(text, style = AppText.label, color = c.on)
    }
}

@Composable
fun TextAction(text: String, modifier: Modifier = Modifier, color: Color = App.colors.pri, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AppText.section, color = color, textAlign = TextAlign.Center)
    }
}

@Composable
fun FilledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    textStyle: TextStyle = AppText.bodyMedium.copy(fontSize = 16.sp, fontFeatureSettings = "tnum"),
) {
    val c = App.colors
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.s2).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = AppText.small.copy(fontWeight = FontWeight.Normal), color = c.on2)
        Box {
            if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, style = textStyle, color = c.on2)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = textStyle.copy(color = c.on),
                cursorBrush = SolidColor(c.pri),
                keyboardOptions = keyboard,
                singleLine = singleLine,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            )
        }
    }
}

@Composable
fun IconField(
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    radius: Dp = 14.dp,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val c = App.colors
    Row(
        modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(radius)).background(c.s2).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = c.on2, modifier = Modifier.size(22.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = AppText.body, color = c.on2, maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = AppText.body.copy(color = c.on),
                cursorBrush = SolidColor(c.pri),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        trailing?.invoke(this)
    }
}

@Composable
fun SheetHandle(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.width(32.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(App.colors.s3))
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    val c = App.colors
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(c.s2), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.on2, modifier = Modifier.size(32.dp))
        }
        Text(title, style = AppText.section.copy(fontSize = 16.sp), color = c.on, textAlign = TextAlign.Center)
        Text(body, style = AppText.body.copy(fontSize = 14.sp), color = c.on2, textAlign = TextAlign.Center)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(4.dp))
            PrimaryButton(action, height = 48.dp, onClick = onAction)
        }
    }
}

@Composable
fun SelectRing(selected: Boolean, size: Dp, content: @Composable () -> Unit) {
    val c = App.colors
    Box(
        Modifier
            .size(size + 8.dp)
            .then(if (selected) Modifier.border(2.dp, c.pri, CircleShape) else Modifier)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (RowScope.() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = AppText.title, color = App.colors.on, modifier = Modifier.weight(1f))
        trailing?.invoke(this)
    }
}

val ScreenPadding = PaddingValues(horizontal = 16.dp)

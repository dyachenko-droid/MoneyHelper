package ru.denpom.finance.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.denpom.finance.app.presentation.theme.App

@Composable
fun Modifier.card(shape: Shape = RoundedCornerShape(24.dp), color: Color? = null): Modifier {
    val c = App.colors
    val shadowed = if (c.isDark) this else this.shadow(
        elevation = 6.dp,
        shape = shape,
        ambientColor = Color(0x1A1E3A64),
        spotColor = Color(0x261E3A64),
    )
    return shadowed.clip(shape).background(color ?: c.s1)
}

@Composable
fun CardColumn(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.card(RoundedCornerShape(radius)).padding(padding), content = content)
}

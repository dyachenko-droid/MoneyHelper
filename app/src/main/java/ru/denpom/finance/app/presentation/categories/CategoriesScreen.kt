package ru.denpom.finance.app.presentation.categories

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.denpom.finance.app.domain.model.Category
import ru.denpom.finance.app.domain.model.CategoryKind
import ru.denpom.finance.app.presentation.LocalAppActions
import ru.denpom.finance.app.presentation.components.BackTopBar
import ru.denpom.finance.app.presentation.components.CategoryAvatar
import ru.denpom.finance.app.presentation.components.CategoryChip
import ru.denpom.finance.app.presentation.components.CategoryIcons
import ru.denpom.finance.app.presentation.components.FilledField
import ru.denpom.finance.app.presentation.components.PrimaryButton
import ru.denpom.finance.app.presentation.components.SectionHeader
import ru.denpom.finance.app.presentation.components.Segmented
import ru.denpom.finance.app.presentation.components.SelectRing
import ru.denpom.finance.app.presentation.components.SheetHandle
import ru.denpom.finance.app.presentation.components.TextAction
import ru.denpom.finance.app.presentation.components.card
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import ru.denpom.finance.app.presentation.components.HideSystemBars

private val hues = listOf(150f, 120f, 85f, 65f, 40f, 20f, 345f, 330f, 300f, 275f, 245f, 225f, 205f, 180f)

private data class CatEdit(val category: Category?, val kind: CategoryKind)

@Composable
fun CategoriesScreen(onBack: () -> Unit, viewModel: CategoriesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = App.colors
    val actions = LocalAppActions.current
    var tab by remember { mutableStateOf(0) }
    var edit by remember { mutableStateOf<CatEdit?>(null) }
    var confirm by remember { mutableStateOf<Category?>(null) }
    val kind = if (tab == 0) CategoryKind.EXPENSE else CategoryKind.INCOME
    val catMap = state.categories.associateBy { it.id }

    Column(Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding()) {
        BackTopBar("Категории и правила", onBack)
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Segmented(listOf("Расходы", "Доходы"), tab, { tab = it }) }
            item {
                Column(Modifier.card(RoundedCornerShape(20.dp)).padding(vertical = 4.dp)) {
                    state.categories.filter { it.kind == kind }.forEach { cat ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { edit = CatEdit(cat, cat.kind) }.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CategoryAvatar(cat.hue, CategoryIcons.of(cat.icon))
                            Text(cat.name, style = AppText.bodyMedium, color = c.on, modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.Edit, null, tint = c.on2, modifier = Modifier.size(18.dp))
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { edit = CatEdit(null, kind) }.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(c.s2), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Add, null, tint = c.pri, modifier = Modifier.size(22.dp))
                        }
                        Text("Новая категория", style = AppText.bodyMedium, color = c.pri)
                    }
                }
            }
            if (tab == 0) {
                item { SectionHeader("Правила автокатегорий", Modifier.padding(top = 8.dp)) }
                item {
                    if (state.rules.isEmpty()) {
                        Text(
                            "Когда вы меняете категорию позиции в чеке, приложение запоминает правило и в следующий раз разложит такой товар само.",
                            style = AppText.caption,
                            color = c.on2,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    } else {
                        Column(Modifier.card(RoundedCornerShape(20.dp)).padding(vertical = 4.dp)) {
                            state.rules.forEach { rule ->
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 16.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text("«${rule.pattern}»", style = AppText.body, color = c.on, modifier = Modifier.weight(1f), maxLines = 1)
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = c.on2, modifier = Modifier.size(16.dp))
                                    CategoryChip(catMap[rule.categoryId])
                                    Box(Modifier.size(40.dp).clip(CircleShape).clickable { viewModel.deleteRule(rule.pattern) }, contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.Close, "Удалить правило", tint = c.on2, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    edit?.let { e ->
        CategorySheet(
            category = e.category,
            onDismiss = { edit = null },
            onSave = { name, icon, hue ->
                viewModel.save(e.category?.id ?: 0, name, icon, hue, e.kind)
                edit = null
            },
            onDelete = e.category?.let { cat -> { confirm = cat; edit = null } },
        )
    }
    confirm?.let { cat ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = c.s1,
            title = {
                HideSystemBars()
                Text("Удалить «${cat.name}»?")
            },
            text = { Text("Операции останутся, но будут без категории. Правила для неё удалятся.") },
            confirmButton = { TextButton({ viewModel.delete(cat.id); actions.snack("Категория удалена"); confirm = null }) { Text("Удалить", color = c.exp) } },
            dismissButton = { TextButton({ confirm = null }) { Text("Отмена") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySheet(category: Category?, onDismiss: () -> Unit, onSave: (String, String, Float) -> Unit, onDelete: (() -> Unit)?) {
    val c = App.colors
    var name by remember { mutableStateOf(category?.name.orEmpty()) }
    var icon by remember { mutableStateOf(category?.icon ?: "shopping_bag") }
    var hue by remember { mutableStateOf(category?.hue ?: 245f) }
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryAvatar(hue, CategoryIcons.of(icon), size = 48.dp, iconSize = 24.dp)
                Text(if (category == null) "Новая категория" else "Категория", style = AppText.section.copy(fontSize = 16.sp), color = c.on)
            }
            FilledField("Название", name, { name = it })
            Text("Цвет", style = AppText.caption, color = c.on2)
            hues.chunked(7).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { h ->
                        SelectRing(h == hue, 32.dp) {
                            Box(Modifier.size(32.dp).clip(CircleShape).background(c.chart(h)).clickable { hue = h })
                        }
                    }
                }
            }
            Text("Иконка", style = AppText.caption, color = c.on2)
            CategoryIcons.all.keys.toList().chunked(6).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { key ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            SelectRing(key == icon, 40.dp) {
                                Box(Modifier.clip(CircleShape).clickable { icon = key }) { CategoryAvatar(hue, CategoryIcons.of(key)) }
                            }
                        }
                    }
                    repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) TextAction("Удалить", color = c.exp, onClick = onDelete)
                Spacer(Modifier.weight(1f))
                PrimaryButton("Сохранить", height = 48.dp, enabled = name.isNotBlank()) { onSave(name, icon, hue) }
            }
        }
    }
}

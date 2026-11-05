package ru.denpom.finance.app.presentation.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.CurrencyRuble
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.denpom.finance.app.domain.model.Currency
import ru.denpom.finance.app.domain.model.ThemeMode
import ru.denpom.finance.app.presentation.components.ScreenTitle
import ru.denpom.finance.app.presentation.components.Segmented
import ru.denpom.finance.app.presentation.components.card
import ru.denpom.finance.app.presentation.format.Dates
import ru.denpom.finance.app.presentation.navigation.Routes
import ru.denpom.finance.app.presentation.receipt.TextDialog
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import ru.denpom.finance.app.presentation.components.HideSystemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun MoreScreen(onNavigate: (String) -> Unit, viewModel: MoreViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = App.colors
    var currencyDialog by remember { mutableStateOf(false) }
    var nameDialog by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenTitle("Ещё") }
        item {
            Column(Modifier.card().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Тема", style = AppText.bodyMedium, color = c.on)
                Segmented(listOf("Светлая", "Тёмная", "Системная"), state.settings.theme.ordinal, { viewModel.setTheme(ThemeMode.entries[it]) })
            }
        }
        item {
            Column(Modifier.card().padding(vertical = 8.dp)) {
                SettingRow(Icons.Rounded.Insights, "Аналитика", "") { onNavigate(Routes.ANALYTICS) }
                SettingRow(Icons.Rounded.AccountBalance, "Счета", state.accounts.toString()) { onNavigate(Routes.ACCOUNTS) }
                SettingRow(Icons.Rounded.Autorenew, "Подписки и платежи", state.recurring.takeIf { it > 0 }?.toString().orEmpty()) { onNavigate(Routes.SUBSCRIPTIONS) }
                SettingRow(Icons.Rounded.Savings, "Цели-копилки", "") { onNavigate(Routes.GOALS) }
                SettingRow(Icons.Rounded.Handshake, "Долги и кредиты", "") { onNavigate(Routes.DEBTS) }
                SettingRow(
                    Icons.Rounded.Category,
                    "Категории и правила",
                    if (state.rules > 0) "${state.rules} ${Dates.plural(state.rules.toLong(), "правило", "правила", "правил")}" else "",
                ) { onNavigate(Routes.CATEGORIES) }
            }
        }
        item {
            Column(Modifier.card().padding(vertical = 8.dp)) {
                SettingRow(Icons.Rounded.CurrencyRuble, "Валюта", state.settings.currency.symbol) { currencyDialog = true }
                SettingRow(Icons.Rounded.Person, "Как к вам обращаться", state.settings.userName) { nameDialog = true }
                SettingRow(Icons.Rounded.Fingerprint, "PIN и биометрия", if (state.settings.pinEnabled) "Вкл." else "Выкл.") { onNavigate(Routes.SECURITY) }
                SettingRow(
                    Icons.Rounded.Notifications,
                    "Напоминания",
                    if (state.settings.dailyReminder || state.settings.paymentReminders) "%02d:%02d".format(state.settings.reminderMinutes / 60, state.settings.reminderMinutes % 60) else "Выкл.",
                ) { onNavigate(Routes.REMINDERS) }
                SettingRow(Icons.Rounded.UploadFile, "Импорт выписки", "CSV") { onNavigate(Routes.IMPORT) }
                SettingRow(Icons.Rounded.CloudUpload, "Бэкап и экспорт", "CSV, копия") { onNavigate(Routes.BACKUP) }
            }
        }
        item { Text("Денежный помощник ${ru.denpom.finance.app.BuildConfig.VERSION_NAME}", style = AppText.caption, color = c.on2, modifier = Modifier.padding(horizontal = 4.dp)) }
    }

    if (currencyDialog) {
        AlertDialog(
            onDismissRequest = { currencyDialog = false },
            containerColor = c.s1,
            title = {
                HideSystemBars()
                Text("Валюта")
            },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    Currency.entries.forEach { cur ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { viewModel.setCurrency(cur); currencyDialog = false },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${cur.symbol}  ${cur.title}", style = AppText.body, color = if (cur == state.settings.currency) c.pri else c.on)
                        }
                    }
                }
            },
            confirmButton = { TextButton({ currencyDialog = false }) { Text("Закрыть") } },
        )
    }
    if (nameDialog) {
        TextDialog("Как к вам обращаться", state.settings.userName, { nameDialog = false }, label = "Имя") {
            viewModel.setName(it)
            nameDialog = false
        }
    }
}

@Composable
fun SettingRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    val c = App.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, tint = c.on2, modifier = Modifier.size(22.dp))
        Text(label, style = AppText.body, color = c.on, modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) Text(value, style = AppText.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = c.on2, maxLines = 1)
        Icon(Icons.Rounded.ChevronRight, null, tint = c.on2, modifier = Modifier.size(20.dp))
    }
}


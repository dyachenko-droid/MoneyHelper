package ru.denpom.finance.app.presentation.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.denpom.finance.app.domain.model.Account
import ru.denpom.finance.app.domain.model.AccountType
import ru.denpom.finance.app.presentation.LocalAppActions
import ru.denpom.finance.app.presentation.components.BackTopBar
import ru.denpom.finance.app.presentation.components.CategoryIcons
import ru.denpom.finance.app.presentation.components.FilledField
import ru.denpom.finance.app.presentation.components.PrimaryButton
import ru.denpom.finance.app.presentation.components.ProgressLine
import ru.denpom.finance.app.presentation.components.SheetHandle
import ru.denpom.finance.app.presentation.components.TextAction
import ru.denpom.finance.app.presentation.format.Money
import ru.denpom.finance.app.presentation.format.amountInput
import ru.denpom.finance.app.presentation.format.parseAmount
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import ru.denpom.finance.app.presentation.components.HideSystemBars

private data class AccountEdit(val account: Account?)

@Composable
fun AccountsScreen(onBack: () -> Unit, onReconcile: () -> Unit, viewModel: AccountsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = App.colors
    val fmt = Money.fmt
    val actions = LocalAppActions.current
    var edit by remember { mutableStateOf<AccountEdit?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding()) {
        BackTopBar("Счета", onBack) {
            TextButton(onReconcile) { Text("Сверить") }
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.padding(bottom = 4.dp, start = 4.dp)) {
                    Text("Всего на счетах", style = AppText.caption, color = c.on2)
                    Text(fmt.format(state.total), style = AppText.amountL, color = c.on)
                    if (state.debt < 0) Text("Из них долг по кредиткам ${fmt.format(-state.debt)}", style = AppText.caption, color = c.exp)
                }
            }
            items(state.accounts, key = { it.id }) { account -> AccountCard(account) { edit = AccountEdit(account) } }
        }
        PrimaryButton("Добавить счёт", Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), leading = Icons.Rounded.Add) {
            edit = AccountEdit(null)
        }
    }

    edit?.let { e ->
        AccountSheet(
            account = e.account,
            canDelete = e.account != null && e.account.id !in state.used,
            onDismiss = { edit = null },
            onSave = { name, type, sub, bal, limit ->
                viewModel.save(e.account?.id ?: 0, name, type, sub, bal, limit)
                actions.snack(if (e.account == null) "Счёт «$name» создан" else "Счёт сохранён")
                edit = null
            },
            onDelete = {
                e.account?.let { viewModel.delete(it.id) }
                actions.snack("Счёт удалён")
                edit = null
            },
        )
    }
}

@Composable
private fun AccountCard(account: Account, onClick: () -> Unit) {
    val c = App.colors
    val fmt = Money.fmt
    val (bg, fg, sub) = when (account.type) {
        AccountType.CARD -> Triple(c.hero, c.onHero, c.onHero.copy(alpha = 0.75f))
        AccountType.CASH -> Triple(c.s2, c.on, c.on2)
        AccountType.DEPOSIT -> Triple(c.ambC, c.onAmbC, c.onAmbC.copy(alpha = 0.8f))
        AccountType.CREDIT -> Triple(c.s3, c.on, c.on2)
    }
    val typeName = when (account.type) {
        AccountType.CARD -> "Карта"
        AccountType.CASH -> "Наличные"
        AccountType.DEPOSIT -> "Вклад"
        AccountType.CREDIT -> "Кредитная карта"
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(bg).clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(account.name, style = AppText.caption, color = sub, modifier = Modifier.weight(1f))
            Icon(CategoryIcons.account(account.type), null, tint = sub, modifier = Modifier.size(20.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(fmt.format(account.balance), style = AppText.amountM, color = if (account.balance < 0 && account.type != AccountType.CARD) c.exp else fg)
            Text(listOf(typeName, account.subtitle).filter { it.isNotBlank() }.joinToString(" · "), style = AppText.small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = sub)
        }
        val limit = account.creditLimit
        if (account.type == AccountType.CREDIT && limit != null && limit > 0) {
            val used = (-account.balance).coerceAtLeast(0)
            ProgressLine(used.toFloat() / limit, c.exp, track = c.s1.copy(alpha = 0.6f))
            Text("Доступно ${fmt.format(limit - used, false)} из ${fmt.format(limit, false)}", style = AppText.small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = sub)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSheet(
    account: Account?,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, AccountType, String, Long, Long?) -> Unit,
    onDelete: () -> Unit,
) {
    val c = App.colors
    var name by remember { mutableStateOf(account?.name.orEmpty()) }
    var type by remember { mutableStateOf(account?.type ?: AccountType.CARD) }
    var subtitle by remember { mutableStateOf(account?.subtitle.orEmpty()) }
    var balance by remember { mutableStateOf(account?.let { (if (it.balance < 0) "-" else "") + amountInput(it.balance) }.orEmpty()) }
    var limit by remember { mutableStateOf(account?.creditLimit?.let { amountInput(it) }.orEmpty()) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.s1,
        dragHandle = { SheetHandle(Modifier.padding(top = 8.dp)) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        HideSystemBars()
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (account == null) "Новый счёт" else "Счёт", style = AppText.section.copy(fontSize = 16.sp), color = c.on)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(AccountType.CARD to "Карта", AccountType.CASH to "Наличные", AccountType.DEPOSIT to "Вклад", AccountType.CREDIT to "Кредитка").forEach { (t, label) ->
                    val active = t == type
                    Row(
                        Modifier.height(40.dp).clip(RoundedCornerShape(12.dp)).background(if (active) c.priC else c.s2).clickable { type = t }.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(CategoryIcons.account(t), null, tint = if (active) c.onPriC else c.on, modifier = Modifier.size(18.dp))
                        Text(label, style = AppText.label, color = if (active) c.onPriC else c.on)
                    }
                }
            }
            FilledField("Название", name, { name = it }, placeholder = "Например, Карта •• 4821")
            FilledField("Подпись", subtitle, { subtitle = it }, placeholder = "Дебетовая · Мир, 16% годовых…")
            FilledField(
                if (account == null) "Текущий баланс, ${Money.fmt.symbol}" else "Баланс сейчас, ${Money.fmt.symbol}",
                balance,
                { v -> balance = v.filter { it.isDigit() || it == ',' || it == '.' || it == '-' } },
                placeholder = "0",
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            if (type == AccountType.CREDIT) {
                FilledField("Кредитный лимит, ${Money.fmt.symbol}", limit, { v -> limit = v.filter { it.isDigit() || it == ',' || it == '.' } }, keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (canDelete) TextAction("Удалить", color = c.exp, onClick = onDelete)
                else if (account != null) Text("Счёт используется в операциях", style = AppText.small, color = c.on2, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.weight(1f))
                PrimaryButton("Сохранить", height = 48.dp, enabled = name.isNotBlank()) {
                    val sign = if (balance.trim().startsWith("-")) -1 else 1
                    onSave(name, type, subtitle, sign * parseAmount(balance.replace("-", "")), parseAmount(limit).takeIf { it > 0 })
                }
            }
        }
    }
}


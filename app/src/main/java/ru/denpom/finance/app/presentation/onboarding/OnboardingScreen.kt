package ru.denpom.finance.app.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.denpom.finance.app.domain.model.AccountType
import ru.denpom.finance.app.domain.model.Currency
import ru.denpom.finance.app.presentation.components.CategoryIcons
import ru.denpom.finance.app.presentation.components.FilledField
import ru.denpom.finance.app.presentation.components.PrimaryButton
import ru.denpom.finance.app.presentation.components.TextAction
import ru.denpom.finance.app.presentation.components.card
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText

private data class Slide(val title: String, val body: String, val art: @Composable () -> Unit)

private val slides = listOf(
    Slide("Сканируй чеки — позиции разложатся сами", "Сфотографируйте чек — приложение распознает позиции и разложит их по категориям. QR-код заполнит сумму и дату.") { ReceiptIllustration() },
    Slide("Бюджеты и подписки под контролем", "Лимиты по категориям, прогноз до конца месяца и ближайшие списания — до того, как они случатся.") { BudgetIllustration() },
    Slide("Данные хранятся на телефоне", "Без регистрации и облака. Резервную копию и экспорт в таблицу вы делаете сами.") { PrivacyIllustration() },
)

@Composable
fun OnboardingScreen(onFinished: (String) -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = App.colors
    BackHandler(enabled = state.page > 0) { viewModel.back() }

    Box(Modifier.fillMaxSize().background(c.bg).statusBarsPadding().displayCutoutPadding().navigationBarsPadding()) {
        AnimatedContent(state.page == 3, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "onb") { setup ->
            if (!setup) Intro(state.page, viewModel::next, viewModel::skip)
            else Setup(viewModel) { viewModel.finish(onFinished) }
        }
    }
}

@Composable
private fun Intro(page: Int, onNext: () -> Unit, onSkip: () -> Unit) {
    val c = App.colors
    Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)) {
        Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextAction("Пропустить", onClick = onSkip)
        }
        AnimatedContent(page, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "slide", modifier = Modifier.weight(1f)) { p ->
            val slide = slides[p.coerceIn(0, 2)]
            Column {
                Box(Modifier.fillMaxWidth().height(328.dp).padding(top = 8.dp)) { slide.art() }
                Row(Modifier.padding(top = 28.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { i ->
                        val w by animateDpAsState(if (i == p) 24.dp else 6.dp, label = "dot")
                        Box(Modifier.width(w).height(6.dp).clip(RoundedCornerShape(3.dp)).background(if (i == p) c.pri else c.s3))
                    }
                }
                Text(slide.title, style = AppText.headline, color = c.on)
                Text(slide.body, style = AppText.body.copy(lineHeight = 22.sp), color = c.on2, modifier = Modifier.padding(top = 12.dp))
            }
        }
        PrimaryButton("Далее", Modifier.fillMaxWidth().padding(top = 24.dp), height = 56.dp, onClick = onNext)
    }
}

@Composable
private fun Setup(viewModel: OnboardingViewModel, onFinish: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = App.colors
    Column(Modifier.fillMaxSize().imePadding().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Валюта и первый счёт", style = AppText.headline, color = c.on)
                Text("Всё это можно изменить позже в настройках.", style = AppText.body, color = c.on2)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Основная валюта", style = AppText.caption.copy(fontWeight = FontWeight.SemiBold), color = c.on2)
                Column(Modifier.card(RoundedCornerShape(20.dp))) {
                    Currency.entries.forEach { cur ->
                        val active = cur == state.currency
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { viewModel.setCurrency(cur) }.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(Modifier.size(36.dp).clip(CircleShape).background(c.s2), contentAlignment = Alignment.Center) {
                                Text(cur.symbol, style = if (cur.symbol.length > 2) AppText.small else AppText.section, color = c.on)
                            }
                            Text(cur.title, style = AppText.body, color = c.on, modifier = Modifier.weight(1f))
                            Icon(
                                if (active) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                                null,
                                tint = if (active) c.pri else c.on2,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Первый счёт", style = AppText.caption.copy(fontWeight = FontWeight.SemiBold), color = c.on2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(AccountType.CARD to "Карта", AccountType.CASH to "Наличные", AccountType.DEPOSIT to "Вклад").forEach { (t, label) ->
                        val active = t == state.type
                        Row(
                            Modifier.height(40.dp).clip(RoundedCornerShape(12.dp)).background(if (active) c.priC else c.s2).clickable { viewModel.setType(t) }.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(CategoryIcons.account(t), null, tint = if (active) c.onPriC else c.on, modifier = Modifier.size(18.dp))
                            Text(label, style = AppText.label, color = if (active) c.onPriC else c.on)
                        }
                    }
                }
                FilledField("Название", state.name, viewModel::setName)
                FilledField(
                    "Текущий баланс, ${state.currency.symbol}",
                    state.balance,
                    viewModel::setBalance,
                    placeholder = "0,00",
                    keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Начать", Modifier.fillMaxWidth(), height = 56.dp, onClick = onFinish)
    }
}

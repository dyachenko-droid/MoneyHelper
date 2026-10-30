package ru.denpom.finance.app.presentation.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.denpom.finance.app.domain.model.AppSettings
import ru.denpom.finance.app.domain.repository.SettingsRepository
import ru.denpom.finance.app.presentation.LocalAppActions
import ru.denpom.finance.app.presentation.components.BackTopBar
import ru.denpom.finance.app.presentation.components.TextAction
import ru.denpom.finance.app.presentation.components.card
import ru.denpom.finance.app.presentation.subscriptions.ToggleRow
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import javax.inject.Inject
import ru.denpom.finance.app.presentation.components.HideSystemBars

@HiltViewModel
class SecurityViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<AppSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    fun setPin(pin: String?) = viewModelScope.launch { settings.setPin(pin) }
    fun setBiometric(on: Boolean) = viewModelScope.launch { settings.setBiometric(on) }
}

@Composable
fun SecurityScreen(onBack: () -> Unit, viewModel: SecurityViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val c = App.colors
    val context = LocalContext.current
    val actions = LocalAppActions.current
    var setup by remember { mutableStateOf(false) }
    val bioAvailable = remember { canUseBiometric(context) }

    Column(Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding()) {
        BackTopBar("PIN и биометрия", onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ToggleRow("Вход по PIN-коду", s.pinEnabled) { on ->
                if (on) setup = true else {
                    viewModel.setPin(null)
                    actions.snack("PIN отключён")
                }
            }
            if (s.pinEnabled) {
                if (bioAvailable) {
                    ToggleRow("Вход по отпечатку или лицу", s.biometric) { on ->
                        if (on) showBiometric(context, "Подтвердите биометрию") { viewModel.setBiometric(true) } else viewModel.setBiometric(false)
                    }
                }
                TextAction("Сменить PIN") { setup = true }
            }
            Column(Modifier.card().padding(16.dp)) {
                Text(
                    "Приложение заблокируется при запуске и если пробыло в фоне дольше 30 секунд. " +
                        "Если забудете PIN, данные можно вернуть только из резервной копии — сделайте её в «Бэкап и экспорт».",
                    style = AppText.caption,
                    color = c.on2,
                )
            }
        }
    }

    if (setup) {
        PinSetupDialog(
            onDismiss = { setup = false },
            onDone = { pin ->
                viewModel.setPin(pin)
                setup = false
                actions.snack("PIN установлен")
            },
        )
    }
}

@Composable
private fun PinSetupDialog(onDismiss: () -> Unit, onDone: (String) -> Unit) {
    val c = App.colors
    var first by remember { mutableStateOf<String?>(null) }
    var pin by remember { mutableStateOf("") }
    var errors by remember { mutableIntStateOf(0) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        HideSystemBars()
        Box(Modifier.fillMaxSize().background(c.bg)) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding().navigationBarsPadding().padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BackTopBar("", onDismiss)
                Spacer(Modifier.weight(1f))
                Text(
                    when {
                        errors > 0 && first == null -> "PIN не совпал, ещё раз"
                        first == null -> "Придумайте PIN из 4 цифр"
                        else -> "Повторите PIN"
                    },
                    style = AppText.title.copy(fontSize = 20.sp),
                    color = if (errors > 0 && first == null) c.exp else c.on,
                )
                Spacer(Modifier.height(24.dp))
                PinDots(pin.length, errors)
                Spacer(Modifier.weight(1f))
                PinPad(
                    onDigit = { d ->
                        if (pin.length >= PIN_LENGTH) return@PinPad
                        pin += d
                        if (pin.length == PIN_LENGTH) {
                            val f = first
                            when {
                                f == null -> {
                                    first = pin
                                    pin = ""
                                }
                                f == pin -> onDone(pin)
                                else -> {
                                    errors++
                                    first = null
                                    pin = ""
                                }
                            }
                        }
                    },
                    onDelete = { pin = pin.dropLast(1) },
                    onBiometric = null,
                )
            }
        }
    }
}

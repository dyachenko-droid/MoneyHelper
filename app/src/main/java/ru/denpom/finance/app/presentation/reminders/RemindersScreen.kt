package ru.denpom.finance.app.presentation.reminders

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.denpom.finance.app.data.reminders.ReminderScheduler
import ru.denpom.finance.app.domain.model.AppSettings
import ru.denpom.finance.app.domain.repository.SettingsRepository
import ru.denpom.finance.app.presentation.components.BackTopBar
import ru.denpom.finance.app.presentation.components.FilterChip
import ru.denpom.finance.app.presentation.components.card
import ru.denpom.finance.app.presentation.subscriptions.ToggleRow
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import javax.inject.Inject
import ru.denpom.finance.app.presentation.components.HideSystemBars
import ru.denpom.finance.app.presentation.components.rememberPermissionRequest

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler,
) : ViewModel() {
    val state: StateFlow<AppSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setDaily(on: Boolean, minutes: Int) = viewModelScope.launch {
        settings.setDailyReminder(on, minutes)
        scheduler.apply(settings.current())
    }

    fun setPayments(on: Boolean) = viewModelScope.launch {
        settings.setPaymentReminders(on)
        scheduler.apply(settings.current())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(onBack: () -> Unit, viewModel: RemindersViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val c = App.colors
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showTime by remember { mutableStateOf(false) }
    val permission = rememberPermissionRequest { granted ->
        if (granted) pending?.invoke()
        pending = null
    }
    fun withPermission(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            pending = action
            permission(Manifest.permission.POST_NOTIFICATIONS)
        } else action()
    }
    val time = "%02d:%02d".format(s.reminderMinutes / 60, s.reminderMinutes % 60)

    Column(Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding()) {
        BackTopBar("Напоминания", onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ToggleRow("Напомнить внести траты", s.dailyReminder) { on ->
                if (on) withPermission { viewModel.setDaily(true, s.reminderMinutes) } else viewModel.setDaily(false, s.reminderMinutes)
            }
            ToggleRow("Предупреждать о списаниях и долгах", s.paymentReminders) { on ->
                if (on) withPermission { viewModel.setPayments(true) } else viewModel.setPayments(false)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Время напоминаний", style = AppText.body, color = c.on2)
                FilterChip(time, leading = Icons.Rounded.Schedule, trailing = null) { showTime = true }
            }
            Column(Modifier.card().padding(16.dp)) {
                Text(
                    "Вечером пришлём напоминание, только если за день не было ни одной записи. " +
                        "Накануне подписок, регулярных платежей и сроков по долгам — короткую сводку, сколько спишется завтра.",
                    style = AppText.caption,
                    color = c.on2,
                )
            }
        }
    }

    if (showTime) {
        val picker = rememberTimePickerState(s.reminderMinutes / 60, s.reminderMinutes % 60, is24Hour = true)
        TimePickerDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton({
                    viewModel.setDaily(s.dailyReminder, picker.hour * 60 + picker.minute)
                    showTime = false
                }) { Text("Готово") }
            },
            dismissButton = { TextButton({ showTime = false }) { Text("Отмена") } },
            title = {
                HideSystemBars()
                Text("Время напоминаний")
            },
        ) { TimePicker(picker) }
    }
}

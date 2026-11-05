package ru.denpom.finance.app.presentation.more

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.denpom.finance.app.domain.repository.*
import ru.denpom.finance.app.presentation.components.BackTopBar
import ru.denpom.finance.app.presentation.components.card
import ru.denpom.finance.app.presentation.format.Dates
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class BackupUiState(
    val busy: String? = null, val message: String? = null, val error: String? = null,
    val status: BackupStatus = BackupStatus(), val preview: BackupPreview? = null,
)

@HiltViewModel
class BackupViewModel @Inject constructor(private val backup: BackupRepository) : ViewModel() {
    private val mutable = MutableStateFlow(BackupUiState())
    val state = mutable.asStateFlow()
    init { viewModelScope.launch { mutable.update { it.copy(status = backup.status()) } } }
    private fun run(label: String, block: suspend () -> String) {
        if (mutable.value.busy != null) return
        mutable.update { it.copy(busy = label, error = null, message = null) }
        viewModelScope.launch {
            try {
                val result = block()
                val status = backup.status()
                mutable.update { it.copy(busy = null, message = result, status = status) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(busy = null, error = e.message ?: "Не удалось выполнить операцию. Текущие данные сохранены") } }
        }
    }
    fun exportCsv(uri: String) = run("Экспорт операций…") {
        val count = backup.exportCsv(uri)
        "Выгружено $count ${Dates.plural(count.toLong(), "операция", "операции", "операций")}" }
    fun backup(uri: String) = run("Сохранение копии…") { backup.backup(uri); "Резервная копия сохранена" }
    fun preview(uri: String) = run("Проверка всей копии…") {
        val preview = backup.preview(uri)
        mutable.update { it.copy(preview = preview) }
        "Копия проверена. Подтвердите восстановление" }
    fun cancel() {
        val token = mutable.value.preview?.token ?: return
        mutable.update { it.copy(preview = null, message = null) }
        viewModelScope.launch { backup.discard(token) }
    }
    fun restore() {
        val token = mutable.value.preview?.token ?: return
        mutable.update { it.copy(preview = null) }
        run("Восстановление…") { backup.restore(token); "Данные восстановлены. Предыдущее состояние доступно для отката" }
    }
    fun rollback() = run("Возврат предыдущих данных…") { backup.rollback(); "Предыдущие данные возвращены" }
}

private fun backupDate(timestamp: Long) = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

@Composable
fun BackupScreen(onBack: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    val c = App.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val busy = state.busy != null
    var confirmRollback by remember { mutableStateOf(false) }
    val stamp = LocalDate.now().toString()
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let { viewModel.exportCsv(it.toString()) } }
    val copy = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri -> uri?.let { viewModel.backup(it.toString()) } }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { viewModel.preview(it.toString()) } }
    BackHandler(enabled = busy) {}
    Column(Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding()) {
        BackTopBar("Бэкап и экспорт", { if (!busy) onBack() })
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.card().padding(vertical = 8.dp)) {
                SettingRow(Icons.Rounded.TableChart, "Экспорт в CSV для Excel", "") { if (!busy) csv.launch("operations-$stamp.csv") }
                SettingRow(Icons.Rounded.Backup, "Сохранить резервную копию", "") { if (!busy) copy.launch("money-helper-$stamp.backup") }
                SettingRow(Icons.Rounded.Restore, "Восстановить из копии", "") { if (!busy) restore.launch(arrayOf("*/*")) }
            }
            Text(state.status.lastBackup?.let { "Последняя успешная копия: ${backupDate(it)}" } ?: "Резервная копия ещё не сохранялась", style = AppText.caption, color = c.on2)
            if (busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = c.pri)
                Text(state.busy.orEmpty(), style = AppText.body, color = c.on)
            }
            state.message?.let { Text(it, style = AppText.body, color = c.pri) }
            state.error?.let { Text("Ошибка: $it", style = AppText.body, color = c.exp) }
            if (state.status.canRollback) OutlinedButton({ confirmRollback = true }, enabled = !busy) { Text("Вернуть предыдущие данные") }
            Text("Копия содержит счета, операции, бюджеты, цели, долги, платежи и доступные фотографии чеков. Перед восстановлением проверяется весь файл. Настройки оформления, PIN и напоминаний остаются текущими.", style = AppText.caption, color = c.on2)
            Text("После восстановления можно вернуть предыдущее состояние. Данные хранятся на телефоне: сохраняйте копии также в надёжном месте вне устройства.", style = AppText.caption, color = c.on2)
        }
    }
    state.preview?.let { p ->
        AlertDialog(onDismissRequest = viewModel::cancel, containerColor = c.s1,
            title = { Text("Восстановить проверенную копию?") },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                p.createdAt?.let { Text("Создана ${backupDate(it)}") }
                Text("Счета: ${p.accounts}\\nОперации: ${p.operations}\\nЧеки: ${p.receipts}\\nЦели: ${p.goals}\\nДолги: ${p.debts}\\nФотографии в файле: ${p.photos}".replace("\\n", "\n"))
                if (p.legacy) Text("Старая копия: валюта и фотографии в ней не сохранены. Используется текущая валюта приложения.")
                if (p.missingPhotos > 0) Text("Недоступных фотографий: ${p.missingPhotos}. Сами чеки и позиции сохранены.")
                Text("Текущие финансовые данные будут заменены. Перед заменой сохранится копия для отката.")
            } }, confirmButton = { TextButton(viewModel::restore) { Text("Восстановить", color = c.exp) } }, dismissButton = { TextButton(viewModel::cancel) { Text("Отмена") } })
    }
    if (confirmRollback) AlertDialog(onDismissRequest = { confirmRollback = false }, title = { Text("Вернуть предыдущие данные?") },
        text = { Text("Финансовые данные вернутся к состоянию перед последним восстановлением. Текущее состояние сохранится для обратного отката.") },
        confirmButton = { TextButton({ confirmRollback = false; viewModel.rollback() }) { Text("Вернуть") } }, dismissButton = { TextButton({ confirmRollback = false }) { Text("Отмена") } })
}

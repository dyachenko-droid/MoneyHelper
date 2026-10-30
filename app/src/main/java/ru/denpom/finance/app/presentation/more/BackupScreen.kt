package ru.denpom.finance.app.presentation.more

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.denpom.finance.app.domain.repository.BackupRepository
import ru.denpom.finance.app.presentation.LocalAppActions
import ru.denpom.finance.app.presentation.components.BackTopBar
import ru.denpom.finance.app.presentation.components.card
import ru.denpom.finance.app.presentation.format.Dates
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import java.time.LocalDate
import javax.inject.Inject
import kotlin.system.exitProcess
import ru.denpom.finance.app.presentation.components.HideSystemBars

@HiltViewModel
class BackupViewModel @Inject constructor(private val backup: BackupRepository) : ViewModel() {
    fun exportCsv(uri: String, onDone: (String) -> Unit) = viewModelScope.launch {
        val result = runCatching { backup.exportCsv(uri) }
        onDone(result.fold({ "Выгружено $it ${Dates.plural(it.toLong(), "операция", "операции", "операций")}" }, { "Не удалось сохранить файл" }))
    }

    fun backup(uri: String, onDone: (String) -> Unit) = viewModelScope.launch {
        onDone(if (runCatching { backup.backup(uri) }.isSuccess) "Резервная копия сохранена" else "Не удалось сохранить копию")
    }

    fun restore(uri: String, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        onDone(runCatching { backup.restore(uri) }.getOrDefault(false))
    }
}

@Composable
fun BackupScreen(onBack: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    val c = App.colors
    val context = LocalContext.current
    val actions = LocalAppActions.current
    var confirmRestore by remember { mutableStateOf<String?>(null) }
    val stamp = LocalDate.now().toString()

    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { viewModel.exportCsv(it.toString(), actions::snack) }
    }
    val copy = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { viewModel.backup(it.toString(), actions::snack) }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        confirmRestore = uri?.toString()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding()) {
        BackTopBar("Бэкап и экспорт", onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.card().padding(vertical = 8.dp)) {
                SettingRow(Icons.Rounded.TableChart, "Экспорт в CSV для Excel", "") { csv.launch("operations-$stamp.csv") }
                SettingRow(Icons.Rounded.Backup, "Сохранить резервную копию", "") { copy.launch("money-helper-$stamp.backup") }
                SettingRow(Icons.Rounded.Restore, "Восстановить из копии", "") { restore.launch(arrayOf("*/*")) }
            }
            Text(
                "Данные хранятся только на этом телефоне. Сохраняйте копию в облако или на компьютер — с неё можно восстановить всё на новом устройстве.",
                style = AppText.caption,
                color = c.on2,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }

    confirmRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmRestore = null },
            containerColor = c.s1,
            title = {
                HideSystemBars()
                Text("Восстановить из копии?")
            },
            text = { Text("Текущие данные будут заменены данными из файла. Приложение перезапустится.") },
            confirmButton = {
                TextButton({
                    confirmRestore = null
                    viewModel.restore(uri) { ok ->
                        if (ok) {
                            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            context.startActivity(intent)
                            exitProcess(0)
                        } else {
                            actions.snack("Файл не похож на резервную копию")
                        }
                    }
                }) { Text("Восстановить", color = c.exp) }
            },
            dismissButton = { TextButton({ confirmRestore = null }) { Text("Отмена") } },
        )
    }
}

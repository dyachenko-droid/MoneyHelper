package ru.denpom.finance.app.presentation.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.denpom.finance.app.data.repository.OperationTemplateRepository
import ru.denpom.finance.app.domain.model.Account
import ru.denpom.finance.app.domain.model.Category
import ru.denpom.finance.app.domain.model.CategoryKind
import ru.denpom.finance.app.domain.model.OperationTemplate
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.repository.AccountRepository
import ru.denpom.finance.app.domain.repository.CategoryRepository
import ru.denpom.finance.app.presentation.AddRequest
import ru.denpom.finance.app.presentation.LocalAppActions
import ru.denpom.finance.app.presentation.components.PrimaryButton
import ru.denpom.finance.app.presentation.components.TextAction
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import javax.inject.Inject

data class TemplatesState(
    val templates: List<OperationTemplate> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
)

@HiltViewModel
class OperationTemplatesViewModel @Inject constructor(
    private val repository: OperationTemplateRepository,
    accounts: AccountRepository,
    categories: CategoryRepository,
) : ViewModel() {
    val state = combine(repository.templates, accounts.observeAccounts(), categories.observeCategories(), ::TemplatesState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TemplatesState())

    fun save(template: OperationTemplate, onResult: (String?) -> Unit) = update(onResult) { repository.save(template) }

    fun delete(template: OperationTemplate, onResult: (String?) -> Unit) = update(onResult) { repository.delete(template.id) }

    private fun update(onResult: (String?) -> Unit, action: suspend () -> Unit) = viewModelScope.launch {
        try {
            action()
            onResult(null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onResult(e.message ?: "Не удалось сохранить шаблон")
        }
    }
}

@Composable
fun OperationTemplatesScreen(onBack: () -> Unit, viewModel: OperationTemplatesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    var edit by remember { mutableStateOf<OperationTemplate?>(null) }
    var remove by remember { mutableStateOf<OperationTemplate?>(null) }
    var busy by remember { mutableStateOf(false) }

    PlanningScreen("Шаблоны операций", onBack) {
        item {
            PlanningCard {
                Caption("Выберите шаблон и введите сумму. Счёт и категория подставятся автоматически.")
                PrimaryButton("Новый шаблон", Modifier.fillMaxWidth(), enabled = state.accounts.isNotEmpty()) {
                    edit = OperationTemplate(name = "", type = OperationType.EXPENSE, accountId = state.accounts.first().id, categoryId = null)
                }
                if (state.accounts.isEmpty()) Caption("Сначала добавьте счёт.")
            }
        }
        if (state.templates.isEmpty()) item { Caption("Здесь будут ваши частые операции") }
        items(state.templates, key = { it.id }) { template ->
            PlanningCard {
                Text(template.name, style = AppText.section, color = App.colors.on)
                val account = state.accounts.firstOrNull { it.id == template.accountId }?.name.orEmpty()
                val category = state.categories.firstOrNull { it.id == template.categoryId }?.name ?: "Без категории"
                Caption("${template.type.label()} · $account · $category")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextAction("Добавить") { actions.openAdd(AddRequest(template = template)) }
                    TextAction("Изменить") { edit = template }
                    TextAction("Удалить", color = App.colors.exp) { remove = template }
                }
            }
        }
    }

    edit?.let { template ->
        val kind = if (template.type == OperationType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
        val categories = state.categories.filter { it.kind == kind }
        val selectedAccount = state.accounts.firstOrNull { it.id == template.accountId }
        val selectedCategory = categories.firstOrNull { it.id == template.categoryId }
        AlertDialog(
            onDismissRequest = { if (!busy) edit = null },
            title = { Text(if (template.id == 0L) "Новый шаблон" else "Изменить шаблон") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = template.name, onValueChange = { edit = template.copy(name = it.take(80)) },
                        label = { Text("Название") }, singleLine = true,
                    )
                    ChoiceChip(template.type.label(), listOf(OperationType.EXPENSE, OperationType.INCOME), { it.label() }) {
                        if (it != template.type) edit = template.copy(type = it, categoryId = null)
                    }
                    ChoiceChip(selectedAccount?.name ?: "Выберите счёт", state.accounts, { it.name }) {
                        edit = template.copy(accountId = it.id)
                    }
                    ChoiceChip(selectedCategory?.name ?: "Без категории", listOf<Category?>(null) + categories, { it?.name ?: "Без категории" }) {
                        edit = template.copy(categoryId = it?.id)
                    }
                }
            },
            confirmButton = {
                TextButton({
                    if (busy) return@TextButton
                    busy = true
                    viewModel.save(template.copy(categoryId = selectedCategory?.id)) { error ->
                        busy = false
                        if (error == null) edit = null
                        actions.snack(error ?: "Шаблон сохранён")
                    }
                }, enabled = !busy && selectedAccount != null && template.name.isNotBlank()) { Text("Сохранить") }
            },
            dismissButton = { TextButton({ edit = null }, enabled = !busy) { Text("Отмена") } },
        )
    }

    remove?.let { template ->
        AlertDialog(
            onDismissRequest = { if (!busy) remove = null },
            title = { Text("Удалить шаблон «${template.name}»?") },
            confirmButton = {
                TextButton({
                    if (busy) return@TextButton
                    busy = true
                    viewModel.delete(template) { error ->
                        busy = false
                        remove = null
                        actions.snack(error ?: "Шаблон удалён")
                    }
                }, enabled = !busy) { Text("Удалить") }
            },
            dismissButton = { TextButton({ remove = null }, enabled = !busy) { Text("Отмена") } },
        )
    }
}

private fun OperationType.label() = if (this == OperationType.INCOME) "Доход" else "Расход"

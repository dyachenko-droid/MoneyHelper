package ru.denpom.finance.app.presentation.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.denpom.finance.app.domain.model.Account
import ru.denpom.finance.app.domain.model.Category
import ru.denpom.finance.app.domain.model.CategoryKind
import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.domain.repository.AccountRepository
import ru.denpom.finance.app.domain.repository.CategoryRepository
import ru.denpom.finance.app.domain.repository.OperationRepository
import ru.denpom.finance.app.domain.usecase.CategorizeItemsUseCase
import ru.denpom.finance.app.domain.usecase.SaveOperationUseCase
import ru.denpom.finance.app.presentation.AddRequest
import ru.denpom.finance.app.presentation.format.amountInput
import ru.denpom.finance.app.presentation.format.parseAmount
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class AddState(
    val type: OperationType = OperationType.EXPENSE,
    val amount: String = "",
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val toAccountId: Long? = null,
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now(),
    val note: String = "",
    val title: String = "",
    val repeat: Boolean = false,
    val editId: Long? = null,
    val receiptId: Long? = null,
    val original: Operation? = null,
) {
    val value: Long get() = parseAmount(amount)
}

@HiltViewModel
class AddOperationViewModel @Inject constructor(
    private val accountsRepo: AccountRepository,
    private val categoriesRepo: CategoryRepository,
    private val operations: OperationRepository,
    private val saveOperation: SaveOperationUseCase,
    private val categorize: CategorizeItemsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AddState())
    val state: StateFlow<AddState> = _state.asStateFlow()

    val accounts: StateFlow<List<Account>> = accountsRepo.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val categories: StateFlow<List<Category>> = categoriesRepo.observeCategories()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun start(request: AddRequest) {
        viewModelScope.launch {
            val accs = accountsRepo.observeAccounts().first()
            val cats = categoriesRepo.categories()
            val editing = request.editId?.let { operations.get(it) }
            if (editing != null) {
                val dt = Instant.ofEpochMilli(editing.timestamp).atZone(Period.zone)
                _state.value = AddState(
                    type = editing.type,
                    amount = amountInput(editing.amount),
                    categoryId = editing.categoryId,
                    accountId = editing.accountId,
                    toAccountId = editing.toAccountId,
                    date = dt.toLocalDate(),
                    time = dt.toLocalTime(),
                    note = editing.note,
                    title = editing.title,
                    editId = editing.id,
                    receiptId = editing.receiptId,
                    original = editing,
                )
                return@launch
            }
            val ts = request.timestamp?.let { Instant.ofEpochMilli(it).atZone(Period.zone) }
            val kind = if (request.type == OperationType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
            val guessed = if (request.title.isNotBlank()) categorize.categoryForTitle(request.title) else null
            _state.value = AddState(
                type = request.type,
                amount = if (request.amount > 0) amountInput(request.amount) else "",
                categoryId = guessed ?: cats.firstOrNull { it.kind == kind }?.id,
                accountId = accs.firstOrNull()?.id,
                toAccountId = accs.getOrNull(1)?.id,
                date = ts?.toLocalDate() ?: LocalDate.now(),
                time = ts?.toLocalTime() ?: LocalTime.now(),
                note = request.note,
                title = request.title,
            )
        }
    }

    fun setType(type: OperationType) {
        val kind = if (type == OperationType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
        _state.update { s ->
            val keep = categories.value.firstOrNull { it.id == s.categoryId }?.kind == kind
            s.copy(type = type, categoryId = if (keep) s.categoryId else categories.value.firstOrNull { it.kind == kind }?.id)
        }
    }

    fun press(key: String) {
        _state.update { s ->
            var a = s.amount
            when (key) {
                "⌫" -> a = a.dropLast(1)
                "," -> if (!a.contains(',')) a = (a.ifEmpty { "0" }) + ","
                else -> {
                    val parts = a.split(',')
                    if (parts.size == 2 && parts[1].length >= 2) return@update s
                    if (parts.size == 1 && parts[0].length >= 9) return@update s
                    a = if (a == "0") key else a + key
                }
            }
            s.copy(amount = a)
        }
    }

    fun clearAmount() = _state.update { it.copy(amount = "") }
    fun setCategory(id: Long) = _state.update { it.copy(categoryId = id) }
    fun setAccount(id: Long) = _state.update { it.copy(accountId = id) }
    fun setToAccount(id: Long) = _state.update { it.copy(toAccountId = id) }
    fun setDate(date: LocalDate) = _state.update { it.copy(date = date) }
    fun setNote(note: String) = _state.update { it.copy(note = note) }
    fun toggleRepeat() = _state.update { it.copy(repeat = !it.repeat) }

    fun save(onDone: (String) -> Unit, onError: (String) -> Unit) {
        val s = _state.value
        val amount = s.value
        if (amount <= 0) return
        val accountId = s.accountId ?: return onError("Сначала добавьте счёт")
        if (s.type == OperationType.TRANSFER && (s.toAccountId == null || s.toAccountId == accountId)) {
            return onError("Выберите другой счёт для перевода")
        }
        val category = categories.value.firstOrNull { it.id == s.categoryId }
        val toName = accounts.value.firstOrNull { it.id == s.toAccountId }?.name.orEmpty()
        val noteTitle = s.note.trim().takeIf { it.isNotEmpty() && !it.startsWith("#") }
        val title = when {
            s.type == OperationType.TRANSFER -> "Перевод на «$toName»"
            s.title.isNotBlank() && s.editId == null -> s.title
            s.editId != null && s.original != null && s.original.title.isNotBlank() && s.original.title != (categories.value.firstOrNull { it.id == s.original.categoryId }?.name) -> s.original.title
            noteTitle != null -> noteTitle
            else -> category?.name ?: "Операция"
        }
        val note = if (noteTitle != null && title == noteTitle) "" else s.note.trim()
        val ts = s.date.atTime(s.time).atZone(Period.zone).toInstant().toEpochMilli()
        val op = Operation(
            id = s.editId ?: 0,
            type = s.type,
            amount = amount,
            accountId = accountId,
            toAccountId = if (s.type == OperationType.TRANSFER) s.toAccountId else null,
            categoryId = if (s.type == OperationType.TRANSFER) null else s.categoryId,
            title = title,
            note = note,
            timestamp = ts,
            receiptId = s.receiptId,
            splits = s.original?.takeIf { it.amount == amount && it.categoryId == s.categoryId }?.splits.orEmpty(),
        )
        viewModelScope.launch {
            saveOperation(op, repeatMonthly = s.repeat)
            onDone(
                when {
                    s.editId != null -> "Операция изменена"
                    s.type == OperationType.INCOME -> "Доход добавлен"
                    s.type == OperationType.TRANSFER -> "Перевод выполнен"
                    else -> "Расход добавлен"
                }
            )
        }
    }
}

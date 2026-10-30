package ru.denpom.finance.app.presentation.receipt

import androidx.lifecycle.SavedStateHandle
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
import ru.denpom.finance.app.domain.model.CategoryRule
import ru.denpom.finance.app.domain.model.Debt
import ru.denpom.finance.app.domain.model.DebtDirection
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.domain.model.Receipt
import ru.denpom.finance.app.domain.model.ReceiptItem
import ru.denpom.finance.app.domain.model.ReceiptStatus
import ru.denpom.finance.app.domain.repository.AccountRepository
import ru.denpom.finance.app.domain.repository.CategoryRepository
import ru.denpom.finance.app.domain.repository.DebtRepository
import ru.denpom.finance.app.domain.repository.OperationRepository
import ru.denpom.finance.app.domain.repository.ReceiptRepository
import ru.denpom.finance.app.domain.usecase.CategorizeItemsUseCase
import ru.denpom.finance.app.domain.usecase.SaveReceiptUseCase
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToLong

data class ReceiptState(
    val receipt: Receipt? = null,
    val accountId: Long? = null,
    val saved: Boolean = false,
    val missing: Boolean = false,
)

@HiltViewModel
class ReceiptViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val holder: ReceiptDraftHolder,
    private val receipts: ReceiptRepository,
    private val operations: OperationRepository,
    private val categoriesRepo: CategoryRepository,
    accountsRepo: AccountRepository,
    private val saveReceipt: SaveReceiptUseCase,
    private val categorize: CategorizeItemsUseCase,
    private val debts: DebtRepository,
) : ViewModel() {

    private val receiptId: Long? = savedState.get<String>("id")?.toLongOrNull()

    private val _state = MutableStateFlow(ReceiptState())
    val state: StateFlow<ReceiptState> = _state.asStateFlow()

    val accounts: StateFlow<List<Account>> = accountsRepo.observeAccounts().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val categories: StateFlow<List<Category>> = categoriesRepo.observeCategories().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            val accs = accountsRepo.observeAccounts().first()
            if (receiptId == null) {
                val draft = holder.draft
                _state.value = if (draft == null) ReceiptState(missing = true) else ReceiptState(draft.receipt, accs.firstOrNull()?.id)
            } else {
                val r = receipts.get(receiptId)
                val accountId = r?.operationId?.let { operations.get(it)?.accountId } ?: accs.firstOrNull()?.id
                _state.value = if (r == null) ReceiptState(missing = true) else ReceiptState(r, accountId, saved = true)
            }
        }
    }

    val isEditable: Boolean get() = _state.value.receipt?.status == ReceiptStatus.RECOGNIZED

    fun setAccount(id: Long) {
        _state.update { it.copy(accountId = id) }
        persistIfSaved()
    }

    fun setItemCategory(index: Int, categoryId: Long) {
        val item = _state.value.receipt?.items?.getOrNull(index) ?: return
        updateReceipt { r -> r.copy(items = r.items.mapIndexed { i, it -> if (i == index) it.copy(categoryId = categoryId) else it }) }
        viewModelScope.launch {
            val key = categorize.ruleKey(item.name)
            if (key.isNotBlank()) categoriesRepo.saveRule(CategoryRule(key, categoryId))
        }
        persistIfSaved()
    }

    fun upsertItem(index: Int?, name: String, quantity: Double, price: Long) {
        val sum = (price * quantity).roundToLong()
        val unit = if (quantity % 1.0 != 0.0) "кг" else "шт"
        viewModelScope.launch {
            val r = _state.value.receipt ?: return@launch
            val existing = index?.let { r.items.getOrNull(it) }
            val categoryId = existing?.categoryId ?: categorize.categorize(r.shop, listOf(ReceiptItem(name = name, quantity = quantity, unit = unit, price = price, sum = sum, categoryId = 0))).first().categoryId
            val item = ReceiptItem(name = name, quantity = quantity, unit = unit, price = price, sum = sum, categoryId = categoryId)
            updateReceipt { cur ->
                val items = if (index == null) cur.items + item else cur.items.mapIndexed { i, it -> if (i == index) item else it }
                cur.copy(items = items, total = if (cur.status == ReceiptStatus.RECOGNIZED) items.sumOf { it.sum } else cur.total)
            }
            persistIfSaved()
        }
    }

    fun deleteItem(index: Int) {
        updateReceipt { cur ->
            val items = cur.items.filterIndexed { i, _ -> i != index }
            cur.copy(items = items, total = if (cur.status == ReceiptStatus.RECOGNIZED && items.isNotEmpty()) items.sumOf { it.sum } else cur.total)
        }
        persistIfSaved()
    }

    fun rename(shop: String) {
        if (shop.isBlank()) return
        updateReceipt { it.copy(shop = shop.trim()) }
        persistIfSaved()
    }

    fun setDate(date: LocalDate) {
        updateReceipt { r ->
            val time = Instant.ofEpochMilli(r.timestamp).atZone(Period.zone).toLocalTime()
            r.copy(timestamp = date.atTime(time).atZone(Period.zone).toInstant().toEpochMilli())
        }
        persistIfSaved()
    }

    fun toggleWarranty(): Boolean {
        var enabled = false
        updateReceipt { r ->
            enabled = r.warrantyUntil == null
            r.copy(warrantyUntil = if (enabled) Period.startOfDay(Period.dateOf(r.timestamp).plusYears(1)) else null)
        }
        persistIfSaved()
        return enabled
    }

    fun save(onDone: (Int) -> Unit, onError: (String) -> Unit) {
        val s = _state.value
        val r = s.receipt ?: return
        val accountId = s.accountId ?: return onError("Сначала добавьте счёт")
        if (r.total <= 0) return onError("Сумма чека должна быть больше нуля")
        viewModelScope.launch {
            val id = saveReceipt(r, accountId)
            holder.draft = null
            _state.update { it.copy(saved = true, receipt = r.copy(id = id)) }
            onDone(r.items.size)
        }
    }

    fun delete(onDone: () -> Unit) {
        val r = _state.value.receipt ?: return
        viewModelScope.launch {
            r.operationId?.let { operations.delete(it) }
            if (r.id != 0L) receipts.delete(r.id)
            onDone()
        }
    }

    fun recordSplit(shares: Map<String, Long>, onDone: (Int) -> Unit) {
        val r = _state.value.receipt ?: return
        val note = "Чек «${r.shop}», ${ru.denpom.finance.app.presentation.format.Dates.dayMonth(Period.dateOf(r.timestamp))}"
        val list = shares.filter { it.value > 0 }.map { (person, amount) ->
            Debt(0, person, DebtDirection.TO_ME, amount, note, System.currentTimeMillis(), null)
        }
        viewModelScope.launch {
            debts.saveAll(list)
            onDone(list.size)
        }
    }

    fun expenseCategories(): List<Category> = categories.value.filter { it.kind == CategoryKind.EXPENSE }

    private fun updateReceipt(block: (Receipt) -> Receipt) {
        _state.update { s -> s.receipt?.let { s.copy(receipt = block(it)) } ?: s }
    }

    private fun persistIfSaved() {
        val s = _state.value
        if (!s.saved) return
        val r = s.receipt ?: return
        val accountId = s.accountId ?: return
        viewModelScope.launch {
            val current = receipts.get(r.id)
            saveReceipt(r.copy(operationId = current?.operationId ?: r.operationId), accountId)
        }
    }

}

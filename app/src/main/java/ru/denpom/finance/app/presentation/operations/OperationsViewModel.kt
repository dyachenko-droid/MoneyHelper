package ru.denpom.finance.app.presentation.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.denpom.finance.app.domain.model.Account
import ru.denpom.finance.app.domain.model.Category
import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.domain.model.Receipt
import ru.denpom.finance.app.domain.repository.AccountRepository
import ru.denpom.finance.app.domain.repository.CategoryRepository
import ru.denpom.finance.app.domain.repository.OperationRepository
import ru.denpom.finance.app.domain.repository.ReceiptRepository
import ru.denpom.finance.app.domain.model.OpsFilter
import ru.denpom.finance.app.domain.model.TypeFilter
import ru.denpom.finance.app.domain.model.OperationsSummary
import ru.denpom.finance.app.domain.usecase.OperationQuery
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class DayGroup(val date: LocalDate, val total: Long, val items: List<Operation>)

data class OpsState(
    val filter: OpsFilter = OpsFilter(),
    val groups: List<DayGroup> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val accounts: List<Account> = emptyList(),
    val months: List<YearMonth> = emptyList(),
    val summary: OperationsSummary = OperationsSummary(),
    val loaded: Boolean = false,
    val hasAny: Boolean = false,
)

@HiltViewModel
class OperationsViewModel @Inject constructor(
    private val operations: OperationRepository,
    private val receipts: ReceiptRepository,
    categories: CategoryRepository,
    accounts: AccountRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(OpsFilter())

    val state: StateFlow<OpsState> = combine(
        operations.observeOperations(),
        categories.observeCategories(),
        accounts.observeAccounts(),
        filter,
    ) { ops, cats, accs, f ->
        val catMap = cats.associateBy { it.id }
        val accNames = accs.associate { it.id to it.name }
        val filtered = OperationQuery.filter(ops, f, catMap, accNames)
        val groups = filtered.groupBy { Period.dateOf(it.timestamp) }.map { (date, items) ->
            DayGroup(date, items.sumOf { OperationQuery.accountDelta(it, f.accountId) }, items)
        }
        val now = YearMonth.now()
        val oldest = ops.minOfOrNull { it.timestamp }?.let { Period.monthOf(it) } ?: now
        val newest = ops.maxOfOrNull { it.timestamp }?.let { Period.monthOf(it) }?.takeIf { it.isAfter(now) } ?: now
        val months = generateSequence(newest) { it.minusMonths(1) }.takeWhile { !it.isBefore(oldest) }.take(120).toList()
        OpsState(f, groups, catMap, accs, months.ifEmpty { listOf(now) }, OperationQuery.summary(filtered, f.accountId), true, ops.isNotEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OpsState())

    fun setQuery(q: String) = filter.update { it.copy(query = q) }
    fun setMonth(m: YearMonth?) = filter.update { it.copy(month = m) }
    fun cycleType() = filter.update { it.copy(type = TypeFilter.entries[(it.type.ordinal + 1) % TypeFilter.entries.size]) }
    fun setAccount(id: Long?) = filter.update { it.copy(accountId = id) }
    fun setCategory(id: Long?) = filter.update { it.copy(categoryId = id) }
    fun reset() = filter.update { OpsFilter(month = null) }

    fun delete(op: Operation, onDeleted: (undo: () -> Unit) -> Unit) {
        viewModelScope.launch {
            val receipt: Receipt? = op.receiptId?.let { receipts.get(it) }
            operations.delete(op.id)
            receipt?.let { receipts.delete(it.id) }
            onDeleted {
                viewModelScope.launch {
                    receipt?.let { receipts.save(it) }
                    operations.restore(op)
                }
            }
        }
    }

    fun duplicate(op: Operation) {
        viewModelScope.launch {
            operations.save(op.copy(id = 0, receiptId = null, timestamp = System.currentTimeMillis()))
        }
    }
}

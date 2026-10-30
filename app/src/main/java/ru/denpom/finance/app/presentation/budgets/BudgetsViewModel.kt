package ru.denpom.finance.app.presentation.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.denpom.finance.app.domain.model.Budget
import ru.denpom.finance.app.domain.model.BudgetProgress
import ru.denpom.finance.app.domain.model.Category
import ru.denpom.finance.app.domain.model.CategoryKind
import ru.denpom.finance.app.domain.repository.BudgetRepository
import ru.denpom.finance.app.domain.repository.CategoryRepository
import ru.denpom.finance.app.domain.repository.OperationRepository
import ru.denpom.finance.app.domain.usecase.SpendingCalculator
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class BudgetsState(
    val month: YearMonth = YearMonth.now(),
    val months: List<YearMonth> = listOf(YearMonth.now()),
    val limit: Long? = null,
    val spent: Long = 0,
    val daysLeft: Int = 0,
    val items: List<BudgetProgress> = emptyList(),
    val unbudgeted: List<Pair<Category, Long>> = emptyList(),
    val loaded: Boolean = false,
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    operations: OperationRepository,
    categories: CategoryRepository,
    private val budgets: BudgetRepository,
    private val calc: SpendingCalculator,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    val state: StateFlow<BudgetsState> = combine(
        operations.observeOperations(),
        categories.observeCategories(),
        budgets.observeBudgets(),
        month,
    ) { ops, cats, buds, m ->
        val expenseCats = cats.filter { it.kind == CategoryKind.EXPENSE }
        val spentBy = calc.byCategory(ops, m)
        val byId = expenseCats.associateBy { it.id }
        val items = buds.mapNotNull { b -> b.categoryId?.let { id -> byId[id]?.let { BudgetProgress(it, b.limit, spentBy[id] ?: 0) } } }
            .sortedByDescending { it.ratio }
        val budgeted = items.map { it.category.id }.toSet()
        val unbudgeted = expenseCats.filter { it.id !in budgeted }.map { it to (spentBy[it.id] ?: 0L) }.sortedByDescending { it.second }
        val today = LocalDate.now()
        val now = YearMonth.from(today)
        BudgetsState(
            month = m,
            months = (0 until 12).map { now.minusMonths(it.toLong()) },
            limit = buds.firstOrNull { it.categoryId == null }?.limit,
            spent = calc.totalSpent(ops, m),
            daysLeft = if (m == now) m.lengthOfMonth() - today.dayOfMonth + 1 else 0,
            items = items,
            unbudgeted = unbudgeted,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsState())

    fun setMonth(m: YearMonth) {
        month.value = m
    }

    fun save(categoryId: Long?, limit: Long) {
        viewModelScope.launch {
            if (limit <= 0) budgets.delete(categoryId) else budgets.save(Budget(categoryId, limit))
        }
    }

    fun delete(categoryId: Long?) {
        viewModelScope.launch { budgets.delete(categoryId) }
    }
}

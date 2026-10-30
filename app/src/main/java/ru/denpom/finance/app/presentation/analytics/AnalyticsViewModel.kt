package ru.denpom.finance.app.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import ru.denpom.finance.app.domain.model.Category
import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.domain.model.Receipt
import ru.denpom.finance.app.domain.repository.CategoryRepository
import ru.denpom.finance.app.domain.repository.OperationRepository
import ru.denpom.finance.app.domain.repository.ReceiptRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlin.math.roundToLong

enum class Range { WEEK, MONTH, YEAR }

data class CategoryShare(val category: Category?, val amount: Long)
data class MonthBar(val month: YearMonth, val income: Long, val expense: Long)
data class ShopStat(val name: String, val amount: Long, val count: Int)
data class PricePoint(val name: String, val first: Long, val last: Long, val points: List<Long>, val from: LocalDate, val to: LocalDate) {
    val change: Int get() = if (first == 0L) 0 else ((last - first) * 100 / first).toInt()
}

data class AnalyticsState(
    val range: Range = Range.MONTH,
    val offset: Int = 0,
    val title: String = "",
    val expense: Long = 0,
    val income: Long = 0,
    val shares: List<CategoryShare> = emptyList(),
    val bars: List<MonthBar> = emptyList(),
    val shops: List<ShopStat> = emptyList(),
    val averageCheck: Long = 0,
    val inflation: List<PricePoint> = emptyList(),
    val previousExpense: Long = 0,
) {
    val savingsRate: Int get() = if (income <= 0) 0 else ((income - expense) * 100 / income).toInt()
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    operations: OperationRepository,
    categories: CategoryRepository,
    receipts: ReceiptRepository,
) : ViewModel() {

    private val selection = MutableStateFlow(Range.MONTH to 0)

    val state: StateFlow<AnalyticsState> = combine(
        operations.observeOperations(),
        categories.observeCategories(),
        receipts.observeReceipts(),
        selection,
    ) { ops, cats, recs, (range, offset) -> build(ops, cats, recs, range, offset) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsState())

    fun setRange(range: Range) = selection.update { range to 0 }
    fun shift(delta: Int) = selection.update { (r, o) -> r to (o + delta).coerceAtMost(0) }

    private fun bounds(range: Range, offset: Int): Triple<LocalDate, LocalDate, String> {
        val today = LocalDate.now()
        return when (range) {
            Range.WEEK -> {
                val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(offset.toLong())
                val end = start.plusDays(7)
                Triple(start, end, if (offset == 0) "Эта неделя" else "${ru.denpom.finance.app.presentation.format.Dates.dayMonth(start)} – ${ru.denpom.finance.app.presentation.format.Dates.dayMonth(end.minusDays(1))}")
            }
            Range.MONTH -> {
                val m = YearMonth.now().plusMonths(offset.toLong())
                Triple(m.atDay(1), m.plusMonths(1).atDay(1), ru.denpom.finance.app.presentation.format.Dates.monthTitle(m))
            }
            Range.YEAR -> {
                val y = today.year + offset
                Triple(LocalDate.of(y, 1, 1), LocalDate.of(y + 1, 1, 1), "$y год")
            }
        }
    }

    private fun build(ops: List<Operation>, cats: List<Category>, receipts: List<Receipt>, range: Range, offset: Int): AnalyticsState {
        val (start, end, title) = bounds(range, offset)
        val (pStart, pEnd, _) = bounds(range, offset - 1)
        val from = Period.startOfDay(start)
        val to = Period.startOfDay(end)
        val inRange = ops.filter { it.timestamp in from until to }
        val expenses = inRange.filter { it.type == OperationType.EXPENSE }
        val byCat = HashMap<Long?, Long>()
        expenses.forEach { op ->
            if (op.splits.isEmpty()) byCat.merge(op.categoryId, op.amount, Long::plus)
            else op.splits.forEach { byCat.merge(it.categoryId, it.amount, Long::plus) }
        }
        val catMap = cats.associateBy { it.id }
        val shares = byCat.map { (id, sum) -> CategoryShare(id?.let { catMap[it] }, sum) }.sortedByDescending { it.amount }
        val now = YearMonth.now()
        val bars = (4 downTo 0).map { i ->
            val m = now.minusMonths(i.toLong())
            MonthBar(
                m,
                ops.filter { it.type == OperationType.INCOME && Period.inMonth(it.timestamp, m) }.sumOf { it.amount },
                ops.filter { it.type == OperationType.EXPENSE && Period.inMonth(it.timestamp, m) }.sumOf { it.amount },
            )
        }
        val shops = expenses.groupBy { it.title.trim().lowercase() }
            .map { (_, list) -> ShopStat(list.first().title, list.sumOf { it.amount }, list.size) }
            .sortedByDescending { it.amount }
            .take(5)
        val prevFrom = Period.startOfDay(pStart)
        val prevTo = Period.startOfDay(pEnd)
        return AnalyticsState(
            range = range,
            offset = offset,
            title = title,
            expense = expenses.sumOf { it.amount },
            income = inRange.filter { it.type == OperationType.INCOME }.sumOf { it.amount },
            shares = shares,
            bars = bars,
            shops = shops,
            averageCheck = if (expenses.isEmpty()) 0 else expenses.sumOf { it.amount } / expenses.size,
            inflation = inflation(receipts),
            previousExpense = ops.filter { it.type == OperationType.EXPENSE && it.timestamp in prevFrom until prevTo }.sumOf { it.amount },
        )
    }

    private fun inflation(receipts: List<Receipt>): List<PricePoint> {
        data class Seen(val name: String, val date: LocalDate, val unitPrice: Long)
        val seen = receipts.flatMap { r ->
            val date = Period.dateOf(r.timestamp)
            r.items.filter { it.quantity > 0 && it.sum > 0 }.map { Seen(it.name, date, (it.sum / it.quantity).roundToLong()) }
        }
        return seen.groupBy { normalize(it.name) }
            .filterKeys { it.length >= 4 }
            .mapNotNull { (_, list) ->
                val sorted = list.sortedBy { it.date }
                if (sorted.size < 2 || sorted.first().date == sorted.last().date) return@mapNotNull null
                PricePoint(sorted.last().name, sorted.first().unitPrice, sorted.last().unitPrice, sorted.map { it.unitPrice }, sorted.first().date, sorted.last().date)
            }
            .filter { it.change != 0 }
            .sortedByDescending { it.change }
            .take(5)
    }

    private fun normalize(name: String) = name.lowercase().replace(Regex("[^a-zа-яё0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()
}

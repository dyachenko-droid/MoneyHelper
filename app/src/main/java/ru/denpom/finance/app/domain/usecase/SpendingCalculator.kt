package ru.denpom.finance.app.domain.usecase

import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.model.Period
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class SpendingCalculator @Inject constructor() {

    fun expenses(operations: List<Operation>, month: YearMonth): List<Operation> =
        operations.filter { it.type == OperationType.EXPENSE && Period.inMonth(it.timestamp, month) }

    fun totalSpent(operations: List<Operation>, month: YearMonth): Long = expenses(operations, month).sumOf { it.amount }

    fun totalIncome(operations: List<Operation>, month: YearMonth): Long =
        operations.filter { it.type == OperationType.INCOME && Period.inMonth(it.timestamp, month) }.sumOf { it.amount }

    fun byCategory(operations: List<Operation>, month: YearMonth, untilDay: Int? = null): Map<Long, Long> {
        val result = HashMap<Long, Long>()
        expenses(operations, month)
            .filter { untilDay == null || Period.dateOf(it.timestamp).dayOfMonth <= untilDay }
            .forEach { op ->
                if (op.splits.isEmpty()) {
                    op.categoryId?.let { result.merge(it, op.amount, Long::plus) }
                } else {
                    op.splits.forEach { result.merge(it.categoryId, it.amount, Long::plus) }
                }
            }
        return result
    }

    fun forecast(spent: Long, month: YearMonth, today: LocalDate): Long {
        if (YearMonth.from(today) != month) return spent
        val day = today.dayOfMonth.coerceAtLeast(1)
        return spent * month.lengthOfMonth() / day
    }
}

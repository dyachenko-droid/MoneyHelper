package ru.denpom.finance.app.domain.usecase

import ru.denpom.finance.app.domain.model.Budget
import ru.denpom.finance.app.domain.model.GoalEntry
import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.model.Period
import java.time.LocalDate
import java.time.YearMonth

data class ReportLine(val operation: Operation, val amount: Long)
data class CategoryChange(val categoryId: Long?, val current: List<ReportLine>, val previous: List<ReportLine>) {
    val increase: Long get() = current.sumOf { it.amount } - previous.sumOf { it.amount }
}
data class BudgetOverage(val categoryId: Long?, val limit: Long, val lines: List<ReportLine>) {
    val spent: Long get() = lines.sumOf { it.amount }
}
data class MonthlyReport(
    val income: List<ReportLine>,
    val expenses: List<ReportLine>,
    val growth: List<CategoryChange>,
    val overages: List<BudgetOverage>,
    val savings: List<GoalEntry>,
    val partial: Boolean,
) {
    val incomeTotal: Long get() = income.sumOf { it.amount }
    val expenseTotal: Long get() = expenses.sumOf { it.amount }
    val net: Long get() = incomeTotal - expenseTotal
}

object MonthlyReports {
    fun calculate(
        month: YearMonth,
        today: LocalDate,
        operations: List<Operation>,
        budgets: List<Budget>,
        goalEntries: List<GoalEntry>,
    ): MonthlyReport {
        val currentMonth = YearMonth.from(today)
        val partial = month == currentMonth && today.dayOfMonth < month.lengthOfMonth()
        val lastDay = if (month == currentMonth) today else month.atEndOfMonth()
        val previous = month.minusMonths(1)
        val previousLast = if (partial) previous.atDay(minOf(today.dayOfMonth, previous.lengthOfMonth())) else previous.atEndOfMonth()
        val currentRows = operations.filter { it.timestamp >= Period.startOf(month) && it.timestamp < Period.startOfDay(lastDay.plusDays(1)) }
        val previousExpenses = operations.filter {
            it.type == OperationType.EXPENSE && it.timestamp >= Period.startOf(previous) && it.timestamp < Period.startOfDay(previousLast.plusDays(1))
        }
        val expenseRows = currentRows.filter { it.type == OperationType.EXPENSE }
        val income = currentRows.filter { it.type == OperationType.INCOME }.map { ReportLine(it, it.amount) }
        val expenses = expenseRows.map { ReportLine(it, it.amount) }
        val currentCategories = categoryLines(expenseRows)
        val previousCategories = categoryLines(previousExpenses)
        val growth = currentCategories.map { (id, lines) -> CategoryChange(id, lines, previousCategories[id].orEmpty()) }
            .filter { it.increase > 0 }.sortedByDescending { it.increase }
        val overages = if (month == currentMonth) budgets.map { budget ->
            val lines = budget.categoryId?.let { currentCategories[it].orEmpty() } ?: expenses
            BudgetOverage(budget.categoryId, budget.limit, lines)
        }.filter { it.spent > it.limit }.sortedByDescending { it.spent - it.limit } else emptyList()
        val savings = goalEntries.filter { it.timestamp >= Period.startOf(month) && it.timestamp < Period.startOfDay(lastDay.plusDays(1)) }
        return MonthlyReport(income, expenses, growth, overages, savings, partial)
    }

    private fun categoryLines(expenses: List<Operation>): Map<Long?, List<ReportLine>> {
        val result = mutableMapOf<Long?, MutableList<ReportLine>>()
        expenses.forEach { operation ->
            val allocations = if (operation.splits.isEmpty()) mapOf(operation.categoryId to operation.amount)
                else operation.splits.groupingBy { it.categoryId }.fold(0L) { amount, split -> amount + split.amount }
            allocations.forEach { (id, amount) -> result.getOrPut(id) { mutableListOf() }.add(ReportLine(operation, amount)) }
        }
        return result
    }
}

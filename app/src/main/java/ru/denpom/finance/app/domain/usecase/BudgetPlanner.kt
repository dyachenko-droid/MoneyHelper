package ru.denpom.finance.app.domain.usecase

import ru.denpom.finance.app.domain.model.*
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

data class BudgetPlan(val reserved: Long, val paymentCount: Int, val available: Long?, val daily: Long?)

object BudgetPlanner {
    fun calculate(limit: Long?, spent: Long, recurring: List<Recurring>, operations: List<Operation>, today: LocalDate): BudgetPlan {
        val month = YearMonth.from(today)
        fun key(date: LocalDate, amount: Long, account: Long, title: String) = "$date|$amount|$account|${title.trim().lowercase(Locale.ROOT)}"
        val posted = operations.filter { it.type == OperationType.EXPENSE && Period.inMonth(it.timestamp, month) }
            .groupingBy { key(Period.dateOf(it.timestamp), it.amount, it.accountId, it.title) }.eachCount().toMutableMap()
        var reserved = 0L
        var count = 0
        recurring.filter { it.type == OperationType.EXPENSE }.forEach { r ->
            var date = Period.dateOf(r.nextDate)
            var guard = 0
            while (!date.isAfter(month.atEndOfMonth()) && guard++ < 10_000) {
                if (!date.isBefore(today)) {
                    val k = key(date, r.amount, r.accountId, r.title)
                    val matched = posted[k] ?: 0
                    if (matched > 0) posted[k] = matched - 1
                    else { reserved += r.amount; count++ }
                }
                date = when (r.period) {
                    RecurrencePeriod.WEEKLY -> date.plusWeeks(1)
                    RecurrencePeriod.MONTHLY -> date.plusMonths(1)
                    RecurrencePeriod.YEARLY -> date.plusYears(1)
                }
            }
        }
        val available = limit?.let { it - spent - reserved }
        return BudgetPlan(reserved, count, available, available?.coerceAtLeast(0)?.div(month.lengthOfMonth() - today.dayOfMonth + 1))
    }
}

package ru.denpom.finance.app.domain.usecase

import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.domain.model.PaymentOccurrence
import ru.denpom.finance.app.domain.model.Recurring
import java.time.LocalDate
import java.time.YearMonth

data class BudgetPlan(val reserved: Long, val paymentCount: Int, val available: Long?, val daily: Long?)

object BudgetPlanner {
    fun calculate(
        limit: Long?, spent: Long, recurring: List<Recurring>, operations: List<Operation>, today: LocalDate,
        overrides: List<PaymentOccurrence> = emptyList(),
    ): BudgetPlan {
        val month = YearMonth.from(today)
        val pending = PaymentSchedule.pendingExpenses(recurring, overrides, operations, today, month.atEndOfMonth())
        val reserved = pending.sumOf { it.rule.amount }
        val available = limit?.let { it - spent - reserved }
        return BudgetPlan(reserved, pending.size, available, available?.coerceAtLeast(0)?.div(month.lengthOfMonth() - today.dayOfMonth + 1))
    }
}

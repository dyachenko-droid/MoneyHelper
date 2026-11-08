package ru.denpom.finance.app.domain.usecase

import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.model.PaymentStatus
import ru.denpom.finance.app.domain.model.PaymentOccurrence
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.domain.model.RecurrencePeriod
import ru.denpom.finance.app.domain.model.Recurring
import ru.denpom.finance.app.domain.model.ScheduledPayment
import java.time.LocalDate

object PaymentSchedule {
    fun advance(date: LocalDate, period: RecurrencePeriod): LocalDate = when (period) {
        RecurrencePeriod.WEEKLY -> date.plusWeeks(1)
        RecurrencePeriod.MONTHLY -> date.plusMonths(1)
        RecurrencePeriod.YEARLY -> date.plusYears(1)
    }

    fun dates(rule: Recurring, through: LocalDate): Sequence<LocalDate> =
        generateSequence(Period.dateOf(rule.nextDate)) { advance(it, rule.period) }
            .takeWhile { !it.isAfter(through) }

    fun between(
        rules: List<Recurring>,
        overrides: List<PaymentOccurrence>,
        from: LocalDate,
        through: LocalDate,
    ): List<ScheduledPayment> {
        val byRule = overrides.groupBy { it.recurringId }
        return rules.flatMap { rule ->
            val saved = byRule[rule.id].orEmpty().associateBy { it.scheduled }
            val regular = dates(rule, through).filter { it >= from && it !in saved }
                .map { PaymentOccurrence(rule.id, it) }
            (regular + saved.values.asSequence()).filter { it.due in from..through }
                .map { ScheduledPayment(rule, it) }.toList()
        }.sortedWith(compareBy({ it.occurrence.due }, { it.rule.id }, { it.occurrence.scheduled }))
    }

    fun pendingExpenses(
        rules: List<Recurring>,
        overrides: List<PaymentOccurrence>,
        operations: List<Operation>,
        from: LocalDate,
        through: LocalDate,
    ): List<ScheduledPayment> {
        val matched = overrides.mapNotNullTo(mutableSetOf()) { it.operationId }
        val byDate = operations.filter { it.type == OperationType.EXPENSE }
            .groupBy { Period.dateOf(it.timestamp) }
        return between(rules.filter { it.type == OperationType.EXPENSE }, overrides, from, through)
            .filter { it.occurrence.status == PaymentStatus.PENDING }
            .filter { payment ->
                val existing = byDate[payment.occurrence.due].orEmpty()
                    .firstOrNull { it.id !in matched && payment.isRecordedAsScheduled(it) }
                if (existing != null) matched += existing.id
                existing == null
            }
    }
}

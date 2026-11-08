package ru.denpom.finance.app.domain.model

import java.time.LocalDate
import java.util.Locale

enum class PaymentStatus { PENDING, PAID, SKIPPED }

data class PaymentOccurrence(
    val recurringId: Long,
    val scheduled: LocalDate,
    val due: LocalDate = scheduled,
    val status: PaymentStatus = PaymentStatus.PENDING,
    val operationId: Long? = null,
)

data class ScheduledPayment(val rule: Recurring, val occurrence: PaymentOccurrence) {
    fun canLink(operation: Operation): Boolean =
        operation.accountId == rule.accountId && operation.type == rule.type && operation.amount == rule.amount

    fun isRecordedAsScheduled(operation: Operation): Boolean =
        canLink(operation) && Period.dateOf(operation.timestamp) == occurrence.due &&
            operation.title.trim().lowercase(Locale.ROOT) == rule.title.trim().lowercase(Locale.ROOT)
}

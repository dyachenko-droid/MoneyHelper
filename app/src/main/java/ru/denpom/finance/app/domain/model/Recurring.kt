package ru.denpom.finance.app.domain.model

enum class RecurrencePeriod { WEEKLY, MONTHLY, YEARLY }

data class Recurring(
    val id: Long,
    val title: String,
    val amount: Long,
    val type: OperationType,
    val accountId: Long,
    val categoryId: Long?,
    val period: RecurrencePeriod,
    val nextDate: Long,
    val isSubscription: Boolean,
    val autoCreate: Boolean,
)

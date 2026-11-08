package ru.denpom.finance.app.domain.model

data class OperationTemplate(
    val id: Long = 0,
    val name: String,
    val type: OperationType,
    val accountId: Long,
    val categoryId: Long?,
)

data class BalanceCheck(
    val id: Long,
    val accountId: Long,
    val timestamp: Long,
    val recordedBalance: Long,
    val actualBalance: Long,
) {
    val adjustment: Long get() = actualBalance - recordedBalance
}

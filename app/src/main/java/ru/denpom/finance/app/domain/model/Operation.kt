package ru.denpom.finance.app.domain.model

enum class OperationType { EXPENSE, INCOME, TRANSFER }

data class Split(val categoryId: Long, val amount: Long)

data class Operation(
    val id: Long,
    val type: OperationType,
    val amount: Long,
    val accountId: Long,
    val toAccountId: Long?,
    val categoryId: Long?,
    val title: String,
    val note: String,
    val timestamp: Long,
    val receiptId: Long?,
    val splits: List<Split> = emptyList(),
    val searchText: String = "",
) {
    val signedAmount: Long
        get() = when (type) {
            OperationType.EXPENSE -> -amount
            OperationType.INCOME -> amount
            OperationType.TRANSFER -> 0
        }
}

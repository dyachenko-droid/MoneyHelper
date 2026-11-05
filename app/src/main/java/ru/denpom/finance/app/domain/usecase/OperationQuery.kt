package ru.denpom.finance.app.domain.usecase

import ru.denpom.finance.app.domain.model.*
import java.util.Locale

object OperationQuery {
    fun filter(operations: List<Operation>, f: OpsFilter, categories: Map<Long, Category>, accounts: Map<Long, String>): List<Operation> {
        val query = f.query.trim().lowercase(Locale.ROOT)
        return operations.filter { op ->
            (f.month == null || Period.inMonth(op.timestamp, f.month)) &&
                (f.type != TypeFilter.EXPENSE || op.type == OperationType.EXPENSE) &&
                (f.type != TypeFilter.INCOME || op.type == OperationType.INCOME) &&
                (f.accountId == null || op.accountId == f.accountId || op.toAccountId == f.accountId) &&
                (f.categoryId == null || op.categoryId == f.categoryId || op.splits.any { it.categoryId == f.categoryId }) &&
                (query.isEmpty() || listOf(op.title, op.note, op.searchText, accounts[op.accountId].orEmpty(),
                    accounts[op.toAccountId].orEmpty(), categories[op.categoryId]?.name.orEmpty(),
                    op.splits.mapNotNull { categories[it.categoryId]?.name }.joinToString(" "))
                    .joinToString(" ").lowercase(Locale.ROOT).contains(query.removePrefix("#")))
        }.sortedWith(compareByDescending<Operation> { it.timestamp }.thenByDescending { it.id })
    }

    fun summary(ops: List<Operation>, accountId: Long?): OperationsSummary {
        val transfers = ops.filter { it.type == OperationType.TRANSFER }
        return OperationsSummary(
            count = ops.size,
            expenses = ops.filter { it.type == OperationType.EXPENSE }.sumOf { it.amount },
            income = ops.filter { it.type == OperationType.INCOME }.sumOf { it.amount },
            transferCount = transfers.size,
            transferAmount = transfers.sumOf { it.amount },
            transferIn = if (accountId == null) 0 else transfers.filter { it.toAccountId == accountId && it.accountId != accountId }.sumOf { it.amount },
            transferOut = if (accountId == null) 0 else transfers.filter { it.accountId == accountId && it.toAccountId != accountId }.sumOf { it.amount },
        )
    }

    fun accountDelta(op: Operation, accountId: Long?): Long = when {
        op.type != OperationType.TRANSFER -> op.signedAmount
        accountId == null || op.accountId == op.toAccountId -> 0
        op.accountId == accountId -> -op.amount
        op.toAccountId == accountId -> op.amount
        else -> 0
    }
}

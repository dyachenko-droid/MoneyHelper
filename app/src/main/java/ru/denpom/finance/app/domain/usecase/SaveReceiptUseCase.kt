package ru.denpom.finance.app.domain.usecase

import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.model.Receipt
import ru.denpom.finance.app.domain.model.Split
import ru.denpom.finance.app.domain.repository.OperationRepository
import ru.denpom.finance.app.domain.repository.ReceiptRepository
import javax.inject.Inject

class SaveReceiptUseCase @Inject constructor(
    private val receipts: ReceiptRepository,
    private val operations: OperationRepository,
) {

    suspend operator fun invoke(receipt: Receipt, accountId: Long): Long {
        val receiptId = receipts.save(receipt)
        val existing = receipt.operationId?.let { operations.get(it) }
        val splits = splitsOf(receipt)
        val dominant = splits.maxByOrNull { it.amount }?.categoryId ?: existing?.categoryId
        val operation = Operation(
            id = existing?.id ?: 0,
            type = OperationType.EXPENSE,
            amount = receipt.total,
            accountId = accountId,
            toAccountId = null,
            categoryId = dominant,
            title = receipt.shop,
            note = existing?.note.orEmpty(),
            timestamp = receipt.timestamp,
            receiptId = receiptId,
            splits = splits,
        )
        operations.save(operation)
        return receiptId
    }

    fun splitsOf(receipt: Receipt): List<Split> {
        if (receipt.items.isEmpty()) return emptyList()
        val grouped = receipt.items.groupBy { it.categoryId }.mapValues { (_, list) -> list.sumOf { it.sum } }
        val itemsTotal = grouped.values.sum()
        val diff = receipt.total - itemsTotal
        val result = grouped.map { (id, sum) -> Split(id, sum) }.toMutableList()
        if (diff != 0L && result.isNotEmpty()) {
            val biggest = result.indices.maxBy { result[it].amount }
            result[biggest] = result[biggest].copy(amount = result[biggest].amount + diff)
        }
        return result.filter { it.amount > 0 }
    }
}

package ru.denpom.finance.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import ru.denpom.finance.app.data.db.ReceiptDao
import ru.denpom.finance.app.domain.model.Receipt
import ru.denpom.finance.app.domain.repository.ReceiptRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptRepositoryImpl @Inject constructor(
    private val dao: ReceiptDao,
) : ReceiptRepository {

    override fun observeReceipt(id: Long): Flow<Receipt?> =
        combine(dao.observe(id), dao.observeItems(id), dao.observeOperationFor(id)) { r, items, op ->
            r?.toDomain(items, op)
        }

    override fun observeReceipts(): Flow<List<Receipt>> =
        combine(dao.observeAll(), dao.observeAllItems()) { receipts, items ->
            val byReceipt = items.groupBy { it.receiptId }
            receipts.map { it.toDomain(byReceipt[it.id].orEmpty(), null) }
        }

    override suspend fun get(id: Long): Receipt? = dao.get(id)?.toDomain(dao.items(id), dao.operationFor(id))

    override suspend fun save(receipt: Receipt): Long =
        dao.saveWithItems(receipt.toEntity(), receipt.items.mapIndexed { i, it -> it.toEntity(i) })

    override suspend fun delete(id: Long) = dao.delete(id)
}

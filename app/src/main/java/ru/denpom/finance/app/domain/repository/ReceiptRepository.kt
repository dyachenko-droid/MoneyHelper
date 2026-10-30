package ru.denpom.finance.app.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.denpom.finance.app.domain.model.Receipt

interface ReceiptRepository {
    fun observeReceipt(id: Long): Flow<Receipt?>
    fun observeReceipts(): Flow<List<Receipt>>
    suspend fun get(id: Long): Receipt?
    suspend fun save(receipt: Receipt): Long
    suspend fun delete(id: Long)
}

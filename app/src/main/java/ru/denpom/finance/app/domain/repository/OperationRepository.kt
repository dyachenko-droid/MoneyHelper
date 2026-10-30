package ru.denpom.finance.app.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.denpom.finance.app.domain.model.Operation

interface OperationRepository {
    fun observeOperations(): Flow<List<Operation>>
    suspend fun get(id: Long): Operation?
    suspend fun save(operation: Operation): Long
    suspend fun delete(id: Long)
    suspend fun restore(operation: Operation)
    suspend fun all(): List<Operation>
}

package ru.denpom.finance.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import ru.denpom.finance.app.data.db.OperationDao
import ru.denpom.finance.app.data.db.SplitEntity
import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.domain.repository.OperationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OperationRepositoryImpl @Inject constructor(
    private val dao: OperationDao,
) : OperationRepository {

    override fun observeOperations(): Flow<List<Operation>> =
        combine(dao.observeAll(), dao.observeSplits()) { ops, splits ->
            val byOp = splits.groupBy { it.operationId }
            ops.map { it.toDomain(byOp[it.id].orEmpty()) }
        }

    override suspend fun get(id: Long): Operation? = dao.get(id)?.toDomain(dao.splits(id))

    override suspend fun save(operation: Operation): Long =
        dao.saveWithSplits(
            operation.toEntity(),
            operation.splits.map { SplitEntity(operationId = operation.id, categoryId = it.categoryId, amount = it.amount) },
        )

    override suspend fun saveAll(operations: List<Operation>) = dao.saveBatch(
        operations.map { it.toEntity() },
        operations.map { op -> op.splits.map { SplitEntity(operationId = op.id, categoryId = it.categoryId, amount = it.amount) } },
    )

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun restore(operation: Operation) {
        save(operation)
    }

    override suspend fun all(): List<Operation> {
        val splits = dao.allSplits().groupBy { it.operationId }
        return dao.all().map { it.toDomain(splits[it.id].orEmpty()) }
    }
}

package ru.denpom.finance.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.denpom.finance.app.data.db.RecurringDao
import ru.denpom.finance.app.domain.model.Recurring
import ru.denpom.finance.app.domain.repository.RecurringRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRepositoryImpl @Inject constructor(
    private val dao: RecurringDao,
) : RecurringRepository {

    override fun observeRecurring(): Flow<List<Recurring>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun all(): List<Recurring> = dao.all().map { it.toDomain() }

    override suspend fun save(recurring: Recurring): Long {
        val entity = recurring.toEntity()
        return if (recurring.id == 0L) dao.upsert(entity) else {
            dao.upsert(entity)
            recurring.id
        }
    }

    override suspend fun delete(id: Long) = dao.delete(id)
}

package ru.denpom.finance.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.denpom.finance.app.data.db.BudgetDao
import ru.denpom.finance.app.data.db.BudgetEntity
import ru.denpom.finance.app.domain.model.Budget
import ru.denpom.finance.app.domain.repository.BudgetRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val TOTAL_KEY = 0L

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val dao: BudgetDao,
) : BudgetRepository {

    override fun observeBudgets(): Flow<List<Budget>> = dao.observeAll().map { list ->
        list.map { Budget(it.categoryId.takeIf { id -> id != TOTAL_KEY }, it.limitAmount) }
    }

    override suspend fun save(budget: Budget) = dao.upsert(BudgetEntity(budget.categoryId ?: TOTAL_KEY, budget.limit))

    override suspend fun delete(categoryId: Long?) = dao.delete(categoryId ?: TOTAL_KEY)
}

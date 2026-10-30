package ru.denpom.finance.app.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.denpom.finance.app.domain.model.Budget

interface BudgetRepository {
    fun observeBudgets(): Flow<List<Budget>>
    suspend fun save(budget: Budget)
    suspend fun delete(categoryId: Long?)
}

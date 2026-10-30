package ru.denpom.finance.app.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.denpom.finance.app.domain.model.Recurring

interface RecurringRepository {
    fun observeRecurring(): Flow<List<Recurring>>
    suspend fun all(): List<Recurring>
    suspend fun save(recurring: Recurring): Long
    suspend fun delete(id: Long)
}

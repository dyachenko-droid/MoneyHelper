package ru.denpom.finance.app.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.denpom.finance.app.domain.model.Account

interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>>
    suspend fun save(account: Account): Long
    suspend fun delete(id: Long)
    suspend fun count(): Int
}

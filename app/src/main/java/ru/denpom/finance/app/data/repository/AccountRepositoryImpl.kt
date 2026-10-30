package ru.denpom.finance.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import ru.denpom.finance.app.data.db.AccountDao
import ru.denpom.finance.app.data.db.OperationDao
import ru.denpom.finance.app.domain.model.Account
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.repository.AccountRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val operationDao: OperationDao,
) : AccountRepository {

    override fun observeAccounts(): Flow<List<Account>> =
        combine(accountDao.observeAll(), operationDao.observeAll()) { accounts, operations ->
            val delta = HashMap<Long, Long>()
            operations.forEach { op ->
                when (OperationType.valueOf(op.type)) {
                    OperationType.EXPENSE -> delta.merge(op.accountId, -op.amount, Long::plus)
                    OperationType.INCOME -> delta.merge(op.accountId, op.amount, Long::plus)
                    OperationType.TRANSFER -> {
                        delta.merge(op.accountId, -op.amount, Long::plus)
                        op.toAccountId?.let { delta.merge(it, op.amount, Long::plus) }
                    }
                }
            }
            accounts.map { it.toDomain(it.openingBalance + (delta[it.id] ?: 0L)) }
        }

    override suspend fun save(account: Account): Long {
        val entity = account.toEntity()
        return if (account.id == 0L) accountDao.upsert(entity) else {
            accountDao.upsert(entity)
            account.id
        }
    }

    override suspend fun delete(id: Long) = accountDao.delete(id)

    override suspend fun count(): Int = accountDao.count()
}

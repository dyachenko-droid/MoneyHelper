package ru.denpom.finance.app.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.map
import ru.denpom.finance.app.data.db.AppDatabase
import ru.denpom.finance.app.data.db.BalanceCheckEntity
import ru.denpom.finance.app.data.db.OperationTemplateEntity
import ru.denpom.finance.app.domain.model.BalanceCheck
import ru.denpom.finance.app.domain.model.CategoryKind
import ru.denpom.finance.app.domain.model.OperationTemplate
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.repository.OperationRepository
import ru.denpom.finance.app.domain.usecase.OperationQuery
import javax.inject.Inject

class BalanceCheckRepository @Inject constructor(
    private val db: AppDatabase,
    private val operations: OperationRepository,
) {
    val checks = db.balanceCheckDao().observeAll().map { rows ->
        rows.map { BalanceCheck(it.id, it.accountId, it.timestamp, it.recordedBalance, it.actualBalance) }
    }

    suspend fun save(accountId: Long, expected: Long, actual: Long, adjust: Boolean) = db.withTransaction {
        val accounts = db.accountDao()
        val account = requireNotNull(accounts.get(accountId)) { "Счёт удалён" }
        val recorded = operations.all().fold(account.openingBalance) { balance, operation ->
            if (operation.accountId == accountId || (operation.type == OperationType.TRANSFER && operation.toAccountId == accountId)) {
                Math.addExact(balance, OperationQuery.accountDelta(operation, accountId))
            } else balance
        }
        require(recorded == expected) { "Баланс изменился. Сверьте суммы заново" }
        val adjustment = Math.subtractExact(actual, recorded)
        require(adjust || adjustment == 0L) { "Подтвердите корректировку остатка" }
        if (adjustment != 0L) accounts.updateOpeningBalance(accountId, Math.addExact(account.openingBalance, adjustment))
        db.balanceCheckDao().insert(BalanceCheckEntity(
            accountId = accountId, timestamp = System.currentTimeMillis(),
            recordedBalance = recorded, actualBalance = actual,
        ))
    }
}

class OperationTemplateRepository @Inject constructor(private val db: AppDatabase) {
    private val dao = db.operationTemplateDao()
    val templates = dao.observeAll().map { rows ->
        rows.map { OperationTemplate(it.id, it.name, OperationType.valueOf(it.type), it.accountId, it.categoryId) }
    }

    suspend fun save(template: OperationTemplate) = db.withTransaction {
        val name = template.name.trim()
        require(name.isNotEmpty() && name.length <= 80) { "Введите название до 80 символов" }
        require(template.type != OperationType.TRANSFER) { "Шаблон поддерживает расходы и доходы" }
        require(db.accountDao().get(template.accountId) != null) { "Выберите существующий счёт" }
        template.categoryId?.let { id ->
            val expected = if (template.type == OperationType.EXPENSE) CategoryKind.EXPENSE else CategoryKind.INCOME
            require(db.categoryDao().all().any { it.id == id && it.kind == expected.name }) { "Выберите категорию" }
        }
        dao.save(OperationTemplateEntity(template.id, name, template.type.name, template.accountId, template.categoryId))
    }

    suspend fun delete(id: Long) = dao.delete(id)
}

package ru.denpom.finance.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ru.denpom.finance.app.data.db.DebtDao
import ru.denpom.finance.app.data.db.DebtEntity
import ru.denpom.finance.app.data.db.DebtPaymentEntity
import ru.denpom.finance.app.data.db.GoalDao
import ru.denpom.finance.app.data.db.GoalEntity
import ru.denpom.finance.app.data.db.GoalEntryEntity
import ru.denpom.finance.app.data.db.LoanEntity
import ru.denpom.finance.app.domain.model.Debt
import ru.denpom.finance.app.domain.model.DebtDirection
import ru.denpom.finance.app.domain.model.Goal
import ru.denpom.finance.app.domain.model.GoalEntry
import ru.denpom.finance.app.domain.model.Loan
import ru.denpom.finance.app.domain.repository.DebtRepository
import ru.denpom.finance.app.domain.repository.GoalRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepositoryImpl @Inject constructor(private val dao: GoalDao) : GoalRepository {

    override fun observeGoals(): Flow<List<Goal>> = combine(dao.observeGoals(), dao.observeEntries()) { goals, entries ->
        val saved = entries.groupBy { it.goalId }.mapValues { (_, list) -> list.sumOf { it.amount } }
        goals.map { Goal(it.id, it.name, it.target, it.deadline, it.icon, it.hue, it.createdAt, saved[it.id] ?: 0) }
    }

    override fun observeEntries(): Flow<List<GoalEntry>> =
        dao.observeEntries().map { list -> list.map { GoalEntry(it.id, it.goalId, it.amount, it.timestamp, it.operationId) } }

    override suspend fun save(goal: Goal): Long {
        val entity = GoalEntity(goal.id, goal.name, goal.target, goal.deadline, goal.icon, goal.hue, goal.createdAt)
        return if (goal.id == 0L) dao.upsert(entity) else {
            dao.upsert(entity)
            goal.id
        }
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun addEntry(goalId: Long, amount: Long, operationId: Long?) {
        dao.insertEntry(GoalEntryEntity(goalId = goalId, amount = amount, timestamp = System.currentTimeMillis(), operationId = operationId))
    }
}

@Singleton
class DebtRepositoryImpl @Inject constructor(private val dao: DebtDao) : DebtRepository {

    override fun observeDebts(): Flow<List<Debt>> = combine(dao.observeDebts(), dao.observePayments()) { debts, payments ->
        val paid = payments.groupBy { it.debtId }.mapValues { (_, list) -> list.sumOf { it.amount } }
        debts.map { Debt(it.id, it.person, DebtDirection.valueOf(it.direction), it.amount, it.note, it.createdAt, it.dueDate, paid[it.id] ?: 0) }
    }

    override suspend fun save(debt: Debt): Long {
        val entity = debt.toEntity()
        return if (debt.id == 0L) dao.upsert(entity) else {
            dao.upsert(entity)
            debt.id
        }
    }

    override suspend fun saveAll(debts: List<Debt>) = dao.insertAll(debts.map { it.toEntity() })

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun pay(debtId: Long, amount: Long) {
        dao.insertPayment(DebtPaymentEntity(debtId = debtId, amount = amount, timestamp = System.currentTimeMillis()))
    }

    override fun observeLoans(): Flow<List<Loan>> = dao.observeLoans().map { list ->
        list.map { Loan(it.id, it.name, it.principal, it.ratePercent, it.termMonths, it.startDate, it.recurringId) }
    }

    override suspend fun saveLoan(loan: Loan): Long {
        val entity = LoanEntity(loan.id, loan.name, loan.principal, loan.ratePercent, loan.termMonths, loan.startDate, loan.recurringId)
        return if (loan.id == 0L) dao.upsertLoan(entity) else {
            dao.upsertLoan(entity)
            loan.id
        }
    }

    override suspend fun deleteLoan(id: Long) = dao.deleteLoan(id)

    private fun Debt.toEntity() = DebtEntity(id, person, direction.name, amount, note, createdAt, dueDate)
}

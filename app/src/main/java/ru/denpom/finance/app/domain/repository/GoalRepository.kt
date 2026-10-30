package ru.denpom.finance.app.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.denpom.finance.app.domain.model.Debt
import ru.denpom.finance.app.domain.model.Goal
import ru.denpom.finance.app.domain.model.GoalEntry
import ru.denpom.finance.app.domain.model.Loan

interface GoalRepository {
    fun observeGoals(): Flow<List<Goal>>
    fun observeEntries(): Flow<List<GoalEntry>>
    suspend fun save(goal: Goal): Long
    suspend fun delete(id: Long)
    suspend fun addEntry(goalId: Long, amount: Long, operationId: Long?)
}

interface DebtRepository {
    fun observeDebts(): Flow<List<Debt>>
    suspend fun save(debt: Debt): Long
    suspend fun saveAll(debts: List<Debt>)
    suspend fun delete(id: Long)
    suspend fun pay(debtId: Long, amount: Long)
    fun observeLoans(): Flow<List<Loan>>
    suspend fun saveLoan(loan: Loan): Long
    suspend fun deleteLoan(id: Long)
}

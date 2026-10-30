package ru.denpom.finance.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY sort, id")
    fun observeAll(): Flow<List<AccountEntity>>

    @Upsert
    suspend fun upsert(entity: AccountEntity): Long

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sort, id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sort, id")
    suspend fun all(): List<CategoryEntity>

    @Upsert
    suspend fun upsert(entity: CategoryEntity): Long

    @Insert
    suspend fun insertAll(entities: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT * FROM rules ORDER BY pattern")
    fun observeRules(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules")
    suspend fun rules(): List<RuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity)

    @Query("DELETE FROM rules WHERE pattern = :pattern")
    suspend fun deleteRule(pattern: String)

    @Query("DELETE FROM rules WHERE categoryId = :categoryId")
    suspend fun deleteRulesFor(categoryId: Long)
}

data class OperationWithSearch(
    val id: Long,
    val type: String,
    val amount: Long,
    val accountId: Long,
    val toAccountId: Long?,
    val categoryId: Long?,
    val title: String,
    val note: String,
    val timestamp: Long,
    val receiptId: Long?,
    val items: String?,
)

@Dao
interface OperationDao {
    @Query(
        """
        SELECT o.*, (SELECT group_concat(name, ' ') FROM receipt_items WHERE receiptId = o.receiptId) AS items
        FROM operations o ORDER BY o.timestamp DESC, o.id DESC
        """
    )
    fun observeAll(): Flow<List<OperationWithSearch>>

    @Query("SELECT * FROM splits")
    fun observeSplits(): Flow<List<SplitEntity>>

    @Query("SELECT * FROM operations ORDER BY timestamp DESC, id DESC")
    suspend fun all(): List<OperationEntity>

    @Query("SELECT * FROM splits")
    suspend fun allSplits(): List<SplitEntity>

    @Query("SELECT * FROM operations WHERE id = :id")
    suspend fun get(id: Long): OperationEntity?

    @Query("SELECT * FROM splits WHERE operationId = :id")
    suspend fun splits(id: Long): List<SplitEntity>

    @Upsert
    suspend fun upsert(entity: OperationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OperationEntity): Long

    @Insert
    suspend fun insertSplits(splits: List<SplitEntity>)

    @Query("DELETE FROM splits WHERE operationId = :id")
    suspend fun deleteSplits(id: Long)

    @Query("DELETE FROM operations WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE operations SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategory(categoryId: Long)

    @Transaction
    suspend fun saveWithSplits(entity: OperationEntity, splits: List<SplitEntity>): Long {
        val id = if (entity.id == 0L) insert(entity) else {
            upsert(entity)
            entity.id
        }
        deleteSplits(id)
        insertSplits(splits.map { it.copy(id = 0, operationId = id) })
        return id
    }
}

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts WHERE id = :id")
    fun observe(id: Long): Flow<ReceiptEntity?>

    @Query("SELECT * FROM receipts ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipt_items ORDER BY receiptId, position")
    fun observeAllItems(): Flow<List<ReceiptItemEntity>>

    @Query("SELECT * FROM receipt_items WHERE receiptId = :id ORDER BY position")
    fun observeItems(id: Long): Flow<List<ReceiptItemEntity>>

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun get(id: Long): ReceiptEntity?

    @Query("SELECT * FROM receipt_items WHERE receiptId = :id ORDER BY position")
    suspend fun items(id: Long): List<ReceiptItemEntity>

    @Query("SELECT id FROM operations WHERE receiptId = :id LIMIT 1")
    suspend fun operationFor(id: Long): Long?

    @Query("SELECT id FROM operations WHERE receiptId = :id LIMIT 1")
    fun observeOperationFor(id: Long): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReceiptEntity): Long

    @Insert
    suspend fun insertItems(items: List<ReceiptItemEntity>)

    @Query("DELETE FROM receipt_items WHERE receiptId = :id")
    suspend fun deleteItems(id: Long)

    @Query("DELETE FROM receipts WHERE id = :id")
    suspend fun delete(id: Long)

    @Transaction
    suspend fun saveWithItems(entity: ReceiptEntity, items: List<ReceiptItemEntity>): Long {
        val id = insert(entity)
        deleteItems(id)
        insertItems(items.mapIndexed { i, it -> it.copy(id = 0, receiptId = id, position = i) })
        return id
    }
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Upsert
    suspend fun upsert(entity: BudgetEntity)

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId")
    suspend fun delete(categoryId: Long)
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring ORDER BY nextDate")
    fun observeAll(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring ORDER BY nextDate")
    suspend fun all(): List<RecurringEntity>

    @Upsert
    suspend fun upsert(entity: RecurringEntity): Long

    @Query("DELETE FROM recurring WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdAt")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goal_entries ORDER BY timestamp DESC")
    fun observeEntries(): Flow<List<GoalEntryEntity>>

    @Upsert
    suspend fun upsert(goal: GoalEntity): Long

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert
    suspend fun insertEntry(entry: GoalEntryEntity): Long
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY createdAt DESC")
    fun observeDebts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debt_payments ORDER BY timestamp DESC")
    fun observePayments(): Flow<List<DebtPaymentEntity>>

    @Upsert
    suspend fun upsert(debt: DebtEntity): Long

    @Insert
    suspend fun insertAll(debts: List<DebtEntity>)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert
    suspend fun insertPayment(payment: DebtPaymentEntity): Long

    @Query("SELECT * FROM loans ORDER BY startDate")
    fun observeLoans(): Flow<List<LoanEntity>>

    @Upsert
    suspend fun upsertLoan(loan: LoanEntity): Long

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteLoan(id: Long)
}

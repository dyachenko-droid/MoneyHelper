package ru.denpom.finance.app.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "payment_occurrences",
    foreignKeys = [ForeignKey(entity = RecurringEntity::class, parentColumns = ["id"], childColumns = ["recurringId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["recurringId", "scheduledDay"], unique = true), Index(value = ["operationId"], unique = true)],
)
data class PaymentOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recurringId: Long,
    val scheduledDay: Long,
    val dueDay: Long,
    val status: String,
    val operationId: Long?,
)

@Dao
interface PaymentOccurrenceDao {
    @Query("SELECT * FROM payment_occurrences")
    fun observeAll(): Flow<List<PaymentOccurrenceEntity>>

    @Query("SELECT * FROM payment_occurrences WHERE recurringId = :id")
    suspend fun forSchedule(id: Long): List<PaymentOccurrenceEntity>

    @Query("SELECT * FROM payment_occurrences WHERE recurringId = :id AND scheduledDay = :day")
    suspend fun find(id: Long, day: Long): PaymentOccurrenceEntity?

    @Query("SELECT operationId FROM payment_occurrences WHERE operationId IS NOT NULL")
    suspend fun linkedOperations(): List<Long>

    @Upsert
    suspend fun save(entry: PaymentOccurrenceEntity)
}

@Entity(
    tableName = "balance_checks",
    foreignKeys = [ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("accountId")],
)
data class BalanceCheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val timestamp: Long,
    val recordedBalance: Long,
    val actualBalance: Long,
)

@Dao
interface BalanceCheckDao {
    @Query("SELECT * FROM balance_checks ORDER BY timestamp DESC, id DESC")
    fun observeAll(): Flow<List<BalanceCheckEntity>>

    @Insert
    suspend fun insert(check: BalanceCheckEntity)
}

@Entity(
    tableName = "operation_templates",
    foreignKeys = [ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("accountId")],
)
data class OperationTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val accountId: Long,
    val categoryId: Long?,
)

@Dao
interface OperationTemplateDao {
    @Query("SELECT * FROM operation_templates ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<OperationTemplateEntity>>

    @Upsert
    suspend fun save(template: OperationTemplateEntity)

    @Query("DELETE FROM operation_templates WHERE id = :id")
    suspend fun delete(id: Long)
}

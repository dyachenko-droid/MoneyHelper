package ru.denpom.finance.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val subtitle: String,
    val openingBalance: Long,
    val creditLimit: Long?,
    val sort: Int = 0,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val hue: Float,
    val kind: String,
    val sort: Int,
)

@Entity(
    tableName = "operations",
    indices = [Index("timestamp"), Index("accountId"), Index("receiptId")],
)
data class OperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Long,
    val accountId: Long,
    val toAccountId: Long?,
    val categoryId: Long?,
    val title: String,
    val note: String,
    val timestamp: Long,
    val receiptId: Long?,
)

@Entity(
    tableName = "splits",
    foreignKeys = [ForeignKey(
        entity = OperationEntity::class,
        parentColumns = ["id"],
        childColumns = ["operationId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("operationId"), Index("categoryId")],
)
data class SplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operationId: Long,
    val categoryId: Long,
    val amount: Long,
)

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shop: String,
    val address: String,
    val timestamp: Long,
    val total: Long,
    val qrRaw: String?,
    val status: String,
    val photoPath: String?,
    val warrantyUntil: Long?,
)

@Entity(
    tableName = "receipt_items",
    foreignKeys = [ForeignKey(
        entity = ReceiptEntity::class,
        parentColumns = ["id"],
        childColumns = ["receiptId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("receiptId")],
)
data class ReceiptItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptId: Long,
    val position: Int,
    val name: String,
    val quantity: Double,
    val unit: String,
    val price: Long,
    val sum: Long,
    val categoryId: Long,
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val categoryId: Long,
    val limitAmount: Long,
)

@Entity(tableName = "recurring")
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Long,
    val type: String,
    val accountId: Long,
    val categoryId: Long?,
    val period: String,
    val nextDate: Long,
    val isSubscription: Boolean,
    val autoCreate: Boolean,
)

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey val pattern: String,
    val categoryId: Long,
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: Long,
    val deadline: Long?,
    val icon: String,
    val hue: Float,
    val createdAt: Long,
)

@Entity(
    tableName = "goal_entries",
    foreignKeys = [ForeignKey(
        entity = GoalEntity::class,
        parentColumns = ["id"],
        childColumns = ["goalId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("goalId")],
)
data class GoalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val amount: Long,
    val timestamp: Long,
    val operationId: Long?,
)

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val person: String,
    val direction: String,
    val amount: Long,
    val note: String,
    val createdAt: Long,
    val dueDate: Long?,
)

@Entity(
    tableName = "debt_payments",
    foreignKeys = [ForeignKey(
        entity = DebtEntity::class,
        parentColumns = ["id"],
        childColumns = ["debtId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("debtId")],
)
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val amount: Long,
    val timestamp: Long,
)

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val principal: Long,
    val ratePercent: Double,
    val termMonths: Int,
    val startDate: Long,
    val recurringId: Long?,
)

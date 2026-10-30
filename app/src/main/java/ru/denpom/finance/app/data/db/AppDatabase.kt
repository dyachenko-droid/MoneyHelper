package ru.denpom.finance.app.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        OperationEntity::class,
        SplitEntity::class,
        ReceiptEntity::class,
        ReceiptItemEntity::class,
        BudgetEntity::class,
        RecurringEntity::class,
        RuleEntity::class,
        GoalEntity::class,
        GoalEntryEntity::class,
        DebtEntity::class,
        DebtPaymentEntity::class,
        LoanEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun operationDao(): OperationDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringDao(): RecurringDao
    abstract fun goalDao(): GoalDao
    abstract fun debtDao(): DebtDao

    companion object {
        const val NAME = "money.db"
    }
}

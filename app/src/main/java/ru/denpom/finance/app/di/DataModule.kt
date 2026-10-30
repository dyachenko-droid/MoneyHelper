package ru.denpom.finance.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.denpom.finance.app.data.db.AppDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun accountDao(db: AppDatabase) = db.accountDao()

    @Provides
    fun categoryDao(db: AppDatabase) = db.categoryDao()

    @Provides
    fun operationDao(db: AppDatabase) = db.operationDao()

    @Provides
    fun receiptDao(db: AppDatabase) = db.receiptDao()

    @Provides
    fun budgetDao(db: AppDatabase) = db.budgetDao()

    @Provides
    fun recurringDao(db: AppDatabase) = db.recurringDao()

    @Provides
    fun goalDao(db: AppDatabase) = db.goalDao()

    @Provides
    fun debtDao(db: AppDatabase) = db.debtDao()
}

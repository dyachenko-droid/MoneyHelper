package ru.denpom.finance.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.denpom.finance.app.data.receipt.CsvStatementReader
import ru.denpom.finance.app.data.receipt.MlKitQrDecoder
import ru.denpom.finance.app.data.receipt.FiscalQrParser
import ru.denpom.finance.app.data.receipt.TesseractRecognizer
import ru.denpom.finance.app.data.repository.AccountRepositoryImpl
import ru.denpom.finance.app.data.repository.BackupRepositoryImpl
import ru.denpom.finance.app.data.repository.BudgetRepositoryImpl
import ru.denpom.finance.app.data.repository.CategoryRepositoryImpl
import ru.denpom.finance.app.data.repository.DebtRepositoryImpl
import ru.denpom.finance.app.data.repository.GoalRepositoryImpl
import ru.denpom.finance.app.data.repository.OperationRepositoryImpl
import ru.denpom.finance.app.data.repository.ReceiptRepositoryImpl
import ru.denpom.finance.app.data.repository.RecurringRepositoryImpl
import ru.denpom.finance.app.data.settings.SettingsRepositoryImpl
import ru.denpom.finance.app.domain.repository.AccountRepository
import ru.denpom.finance.app.domain.repository.BackupRepository
import ru.denpom.finance.app.domain.repository.BudgetRepository
import ru.denpom.finance.app.domain.repository.CategoryRepository
import ru.denpom.finance.app.domain.repository.DebtRepository
import ru.denpom.finance.app.domain.repository.GoalRepository
import ru.denpom.finance.app.domain.repository.FiscalQrReader
import ru.denpom.finance.app.domain.repository.OperationRepository
import ru.denpom.finance.app.domain.repository.QrImageDecoder
import ru.denpom.finance.app.domain.repository.ReceiptRecognizer
import ru.denpom.finance.app.domain.repository.ReceiptRepository
import ru.denpom.finance.app.domain.repository.RecurringRepository
import ru.denpom.finance.app.domain.repository.SettingsRepository
import ru.denpom.finance.app.domain.repository.StatementReader

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun accounts(impl: AccountRepositoryImpl): AccountRepository
    @Binds abstract fun categories(impl: CategoryRepositoryImpl): CategoryRepository
    @Binds abstract fun operations(impl: OperationRepositoryImpl): OperationRepository
    @Binds abstract fun receipts(impl: ReceiptRepositoryImpl): ReceiptRepository
    @Binds abstract fun budgets(impl: BudgetRepositoryImpl): BudgetRepository
    @Binds abstract fun recurring(impl: RecurringRepositoryImpl): RecurringRepository
    @Binds abstract fun settings(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds abstract fun fiscalQr(impl: FiscalQrParser): FiscalQrReader
    @Binds abstract fun recognizer(impl: TesseractRecognizer): ReceiptRecognizer
    @Binds abstract fun backup(impl: BackupRepositoryImpl): BackupRepository
    @Binds abstract fun goals(impl: GoalRepositoryImpl): GoalRepository
    @Binds abstract fun debts(impl: DebtRepositoryImpl): DebtRepository
    @Binds abstract fun statementReader(impl: CsvStatementReader): StatementReader
    @Binds abstract fun qrDecoder(impl: MlKitQrDecoder): QrImageDecoder
}

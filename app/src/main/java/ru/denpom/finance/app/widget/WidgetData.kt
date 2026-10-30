package ru.denpom.finance.app.widget

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.repository.AccountRepository
import ru.denpom.finance.app.domain.repository.BudgetRepository
import ru.denpom.finance.app.domain.repository.CategoryRepository
import ru.denpom.finance.app.domain.repository.OperationRepository
import ru.denpom.finance.app.domain.repository.SettingsRepository
import ru.denpom.finance.app.domain.usecase.SpendingCalculator
import ru.denpom.finance.app.presentation.format.MoneyFormat
import java.time.LocalDate
import java.time.YearMonth

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun accounts(): AccountRepository
    fun operations(): OperationRepository
    fun budgets(): BudgetRepository
    fun categories(): CategoryRepository
    fun settings(): SettingsRepository
    fun calculator(): SpendingCalculator
}

data class WidgetOp(val title: String, val amount: String, val income: Boolean)

data class WidgetSnapshot(
    val fmt: MoneyFormat,
    val total: Long,
    val month: YearMonth,
    val limit: Long?,
    val spent: Long,
    val daysLeft: Int,
    val recent: List<WidgetOp>,
    val locked: Boolean,
)

suspend fun loadWidgetSnapshot(context: Context): WidgetSnapshot {
    val deps = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
    val settings = deps.settings().current()
    val fmt = MoneyFormat(settings.currency.symbol, settings.hideAmounts || settings.pinEnabled)
    val ops = deps.operations().all()
    val today = LocalDate.now()
    val month = YearMonth.from(today)
    val cats = deps.categories().categories().associateBy { it.id }
    return WidgetSnapshot(
        fmt = fmt,
        total = deps.accounts().observeAccounts().first().sumOf { it.balance },
        month = month,
        limit = deps.budgets().observeBudgets().first().firstOrNull { it.categoryId == null }?.limit,
        spent = deps.calculator().totalSpent(ops, month),
        daysLeft = month.lengthOfMonth() - today.dayOfMonth + 1,
        recent = ops.filter { it.type != OperationType.TRANSFER }.take(3).map { op: Operation ->
            WidgetOp(op.title.ifBlank { op.categoryId?.let { cats[it]?.name } ?: "Операция" }, fmt.signed(op.signedAmount, false), op.type == OperationType.INCOME)
        },
        locked = settings.pinEnabled,
    )
}

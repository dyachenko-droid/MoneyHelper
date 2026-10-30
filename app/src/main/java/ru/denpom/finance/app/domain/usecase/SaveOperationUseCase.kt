package ru.denpom.finance.app.domain.usecase

import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.domain.model.RecurrencePeriod
import ru.denpom.finance.app.domain.model.Recurring
import ru.denpom.finance.app.domain.model.Split
import ru.denpom.finance.app.domain.repository.OperationRepository
import ru.denpom.finance.app.domain.repository.RecurringRepository
import javax.inject.Inject

class SaveOperationUseCase @Inject constructor(
    private val operations: OperationRepository,
    private val recurring: RecurringRepository,
) {

    suspend operator fun invoke(
        operation: Operation,
        repeatMonthly: Boolean = false,
    ): Long {
        val keepSplits = operation.id != 0L && operation.splits.size > 1 &&
            operation.splits.sumOf { it.amount } == operation.amount
        val splits = when {
            operation.type == OperationType.TRANSFER -> emptyList()
            keepSplits -> operation.splits
            operation.categoryId != null -> listOf(Split(operation.categoryId, operation.amount))
            else -> emptyList()
        }
        val id = operations.save(operation.copy(splits = splits))
        if (repeatMonthly && operation.type != OperationType.TRANSFER) {
            val next = Period.dateOf(operation.timestamp).plusMonths(1)
            recurring.save(
                Recurring(
                    id = 0,
                    title = operation.title,
                    amount = operation.amount,
                    type = operation.type,
                    accountId = operation.accountId,
                    categoryId = operation.categoryId,
                    period = RecurrencePeriod.MONTHLY,
                    nextDate = Period.startOfDay(next),
                    isSubscription = false,
                    autoCreate = true,
                )
            )
        }
        return id
    }
}

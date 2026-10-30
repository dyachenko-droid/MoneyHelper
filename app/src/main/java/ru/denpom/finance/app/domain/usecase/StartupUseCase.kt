package ru.denpom.finance.app.domain.usecase

import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.domain.model.RecurrencePeriod
import ru.denpom.finance.app.domain.model.Recurring
import ru.denpom.finance.app.domain.model.Split
import ru.denpom.finance.app.domain.repository.CategoryRepository
import ru.denpom.finance.app.domain.repository.OperationRepository
import ru.denpom.finance.app.domain.repository.RecurringRepository
import java.time.LocalDate
import javax.inject.Inject

class StartupUseCase @Inject constructor(
    private val categories: CategoryRepository,
    private val recurring: RecurringRepository,
    private val operations: OperationRepository,
) {

    suspend fun run() {
        categories.ensureDefaults()
        materializeRecurring()
    }

    suspend fun materializeRecurring() {
        val today = Period.startOfDay(LocalDate.now())
        recurring.all().filter { it.autoCreate }.forEach { r ->
            var next = r.nextDate
            var guard = 0
            while (next <= today && guard < 24) {
                operations.save(
                    Operation(
                        id = 0,
                        type = r.type,
                        amount = r.amount,
                        accountId = r.accountId,
                        toAccountId = null,
                        categoryId = r.categoryId,
                        title = r.title,
                        note = "",
                        timestamp = next + 12 * 3600 * 1000L,
                        receiptId = null,
                        splits = r.categoryId?.let { listOf(Split(it, r.amount)) }.orEmpty(),
                    )
                )
                next = advance(r, next)
                guard++
            }
            if (next != r.nextDate) recurring.save(r.copy(nextDate = next))
        }
    }

    fun advance(r: Recurring, from: Long): Long {
        val date = Period.dateOf(from)
        val next = when (r.period) {
            RecurrencePeriod.WEEKLY -> date.plusWeeks(1)
            RecurrencePeriod.MONTHLY -> date.plusMonths(1)
            RecurrencePeriod.YEARLY -> date.plusYears(1)
        }
        return Period.startOfDay(next)
    }
}

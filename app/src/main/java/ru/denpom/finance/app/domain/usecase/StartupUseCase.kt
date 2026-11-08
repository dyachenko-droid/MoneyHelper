package ru.denpom.finance.app.domain.usecase

import ru.denpom.finance.app.data.repository.PaymentCalendarRepository
import ru.denpom.finance.app.domain.repository.CategoryRepository
import javax.inject.Inject

class StartupUseCase @Inject constructor(
    private val categories: CategoryRepository,
    private val calendar: PaymentCalendarRepository,
) {
    suspend fun run() {
        categories.ensureDefaults()
        materializeRecurring()
    }

    suspend fun materializeRecurring() = calendar.materializeAutomatic()
}

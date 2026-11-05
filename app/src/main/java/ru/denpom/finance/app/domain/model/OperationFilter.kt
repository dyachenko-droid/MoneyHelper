package ru.denpom.finance.app.domain.model

import java.time.YearMonth

enum class TypeFilter(val label: String) { ALL("Все типы"), EXPENSE("Расходы"), INCOME("Доходы") }

data class OpsFilter(
    val query: String = "",
    val month: YearMonth? = YearMonth.now(),
    val type: TypeFilter = TypeFilter.ALL,
    val accountId: Long? = null,
    val categoryId: Long? = null,
)

data class OperationsSummary(
    val count: Int = 0,
    val expenses: Long = 0,
    val income: Long = 0,
    val transferCount: Int = 0,
    val transferAmount: Long = 0,
    val transferIn: Long = 0,
    val transferOut: Long = 0,
) {
    val net: Long get() = income - expenses + transferIn - transferOut
}

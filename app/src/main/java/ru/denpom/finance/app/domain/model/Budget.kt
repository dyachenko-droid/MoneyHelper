package ru.denpom.finance.app.domain.model

data class Budget(val categoryId: Long?, val limit: Long)

data class BudgetProgress(
    val category: Category,
    val limit: Long,
    val spent: Long,
) {
    val ratio: Float get() = if (limit <= 0) 0f else spent.toFloat() / limit
}

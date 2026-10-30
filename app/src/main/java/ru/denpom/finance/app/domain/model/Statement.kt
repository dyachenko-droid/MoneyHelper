package ru.denpom.finance.app.domain.model

data class StatementRow(
    val timestamp: Long,
    val amount: Long,
    val description: String,
    val bankCategory: String,
) {
    val isTransfer: Boolean
        get() = bankCategory.contains("перевод", true) || description.startsWith("Перевод", true) ||
            bankCategory.contains("пополнен", true)
}

data class ImportPreview(
    val rows: List<StatementRow>,
    val duplicates: Int,
)

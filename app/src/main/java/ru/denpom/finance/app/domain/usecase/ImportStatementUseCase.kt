package ru.denpom.finance.app.domain.usecase

import ru.denpom.finance.app.domain.model.CategoryKind
import ru.denpom.finance.app.domain.model.ImportPreview
import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.domain.model.Split
import ru.denpom.finance.app.domain.model.StatementRow
import ru.denpom.finance.app.domain.repository.CategoryRepository
import ru.denpom.finance.app.domain.repository.OperationRepository
import ru.denpom.finance.app.domain.repository.StatementReader
import javax.inject.Inject
import kotlin.math.abs

class ImportStatementUseCase @Inject constructor(
    private val reader: StatementReader,
    private val operations: OperationRepository,
    private val categories: CategoryRepository,
    private val categorize: CategorizeItemsUseCase,
) {

    private val bankMap = listOf(
        "shopping_basket" to listOf("супермаркет", "продукт"),
        "local_cafe" to listOf("ресторан", "фастфуд", "кафе", "кофе"),
        "local_taxi" to listOf("такси", "транспорт", "азс", "топливо", "каршеринг", "авто"),
        "shopping_bag" to listOf("одежда", "маркетплейс", "онлайн", "электроника", "товары", "дом и ремонт", "спорттовары"),
        "medication" to listOf("аптек", "медицин", "здоровье"),
        "spa" to listOf("красота", "косметика"),
        "theaters" to listOf("развлечен", "кино", "книги", "музыка"),
        "smartphone" to listOf("связь", "мобильн", "интернет"),
        "autorenew" to listOf("цифровые", "подписк"),
        "pets" to listOf("животн"),
        "child_care" to listOf("дет"),
        "redeem" to listOf("подарки", "цветы"),
        "chair" to listOf("жкх", "коммунал", "хозтовары"),
    )

    suspend fun preview(source: String): ImportPreview {
        val rows = reader.read(source)
        val existing = operations.all().map { key(it.timestamp, it.signedAmount, it.title) }.toSet()
        return ImportPreview(rows, rows.count { key(it.timestamp, it.amount, it.description) in existing })
    }

    suspend fun commit(rows: List<StatementRow>, accountId: Long, skipTransfers: Boolean): Int {
        val existing = operations.all().map { key(it.timestamp, it.signedAmount, it.title) }.toMutableSet()
        val cats = categories.categories()
        val expense = cats.filter { it.kind == CategoryKind.EXPENSE }
        val income = cats.filter { it.kind == CategoryKind.INCOME }
        var count = 0
        rows.forEach { row ->
            if (skipTransfers && row.isTransfer) return@forEach
            val k = key(row.timestamp, row.amount, row.description)
            if (!existing.add(k)) return@forEach
            val isIncome = row.amount > 0
            val categoryId = if (isIncome) {
                val lower = (row.bankCategory + " " + row.description).lowercase()
                when {
                    lower.contains("кэшбэк") || lower.contains("кешбэк") || lower.contains("бонус") || lower.contains("процент") -> income.firstOrNull { it.icon == "percent" }
                    lower.contains("зарплат") || lower.contains("заработн") -> income.firstOrNull { it.icon == "payments" }
                    else -> null
                }?.id ?: income.firstOrNull()?.id
            } else {
                val lower = row.bankCategory.lowercase()
                bankMap.firstOrNull { (_, words) -> words.any { lower.contains(it) } }?.let { (icon, _) -> expense.firstOrNull { it.icon == icon }?.id }
                    ?: categorize.categoryForTitle(row.description)
                    ?: expense.firstOrNull { it.icon == "shopping_bag" }?.id
            }
            val amount = abs(row.amount)
            operations.save(
                Operation(
                    id = 0,
                    type = if (isIncome) OperationType.INCOME else OperationType.EXPENSE,
                    amount = amount,
                    accountId = accountId,
                    toAccountId = null,
                    categoryId = categoryId,
                    title = row.description.ifBlank { row.bankCategory.ifBlank { "Операция" } },
                    note = "",
                    timestamp = row.timestamp,
                    receiptId = null,
                    splits = categoryId?.let { listOf(Split(it, amount)) }.orEmpty(),
                )
            )
            count++
        }
        return count
    }

    private fun key(timestamp: Long, signed: Long, title: String) =
        "${Period.dateOf(timestamp)}|$signed|${title.trim().lowercase()}"
}

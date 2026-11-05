package ru.denpom.finance.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import ru.denpom.finance.app.domain.model.*
import ru.denpom.finance.app.domain.repository.*
import ru.denpom.finance.app.domain.usecase.*
import java.time.LocalDate

class ImportStatementTest {
    private val date = Period.startOfDay(LocalDate.of(2026, 11, 5))
    private class Operations : OperationRepository {
        val data = MutableStateFlow<List<Operation>>(emptyList())
        var batches = 0
        override fun observeOperations() = data
        override suspend fun all() = data.value
        override suspend fun get(id: Long) = data.value.firstOrNull { it.id == id }
        override suspend fun save(operation: Operation): Long { val id = data.value.size + 1L; data.value += operation.copy(id = id); return id }
        override suspend fun saveAll(operations: List<Operation>) { batches++; operations.forEach { save(it) } }
        override suspend fun delete(id: Long) { data.value = data.value.filterNot { it.id == id } }
        override suspend fun restore(operation: Operation) { save(operation) }
    }
    private val cats = object : CategoryRepository {
        override fun observeCategories() = flowOf(emptyList<Category>())
        override suspend fun categories() = emptyList<Category>()
        override suspend fun save(category: Category) = 1L
        override suspend fun delete(id: Long) = Unit
        override suspend fun ensureDefaults() = Unit
        override fun observeRules() = flowOf(emptyList<CategoryRule>())
        override suspend fun rules() = emptyList<CategoryRule>()
        override suspend fun saveRule(rule: CategoryRule) = Unit
        override suspend fun deleteRule(pattern: String) = Unit
    }
    private val ops = Operations()
    private val reader = object : StatementReader { override suspend fun read(source: String, options: CsvOptions) = StatementReport(emptyList(), 0, 0, emptyList(), "UTF-8", ";") }
    private val importer = ImportStatementUseCase(reader, ops, cats, CategorizeItemsUseCase(cats))
    private fun row(amount: Long = -50, title: String = "Кофе") = StatementRow(date, amount, title, "")
    @Test fun reimportSkipsSameAccountDuplicates() = runTest { assertEquals(1, importer.commit(listOf(row()), 1, true, true)); assertEquals(0, importer.commit(listOf(row()), 1, true, true)); assertEquals(1, ops.data.value.size) }
    @Test fun samePurchaseOnAnotherAccountIsNotDuplicate() = runTest { importer.commit(listOf(row()), 1, true, true); assertEquals(0, importer.duplicates(listOf(row()), 2)); assertEquals(1, importer.commit(listOf(row()), 2, true, true)) }
    @Test fun duplicatesInsideFileAreCountedAndSkipped() = runTest { assertEquals(1, importer.duplicates(listOf(row(), row()), 1)); assertEquals(1, importer.commit(listOf(row(), row()), 1, true, true)); assertEquals(1, ops.batches) }
    @Test fun userCanKeepLegitimateIdenticalPurchases() = runTest { assertEquals(2, importer.commit(listOf(row(), row()), 1, true, false)); assertEquals(2, ops.data.value.size) }
    @Test fun skipTransfersDoesNotImportThem() = runTest { assertEquals(0, importer.commit(listOf(row().copy(explicitTransfer = true)), 1, true, true)); assertTrue(ops.data.value.isEmpty()) }
    @Test fun positiveAndNegativeCentsKeepTheirMeaning() = runTest { importer.commit(listOf(row(-29), row(31, "Бонус")), 1, false, true); assertEquals(listOf(-29L, 31L), ops.data.value.map { it.signedAmount }) }
    @Test fun emptyTitleFallbackIsStableOnReimport() = runTest { val r = row(title = ""); importer.commit(listOf(r), 1, true, true); assertEquals("Операция", ops.data.value.single().title); assertEquals(1, importer.duplicates(listOf(r), 1)); assertEquals(0, importer.commit(listOf(r), 1, true, true)) }
}

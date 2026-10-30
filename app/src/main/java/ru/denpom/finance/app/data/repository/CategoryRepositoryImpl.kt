package ru.denpom.finance.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.denpom.finance.app.data.db.CategoryDao
import ru.denpom.finance.app.data.db.CategoryEntity
import ru.denpom.finance.app.data.db.OperationDao
import ru.denpom.finance.app.data.db.RuleEntity
import ru.denpom.finance.app.domain.model.Category
import ru.denpom.finance.app.domain.model.CategoryKind
import ru.denpom.finance.app.domain.model.CategoryRule
import ru.denpom.finance.app.domain.repository.CategoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao,
    private val operationDao: OperationDao,
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun categories(): List<Category> = dao.all().map { it.toDomain() }

    override suspend fun save(category: Category): Long {
        val entity = category.toEntity()
        return if (category.id == 0L) dao.upsert(entity) else {
            dao.upsert(entity)
            category.id
        }
    }

    override suspend fun delete(id: Long) {
        operationDao.clearCategory(id)
        dao.deleteRulesFor(id)
        dao.delete(id)
    }

    private val seedLock = Mutex()

    override suspend fun ensureDefaults() = seedLock.withLock {
        if (dao.count() > 0) return@withLock
        val expense = listOf(
            Triple("Продукты", "shopping_basket", 150f),
            Triple("Кафе", "local_cafe", 65f),
            Triple("Транспорт", "local_taxi", 245f),
            Triple("Покупки", "shopping_bag", 300f),
            Triple("Дом", "chair", 205f),
            Triple("Здоровье", "medication", 180f),
            Triple("Развлечения", "theaters", 20f),
            Triple("Красота", "spa", 345f),
            Triple("Подписки", "autorenew", 275f),
            Triple("Связь", "smartphone", 225f),
            Triple("Дети", "child_care", 100f),
            Triple("Питомцы", "pets", 40f),
            Triple("Подарки", "redeem", 330f),
        )
        val income = listOf(
            Triple("Зарплата", "payments", 155f),
            Triple("Подработка", "work", 120f),
            Triple("Кешбэк", "percent", 85f),
            Triple("Подарки", "redeem", 330f),
        )
        val entities = expense.mapIndexed { i, (n, ic, h) -> CategoryEntity(name = n, icon = ic, hue = h, kind = CategoryKind.EXPENSE.name, sort = i) } +
            income.mapIndexed { i, (n, ic, h) -> CategoryEntity(name = n, icon = ic, hue = h, kind = CategoryKind.INCOME.name, sort = 100 + i) }
        dao.insertAll(entities)
    }

    override fun observeRules(): Flow<List<CategoryRule>> =
        dao.observeRules().map { list -> list.map { CategoryRule(it.pattern, it.categoryId) } }

    override suspend fun rules(): List<CategoryRule> = dao.rules().map { CategoryRule(it.pattern, it.categoryId) }

    override suspend fun saveRule(rule: CategoryRule) = dao.insertRule(RuleEntity(rule.pattern, rule.categoryId))

    override suspend fun deleteRule(pattern: String) = dao.deleteRule(pattern)
}

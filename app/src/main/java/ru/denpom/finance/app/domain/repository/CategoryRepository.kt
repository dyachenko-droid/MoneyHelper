package ru.denpom.finance.app.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.denpom.finance.app.domain.model.Category
import ru.denpom.finance.app.domain.model.CategoryRule

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>
    suspend fun categories(): List<Category>
    suspend fun save(category: Category): Long
    suspend fun delete(id: Long)
    suspend fun ensureDefaults()
    fun observeRules(): Flow<List<CategoryRule>>
    suspend fun rules(): List<CategoryRule>
    suspend fun saveRule(rule: CategoryRule)
    suspend fun deleteRule(pattern: String)
}

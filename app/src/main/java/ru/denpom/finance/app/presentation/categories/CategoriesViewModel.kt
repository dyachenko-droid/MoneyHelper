package ru.denpom.finance.app.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.denpom.finance.app.domain.model.Category
import ru.denpom.finance.app.domain.model.CategoryKind
import ru.denpom.finance.app.domain.model.CategoryRule
import ru.denpom.finance.app.domain.repository.CategoryRepository
import javax.inject.Inject

data class CategoriesState(
    val categories: List<Category> = emptyList(),
    val rules: List<CategoryRule> = emptyList(),
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repo: CategoryRepository,
) : ViewModel() {

    val state: StateFlow<CategoriesState> = combine(repo.observeCategories(), repo.observeRules()) { c, r -> CategoriesState(c, r) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesState())

    fun save(id: Long, name: String, icon: String, hue: Float, kind: CategoryKind) {
        viewModelScope.launch {
            val sort = state.value.categories.firstOrNull { it.id == id }?.sort
                ?: ((state.value.categories.filter { it.kind == kind }.maxOfOrNull { it.sort } ?: 0) + 1)
            repo.save(Category(id, name.trim(), icon, hue, kind, sort))
        }
    }

    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }

    fun deleteRule(pattern: String) = viewModelScope.launch { repo.deleteRule(pattern) }
}

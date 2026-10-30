package ru.denpom.finance.app.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.denpom.finance.app.domain.model.Goal
import ru.denpom.finance.app.domain.repository.GoalRepository
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goals: GoalRepository,
) : ViewModel() {

    val state: StateFlow<List<Goal>> = goals.observeGoals().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(goal: Goal) = viewModelScope.launch { goals.save(goal) }

    fun delete(id: Long) = viewModelScope.launch { goals.delete(id) }

    fun deposit(goalId: Long, amount: Long) = viewModelScope.launch { goals.addEntry(goalId, amount, null) }
}

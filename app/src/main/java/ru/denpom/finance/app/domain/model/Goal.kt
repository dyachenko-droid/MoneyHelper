package ru.denpom.finance.app.domain.model

data class Goal(
    val id: Long,
    val name: String,
    val target: Long,
    val deadline: Long?,
    val icon: String,
    val hue: Float,
    val createdAt: Long,
    val saved: Long = 0,
) {
    val ratio: Float get() = if (target <= 0) 0f else saved.toFloat() / target
    val left: Long get() = (target - saved).coerceAtLeast(0)
}

data class GoalEntry(val id: Long, val goalId: Long, val amount: Long, val timestamp: Long, val operationId: Long?)

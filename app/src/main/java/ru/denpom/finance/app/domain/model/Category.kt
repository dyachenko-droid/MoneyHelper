package ru.denpom.finance.app.domain.model

enum class CategoryKind { EXPENSE, INCOME }

data class Category(
    val id: Long,
    val name: String,
    val icon: String,
    val hue: Float,
    val kind: CategoryKind,
    val sort: Int = 0,
)

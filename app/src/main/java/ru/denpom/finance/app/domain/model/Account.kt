package ru.denpom.finance.app.domain.model

enum class AccountType { CARD, CASH, DEPOSIT, CREDIT }

data class Account(
    val id: Long,
    val name: String,
    val type: AccountType,
    val subtitle: String,
    val openingBalance: Long,
    val creditLimit: Long?,
    val balance: Long = openingBalance,
)

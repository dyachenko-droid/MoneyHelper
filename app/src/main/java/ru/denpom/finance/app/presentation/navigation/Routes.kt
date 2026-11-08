package ru.denpom.finance.app.presentation.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val OPS = "ops"
    const val BUDGETS = "budgets"
    const val MORE = "more"
    const val SCAN = "scan"
    const val RECEIPT_DRAFT = "receipt/draft"
    const val RECEIPT = "receipt/{id}"
    const val ACCOUNTS = "accounts"
    const val SUBSCRIPTIONS = "subscriptions"
    const val CATEGORIES = "categories"
    const val ANALYTICS = "analytics"
    const val BACKUP = "backup"
    const val GOALS = "goals"
    const val DEBTS = "debts"
    const val SECURITY = "security"
    const val REMINDERS = "reminders"
    const val IMPORT = "import"
    const val PAYMENTS = "payments"
    const val RECONCILE = "reconcile"
    const val TEMPLATES = "templates"
    const val MONTHLY_REPORT = "monthly-report"

    fun receipt(id: Long) = "receipt/$id"

    val tabs = setOf(HOME, OPS, BUDGETS, MORE)
}

package ru.denpom.finance.app.domain.model

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class Currency(val code: String, val symbol: String, val title: String) {
    RUB("RUB", "₽", "Российский рубль"),
    BYN("BYN", "Br", "Белорусский рубль"),
    KZT("KZT", "₸", "Казахстанский тенге"),
    UZS("UZS", "сўм", "Узбекский сум"),
    KGS("KGS", "сом", "Киргизский сом"),
    TJS("TJS", "смн", "Таджикский сомони"),
    AMD("AMD", "֏", "Армянский драм"),
    AZN("AZN", "₼", "Азербайджанский манат"),
    MDL("MDL", "лей", "Молдавский лей"),
    TMT("TMT", "ман", "Туркменский манат"),
    USD("USD", "$", "Доллар США"),
    EUR("EUR", "€", "Евро"),
}

data class AppSettings(
    val onboardingDone: Boolean = false,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val hideAmounts: Boolean = false,
    val currency: Currency = Currency.RUB,
    val userName: String = "",
    val pinHash: String = "",
    val biometric: Boolean = false,
    val dailyReminder: Boolean = false,
    val reminderMinutes: Int = 21 * 60,
    val paymentReminders: Boolean = false,
) {
    val pinEnabled: Boolean get() = pinHash.isNotBlank()
}

package ru.denpom.finance.app.domain.usecase

import ru.denpom.finance.app.domain.model.Category
import ru.denpom.finance.app.domain.model.CategoryKind
import ru.denpom.finance.app.domain.model.ReceiptItem
import ru.denpom.finance.app.domain.repository.CategoryRepository
import javax.inject.Inject

class CategorizeItemsUseCase @Inject constructor(
    private val categories: CategoryRepository,
) {

    private val keywords = listOf(
        "spa" to listOf("шампун", "гель для", "крем", "дезодор", "зубн", "мыло", "бальзам", "маска для", "лак ", "косметич", "бритв"),
        "chair" to listOf("губк", "порош", "салфет", "туалетн", "моющ", "fairy", "чистящ", "пакет", "мешк", "лампа", "батарейк", "фольг", "освежит"),
        "medication" to listOf("таблет", "капсул", "витамин", "аптек", "сироп", "пластыр", "бинт", "спрей наз", "мг "),
        "pets" to listOf("корм для", "whiskas", "pedigree", "felix", "наполнит", "для кошек", "для собак"),
        "child_care" to listOf("подгуз", "детск", "пюре дет", "agusha", "агуша", "фрутоняня"),
        "local_cafe" to listOf("капучино", "латте", "американо", "эспрессо", "раф ", "круассан"),
        "shopping_basket" to listOf(
            "молоко", "хлеб", "батон", "сыр", "масло", "кефир", "йогурт", "творог", "сметан", "яйц", "мясо", "кур", "свин",
            "говяд", "фарш", "колбас", "сосис", "рыба", "лосос", "банан", "яблок", "апельсин", "огур", "помидор", "томат",
            "картоф", "лук", "морков", "капуст", "рис", "гречк", "макарон", "мука", "сахар", "соль", "чай", "кофе", "вода",
            "сок", "печень", "шоколад", "конфет", "вафл", "пельмен", "хумус", "овощ", "фрукт", "зелень", "кетчуп", "майонез",
        ),
    )

    private val merchants = listOf(
        "shopping_basket" to listOf("пятёрочка", "пятерочка", "перекрёсток", "перекресток", "вкусвилл", "магнит", "лента", "ашан", "дикси", "окей", "metro", "азбука", "самокат", "агроторг", "тандер"),
        "local_cafe" to listOf("кофе", "кафе", "coffee", "ресторан", "бургер", "kfc", "вкусно", "шоколадница", "додо", "пицц", "суши"),
        "local_taxi" to listOf("такси", "taxi", "метро", "транспорт", "азс", "лукойл", "газпромнефть", "роснефть", "каршеринг"),
        "shopping_bag" to listOf("ozon", "озон", "wildberries", "вайлдберриз", "lamoda", "dns", "м.видео", "эльдорадо", "спортмастер"),
        "medication" to listOf("аптек", "ригла", "горздрав", "здравсити"),
        "spa" to listOf("золотое яблоко", "летуаль", "подружка", "магнит косметик"),
        "autorenew" to listOf("яндекс плюс", "кинопоиск", "icloud", "spotify", "ivi", "okko"),
        "smartphone" to listOf("мтс", "билайн", "мегафон", "теле2", "tele2", "yota"),
        "pets" to listOf("четыре лапы", "бетховен", "зоомагазин"),
    )

    fun ruleKey(name: String): String =
        name.lowercase()
            .replace(Regex("[^a-zа-яё ]"), " ")
            .split(' ')
            .filter { it.length > 2 }
            .take(2)
            .joinToString(" ")

    suspend fun categorize(shop: String, items: List<ReceiptItem>): List<ReceiptItem> {
        val all = categories.categories().filter { it.kind == CategoryKind.EXPENSE }
        val rules = categories.rules().associate { it.pattern to it.categoryId }
        val fallback = merchantCategory(shop, all) ?: all.byIcon("shopping_basket") ?: all.firstOrNull()?.id ?: 0L
        return items.map { item ->
            val lower = item.name.lowercase()
            val id = rules[ruleKey(item.name)]?.takeIf { r -> all.any { it.id == r } }
                ?: keywords.firstOrNull { (_, words) -> words.any { lower.contains(it) } }?.let { all.byIcon(it.first) }
                ?: fallback
            item.copy(categoryId = id)
        }
    }

    suspend fun categoryForTitle(title: String): Long? {
        val all = categories.categories().filter { it.kind == CategoryKind.EXPENSE }
        return merchantCategory(title, all)
    }

    private fun merchantCategory(shop: String, all: List<Category>): Long? {
        val lower = shop.lowercase()
        return merchants.firstOrNull { (_, names) -> names.any { lower.contains(it) } }?.let { all.byIcon(it.first) }
    }

    private fun List<Category>.byIcon(icon: String): Long? = firstOrNull { it.icon == icon }?.id
}

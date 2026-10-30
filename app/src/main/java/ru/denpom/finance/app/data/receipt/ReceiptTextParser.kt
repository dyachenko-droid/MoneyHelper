package ru.denpom.finance.app.data.receipt

import ru.denpom.finance.app.domain.model.ReceiptItem
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.roundToLong

data class ParsedText(
    val shop: String,
    val timestamp: Long?,
    val total: Long?,
    val items: List<ReceiptItem>,
)

class ReceiptTextParser @Inject constructor() {

    private val amount = """(\d{1,6}(?:\s\d{3})*[.,]\s?\d{2})"""
    private val totalRegex = Regex("""(ИТОГ[О0]?|К\s*ОПЛАТЕ|ВСЕГО)[^\d]{0,12}$amount""", RegexOption.IGNORE_CASE)
    private val dateRegex = Regex("""(\d{2})[./-](\d{2})[./-](\d{2,4})\D{0,3}(\d{2}):(\d{2})""")
    private val qtyRegex = Regex("""^(\d+(?:[.,]\d{1,3})?)\s*(?:шт|кг)?\.?\s*[xхXХ*]\s*$amount""", RegexOption.IGNORE_CASE)
    private val inlineQtyRegex = Regex("""\s+(\d+(?:[.,]\d{1,3})?)\s*(?:шт|кг)?\.?\s*[xхXХ*×]$""", RegexOption.IGNORE_CASE)
    private val trailingRegex = Regex("""^(.*?[A-Za-zА-Яа-яЁё]{2,}.*?)\s*[=*]?\s*$amount\s*[=*_A-ZА-Я]?$""")
    private val skip = listOf(
        "ИТОГ", "НДС", "СУММ", "СДАЧ", "НАЛИЧ", "БЕЗНАЛ", "КАРТ", "ОПЛАТ", "КАССИР", "СМЕНА", "ЧЕК", "ИНН",
        "ФН", "ФД", "ФП", "ККТ", "СНО", "ПРИХОД", "ПОЛУЧЕНО", "ВСЕГО", "ЭЛЕКТРОН", "ОБЕСПЕЧ", "СКИДК", "БОНУС",
    )

    fun parse(text: String): ParsedText {
        val lines = text.lines().map { it.trim().replace(Regex("\\s+"), " ") }.filter { it.isNotEmpty() }
        val total = lines.firstNotNullOfOrNull { totalRegex.find(it)?.groupValues?.get(2)?.toKopecks() }
        val timestamp = lines.firstNotNullOfOrNull { line ->
            dateRegex.find(line)?.let { m ->
                val (d, mo, y, h, mi) = m.destructured
                val year = if (y.length == 2) 2000 + y.toInt() else y.toInt()
                runCatching {
                    LocalDateTime.of(year, mo.toInt(), d.toInt(), h.toInt(), mi.toInt())
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }.getOrNull()?.takeIf { it in plausibleRange() }
            }
        }
        val shop = lines.take(6).firstOrNull { line ->
            line.count { it.isLetter() } >= 4 && skip.none { line.uppercase().contains(it) } && !line.contains(Regex("\\d{5,}"))
        }.orEmpty()

        val items = mutableListOf<ReceiptItem>()
        var pendingName: String? = null
        for (line in lines) {
            val upper = line.uppercase()
            if (upper.startsWith("ИТОГ") || upper.contains("К ОПЛАТЕ")) break
            val qty = qtyRegex.find(line)
            if (qty != null && pendingName != null) {
                val quantity = qty.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 1.0
                val price = qty.groupValues[2].toKopecks() ?: continue
                items += item(pendingName, quantity, price)
                pendingName = null
                continue
            }
            if (skip.any { upper.contains(it) }) {
                pendingName = null
                continue
            }
            val trailing = trailingRegex.find(line)
            if (trailing != null) {
                val name = trailing.groupValues[1].trim().trimEnd('*', '=', ' ')
                val sum = trailing.groupValues[2].toKopecks() ?: continue
                if (pendingName != null && name.length < 4) {
                    items += item(pendingName, 1.0, sum)
                    pendingName = null
                } else {
                    val inline = inlineQtyRegex.find(name)
                    val quantity = inline?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
                    if (inline != null && quantity != null && quantity > 0) {
                        items += item(name.substring(0, inline.range.first).trim(), quantity, sum)
                    } else {
                        items += item(name, 1.0, sum)
                    }
                    pendingName = null
                }
                continue
            }
            if (line.count { it.isLetter() } >= 3) pendingName = line
        }
        return ParsedText(shop = shop, timestamp = timestamp, total = total, items = items.filter { it.sum > 0 })
    }

    private fun item(name: String, quantity: Double, price: Long): ReceiptItem {
        val weighted = quantity % 1.0 != 0.0
        return ReceiptItem(
            name = name.replaceFirstChar { it.titlecase() },
            quantity = quantity,
            unit = if (weighted) "кг" else "шт",
            price = price,
            sum = (price * quantity).roundToLong(),
            categoryId = 0,
        )
    }

    private fun String.toKopecks(): Long? =
        replace(" ", "").replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() }

    private fun plausibleRange(): LongRange {
        val now = System.currentTimeMillis()
        return now - 400L * 24 * 3600 * 1000..now + 24L * 3600 * 1000
    }
}

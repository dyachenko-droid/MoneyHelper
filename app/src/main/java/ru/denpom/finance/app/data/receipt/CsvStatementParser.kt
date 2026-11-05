package ru.denpom.finance.app.data.receipt

import ru.denpom.finance.app.domain.model.*
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/** Pure CSV parser. No database writes; every nonempty data record is counted. */
object CsvStatementParser {
    private val dateTimes = listOf("dd.MM.uuuu HH:mm:ss", "dd.MM.uuuu HH:mm", "uuuu-MM-dd HH:mm:ss", "uuuu-MM-dd'T'HH:mm:ss").map { DateTimeFormatter.ofPattern(it).withResolverStyle(ResolverStyle.STRICT) }
    private val dates = listOf("dd.MM.uuuu", "uuuu-MM-dd", "dd.MM.uu").map { DateTimeFormatter.ofPattern(it).withResolverStyle(ResolverStyle.STRICT) }
    private data class Record(val line: Int, val cells: List<String>, val bad: Boolean = false)

    fun parse(bytes: ByteArray, options: CsvOptions = CsvOptions()): StatementReport {
        val utf = runCatching { Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString() }.getOrNull()
        val encoding = when (options.encoding) {
            CsvEncoding.AUTO -> if (utf != null) CsvEncoding.UTF8 else CsvEncoding.WINDOWS1251
            else -> options.encoding
        }
        val text = (if (encoding == CsvEncoding.UTF8) requireNotNull(utf) { "Файл не в UTF-8. Выберите Windows-1251" }
            else String(bytes, Charset.forName("windows-1251"))).removePrefix("\uFEFF")
        val delimiter = options.delimiter.char ?: listOf(';', '\t', ',').maxBy { d ->
            text.lineSequence().take(50).map { line ->
                val cells = records(line, d).firstOrNull()?.cells.orEmpty().map { it.lowercase() }
                if (cells.any { it.contains("дата") } && cells.any { it.contains("сумм") }) cells.size else 0
            }.maxOrNull() ?: 0
        }
        val records = records(text, delimiter).filter { it.cells.any(String::isNotBlank) }
        val headerIndex = records.indexOfFirst { r -> r.cells.any { it.contains("дата", true) } && r.cells.any { it.contains("сумм", true) } }
        require(headerIndex >= 0) { "Не найдены колонки «Дата» и «Сумма». Проверьте разделитель и кодировку" }
        val h = records[headerIndex].cells.map { it.trim().lowercase() }
        fun col(test: (String) -> Boolean) = h.indexOfFirst(test)
        val date = col { it.startsWith("дата операции") }.takeIf { it >= 0 } ?: col { it.contains("дата") }
        val amount = col { it == "сумма операции" }.takeIf { it >= 0 } ?: col { it.contains("сумма") && !it.contains("кэш") && !it.contains("бонус") }
        val desc = col { it.contains("описание") || it.contains("назначение") || it.contains("контрагент") || it == "название" }
        val cat = col { it.contains("категория") }
        val status = col { it.contains("статус") }
        val type = col { it == "тип" }
        val rows = mutableListOf<StatementRow>()
        val issues = mutableListOf<CsvIssue>()
        var rejected = 0
        val reasons = linkedMapOf<String, Int>()
        val data = records.drop(headerIndex + 1)
        data.forEach { r ->
            val cells = r.cells
            fun reject(reason: String) { rejected++; reasons[reason] = (reasons[reason] ?: 0) + 1; if (issues.size < 20) issues += CsvIssue(r.line, reason, cells.joinToString(" · ").take(120)) }
            if (r.bad || cells.size != h.size) { reject("Структура CSV / кавычки"); return@forEach }
            val st = cells.getOrNull(status).orEmpty()
            if (st.contains("FAILED", true) || st.contains("отклон", true) || st.contains("отмен", true)) { reject("Операция отклонена или отменена"); return@forEach }
            val ts = parseDate(cells[date])
            if (ts == null) { reject("Некорректная дата"); return@forEach }
            var sum = parseMoney(cells[amount])
            if (sum == null) { reject("Некорректная сумма (нужна точность до 2 знаков)"); return@forEach }
            if (sum == 0L) { reject("Нулевая сумма"); return@forEach }
            val kind = cells.getOrNull(type).orEmpty()
            if (kind.equals("Расход", true)) sum = -kotlin.math.abs(sum)
            if (kind.equals("Доход", true)) sum = kotlin.math.abs(sum)
            rows += StatementRow(ts, sum, cells.getOrNull(desc).orEmpty().trim().ifBlank { cells.getOrNull(cat).orEmpty() }, cells.getOrNull(cat).orEmpty().trim(), kind.equals("Перевод", true))
        }
        return StatementReport(rows, data.size, rejected, issues, encoding.label, when (delimiter) { ';' -> ";"; ',' -> ","; else -> "TAB" }, reasons)
    }

    fun parseMoney(raw: String): Long? = runCatching {
        var v = raw.trim().replace(Regex("[\\s\\u00a0\\u202f]"), "").replace(Regex("(?i)(₽|руб\\.?|RUB)$"), "")
        if (v.startsWith('(') && v.endsWith(')')) v = "-" + v.substring(1, v.length - 1)
        require(v.matches(Regex("[+-]?[0-9]+([.,][0-9]+)*")))
        if (v.contains('.') && v.contains(',')) {
            val decimal = if (v.lastIndexOf('.') > v.lastIndexOf(',')) '.' else ','
            val thousands = if (decimal == '.') ',' else '.'
            val parts = v.split(decimal)
            require(parts.size == 2 && parts[1].length in 1..2)
            require(parts[0].removePrefix("-").removePrefix("+").matches(Regex("[0-9]{1,3}(" + Regex.escape(thousands.toString()) + "[0-9]{3})+")))
            v = v.replace(thousands.toString(), "").replace(decimal, '.')
        } else {
            v = v.replace(',', '.')
            require(v.count { it == '.' } <= 1)
            require(!v.contains('.') || v.substringAfter('.').length in 1..2)
        }
        BigDecimal(v).movePointRight(2).longValueExact().also { require(it != Long.MIN_VALUE) }
    }.getOrNull()

    private fun parseDate(raw: String): Long? {
        dateTimes.forEach { f -> runCatching { return LocalDateTime.parse(raw.trim(), f).atZone(Period.zone).toInstant().toEpochMilli() } }
        dates.forEach { f -> runCatching { return Period.startOfDay(LocalDate.parse(raw.trim(), f)) + 12 * 3600_000L } }
        return null
    }

    private fun records(text: String, delimiter: Char): List<Record> {
        val out = mutableListOf<Record>()
        var cells = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var afterQuote = false
        var bad = false
        var line = 1
        var startLine = 1
        var i = 0
        fun finish() { cells += cell.toString(); out += Record(startLine, cells, bad); cells = mutableListOf(); cell.clear(); bad = false; afterQuote = false }
        while (i < text.length) {
            val ch = text[i]
            when {
                ch == '"' && quoted && text.getOrNull(i + 1) == '"' -> { cell.append('"'); i++ }
                ch == '"' && quoted -> { quoted = false; afterQuote = true }
                ch == '"' && cell.isEmpty() && !afterQuote -> quoted = true
                ch == '"' -> { bad = true; cell.append(ch) }
                ch == delimiter && !quoted -> { cells += cell.toString(); cell.clear(); afterQuote = false }
                ch == '\n' && !quoted -> { finish(); line++; startLine = line }
                ch == '\r' && !quoted -> { if (text.getOrNull(i + 1) != '\n') { finish(); line++; startLine = line } }
                else -> { if (afterQuote && !ch.isWhitespace()) bad = true; cell.append(ch); if (ch == '\n') line++ }
            }
            i++
        }
        if (cell.isNotEmpty() || cells.isNotEmpty() || quoted) {
            if (quoted) {
                // An unmatched quote must not swallow subsequent independent rows.
                val broken = cell.toString().split('\n')
                out += Record(startLine, cells + broken.first(), true)
                broken.drop(1).forEachIndexed { offset, row -> out += records(row, delimiter).map { it.copy(line = startLine + offset + 1) } }
            } else finish()
        }
        return out
    }
}

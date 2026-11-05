package ru.denpom.finance.app.data.receipt

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.domain.model.StatementRow
import ru.denpom.finance.app.domain.repository.StatementReader
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.roundToLong

class CsvStatementReader @Inject constructor(
    @ApplicationContext private val context: Context,
) : StatementReader {

    private val dateTimeFormats = listOf("dd.MM.yyyy HH:mm:ss", "dd.MM.yyyy HH:mm", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss").map { DateTimeFormatter.ofPattern(it) }
    private val dateFormats = listOf("dd.MM.yyyy", "yyyy-MM-dd", "dd.MM.yy").map { DateTimeFormatter.ofPattern(it) }

    override suspend fun read(source: String): List<StatementRow> = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(Uri.parse(source))?.use { it.readBytes() } ?: return@withContext emptyList()
        val text = decode(bytes)
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return@withContext emptyList()
        val delimiter = listOf(';', '\t', ',').maxBy { d -> lines.first().count { it == d } }
        val headerIndex = lines.indexOfFirst { it.contains("дата", true) && it.contains("сумм", true) }.takeIf { it >= 0 } ?: return@withContext emptyList()
        val header = split(lines[headerIndex], delimiter).map { it.lowercase().trim() }
        val dateCol = header.indexOfFirst { it.startsWith("дата операции") }.takeIf { it >= 0 } ?: header.indexOfFirst { it.contains("дата") }
        val amountCol = header.indexOfFirst { it == "сумма операции" }.takeIf { it >= 0 }
            ?: header.indexOfFirst { it.contains("сумма") && !it.contains("кэш") && !it.contains("бонус") }
        val descCol = header.indexOfFirst { it.contains("описание") || it.contains("назначение") || it.contains("контрагент") }
        val catCol = header.indexOfFirst { it.contains("категория") }
        val statusCol = header.indexOfFirst { it.contains("статус") }
        if (dateCol < 0 || amountCol < 0) return@withContext emptyList()
        lines.drop(headerIndex + 1).mapNotNull { line ->
            val cells = split(line, delimiter)
            val status = cells.getOrNull(statusCol).orEmpty()
            if (status.contains("FAILED", true) || status.contains("отклон", true)) return@mapNotNull null
            val ts = parseDate(cells.getOrNull(dateCol).orEmpty()) ?: return@mapNotNull null
            val amount = cells.getOrNull(amountCol)?.replace(" ", "")?.replace(" ", "")?.replace(',', '.')?.toDoubleOrNull() ?: return@mapNotNull null
            if (amount == 0.0) return@mapNotNull null
            StatementRow(
                timestamp = ts,
                amount = (amount * 100).roundToLong(),
                description = cells.getOrNull(descCol).orEmpty().trim().ifBlank { cells.getOrNull(catCol).orEmpty() },
                bankCategory = cells.getOrNull(catCol).orEmpty().trim(),
            )
        }
    }

    private fun decode(bytes: ByteArray): String {
        val utf = String(bytes, Charsets.UTF_8).removePrefix("\uFEFF")
        return if (utf.contains('�')) String(bytes, Charset.forName("windows-1251")) else utf
    }

    private fun split(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && quoted && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                ch == '"' -> quoted = !quoted
                ch == delimiter && !quoted -> { result += sb.toString(); sb.clear() }
                else -> sb.append(ch)
            }
            i++
        }
        result += sb.toString()
        return result
    }

    private fun parseDate(raw: String): Long? {
        val v = raw.trim()
        dateTimeFormats.forEach { f -> runCatching { return LocalDateTime.parse(v, f).atZone(Period.zone).toInstant().toEpochMilli() } }
        dateFormats.forEach { f -> runCatching { return Period.startOfDay(LocalDate.parse(v, f)) + 12 * 3600_000L } }
        return null
    }
}

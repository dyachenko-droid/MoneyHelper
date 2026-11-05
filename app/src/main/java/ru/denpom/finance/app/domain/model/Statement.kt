package ru.denpom.finance.app.domain.model

enum class CsvEncoding(val label: String) { AUTO("Авто"), UTF8("UTF-8"), WINDOWS1251("Windows-1251") }
enum class CsvDelimiter(val label: String, val char: Char?) { AUTO("Авто", null), SEMICOLON("Точка с запятой", ';'), COMMA("Запятая", ','), TAB("Табуляция", '\t') }
data class CsvOptions(val encoding: CsvEncoding = CsvEncoding.AUTO, val delimiter: CsvDelimiter = CsvDelimiter.AUTO)
data class CsvIssue(val line: Int, val reason: String, val example: String)
data class StatementReport(
    val rows: List<StatementRow>, val total: Int, val rejected: Int, val issues: List<CsvIssue>,
    val encoding: String, val delimiter: String, val reasons: Map<String, Int> = emptyMap(),
)
data class StatementRow(
    val timestamp: Long, val amount: Long, val description: String, val bankCategory: String,
    val explicitTransfer: Boolean = false,
) {
    val isTransfer: Boolean get() = explicitTransfer || bankCategory.contains("перевод", true) ||
        description.startsWith("Перевод", true) || bankCategory.contains("пополнен", true)
}
data class ImportPreview(val report: StatementReport, val duplicates: Int) { val rows get() = report.rows }

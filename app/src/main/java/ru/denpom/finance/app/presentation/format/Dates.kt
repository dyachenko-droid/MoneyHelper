package ru.denpom.finance.app.presentation.format

import ru.denpom.finance.app.domain.model.Period
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object Dates {
    private val ru = Locale.forLanguageTag("ru")
    private val dayMonth = DateTimeFormatter.ofPattern("d MMMM", ru)
    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMMM yyyy", ru)
    private val time = DateTimeFormatter.ofPattern("HH:mm", ru)
    private val short = DateTimeFormatter.ofPattern("dd.MM", ru)

    fun monthName(month: YearMonth): String =
        month.month.getDisplayName(TextStyle.FULL_STANDALONE, ru).replaceFirstChar { it.titlecase(ru) }

    fun monthTitle(month: YearMonth): String =
        if (month.year == YearMonth.now().year) monthName(month) else "${monthName(month)} ${month.year}"

    fun monthShort(month: YearMonth): String =
        month.month.getDisplayName(TextStyle.SHORT_STANDALONE, ru).trimEnd('.')

    fun monthGenitive(month: YearMonth): String = month.month.getDisplayName(TextStyle.FULL, ru)

    fun monthPrepositional(month: YearMonth): String = listOf(
        "январе", "феврале", "марте", "апреле", "мае", "июне", "июле", "августе", "сентябре", "октябре", "ноябре", "декабре",
    )[month.monthValue - 1]

    fun dayHeader(date: LocalDate): String {
        val today = LocalDate.now()
        val base = date.format(dayMonth)
        return when (date) {
            today -> "Сегодня, $base"
            today.minusDays(1) -> "Вчера, $base"
            else -> date.dayOfWeek.getDisplayName(TextStyle.FULL, ru).replaceFirstChar { it.titlecase(ru) } + ", " +
                if (date.year == today.year) base else date.format(dayMonthYear)
        }
    }

    fun dayMonth(date: LocalDate): String = date.format(dayMonth)

    fun shortDate(date: LocalDate): String = date.format(short)

    fun relative(date: LocalDate): String {
        val today = LocalDate.now()
        return when (date) {
            today -> "Сегодня, ${date.format(dayMonth)}"
            today.plusDays(1) -> "Завтра, ${date.format(dayMonth)}"
            today.plusDays(2) -> "Послезавтра, ${date.format(dayMonth)}"
            else -> date.format(if (date.year == today.year) dayMonth else dayMonthYear)
        }
    }

    fun full(timestamp: Long): String {
        val dt = java.time.Instant.ofEpochMilli(timestamp).atZone(Period.zone)
        return dt.toLocalDate().format(dayMonthYear) + ", " + dt.toLocalTime().format(time)
    }

    fun chipDate(date: LocalDate): String = when (date) {
        LocalDate.now() -> "Сегодня"
        LocalDate.now().minusDays(1) -> "Вчера"
        else -> date.format(dayMonth)
    }

    fun plural(n: Long, one: String, few: String, many: String): String {
        val mod10 = n % 10
        val mod100 = n % 100
        return when {
            mod10 == 1L && mod100 != 11L -> one
            mod10 in 2..4 && mod100 !in 12..14 -> few
            else -> many
        }
    }

    fun days(n: Long) = "$n ${plural(n, "день", "дня", "дней")}"
}

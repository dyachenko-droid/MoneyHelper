package ru.denpom.finance.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

object Period {
    val zone: ZoneId get() = ZoneId.systemDefault()

    fun monthOf(timestamp: Long): YearMonth = YearMonth.from(Instant.ofEpochMilli(timestamp).atZone(zone))

    fun dateOf(timestamp: Long): LocalDate = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()

    fun startOf(month: YearMonth): Long = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

    fun endOf(month: YearMonth): Long = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

    fun startOfDay(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun inMonth(timestamp: Long, month: YearMonth): Boolean = timestamp >= startOf(month) && timestamp < endOf(month)
}

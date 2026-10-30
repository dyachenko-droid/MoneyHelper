package ru.denpom.finance.app.data.receipt

import ru.denpom.finance.app.domain.model.FiscalQr
import ru.denpom.finance.app.domain.repository.FiscalQrReader
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.roundToLong

class FiscalQrParser @Inject constructor() : FiscalQrReader {

    private val withSeconds = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val withoutSeconds = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm")

    override fun parse(raw: String): FiscalQr? {
        val params = raw.trim().split('&').mapNotNull { part ->
            val i = part.indexOf('=')
            if (i <= 0) null else part.substring(0, i).lowercase() to part.substring(i + 1)
        }.toMap()
        val t = params["t"] ?: return null
        val s = params["s"] ?: return null
        val fn = params["fn"] ?: return null
        val fd = params["i"] ?: return null
        val fp = params["fp"] ?: return null
        val sum = s.replace(',', '.').toDoubleOrNull() ?: return null
        val dateTime = runCatching { LocalDateTime.parse(t, withSeconds) }
            .recoverCatching { LocalDateTime.parse(t, withoutSeconds) }
            .getOrNull() ?: return null
        return FiscalQr(
            raw = raw.trim(),
            timestamp = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            sum = (sum * 100).roundToLong(),
            fn = fn,
            fd = fd,
            fp = fp,
        )
    }
}

package ru.denpom.finance.app.presentation.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.abs

const val NBSP = ' '
const val MINUS = '−'

@Immutable
data class MoneyFormat(val symbol: String = "₽", val hidden: Boolean = false) {

    fun plain(kopecks: Long, decimals: Boolean = true): String {
        val abs = abs(kopecks)
        val rub = abs / 100
        val kop = abs % 100
        val grouped = group(rub)
        return if (decimals) "$grouped,${kop.toString().padStart(2, '0')}$NBSP$symbol" else "$grouped$NBSP$symbol"
    }

    fun format(kopecks: Long, decimals: Boolean = true): String =
        if (hidden) mask() else (if (kopecks < 0) "$MINUS" else "") + plain(kopecks, decimals)

    fun signed(kopecks: Long, decimals: Boolean = true): String = when {
        hidden -> mask()
        kopecks > 0 -> "+" + plain(kopecks, decimals)
        kopecks < 0 -> "$MINUS" + plain(kopecks, decimals)
        else -> plain(0, decimals)
    }

    fun open(kopecks: Long, decimals: Boolean = false): String = (if (kopecks < 0) "$MINUS" else "") + plain(kopecks, decimals)

    fun number(kopecks: Long): String = group(abs(kopecks) / 100)

    fun mask() = "•••••$NBSP$symbol"

    private fun group(value: Long): String {
        val s = value.toString()
        val sb = StringBuilder()
        s.forEachIndexed { i, ch ->
            if (i > 0 && (s.length - i) % 3 == 0) sb.append(NBSP)
            sb.append(ch)
        }
        return sb.toString()
    }
}

val LocalMoney = staticCompositionLocalOf { MoneyFormat() }

object Money {
    val fmt: MoneyFormat
        @Composable get() = LocalMoney.current
}

fun parseAmount(input: String): Long {
    val normalized = input.replace(NBSP.toString(), "").replace(" ", "").replace(',', '.')
    val value = normalized.toBigDecimalOrNull() ?: return 0
    return value.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
}

fun amountInput(kopecks: Long): String {
    val abs = abs(kopecks)
    val kop = abs % 100
    return if (kop == 0L) (abs / 100).toString() else "${abs / 100},${kop.toString().padStart(2, '0')}"
}

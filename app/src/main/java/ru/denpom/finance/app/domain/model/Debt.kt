package ru.denpom.finance.app.domain.model

import kotlin.math.pow
import kotlin.math.roundToLong

enum class DebtDirection { TO_ME, FROM_ME }

data class Debt(
    val id: Long,
    val person: String,
    val direction: DebtDirection,
    val amount: Long,
    val note: String,
    val createdAt: Long,
    val dueDate: Long?,
    val paid: Long = 0,
) {
    val left: Long get() = (amount - paid).coerceAtLeast(0)
    val closed: Boolean get() = left == 0L
}

data class Loan(
    val id: Long,
    val name: String,
    val principal: Long,
    val ratePercent: Double,
    val termMonths: Int,
    val startDate: Long,
    val recurringId: Long?,
) {
    val monthlyPayment: Long
        get() {
            val r = ratePercent / 100 / 12
            if (r == 0.0) return principal / termMonths.coerceAtLeast(1)
            return (principal * r / (1 - (1 + r).pow(-termMonths))).roundToLong()
        }

    val overpayment: Long get() = monthlyPayment * termMonths - principal

    fun schedule(): List<LoanPayment> {
        val r = ratePercent / 100 / 12
        var balance = principal.toDouble()
        val start = Period.dateOf(startDate)
        return (1..termMonths).map { n ->
            val interest = balance * r
            val body = if (n == termMonths) balance else monthlyPayment - interest
            balance -= body
            LoanPayment(n, Period.startOfDay(start.plusMonths(n.toLong())), (body + interest).roundToLong(), body.roundToLong(), interest.roundToLong(), balance.coerceAtLeast(0.0).roundToLong())
        }
    }
}

data class LoanPayment(val number: Int, val date: Long, val total: Long, val principal: Long, val interest: Long, val balanceAfter: Long)

package ru.denpom.finance.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.denpom.finance.app.domain.model.Budget
import ru.denpom.finance.app.domain.model.GoalEntry
import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.model.PaymentOccurrence
import ru.denpom.finance.app.domain.model.PaymentStatus
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.domain.model.RecurrencePeriod
import ru.denpom.finance.app.domain.model.Recurring
import ru.denpom.finance.app.domain.model.Split
import ru.denpom.finance.app.domain.usecase.MonthlyReports
import ru.denpom.finance.app.domain.usecase.PaymentSchedule
import java.time.LocalDate
import java.time.YearMonth

class PlanningLogicTest {
    private val today = LocalDate.of(2026, 11, 8)

    @Test fun paymentMovedAcrossMonthsAppearsOnlyOnTheNewDate() {
        val date = today.plusDays(1)
        val moved = today.plusMonths(1)
        val rule = recurring(date)
        val override = PaymentOccurrence(rule.id, date, moved)
        val november = PaymentSchedule.between(listOf(rule), listOf(override), today, today.plusDays(20))
        val december = PaymentSchedule.between(listOf(rule), listOf(override), moved, moved)
        assertTrue(november.isEmpty())
        assertEquals(listOf(date), december.map { it.occurrence.scheduled })
    }

    @Test fun postedOperationLinkedToOneOccurrenceCannotAlsoCoverAnother() {
        val rule = recurring(today)
        val second = rule.copy(id = 2)
        val expense = operation(7, today, 100)
        val paid = PaymentOccurrence(rule.id, today, status = PaymentStatus.PAID, operationId = expense.id)
        val pending = PaymentSchedule.pendingExpenses(listOf(rule, second), listOf(paid), listOf(expense), today, today)
        assertEquals(listOf(second.id), pending.map { it.rule.id })
    }

    @Test fun partialReportComparesSameDaysAndUsesOnlyTheRelevantSplitAmounts() {
        val thisMonth = operation(1, today, 500, splits = listOf(Split(1, 200), Split(1, 50), Split(2, 250)))
        val prior = operation(2, today.minusMonths(1), 150, splits = listOf(Split(1, 150)))
        val laterPrior = operation(3, today.minusMonths(1).plusDays(1), 800)
        val future = operation(4, today.plusDays(1), 900)
        val transfer = operation(5, today, 2_000).copy(type = OperationType.TRANSFER)
        val income = operation(6, today, 1_000).copy(type = OperationType.INCOME)
        val report = MonthlyReports.calculate(
            YearMonth.from(today), today, listOf(thisMonth, prior, laterPrior, future, transfer, income),
            listOf(Budget(1, 200)), listOf(GoalEntry(1, 1, 250, Period.startOfDay(today), null)),
        )
        assertEquals(500L, report.expenseTotal)
        assertEquals(500L, report.net)
        val change = report.growth.single { it.categoryId == 1L }
        assertEquals(100L, change.increase)
        assertEquals(250L, change.current.single().amount)
        assertEquals(250L, report.overages.single().spent)
        assertEquals(250L, report.savings.single().amount)
    }

    private fun recurring(date: LocalDate) = Recurring(
        1, "Подписка", 100, OperationType.EXPENSE, 1, 1, RecurrencePeriod.MONTHLY,
        Period.startOfDay(date), true, false,
    )

    private fun operation(id: Long, date: LocalDate, amount: Long, splits: List<Split> = emptyList()) = Operation(
        id, OperationType.EXPENSE, amount, 1, null, 1, "Подписка", "", Period.startOfDay(date), null, splits,
    )
}

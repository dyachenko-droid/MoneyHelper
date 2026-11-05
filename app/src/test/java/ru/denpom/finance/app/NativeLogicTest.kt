package ru.denpom.finance.app

import org.junit.Assert.*
import org.junit.Test
import ru.denpom.finance.app.domain.model.*
import ru.denpom.finance.app.domain.usecase.*
import ru.denpom.finance.app.data.receipt.CsvStatementParser
import java.time.LocalDate
import java.time.YearMonth

class NativeLogicTest {
    private val today = LocalDate.of(2026, 11, 5)
    private fun op(id: Long = 1, type: OperationType = OperationType.EXPENSE, amount: Long = 100, account: Long = 1, dest: Long? = null, title: String = "Кофе", date: LocalDate = today, splits: List<Split> = emptyList()) = Operation(id, type, amount, account, dest, null, title, "чек #42", Period.startOfDay(date), null, splits, "Молоко")
    private fun rec(date: LocalDate = today, period: RecurrencePeriod = RecurrencePeriod.MONTHLY, amount: Long = 100) = Recurring(1, "Кофе", amount, OperationType.EXPENSE, 1, null, period, Period.startOfDay(date), true, false)
    private val cats = mapOf(2L to Category(2, "Аптека", "medication", 0f, CategoryKind.EXPENSE))
    private val accounts = mapOf(1L to "Основной", 2L to "Накопления")
    private fun find(ops: List<Operation>, f: OpsFilter) = OperationQuery.filter(ops, f, cats, accounts)
    @Test fun searchFindsSplitCategory() { assertEquals(1, find(listOf(op(splits = listOf(Split(2, 50)))), OpsFilter(query = "апТЕка", month = null)).size) }
    @Test fun searchFindsTransferDestination() { assertEquals(1, find(listOf(op(type = OperationType.TRANSFER, dest = 2)), OpsFilter(query = "накопления", month = null)).size) }
    @Test fun searchCombinesAllFilters() { assertTrue(find(listOf(op()), OpsFilter(query = "кофе", month = YearMonth.of(2026, 11), type = TypeFilter.INCOME)).isEmpty()) }
    @Test fun allTimeReallyRemovesMonth() { assertEquals(1, find(listOf(op(date = today.minusMonths(5))), OpsFilter(month = null)).size) }
    @Test fun categoryFilterIncludesSplits() { assertEquals(1, find(listOf(op(splits = listOf(Split(2, 50)))), OpsFilter(month = null, categoryId = 2)).size) }
    @Test fun receiptItemsAndNotesAreSearchable() { assertEquals(1, find(listOf(op()), OpsFilter(query = "молоко", month = null)).size); assertEquals(1, find(listOf(op()), OpsFilter(query = "#42", month = null)).size) }
    @Test fun selectedRecipientSeesTransfer() { assertEquals(1, find(listOf(op(type = OperationType.TRANSFER, dest = 2)), OpsFilter(month = null, accountId = 2)).size) }
    @Test fun totalsExcludeTransfersFromIncomeAndExpense() {
        val s = OperationQuery.summary(listOf(op(amount = 200), op(2, OperationType.INCOME, 500), op(3, OperationType.TRANSFER, 700, dest = 2)), null)
        assertEquals(3, s.count); assertEquals(200L, s.expenses); assertEquals(500L, s.income); assertEquals(700L, s.transferAmount); assertEquals(300L, s.net)
    }
    @Test fun transferDirectionAffectsOnlySelectedAccount() {
        val transfer = op(type = OperationType.TRANSFER, amount = 700, dest = 2)
        assertEquals(-700L, OperationQuery.summary(listOf(transfer), 1).net)
        assertEquals(700L, OperationQuery.summary(listOf(transfer), 2).net)
        assertEquals(0L, OperationQuery.accountDelta(transfer, null))
    }
    @Test fun selfTransferDoesNotChangeBalance() { val o = op(type = OperationType.TRANSFER, dest = 1); assertEquals(0L, OperationQuery.summary(listOf(o), 1).net); assertEquals(0L, OperationQuery.accountDelta(o, 1)) }
    @Test fun noLimitHasNoDailyAllowance() { val p = BudgetPlanner.calculate(null, 100, listOf(rec()), emptyList(), today); assertNull(p.daily); assertEquals(100L, p.reserved) }
    @Test fun lastDayUsesOneDay() { val date = LocalDate.of(2026, 11, 30); assertEquals(700L, BudgetPlanner.calculate(1000, 200, listOf(rec(date)), emptyList(), date).daily) }
    @Test fun overspendDailyIsZero() { assertEquals(0L, BudgetPlanner.calculate(100, 200, emptyList(), emptyList(), today).daily) }
    @Test fun reserveCanExceedRemainingBudget() { val p = BudgetPlanner.calculate(1000, 900, listOf(rec(amount = 200)), emptyList(), today); assertEquals(-100L, p.available); assertEquals(0L, p.daily) }
    @Test fun weeklyPaymentsCountEveryRemainingOccurrence() { val p = BudgetPlanner.calculate(10000, 0, listOf(rec(period = RecurrencePeriod.WEEKLY)), emptyList(), today); assertEquals(4, p.paymentCount); assertEquals(400L, p.reserved) }
    @Test fun alreadyPostedExpenseIsNotReservedAgain() { assertEquals(0L, BudgetPlanner.calculate(10000, 100, listOf(rec()), listOf(op()), today).reserved) }
    @Test fun onePostedOperationMatchesOnlyOneSubscription() { assertEquals(100L, BudgetPlanner.calculate(10000, 100, listOf(rec(), rec().copy(id = 2)), listOf(op()), today).reserved) }
    @Test fun nextMonthPaymentDoesNotReserveThisMonth() { assertEquals(0L, BudgetPlanner.calculate(1000, 0, listOf(rec(today.plusMonths(1))), emptyList(), today).reserved) }
    @Test fun movingDateRecalculatesReserve() { assertEquals(100L, BudgetPlanner.calculate(1000, 0, listOf(rec(today.plusDays(1))), emptyList(), today).reserved); assertEquals(0L, BudgetPlanner.calculate(1000, 0, listOf(rec(today.plusMonths(1))), emptyList(), today).reserved) }
    @Test fun incomeAndTransfersAreNotMandatoryExpenses() { assertEquals(0, BudgetPlanner.calculate(1000, 0, listOf(rec().copy(type = OperationType.INCOME), rec().copy(type = OperationType.TRANSFER)), emptyList(), today).paymentCount) }
    @Test fun sameTitleDifferentAccountIsNotMatched() { assertEquals(100L, BudgetPlanner.calculate(1000, 100, listOf(rec()), listOf(op(account = 2)), today).reserved) }
    private fun csv(body: String) = CsvStatementParser.parse(body.toByteArray())
    @Test fun decimalMoneyIsExact() { assertEquals(29L, CsvStatementParser.parseMoney("0,29")); assertEquals(-50L, CsvStatementParser.parseMoney("-0.50")); assertEquals(123456L, CsvStatementParser.parseMoney("1\u202f234,56 ₽")) }
    @Test fun groupedInternationalAmounts() { assertEquals(123456L, CsvStatementParser.parseMoney("1,234.56")); assertEquals(123456L, CsvStatementParser.parseMoney("1.234,56")); assertEquals(-123456L, CsvStatementParser.parseMoney("(1234,56)")) }
    @Test fun ambiguousAndInvalidMoneyIsRejected() { listOf("1.234", "1,2,3", "NaN", "1e5", "99999999999999999999", "-92233720368547758.08").forEach { assertNull(it, CsvStatementParser.parseMoney(it)) } }
    @Test fun escapedQuotesAndDelimiterInDescription() { val p = csv("Дата;Сумма;Описание\n05.11.2026;-10,25;\"Кофе; \"\"большой\"\"\"\n"); assertEquals("Кофе; \"большой\"", p.rows.single().description); assertEquals(-1025L, p.rows.single().amount) }
    @Test fun multilineDescriptionsSupported() { assertEquals("Кофе\nи чай", csv("Дата;Сумма;Описание\n05.11.2026;-10;\"Кофе\nи чай\"\n").rows.single().description) }
    @Test fun invalidRowDoesNotBlockFollowing() { val p = csv("Дата;Сумма;Описание\n31.02.2026;-10;А\n05.11.2026;-0,29;Б\n05.11.2026;1.001;В\n"); assertEquals(3, p.total); assertEquals(2, p.rejected); assertEquals(-29L, p.rows.single().amount); assertEquals(2, p.issues.first().line) }
    @Test fun failedAndZeroRowsAreReported() { val p = csv("Дата;Сумма;Статус\n05.11.2026;10;FAILED\n05.11.2026;0;OK\n"); assertEquals(2, p.rejected); assertTrue(p.rows.isEmpty()) }
    @Test fun windows1251AutoDetection() { val p = CsvStatementParser.parse("Дата;Сумма;Описание\r\n05.11.2026;-10;Магазин\r\n".toByteArray(charset("windows-1251"))); assertEquals("Windows-1251", p.encoding); assertEquals("Магазин", p.rows.single().description) }
    @Test fun delimiterDetectionIgnoresPreamble() { val p = csv("Отчёт,банк,2026\nДата;Сумма;Описание\n05.11.2026;-10;Магазин\n"); assertEquals(";", p.delimiter); assertEquals(1, p.total) }
    @Test fun explicitDelimiterAndBom() { val p = CsvStatementParser.parse("\uFEFFДата\tСумма\tОписание\n05.11.2026\t-0,50\tКофе".toByteArray(), CsvOptions(delimiter = CsvDelimiter.TAB)); assertEquals(-50L, p.rows.single().amount) }
    @Test fun ownExportHeaderAndTypesSupported() { val p = csv("Дата;Тип;Сумма;Счёт;Счёт получателя;Категория;Название;Заметка\n05.11.2026 12:00;Расход;0,50;Карта;;Кафе;Кофе;\n05.11.2026 12:00;Перевод;100;Карта;Кошелёк;;Перевод;\n"); assertEquals("Кофе", p.rows[0].description); assertEquals(-50L, p.rows[0].amount); assertTrue(p.rows[1].isTransfer) }
    @Test fun unclosedQuoteDoesNotSwallowLaterRows() { val p = csv("Дата;Сумма;Описание\n05.11.2026;-10;\"сломано\n06.11.2026;-20;Кофе\n"); assertEquals(1, p.rows.size); assertEquals(-2000L, p.rows.single().amount); assertTrue(p.rejected > 0) }
}

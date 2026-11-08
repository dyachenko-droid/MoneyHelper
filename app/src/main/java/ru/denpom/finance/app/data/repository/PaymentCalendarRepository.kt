package ru.denpom.finance.app.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.map
import ru.denpom.finance.app.data.db.AppDatabase
import ru.denpom.finance.app.data.db.DebtPaymentEntity
import ru.denpom.finance.app.data.db.PaymentOccurrenceEntity
import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.domain.model.PaymentOccurrence
import ru.denpom.finance.app.domain.model.PaymentStatus
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.domain.model.Recurring
import ru.denpom.finance.app.domain.model.ScheduledPayment
import ru.denpom.finance.app.domain.model.Split
import ru.denpom.finance.app.domain.repository.OperationRepository
import ru.denpom.finance.app.domain.usecase.PaymentSchedule
import java.time.LocalDate
import javax.inject.Inject

class PaymentCalendarRepository @Inject constructor(
    private val db: AppDatabase,
    private val operations: OperationRepository,
) {
    private val entries = db.paymentOccurrenceDao()
    private val recurring = db.recurringDao()
    val occurrences = entries.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun resolve(
        payment: ScheduledPayment,
        status: PaymentStatus,
        movedTo: LocalDate = payment.occurrence.due,
        linkId: Long? = null,
    ) = db.withTransaction {
        val rule = requireNotNull(recurring.get(payment.rule.id)) { "Расписание удалено" }.toDomain()
        val date = payment.occurrence.scheduled
        val saved = entries.find(rule.id, date.toEpochDay())
        require(saved != null || PaymentSchedule.dates(rule, date).any { it == date }) { "Расписание изменилось" }
        val occurrence = saved?.toDomain() ?: PaymentOccurrence(rule.id, date)
        require(occurrence == payment.occurrence && occurrence.status == PaymentStatus.PENDING &&
            rule.copy(nextDate = payment.rule.nextDate) == payment.rule) { "Платёж изменился. Откройте его заново" }
        val current = ScheduledPayment(rule, occurrence)

        when (status) {
            PaymentStatus.PAID -> {
                val operationId = if (linkId != null) {
                    val op = requireNotNull(operations.get(linkId)) { "Операция удалена" }
                    require(current.canLink(op) && op.id !in entries.linkedOperations()) { "Операция уже связана или изменилась" }
                    op.id
                } else {
                    require(db.accountDao().get(rule.accountId) != null) { "Счёт платежа удалён" }
                    createOperation(rule, LocalDate.now())
                }
                save(occurrence.copy(status = status, operationId = operationId), saved?.id ?: 0)
            }
            PaymentStatus.SKIPPED -> save(occurrence.copy(status = status), saved?.id ?: 0)
            PaymentStatus.PENDING -> {
                require(movedTo >= LocalDate.now()) { "Выберите сегодняшнюю или будущую дату" }
                save(occurrence.copy(due = movedTo), saved?.id ?: 0)
            }
        }
    }

    suspend fun materializeAutomatic(today: LocalDate = LocalDate.now()) = db.withTransaction {
        // A moved payment may become due before its rule's next regular date.
        val automatic = recurring.all().filter { it.autoCreate }
        if (automatic.isEmpty()) return@withTransaction
        val linked = entries.linkedOperations().toMutableSet()
        val posted = operations.all()
        automatic.forEach { entity ->
            val rule = entity.toDomain()
            if (db.accountDao().get(rule.accountId) == null) return@forEach
            val saved = entries.forSchedule(rule.id).associateBy { LocalDate.ofEpochDay(it.scheduledDay) }
            val regular = PaymentSchedule.dates(rule, today).take(24).toList()
            val movedDue = saved.values.asSequence()
                .filter { it.status == PaymentStatus.PENDING.name && it.dueDay <= today.toEpochDay() }
                .map { LocalDate.ofEpochDay(it.scheduledDay) }
                .filterNot { it in regular }
                .toList()
            (movedDue + regular).forEach occurrenceLoop@{ date ->
                val existing = saved[date]
                val occurrence = existing?.toDomain() ?: PaymentOccurrence(rule.id, date)
                if (occurrence.status != PaymentStatus.PENDING || occurrence.due > today) return@occurrenceLoop
                val payment = ScheduledPayment(rule, occurrence)
                val operationId = posted.firstOrNull { it.id !in linked && payment.isRecordedAsScheduled(it) }?.id
                    ?: createOperation(rule, occurrence.due)
                linked += operationId
                save(occurrence.copy(status = PaymentStatus.PAID, operationId = operationId), existing?.id ?: 0)
            }
            regular.lastOrNull()?.let { last ->
                recurring.upsert(entity.copy(nextDate = Period.startOfDay(PaymentSchedule.advance(last, rule.period))))
            }
        }
    }

    suspend fun settleDebt(id: Long, expectedRemaining: Long) = db.withTransaction {
        val debts = db.debtDao()
        val debt = requireNotNull(debts.get(id)) { "Долг удалён" }
        val remaining = debt.amount - debts.paid(id)
        require(remaining > 0 && remaining == expectedRemaining) { "Остаток долга изменился" }
        debts.insertPayment(DebtPaymentEntity(debtId = id, amount = remaining, timestamp = System.currentTimeMillis()))
        Unit
    }

    suspend fun moveDebt(id: Long, expectedDate: Long?, date: LocalDate) = db.withTransaction {
        val debts = db.debtDao()
        val debt = requireNotNull(debts.get(id)) { "Долг удалён" }
        require(debt.dueDate == expectedDate && debt.amount > debts.paid(id)) { "Долг изменился" }
        require(date >= LocalDate.now()) { "Выберите сегодняшнюю или будущую дату" }
        debts.reschedule(id, Period.startOfDay(date))
    }

    private suspend fun createOperation(rule: Recurring, date: LocalDate): Long = operations.save(
        Operation(
            id = 0, type = rule.type, amount = rule.amount, accountId = rule.accountId,
            toAccountId = null, categoryId = rule.categoryId, title = rule.title, note = "",
            timestamp = date.atTime(12, 0).atZone(Period.zone).toInstant().toEpochMilli(), receiptId = null,
            splits = rule.categoryId?.let { listOf(Split(it, rule.amount)) }.orEmpty(),
        ),
    )

    private suspend fun save(occurrence: PaymentOccurrence, id: Long) {
        entries.save(PaymentOccurrenceEntity(
            id, occurrence.recurringId, occurrence.scheduled.toEpochDay(), occurrence.due.toEpochDay(),
            occurrence.status.name, occurrence.operationId,
        ))
    }

    private fun PaymentOccurrenceEntity.toDomain() = PaymentOccurrence(
        recurringId, LocalDate.ofEpochDay(scheduledDay), LocalDate.ofEpochDay(dueDay),
        PaymentStatus.valueOf(status), operationId,
    )
}

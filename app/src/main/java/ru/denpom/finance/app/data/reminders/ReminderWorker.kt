package ru.denpom.finance.app.data.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import ru.denpom.finance.app.MainActivity
import ru.denpom.finance.app.R
import ru.denpom.finance.app.domain.model.DebtDirection
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.domain.repository.DebtRepository
import ru.denpom.finance.app.domain.repository.OperationRepository
import ru.denpom.finance.app.domain.repository.RecurringRepository
import ru.denpom.finance.app.domain.repository.SettingsRepository
import ru.denpom.finance.app.presentation.format.MoneyFormat
import java.time.LocalDate

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun settings(): SettingsRepository
    fun operations(): OperationRepository
    fun recurring(): RecurringRepository
    fun debts(): DebtRepository
    fun scheduler(): ReminderScheduler
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, ReminderEntryPoint::class.java)
        val settings = deps.settings().current()
        val fmt = MoneyFormat(settings.currency.symbol)
        val today = LocalDate.now()
        if (settings.dailyReminder) {
            val start = Period.startOfDay(today)
            val hasToday = deps.operations().all().any { it.timestamp >= start }
            if (!hasToday) {
                notify(1, "Сегодня ещё нет записей", "Добавьте траты за день или отсканируйте чек — это займёт минуту")
            }
        }
        if (settings.paymentReminders) {
            val tomorrow = Period.startOfDay(today.plusDays(1))
            val after = Period.startOfDay(today.plusDays(2))
            val payments = deps.recurring().all().filter { it.type == OperationType.EXPENSE && it.nextDate in tomorrow until after }
            val debts = deps.debts().observeDebts().first().filter { it.direction == DebtDirection.FROM_ME && !it.closed && it.dueDate in tomorrow until after }
            val lines = payments.map { "${it.title} — ${fmt.format(it.amount, it.amount % 100 != 0L)}" } +
                debts.map { "Вернуть ${it.person} — ${fmt.format(it.left, false)}" }
            if (lines.isNotEmpty()) {
                val total = payments.sumOf { it.amount } + debts.sumOf { it.left }
                notify(2, "Завтра спишется ${fmt.format(total, false)}", lines.joinToString("\n"))
            }
        }
        deps.scheduler().apply(settings)
        return Result.success()
    }

    private fun notify(id: Int, title: String, text: String) {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Напоминания", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF1364CE.toInt())
            .setContentTitle(title)
            .setContentText(text.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private companion object {
        const val CHANNEL = "reminders"
    }
}

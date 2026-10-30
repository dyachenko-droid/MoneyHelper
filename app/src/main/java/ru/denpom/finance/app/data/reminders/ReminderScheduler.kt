package ru.denpom.finance.app.data.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.denpom.finance.app.domain.model.AppSettings
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun apply(settings: AppSettings) {
        val manager = WorkManager.getInstance(context)
        if (!settings.dailyReminder && !settings.paymentReminders) {
            manager.cancelUniqueWork(WORK)
            return
        }
        val time = LocalTime.of(settings.reminderMinutes / 60, settings.reminderMinutes % 60)
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(time)
        if (!next.isAfter(now.plusMinutes(1))) next = next.plusDays(1)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        manager.enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, request)
    }

    private companion object {
        const val WORK = "daily_reminder"
    }
}

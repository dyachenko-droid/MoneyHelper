package ru.denpom.finance.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import ru.denpom.finance.app.data.db.AppDatabase
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.domain.repository.AccountRepository
import ru.denpom.finance.app.domain.repository.BackupRepository
import ru.denpom.finance.app.domain.repository.CategoryRepository
import ru.denpom.finance.app.domain.repository.OperationRepository
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val operations: OperationRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
) : BackupRepository {

    override suspend fun exportCsv(target: String): Int = withContext(Dispatchers.IO) {
        val ops = operations.all()
        val accNames = accounts.observeAccounts().first().associate { it.id to it.name }
        val catNames = categories.categories().associate { it.id to it.name }
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        val sb = StringBuilder("﻿")
        sb.append("Дата;Тип;Сумма;Счёт;Счёт получателя;Категория;Название;Заметка\n")
        ops.sortedBy { it.timestamp }.forEach { op ->
            val date = Instant.ofEpochMilli(op.timestamp).atZone(Period.zone).format(formatter)
            val type = when (op.type) {
                OperationType.EXPENSE -> "Расход"
                OperationType.INCOME -> "Доход"
                OperationType.TRANSFER -> "Перевод"
            }
            val amount = (if (op.type == OperationType.EXPENSE) -op.amount else op.amount).let { "%d,%02d".format(it / 100, kotlin.math.abs(it % 100)) }
            val row = listOf(
                date, type, amount, accNames[op.accountId].orEmpty(), op.toAccountId?.let { accNames[it] }.orEmpty(),
                op.categoryId?.let { catNames[it] }.orEmpty(), op.title, op.note,
            ).joinToString(";") { "\"" + it.replace("\"", "\"\"") + "\"" }
            sb.append(row).append('\n')
        }
        context.contentResolver.openOutputStream(Uri.parse(target), "wt")?.use { it.write(sb.toString().toByteArray()) }
        ops.size
    }

    override suspend fun backup(target: String) = withContext(Dispatchers.IO) {
        db.openHelper.writableDatabase.query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)")).use { it.moveToFirst() }
        val file = context.getDatabasePath(AppDatabase.NAME)
        context.contentResolver.openOutputStream(Uri.parse(target), "wt")?.use { out -> file.inputStream().use { it.copyTo(out) } }
        Unit
    }

    override suspend fun restore(source: String): Boolean = withContext(Dispatchers.IO) {
        val temp = File(context.cacheDir, "restore.db")
        context.contentResolver.openInputStream(Uri.parse(source))?.use { input -> temp.outputStream().use { input.copyTo(it) } } ?: return@withContext false
        val header = temp.inputStream().use { ByteArray(16).also { b -> it.read(b) } }
        if (!String(header).startsWith("SQLite format 3")) return@withContext false
        db.close()
        val file = context.getDatabasePath(AppDatabase.NAME)
        File(file.path + "-wal").delete()
        File(file.path + "-shm").delete()
        temp.copyTo(file, overwrite = true)
        temp.delete()
        true
    }
}

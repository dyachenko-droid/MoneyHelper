package ru.denpom.finance.app.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.denpom.finance.app.data.backup.BackupDatabase
import ru.denpom.finance.app.data.backup.DatabaseSnapshot
import ru.denpom.finance.app.data.db.AppDatabase
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.model.Period
import ru.denpom.finance.app.domain.repository.*
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    db: AppDatabase,
    private val operations: OperationRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val settings: SettingsRepository,
) : BackupRepository {
    private val engine = BackupDatabase(context, db)
    private val mutex = Mutex()
    private val prefs = context.getSharedPreferences("backup_status", Context.MODE_PRIVATE)
    private val rollbackFile get() = File(File(context.filesDir, "backups").apply { mkdirs() }, "previous.backup")
    private data class Staged(val dir: File, val loaded: BackupDatabase.Loaded, val photos: Map<Long, File>)
    private val staged = mutableMapOf<String, Staged>()
    private val maxBytes = 128L * 1024 * 1024

    override suspend fun exportCsv(target: String): Int = withContext(Dispatchers.IO) {
        val ops = operations.all()
        val accNames = accounts.observeAccounts().first().associate { it.id to it.name }
        val catNames = categories.categories().associate { it.id to it.name }
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        val sb = StringBuilder("\uFEFF")
        sb.append("Дата;Тип;Сумма;Счёт;Счёт получателя;Категория;Название;Заметка\n")
        ops.sortedBy { it.timestamp }.forEach { op ->
            val date = Instant.ofEpochMilli(op.timestamp).atZone(Period.zone).format(formatter)
            val type = when (op.type) {
                OperationType.EXPENSE -> "Расход"
                OperationType.INCOME -> "Доход"
                OperationType.TRANSFER -> "Перевод"
            }
            val amount = java.math.BigDecimal.valueOf(if (op.type == OperationType.EXPENSE) -op.amount else op.amount, 2).toPlainString().replace('.', ',')
            val row = listOf(
                date, type, amount, accNames[op.accountId].orEmpty(), op.toAccountId?.let { accNames[it] }.orEmpty(),
                op.categoryId?.let { catNames[it] }.orEmpty(), op.title, op.note,
            ).joinToString(";") { "\"" + it.replace("\"", "\"\"") + "\"" }
            sb.append(row).append('\n')
        }
        requireNotNull(context.contentResolver.openOutputStream(Uri.parse(target), "wt")) { "Не удалось открыть файл для записи" }.use { it.write(sb.toString().toByteArray(Charsets.UTF_8)); it.flush() }
        ops.size
    }

    override suspend fun status() = withContext(Dispatchers.IO) {
        BackupStatus(prefs.getLong("last_success", 0).takeIf { it > 0 }, rollbackFile.isFile)
    }
    override suspend fun backup(target: String) = withContext(Dispatchers.IO) { mutex.withLock {
        val temp = File.createTempFile("backup-", ".zip", context.cacheDir)
        try {
            val data = engine.snapshot()
            archive(temp, data, settings.current().currency.name)
            requireNotNull(context.contentResolver.openOutputStream(Uri.parse(target), "wt")) { "Не удалось открыть файл для записи" }.use { out ->
                temp.inputStream().use { it.copyTo(out) }; out.flush()
            }
            // Only a fully written and closed destination is a successful backup.
            prefs.edit().putLong("last_success", System.currentTimeMillis()).commit()
            Unit
        } finally { temp.delete() }
    } }
    override suspend fun preview(source: String): BackupPreview = withContext(Dispatchers.IO) { mutex.withLock {
        val dir = File(context.cacheDir, "restore-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val file = File(dir, "source")
            requireNotNull(context.contentResolver.openInputStream(Uri.parse(source))) { "Не удалось открыть копию" }.use { input ->
                file.outputStream().use { copyBounded(input, it, maxBytes) }
            }
            val stage = stage(file, dir)
            val currency = settings.current().currency.name
            require(stage.loaded.currency == null || stage.loaded.currency == currency) { "В копии валюта ${stage.loaded.currency}. Сначала выберите её в настройках приложения" }
            val token = UUID.randomUUID().toString()
            staged.values.forEach { it.dir.deleteRecursively() }
            staged.clear()
            staged[token] = stage
            summary(token, stage)
        } catch (e: Exception) { dir.deleteRecursively(); throw e }
    } }
    override suspend fun discard(token: String) = withContext(Dispatchers.IO) { mutex.withLock {
        staged.remove(token)?.dir?.deleteRecursively()
        Unit
    } }
    override suspend fun restore(token: String) = withContext(Dispatchers.IO + NonCancellable) { mutex.withLock {
        val source = requireNotNull(staged[token]) { "Выберите копию заново" }
        try { apply(source) } finally { staged.remove(token); source.dir.deleteRecursively() }
    } }
    override suspend fun rollback() = withContext(Dispatchers.IO + NonCancellable) { mutex.withLock {
        require(rollbackFile.isFile) { "Копия для отката не найдена" }
        val dir = File(context.cacheDir, "rollback-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            // Snapshot the rollback file before apply replaces it with the current state.
            val source = rollbackFile.copyTo(File(dir, "source"))
            apply(stage(source, dir))
        } finally { dir.deleteRecursively() }
    } }
    private suspend fun apply(source: Staged) {
        val currency = settings.current().currency.name
        require(source.loaded.currency == null || source.loaded.currency == currency) { "Валюта изменилась. Выберите копию заново" }
        val photosDir = File(context.filesDir, "receipts").apply { mkdirs() }
        val created = mutableListOf<File>()
        try {
            val receipts = source.loaded.data.rows.getValue("receipts").map { row ->
                val photo = source.photos[row[0] as Long]
                val path = if (photo != null) {
                    val dest = File(photosDir, "restored-${UUID.randomUUID()}.jpg")
                    created += dest
                    photo.copyTo(dest)
                    dest.path
                } else safePhoto(row[7] as String?)?.path
                row.toMutableList().apply { this[7] = path }.toList()
            }
            val data = source.loaded.data.copy(rows = source.loaded.data.rows + ("receipts" to receipts))
            engine.replace(data) { current ->
                val candidate = File(rollbackFile.parentFile, "candidate-${UUID.randomUUID()}.backup")
                try {
                    archive(candidate, current, currency)
                    Files.move(candidate.toPath(), rollbackFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                } finally { candidate.delete() }
            }
        } catch (e: Throwable) { created.forEach { it.delete() }; throw e }
    }
    private fun safePhoto(path: String?): File? = path?.let { raw ->
        runCatching {
            val file = File(raw).canonicalFile
            val root = File(context.filesDir, "receipts").canonicalFile
            file.takeIf { it.isFile && it.path.startsWith(root.path + File.separator) }
        }.getOrNull()
    }
    private fun archive(target: File, data: DatabaseSnapshot, currency: String) {
        val sqlFile = File.createTempFile("snapshot-", ".db", context.cacheDir)
        try {
            engine.write(sqlFile, data, currency, System.currentTimeMillis())
            val photos = data.rows.getValue("receipts").mapNotNull { row -> safePhoto(row[7] as String?)?.let { row[0] as Long to it } }
            require(sqlFile.length() + photos.sumOf { it.second.length() } <= maxBytes - 1024 * 1024) { "Копия больше 128 МБ. Сохраните фотографии отдельно" }
            target.outputStream().use { fileOut ->
                ZipOutputStream(fileOut).use { zip ->
                    zip.putNextEntry(ZipEntry("data.db")); sqlFile.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
                    photos.forEach { (id, file) ->
                        zip.putNextEntry(ZipEntry("photos/$id.jpg")); file.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
                    }
                    zip.finish(); zip.flush(); fileOut.fd.sync()
                }
            }
        } finally { sqlFile.delete() }
    }
    private fun stage(file: File, dir: File): Staged {
        val sqlite = file.inputStream().use { input -> val b = ByteArray(16); input.read(b); String(b, Charsets.US_ASCII).startsWith("SQLite format 3") }
        val photos = mutableMapOf<Long, File>()
        val database = if (sqlite) file else {
            ZipFile(file).use { zip ->
                val seen = mutableSetOf<String>()
                var total = 0L
                zip.entries().asSequence().forEach { entry ->
                    require(seen.add(entry.name) && !entry.isDirectory && (entry.name == "data.db" || entry.name.matches(Regex("photos/[1-9][0-9]*\\.jpg")))) { "Неизвестные или повторяющиеся файлы в копии" }
                    require(seen.size <= 5001) { "Слишком много файлов" }
                    val dest = if (entry.name == "data.db") File(dir, "data.db") else File(dir, "photo-${entry.name.substringAfter('/')}" )
                    val crc = CRC32()
                    val copied = zip.getInputStream(entry).use { input -> dest.outputStream().use { out -> copyBounded(input, out, maxBytes - total, crc) } }
                    total += copied
                    require(entry.crc == crc.value) { "Повреждён файл ${entry.name}" }
                    if (entry.name != "data.db") photos[entry.name.substringAfter('/').substringBefore('.').toLong()] = dest
                }
                require("data.db" in seen) { "В копии нет базы данных" }
            }
            File(dir, "data.db")
        }
        val loaded = engine.read(database)
        val ids = loaded.data.rows.getValue("receipts").map { it[0] as Long }.toSet()
        require(photos.keys.all { it in ids }) { "Фотография без чека" }
        return Staged(dir, loaded, photos)
    }
    private fun summary(token: String, s: Staged): BackupPreview {
        val d = s.loaded.data
        val withPhotos = d.rows.getValue("receipts").filter { it[7] != null }
        val missing = withPhotos.count { row -> row[0] !in s.photos && safePhoto(row[7] as String?) == null }
        return BackupPreview(token, d.count("accounts"), d.count("operations"), d.count("receipts"), d.count("goals"), d.count("debts"), s.photos.size, missing, s.loaded.createdAt, s.loaded.currency == null)
    }
    private fun copyBounded(input: InputStream, output: OutputStream, limit: Long, crc: CRC32? = null): Long {
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            total += n
            require(total <= limit) { "Копия больше допустимых 128 МБ" }
            crc?.update(buffer, 0, n)
            output.write(buffer, 0, n)
        }
        return total
    }
}

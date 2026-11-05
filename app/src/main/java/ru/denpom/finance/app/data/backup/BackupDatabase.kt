package ru.denpom.finance.app.data.backup

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import org.json.JSONObject
import ru.denpom.finance.app.data.db.AppDatabase
import ru.denpom.finance.app.domain.model.*
import java.io.File

internal data class BackupColumn(val name: String, val affinity: String, val nullable: Boolean)
internal data class BackupTable(val name: String, val columns: List<BackupColumn>, val createSql: String, val indices: List<String>)
internal data class DatabaseSnapshot(val rows: Map<String, List<List<Any?>>>) {
    fun count(table: String) = rows[table].orEmpty().size
}

/** Copies records, never replaces or closes the application's live SQLite file. */
internal class BackupDatabase(private val context: Context, private val db: AppDatabase) {
    private val schemas = (1..2).associateWith { version -> JSONObject(context.assets.open("backup_schema/$version.json").bufferedReader().use { it.readText() }).getJSONObject("database") }
    val tables: List<BackupTable> = schemaTables(2)
    private fun schemaTables(version: Int): List<BackupTable> {
        val entities = schemas.getValue(version).getJSONArray("entities")
        return (0 until entities.length()).map { n ->
            val e = entities.getJSONObject(n)
            val name = e.getString("tableName")
            val fields = e.getJSONArray("fields")
            val indices = e.optJSONArray("indices") ?: org.json.JSONArray()
            BackupTable(name, (0 until fields.length()).map { i -> fields.getJSONObject(i).let { BackupColumn(it.getString("columnName"), it.getString("affinity"), !it.optBoolean("notNull", false)) } },
                e.getString("createSql").replace("\${TABLE_NAME}", name),
                (0 until indices.length()).map { indices.getJSONObject(it).getString("createSql").replace("\${TABLE_NAME}", name) })
        }
    }
    suspend fun snapshot(): DatabaseSnapshot = db.withTransaction { capture() }
    fun capture(): DatabaseSnapshot {
        val sql = db.openHelper.writableDatabase
        val budget = ReadBudget()
        return DatabaseSnapshot(tables.associate { t -> t.name to sql.query("SELECT ${columns(t)} FROM `${t.name}`").use { readRows(it, budget) } })
    }
    private fun columns(t: BackupTable) = t.columns.joinToString(",") { "`${it.name}`" }
    private class ReadBudget(var rows: Int = 0, var bytes: Long = 0)
    private fun readRows(cursor: Cursor, budget: ReadBudget): List<List<Any?>> = buildList {
        while (cursor.moveToNext()) {
            require(++budget.rows <= 200_000) { "В таблице слишком много записей" }
            budget.bytes += cursor.columnCount * 24L
            require(budget.bytes <= 64 * 1024 * 1024) { "Копия содержит слишком много данных" }
            add((0 until cursor.columnCount).map { i ->
                when (cursor.getType(i)) {
                    Cursor.FIELD_TYPE_NULL -> null
                    Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
                    Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i).also { require(it.isFinite()) { "Некорректное число" } }
                    Cursor.FIELD_TYPE_STRING -> cursor.getString(i).also { budget.bytes += it.length * 2L; require(budget.bytes <= 64 * 1024 * 1024) { "Слишком большой объём текста" } }
                    else -> error("Недопустимый тип данных в копии")
                }
            })
        }
    }
    fun write(file: File, data: DatabaseSnapshot, currency: String, createdAt: Long) {
        SQLiteDatabase.openOrCreateDatabase(file, null).use { output ->
            output.setForeignKeyConstraintsEnabled(true)
            output.beginTransaction()
            try {
                tables.forEach { t -> output.execSQL(t.createSql); t.indices.forEach(output::execSQL) }
                insert(data) { sql, args -> output.execSQL(sql, args) }
                output.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                output.execSQL("INSERT INTO room_master_table VALUES(42,?)", arrayOf(schemas.getValue(2).getString("identityHash")))
                output.execSQL("CREATE TABLE backup_metadata (format TEXT NOT NULL, currency TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                output.execSQL("INSERT INTO backup_metadata VALUES(?,?,?)", arrayOf<Any>("ru.denpom.finance.app/1", currency, createdAt))
                output.version = 2
                output.setTransactionSuccessful()
            } finally { output.endTransaction() }
        }
    }
    data class Loaded(val data: DatabaseSnapshot, val currency: String?, val createdAt: Long?)
    fun read(file: File): Loaded = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { input ->
        require(input.version in 1..2) { "Неподдерживаемая версия базы" }
        input.rawQuery("PRAGMA integrity_check", null).use { require(it.moveToFirst() && it.getString(0) == "ok" && !it.moveToNext()) { "Копия повреждена или неполна" } }
        val identity = input.rawQuery("SELECT identity_hash FROM room_master_table WHERE id=42", null).use { require(it.moveToFirst()) { "Чужая база данных" }; it.getString(0) }
        require(identity == schemas.getValue(input.version).getString("identityHash")) { "Это не копия Денежного помощника" }
        val expected = schemaTables(input.version)
        val names = input.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c -> buildSet { while(c.moveToNext()) add(c.getString(0)) } }
        require(names.containsAll(expected.map { it.name }) && names.all { it in expected.map { t -> t.name } || it in setOf("room_master_table", "android_metadata", "sqlite_sequence", "backup_metadata") }) { "Неверный состав таблиц" }
        val budget = ReadBudget()
        val rows = expected.associate { t ->
            input.rawQuery("PRAGMA table_info(`${t.name}`)", null).use { c ->
                val actual = buildList { while(c.moveToNext()) add(c.getString(1) to c.getString(2)) }
                require(actual == t.columns.map { it.name to it.affinity }) { "Неверная структура ${t.name}" }
            }
            t.name to input.rawQuery("SELECT ${columns(t)} FROM `${t.name}`", null).use { readRows(it, budget) }
        }.toMutableMap()
        tables.forEach { rows.putIfAbsent(it.name, emptyList()) }
        input.rawQuery("PRAGMA foreign_key_check", null).use { require(!it.moveToFirst()) { "Нарушены связи записей" } }
        val data = DatabaseSnapshot(rows)
        validate(data)
        val meta = if ("backup_metadata" in names) input.rawQuery("SELECT format,currency,createdAt FROM backup_metadata", null).use {
            require(it.moveToFirst() && it.getString(0) == "ru.denpom.finance.app/1") { "Неизвестный формат копии" }
            val result = it.getString(1) to it.getLong(2)
            require(!it.moveToNext())
            result
        } else null
        Loaded(data, meta?.first, meta?.second)
    }
    private fun validate(data: DatabaseSnapshot) {
        val enums = mapOf(
            "accounts.type" to AccountType.entries.map { it.name }, "categories.kind" to CategoryKind.entries.map { it.name },
            "operations.type" to OperationType.entries.map { it.name }, "recurring.type" to listOf("EXPENSE", "INCOME"),
            "recurring.period" to RecurrencePeriod.entries.map { it.name }, "receipts.status" to ReceiptStatus.entries.map { it.name },
            "debts.direction" to DebtDirection.entries.map { it.name },
        )
        tables.forEach { t ->
            val pk = if (t.name == "rules") "pattern" else if (t.name == "budgets") "categoryId" else "id"
            val seen = HashSet<Any?>()
            data.rows.getValue(t.name).forEach { row ->
                require(row.size == t.columns.size)
                t.columns.forEachIndexed { i, col ->
                    val value = row[i]
                    if (value == null) require(col.nullable) { "Отсутствует ${t.name}.${col.name}" }
                    else {
                        require(when(col.affinity) { "INTEGER" -> value is Long; "REAL" -> value is Double || value is Long; "TEXT" -> value is String; else -> false }) { "Неверный тип ${t.name}.${col.name}" }
                        enums["${t.name}.${col.name}"]?.let { require(value in it) { "Неизвестное значение ${t.name}.${col.name}" } }
                        if (col.name in setOf("isSubscription", "autoCreate")) require(value == 0L || value == 1L)
                        if (col.name == "id") require((value as Long) > 0)
                        if (col.name in setOf("timestamp", "nextDate", "createdAt", "startDate", "deadline", "dueDate", "warrantyUntil")) require((value as Long) in 0..253402214400000L) { "Некорректная дата" }
                        if (col.name == "amount" && t.name != "goal_entries") require((value as Long) >= 0 && value <= Long.MAX_VALUE / 1000) { "Некорректная сумма" }
                        if (col.name == "termMonths") require((value as Long) in 1..1200)
                        if (col.name == "ratePercent") require((value as Number).toDouble() in 0.0..100_000.0)
                    }
                }
                require(seen.add(row[t.columns.indexOfFirst { it.name == pk }])) { "Повторяющийся ключ ${t.name}" }
            }
        }
        // Validate true foreign keys even if a foreign database disabled enforcement.
        listOf(Triple("splits", "operationId", "operations"), Triple("receipt_items", "receiptId", "receipts"), Triple("goal_entries", "goalId", "goals"), Triple("debt_payments", "debtId", "debts")).forEach { (child, column, parent) ->
            val childIndex = tables.first { it.name == child }.columns.indexOfFirst { it.name == column }
            val ids = data.rows.getValue(parent).map { it[0] }.toSet()
            require(data.rows.getValue(child).all { it[childIndex] in ids }) { "Нарушены связи $child" }
        }
    }
    private val ordered get() = tables.sortedBy { if (it.name in setOf("splits", "receipt_items", "goal_entries", "debt_payments")) 1 else 0 }
    private fun insert(data: DatabaseSnapshot, exec: (String, Array<Any?>) -> Unit) {
        ordered.forEach { t ->
            val sql = "INSERT INTO `${t.name}` (${columns(t)}) VALUES (${t.columns.joinToString(",") { "?" }})"
            data.rows.getValue(t.name).forEach { exec(sql, it.toTypedArray()) }
        }
    }
    /** Caller can save a rollback snapshot inside this SAME transaction, before the first delete. */
    suspend fun replace(data: DatabaseSnapshot, beforeReplace: (DatabaseSnapshot) -> Unit) {
        validate(data)
        db.withTransaction {
            beforeReplace(capture())
            val sql = db.openHelper.writableDatabase
            ordered.asReversed().forEach { sql.execSQL("DELETE FROM `${it.name}`") }
            insert(data) { query, args -> sql.execSQL(query, args) }
        }
        db.invalidationTracker.refreshAsync()
    }
}

package ru.denpom.finance.app

import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.json.JSONObject
import ru.denpom.finance.app.data.backup.*
import ru.denpom.finance.app.data.db.*
import ru.denpom.finance.app.data.repository.*
import ru.denpom.finance.app.domain.model.*
import ru.denpom.finance.app.domain.repository.*
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile

@RunWith(AndroidJUnit4::class)
class BackupSafetyTest {
    private lateinit var db: AppDatabase
    private lateinit var context: Context
    private lateinit var root: File
    private lateinit var engine: BackupDatabase
    private lateinit var repo: BackupRepositoryImpl
    private val settings = TestSettings()
    @Before fun setup() = runBlocking {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val prefix = "backup-test-${UUID.randomUUID()}"
        root = File(target.cacheDir, prefix).apply { mkdirs() }
        context = object : ContextWrapper(target) {
            override fun getFilesDir() = File(root, "files").apply { mkdirs() }
            override fun getCacheDir() = File(root, "cache").apply { mkdirs() }
            override fun getSharedPreferences(name: String, mode: Int) = target.getSharedPreferences("$prefix-$name", mode)
        }
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        engine = BackupDatabase(context, db)
        repo = BackupRepositoryImpl(context, db, OperationRepositoryImpl(db.operationDao()), AccountRepositoryImpl(db.accountDao(), db.operationDao()), CategoryRepositoryImpl(db.categoryDao(), db.operationDao()), settings)
        db.accountDao().upsert(AccountEntity(id = 1, name = "Карта", type = "CARD", subtitle = "", openingBalance = 50000, creditLimit = null))
        db.operationDao().saveWithSplits(OperationEntity(id = 1, type = "EXPENSE", amount = 50, accountId = 1, toAccountId = null, categoryId = null, title = "Кофе", note = "", timestamp = 1_794_000_000_000, receiptId = null), emptyList())
        Unit
    }
    @After fun cleanup() { db.close(); root.deleteRecursively() }
    private fun file(name: String) = File(root, name)
    private fun uri(file: File) = Uri.fromFile(file).toString()
    private suspend fun fails(block: suspend () -> Unit) { try { block(); fail("Expected rejection") } catch (e: Exception) { /* expected */ } }

    @Test fun snapshotRoundTripPreservesAllTablesAndMoney() = runBlocking {
        db.goalDao().upsert(GoalEntity(1, "Отпуск", 100000, null, "savings", 120f, 1_794_000_000_000))
        db.goalDao().insertEntry(GoalEntryEntity(goalId = 1, amount = -50, timestamp = 1_794_000_000_000, operationId = null))
        db.categoryDao().upsert(CategoryEntity(1, "Продукты", "shopping_basket", 150f, "EXPENSE", 0))
        db.categoryDao().insertRule(RuleEntity("магазин", 1))
        db.receiptDao().saveWithItems(ReceiptEntity(1, "Магазин", "Адрес", 1_794_000_000_000, 50, null, "VERIFIED", null, null), listOf(ReceiptItemEntity(receiptId = 1, position = 0, name = "Яблоко", quantity = 0.5, unit = "кг", price = 100, sum = 50, categoryId = 1)))
        db.operationDao().saveWithSplits(OperationEntity(2, "TRANSFER", 1000, 1, 2, null, "Перевод", "", 1_794_000_000_000, null), emptyList())
        db.operationDao().saveWithSplits(OperationEntity(3, "EXPENSE", 50, 1, null, 1, "Продукты", "", 1_794_000_000_000, 1), listOf(SplitEntity(operationId = 3, categoryId = 1, amount = 50)))
        db.budgetDao().upsert(BudgetEntity(0, 100000))
        db.recurringDao().upsert(RecurringEntity(1, "Подписка", 19900, "EXPENSE", 1, 1, "MONTHLY", 1_794_000_000_000, true, false))
        db.debtDao().upsert(DebtEntity(1, "Друг", "TO_ME", 100000, "", 1_794_000_000_000, null))
        db.debtDao().insertPayment(DebtPaymentEntity(debtId = 1, amount = 50, timestamp = 1_794_000_000_000))
        db.debtDao().upsertLoan(LoanEntity(1, "Кредит", 100000, 12.5, 12, 1_794_000_000_000, 1))
        val original = engine.snapshot()
        val out = file("data.db")
        engine.write(out, original, "RUB", 123)
        val loaded = engine.read(out)
        assertEquals(original, loaded.data); assertEquals("RUB", loaded.currency); assertEquals(123L, loaded.createdAt)
        engine.replace(loaded.data) {}
        assertEquals(original, engine.snapshot())
    }
    @Test fun exceptionBeforeReplaceLeavesAllFinancialData() = runBlocking {
        val before = engine.snapshot()
        fails { engine.replace(before) { error("disk full before rollback") } }
        assertEquals(before, engine.snapshot())
    }
    @Test fun sqlFailureAfterDeletesRollsBackEverything() = runBlocking {
        val before = engine.snapshot()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_insert BEFORE INSERT ON operations BEGIN SELECT RAISE(ABORT,'test write failure'); END")
        fails { engine.replace(before) {} }
        assertEquals(before, engine.snapshot())
    }
    @Test fun corruptTruncatedAndForeignFilesNeverChangeDatabase() = runBlocking {
        val before = engine.snapshot()
        val valid = file("valid.db"); engine.write(valid, before, "RUB", 123)
        file("short.db").writeBytes(valid.readBytes().take(100).toByteArray())
        file("foreign.db").writeText("SQLite format 3\u0000not a database")
        listOf(file("short.db"), file("foreign.db")).forEach { bad -> fails { repo.preview(uri(bad)) } }
        assertEquals(before, engine.snapshot())
    }
    @Test fun unknownEnumIsRejectedBeforeConfirmation() = runBlocking {
        val before = engine.snapshot(); val out = file("bad.db")
        engine.write(out, before, "RUB", 123)
        SQLiteDatabase.openDatabase(out.path, null, 0).use { it.execSQL("UPDATE operations SET type='UNKNOWN'") }
        fails { repo.preview(uri(out)) }; assertEquals(before, engine.snapshot())
    }
    @Test fun wrongCurrencyIsRejectedWithoutChangingData() = runBlocking {
        val before = engine.snapshot(); val out = file("usd.db"); engine.write(out, before, "USD", 123)
        fails { repo.preview(uri(out)) }; assertEquals(before, engine.snapshot())
    }
    @Test fun previewCancelIsReadOnlyAndTokenCannotBeUsed() = runBlocking {
        val before = engine.snapshot(); val out = file("copy.backup"); repo.backup(uri(out))
        val p = repo.preview(uri(out)); assertEquals(1, p.operations); assertEquals(1, p.accounts)
        assertEquals(before, engine.snapshot()); repo.discard(p.token)
        fails { repo.restore(p.token) }; assertEquals(before, engine.snapshot())
    }
    @Test fun confirmedRestoreAndRollbackPreservePhotosAndPreviousState() = runBlocking {
        val photo = File(context.filesDir, "receipts/test.jpg").apply { parentFile!!.mkdirs(); writeBytes(byteArrayOf(1,2,3,4)) }
        db.receiptDao().insert(ReceiptEntity(1, "Магазин", "", 1_794_000_000_000, 50, null, "RECOGNIZED", photo.path, null))
        val out = file("copy.backup"); repo.backup(uri(out))
        assertTrue(ZipFile(out).use { it.getEntry("photos/1.jpg") != null })
        val original = engine.snapshot()
        db.accountDao().upsert(AccountEntity(1, "Изменено", "CARD", "", 90000, null))
        val changed = engine.snapshot()
        val p = repo.preview(uri(out)); assertEquals(1, p.photos); repo.restore(p.token)
        assertEquals(original.rows["accounts"], engine.snapshot().rows["accounts"])
        val restoredPhoto = File(engine.snapshot().rows.getValue("receipts").single()[7] as String)
        assertArrayEquals(byteArrayOf(1,2,3,4), restoredPhoto.readBytes())
        assertTrue(repo.status().canRollback)
        repo.rollback()
        assertEquals(changed.rows["accounts"], engine.snapshot().rows["accounts"])
        assertTrue(photo.isFile)
    }
    @Test fun failedBackupDestinationDoesNotRecordSuccess() = runBlocking {
        val before = engine.snapshot()
        fails { repo.backup(uri(root)) }
        assertNull(repo.status().lastBackup); assertEquals(before, engine.snapshot())
    }
    @Test fun csvExportKeepsSignOfSubunitExpense() = runBlocking {
        val out = file("operations.csv"); assertEquals(1, repo.exportCsv(uri(out)))
        assertTrue(out.readText().contains("\"-0,50\""))
        fails { repo.exportCsv(uri(root)) }
    }
    @Test fun legacyVersionOneBackupIsAcceptedAndMigratedInMemory() = runBlocking {
        val out = file("v1.db")
        val schema = JSONObject(context.assets.open("backup_schema/1.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(out, null).use { legacy ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) { val entity = entities.getJSONObject(i); legacy.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName"))) }
            legacy.execSQL("CREATE TABLE room_master_table(id INTEGER PRIMARY KEY,identity_hash TEXT)")
            legacy.execSQL("INSERT INTO room_master_table VALUES(42,?)", arrayOf(schema.getString("identityHash")))
            legacy.execSQL("INSERT INTO accounts VALUES(1,'Старый счёт','CASH','',12345,NULL,0)")
            legacy.version = 1
        }
        val p = repo.preview(uri(out)); assertTrue(p.legacy); assertEquals(1, p.accounts)
        repo.restore(p.token)
        assertEquals("Старый счёт", engine.snapshot().rows.getValue("accounts").single()[1])
        assertEquals(0, engine.snapshot().count("goals"))
        repo.rollback(); assertEquals("Карта", engine.snapshot().rows.getValue("accounts").single()[1])
    }
    @Test fun csvBatchFailureCannotLeavePartialImport() = runBlocking {
        val before = engine.snapshot()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_second BEFORE INSERT ON operations WHEN NEW.title='fail' BEGIN SELECT RAISE(ABORT,'failure'); END")
        val base = Operation(0, OperationType.EXPENSE, 123, 1, null, null, "ok", "", 1_794_000_000_000, null)
        fails { OperationRepositoryImpl(db.operationDao()).saveAll(listOf(base, base.copy(title = "fail"))) }
        assertEquals(before, engine.snapshot())
    }
}

private class TestSettings : SettingsRepository {
    override val settings = MutableStateFlow(AppSettings())
    override suspend fun current() = settings.value
    override suspend fun setOnboardingDone(done: Boolean) = Unit
    override suspend fun setTheme(mode: ThemeMode) = Unit
    override suspend fun setHideAmounts(hide: Boolean) = Unit
    override suspend fun setCurrency(currency: Currency) = Unit
    override suspend fun setUserName(name: String) = Unit
    override suspend fun setPin(pin: String?) = Unit
    override suspend fun checkPin(pin: String) = false
    override suspend fun setBiometric(enabled: Boolean) = Unit
    override suspend fun setDailyReminder(enabled: Boolean, minutes: Int) = Unit
    override suspend fun setPaymentReminders(enabled: Boolean) = Unit
}

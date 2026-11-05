package ru.denpom.finance.app.domain.repository

data class BackupPreview(
    val token: String, val accounts: Int, val operations: Int, val receipts: Int,
    val goals: Int, val debts: Int, val photos: Int, val missingPhotos: Int,
    val createdAt: Long?, val legacy: Boolean,
)
data class BackupStatus(val lastBackup: Long? = null, val canRollback: Boolean = false)

interface BackupRepository {
    suspend fun exportCsv(target: String): Int
    suspend fun backup(target: String)
    suspend fun preview(source: String): BackupPreview
    suspend fun restore(token: String)
    suspend fun rollback()
    suspend fun discard(token: String)
    suspend fun status(): BackupStatus
}

package ru.denpom.finance.app.domain.repository

interface BackupRepository {
    suspend fun exportCsv(target: String): Int
    suspend fun backup(target: String)
    suspend fun restore(source: String): Boolean
}

package ru.denpom.finance.app.data.receipt

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.denpom.finance.app.domain.model.CsvOptions
import ru.denpom.finance.app.domain.model.StatementReport
import ru.denpom.finance.app.domain.repository.StatementReader
import javax.inject.Inject

class CsvStatementReader @Inject constructor(@ApplicationContext private val context: Context) : StatementReader {
    override suspend fun read(source: String, options: CsvOptions): StatementReport = withContext(Dispatchers.IO) {
        val bytes = requireNotNull(context.contentResolver.openInputStream(Uri.parse(source))) { "Не удалось открыть файл" }.use {
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val n = it.read(buffer)
                if (n < 0) break
                require(out.size() + n <= 10 * 1024 * 1024) { "CSV больше 10 МБ" }
                out.write(buffer, 0, n)
            }
            out.toByteArray()
        }
        require(bytes.size <= 10 * 1024 * 1024) { "CSV больше 10 МБ. Разделите выписку на несколько файлов" }
        CsvStatementParser.parse(bytes, options)
    }
}

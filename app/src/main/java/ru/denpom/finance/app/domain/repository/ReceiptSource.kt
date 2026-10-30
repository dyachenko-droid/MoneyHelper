package ru.denpom.finance.app.domain.repository

import ru.denpom.finance.app.domain.model.FiscalQr
import ru.denpom.finance.app.domain.model.Receipt

interface FiscalQrReader {
    fun parse(raw: String): FiscalQr?
}

interface ReceiptRecognizer {
    suspend fun recognize(imagePath: String): Receipt?
}

interface QrImageDecoder {
    suspend fun decode(imagePath: String): List<String>
}

interface StatementReader {
    suspend fun read(source: String): List<ru.denpom.finance.app.domain.model.StatementRow>
}

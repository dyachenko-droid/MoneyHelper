package ru.denpom.finance.app.domain.usecase

import ru.denpom.finance.app.domain.model.FiscalQr
import ru.denpom.finance.app.domain.model.ReceiptDraft
import ru.denpom.finance.app.domain.model.ScanOutcome
import ru.denpom.finance.app.domain.repository.FiscalQrReader
import ru.denpom.finance.app.domain.repository.ReceiptRecognizer
import javax.inject.Inject

class ScanReceiptUseCase @Inject constructor(
    private val qrReader: FiscalQrReader,
    private val recognizer: ReceiptRecognizer,
    private val categorize: CategorizeItemsUseCase,
) {

    fun parseQr(raw: String): FiscalQr? = qrReader.parse(raw)

    suspend fun fromPhoto(path: String, qr: FiscalQr?): ScanOutcome {
        val recognized = recognizer.recognize(path)
        if (recognized == null || recognized.items.isEmpty()) {
            return if (qr != null) ScanOutcome.QrOnly(qr)
            else ScanOutcome.Failed("Не удалось прочитать чек. Сфотографируйте ровнее и при хорошем свете")
        }
        val receipt = if (qr == null) recognized else recognized.copy(total = qr.sum, timestamp = qr.timestamp, qrRaw = qr.raw)
        return ScanOutcome.Ready(ReceiptDraft(receipt.copy(items = categorize.categorize(receipt.shop, receipt.items)), qr))
    }
}

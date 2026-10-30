package ru.denpom.finance.app.domain.model

data class ReceiptDraft(
    val receipt: Receipt,
    val qr: FiscalQr?,
)

sealed interface ScanOutcome {
    data class Ready(val draft: ReceiptDraft) : ScanOutcome
    data class QrOnly(val qr: FiscalQr) : ScanOutcome
    data class Failed(val message: String) : ScanOutcome
}

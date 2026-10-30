package ru.denpom.finance.app.domain.model

enum class ReceiptStatus { VERIFIED, PENDING, RECOGNIZED }

data class ReceiptItem(
    val id: Long = 0,
    val name: String,
    val quantity: Double,
    val unit: String,
    val price: Long,
    val sum: Long,
    val categoryId: Long,
)

data class Receipt(
    val id: Long = 0,
    val shop: String,
    val address: String,
    val timestamp: Long,
    val total: Long,
    val qrRaw: String?,
    val status: ReceiptStatus,
    val photoPath: String?,
    val warrantyUntil: Long?,
    val items: List<ReceiptItem>,
    val operationId: Long? = null,
)

data class FiscalQr(
    val raw: String,
    val timestamp: Long,
    val sum: Long,
    val fn: String,
    val fd: String,
    val fp: String,
)

package ru.denpom.finance.app.presentation.receipt

import ru.denpom.finance.app.domain.model.ReceiptDraft
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptDraftHolder @Inject constructor() {
    var draft: ReceiptDraft? = null
}

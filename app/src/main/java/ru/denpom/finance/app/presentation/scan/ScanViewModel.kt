package ru.denpom.finance.app.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.denpom.finance.app.domain.model.FiscalQr
import ru.denpom.finance.app.domain.model.ScanOutcome
import ru.denpom.finance.app.domain.repository.QrImageDecoder
import ru.denpom.finance.app.domain.usecase.ScanReceiptUseCase
import ru.denpom.finance.app.presentation.receipt.ReceiptDraftHolder
import javax.inject.Inject

enum class ScanMode { QR, PHOTO }

sealed interface ScanPhase {
    data object Idle : ScanPhase
    data class Loading(val title: String, val body: String) : ScanPhase
    data class Failed(val message: String) : ScanPhase
}

data class ScanState(
    val mode: ScanMode = ScanMode.QR,
    val flash: Boolean = false,
    val phase: ScanPhase = ScanPhase.Idle,
    val hint: String? = null,
)

sealed interface ScanEvent {
    data object OpenDraft : ScanEvent
    data class QuickAdd(val qr: FiscalQr) : ScanEvent
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scan: ScanReceiptUseCase,
    private val qrDecoder: QrImageDecoder,
    private val holder: ReceiptDraftHolder,
) : ViewModel() {

    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private val _events = Channel<ScanEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var lastForeign: String? = null
    private var handled = false

    val busy: Boolean get() = handled || _state.value.phase != ScanPhase.Idle

    fun setMode(mode: ScanMode) = _state.update { it.copy(mode = mode, hint = null) }
    fun toggleFlash() = _state.update { it.copy(flash = !it.flash) }
    fun reset() = _state.update { it.copy(phase = ScanPhase.Idle) }

    fun onQrDetected(raw: String) {
        if (busy) return
        val qr = scan.parseQr(raw)
        if (qr == null) {
            if (lastForeign != raw) {
                lastForeign = raw
                _state.update { it.copy(hint = "Это не QR-код кассового чека") }
            }
            return
        }
        handled = true
        viewModelScope.launch { _events.send(ScanEvent.QuickAdd(qr)) }
    }

    fun onPhoto(path: String) {
        if (busy) return
        _state.update { it.copy(phase = ScanPhase.Loading("Распознаём чек…", "Это может занять до 10 секунд"), hint = null) }
        viewModelScope.launch {
            val qr = qrDecoder.decode(path).firstNotNullOfOrNull { scan.parseQr(it) }
            when (val outcome = scan.fromPhoto(path, qr)) {
                is ScanOutcome.Ready -> {
                    holder.draft = outcome.draft
                    handled = true
                    _state.update { it.copy(phase = ScanPhase.Idle) }
                    _events.send(ScanEvent.OpenDraft)
                }
                is ScanOutcome.QrOnly -> {
                    handled = true
                    _state.update { it.copy(phase = ScanPhase.Idle) }
                    _events.send(ScanEvent.QuickAdd(outcome.qr))
                }
                is ScanOutcome.Failed -> _state.update { it.copy(phase = ScanPhase.Failed(outcome.message)) }
            }
        }
    }
}

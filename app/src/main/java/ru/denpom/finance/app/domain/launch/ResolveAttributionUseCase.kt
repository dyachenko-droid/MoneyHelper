package ru.denpom.finance.app.domain.launch

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Attribution policy only; SDK callbacks and HTTP live behind the repository. */
class ResolveAttributionUseCase(private val repository: AttributionRepository) {
    suspend operator fun invoke(): InstallAttribution = coroutineScope {
        val decision = CompletableDeferred<InstallAttribution>()
        var refresh: Deferred<InstallAttribution?>? = null

        suspend fun refreshOnce(): InstallAttribution? {
            val request = refresh?.takeIf { it.isActive } ?: async {
                try { repository.refresh() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
            }.also { refresh = it }
            return request.await()
        }

        fun publish(server: InstallAttribution?, fallback: InstallAttribution? = null) {
            val sdk = repository.updates.value
            val selected = if (sdk?.kind == InstallKind.PAID) {
                sdk.copy(campaign = sdk.campaign ?: server?.takeIf { it.kind == InstallKind.PAID }?.campaign)
            } else server ?: fallback
            if (selected != null) decision.complete(selected)
        }

        val sdkObserver = launch {
            repository.updates.map { it?.kind }.filterNotNull().distinctUntilChanged().collectLatest { kind ->
                if (kind == InstallKind.PAID) publish(repository.updates.value)
                else {
                    delay(5_000)
                    publish(refreshOnce(), repository.updates.value)
                }
            }
        }
        val recovery = launch {
            delay(10_000)
            repeat(3) {
                if (decision.isCompleted) return@launch
                publish(refreshOnce(), repository.updates.value)
                delay(3_000)
            }
        }
        try { decision.await() }
        finally { sdkObserver.cancel(); recovery.cancel(); refresh?.cancel() }
    }
}

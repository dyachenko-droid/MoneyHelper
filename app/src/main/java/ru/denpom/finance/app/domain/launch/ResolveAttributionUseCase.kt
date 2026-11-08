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
        var gcd: InstallAttribution? = null

        suspend fun refreshOnce(): InstallAttribution? {
            val request = refresh?.takeIf { it.isActive } ?: async {
                try { repository.refresh()?.classified().also { gcd = it } }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
            }.also { refresh = it }
            return request.await()
        }

        fun publish(server: InstallAttribution?, fallback: InstallAttribution? = null) {
            val sdk = repository.updates.value?.classified()
            val selected = when {
                sdk?.explicitlyOrganic == true -> sdk
                sdk?.kind == InstallKind.PAID -> {
                    if (sdk.campaign == null && server?.explicitlyOrganic == true) server
                    else sdk.supplement(server)
                }
                else -> server ?: fallback?.classified()
            }
            if (selected != null) decision.complete(selected)
        }

        val sdkObserver = launch {
            repository.updates.filterNotNull()
                .map { it.classified().let { data -> data.kind to data.explicitlyOrganic } }
                .distinctUntilChanged().collectLatest { (kind, explicitlyOrganic) ->
                    if (kind == InstallKind.PAID || explicitlyOrganic) publish(gcd, repository.updates.value)
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

package ru.denpom.finance.app.domain.launch

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

class ResolveLaunchUseCase(
    private val preferences: LaunchPreferences,
    private val attribution: ResolveAttributionUseCase,
    private val attributionRepository: AttributionRepository,
    private val privacyLinks: PrivacyLinkRepository,
    private val composer: PaidLinkComposer,
    private val timeouts: LaunchTimeouts = LaunchTimeouts(),
) {
    suspend operator fun invoke(): LaunchDestination {
        if (!preferences.isFirstLaunch) {
            val saved = preferences.privacyUrl
            return if (preferences.hasRemotePrivacyUrl && saved.isNotEmpty()) {
                LaunchDestination.Web(saved, false)
            } else LaunchDestination.Native
        }
        val install = safely {
            withTimeoutOrNull(timeouts.attribution) { attribution() }
        } ?: InstallAttribution(InstallKind.ORGANIC)
        val raw = safely {
            withTimeoutOrNull(timeouts.firebase) { privacyLinks.read(install.kind.field) }
        }
        if (!raw.isNullOrEmpty()) {
            val finalUrl = if (install.kind == InstallKind.PAID) {
                val uid = attributionRepository.appsFlyerId
                val deviceId = if (install.advertisingId == null) safely {
                    withTimeoutOrNull(timeouts.advertisingId) { attributionRepository.deviceAdvertisingId() }
                } else null
                composer(raw, uid, install, deviceId)
            } else raw
            preferences.completeFirstLaunch(finalUrl, fromFirebase = true)
            return LaunchDestination.Web(finalUrl, false)
        }
        preferences.saveDefaultPrivacy()
        return LaunchDestination.Web(DEFAULT_PRIVACY_URL, true)
    }

    private suspend fun <T> safely(block: suspend () -> T): T? = try { block() }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { null }
}

class AcceptPrivacyUseCase(private val preferences: LaunchPreferences) {
    operator fun invoke() = preferences.completeFirstLaunch(DEFAULT_PRIVACY_URL, fromFirebase = false)
}

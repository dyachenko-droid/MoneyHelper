package ru.denpom.finance.app.data.launch

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.appsflyer.AppsFlyerLib
import com.appsflyer.share.attribution.AppsFlyerRequestListener
import com.onesignal.OneSignal
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import ru.denpom.finance.app.domain.launch.SyncNotificationIdentityUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartupIntegrations @Inject constructor(
    @ApplicationContext private val context: Context,
    private val attribution: AppsFlyerAttributionRepository,
    private val syncIdentity: SyncNotificationIdentityUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var started = false

    fun start() {
        if (started) return
        started = true
        if (IntegrationConfig.oneSignalConfigured) OneSignal.initWithContext(context, IntegrationConfig.ONESIGNAL_APP_ID)
        if (!IntegrationConfig.appsFlyerConfigured) return
        try {
            val sdk = AppsFlyerLib.getInstance()
            sdk.init(IntegrationConfig.APPSFLYER_DEV_KEY, attribution, context)
            sdk.registerConversionListener(attribution)
            syncIdentity()
            val gate = SessionLaunchGate({ sdk.isSessionReady() }) {
                syncIdentity()
                sdk.start(object : AppsFlyerRequestListener {
                    override fun onSuccess() { scope.launch { syncIdentity() } }
                    override fun onError(code: Int, error: String) = Unit
                })
            }
            sdk.registerSessionReadyListener { scope.launch { gate.onReady() } }
            scope.launch { attribution.updates.collect { syncIdentity() } }
            scope.launch {
                try { connectionStatus(context).collect(gate::onNetwork) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* Launch routing has independent deadlines. */ }
            }
        } catch (_: Exception) { /* The domain deadline falls back to organic. */ }
    }
}

class SessionLaunchGate(private val sdkReady: () -> Boolean, private val sendSession: () -> Unit) {
    private var online = false
    private var pending = false
    fun onReady() { pending = true; dispatch() }
    fun onNetwork(value: Boolean) { online = value; dispatch() }
    private fun dispatch() {
        if (!pending || !online || !sdkReady()) return
        pending = false
        try { sendSession() } catch (_: Exception) { pending = true }
    }
}

private fun connectionStatus(context: Context): Flow<Boolean> = callbackFlow {
    val manager = context.getSystemService(ConnectivityManager::class.java)
    fun publish() {
        val capabilities = manager.activeNetwork?.let(manager::getNetworkCapabilities)
        trySend(capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
    }
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = publish()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = publish()
        override fun onLost(network: Network) = publish()
    }
    manager.registerDefaultNetworkCallback(callback)
    publish()
    awaitClose { manager.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()

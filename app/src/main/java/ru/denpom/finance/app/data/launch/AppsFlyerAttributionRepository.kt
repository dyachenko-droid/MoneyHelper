package ru.denpom.finance.app.data.launch

import android.content.Context
import com.appsflyer.AppsFlyerLib
import com.appsflyer.share.AppsFlyerConversionListener
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runInterruptible
import ru.denpom.finance.app.domain.launch.AttributionRepository
import ru.denpom.finance.app.domain.launch.InstallAttribution
import ru.denpom.finance.app.domain.launch.InstallKind
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppsFlyerAttributionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gcd: GcdInstallDataClient,
) : AttributionRepository, AppsFlyerConversionListener {
    private val state = MutableStateFlow<InstallAttribution?>(null)
    override val updates = state.asStateFlow()
    override val appsFlyerId: String
        get() = runCatching { AppsFlyerLib.getInstance().getAppsFlyerUID(context).orEmpty() }.getOrDefault("")

    override suspend fun deviceAdvertisingId(): String? = runInterruptible(Dispatchers.IO) {
        val info = AdvertisingIdClient.getAdvertisingIdInfo(context)
        info.id.takeUnless { info.isLimitAdTrackingEnabled }
    }

    override fun onConversionDataSuccess(data: MutableMap<String, Any?>?) {
        val fields = data.orEmpty().mapNotNull { (key, value) ->
            when (value) {
                is CharSequence, is Number, is Boolean -> key to value.toString()
                else -> null
            }
        }.toMap()
        val incoming = InstallAttribution.from(fields) ?: return
        state.update { previous ->
            if (previous?.kind == InstallKind.PAID && incoming.kind != InstallKind.PAID && !incoming.explicitlyOrganic) previous
            else incoming.supplement(previous)
        }
    }

    override fun onConversionDataFail(error: String?) = Unit

    override suspend fun refresh(): InstallAttribution? = gcd.fetch(context.packageName, appsFlyerId)
}

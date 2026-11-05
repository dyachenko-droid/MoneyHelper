package ru.denpom.finance.app.data.launch

import android.content.Context
import com.appsflyer.AppsFlyerLib
import com.appsflyer.share.AppsFlyerConversionListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    override fun onConversionDataSuccess(data: MutableMap<String, Any>?) {
        val kind = InstallKind.from(data?.get("af_status")) ?: return
        val campaign = data?.get("campaign")?.toString()
        state.update { previous ->
            if (previous?.kind == InstallKind.PAID && kind != InstallKind.PAID) previous
            else InstallAttribution(kind, campaign ?: previous?.takeIf { it.kind == kind }?.campaign)
        }
    }

    override fun onConversionDataFail(error: String?) = Unit
    override suspend fun refresh(): InstallAttribution? = gcd.fetch(context.packageName, appsFlyerId)
}

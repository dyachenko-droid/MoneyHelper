package ru.denpom.finance.app.data.launch

import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import ru.denpom.finance.app.domain.launch.InstallAttribution
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GcdInstallDataClient @Inject constructor() {
    private val executor = Executors.newFixedThreadPool(2) { work ->
        Thread(work, "moneyhelper-attribution").apply { isDaemon = true }
    }

    suspend fun fetch(packageName: String, uid: String): InstallAttribution? {
        if (uid.isBlank() || !IntegrationConfig.appsFlyerConfigured) return null
        return suspendCancellableCoroutine { continuation ->
            val connection = AtomicReference<HttpURLConnection?>()
            val work = executor.submit {
                try {
                    val url = URI("https://gcdsdk.appsflyer.com/install_data/v4.0/$packageName?device_id=${encode(uid)}&devkey=${encode(IntegrationConfig.APPSFLYER_DEV_KEY)}").toURL()
                    val request = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8_000
                        readTimeout = 8_000
                        instanceFollowRedirects = false
                        setRequestProperty("Accept", "application/json")
                    }
                    connection.set(request)
                    if (!continuation.isActive) return@submit
                    val code = request.responseCode
                    val data = if (code in 200..299) {
                        val body = request.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                        parseInstallData(body)
                    } else null
                    if (continuation.isActive) continuation.resume(data)
                } catch (_: Exception) {
                    if (continuation.isActive) continuation.resume(null)
                } finally { connection.get()?.disconnect() }
            }
            continuation.invokeOnCancellation {
                work.cancel(true)
                executor.execute { connection.get()?.disconnect() }
            }
        }
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
}

internal fun parseInstallData(body: String): InstallAttribution? = try {
    val data = Json.parseToJsonElement(body).jsonObject
    val fields = data.mapNotNull { (key, value) ->
        (value as? JsonPrimitive)?.contentOrNull?.let { key to it }
    }.toMap()
    InstallAttribution.from(fields)
} catch (_: Exception) { null }

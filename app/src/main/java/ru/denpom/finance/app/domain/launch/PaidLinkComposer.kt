package ru.denpom.finance.app.domain.launch

import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale

private val macroAliases = mapOf(
    "ad.id" to listOf("ad_id", "af_ad_id", "adgroup_id"),
    "adset.id" to listOf("adset_id", "af_adset_id"),
    "campaign.id" to listOf("campaign_id", "af_c_id"),
    "ad.name" to listOf("ad_name", "af_ad", "adgroup", "adgroup_name"),
    "adset.name" to listOf("adset_name", "adset", "af_adset"),
    "campaign.name" to campaignKeys,
    "advertising.id" to listOf("advertising_id"),
)
private val macroPattern = Regex(
    "\\{\\{([A-Za-z0-9_.-]+)\\}\\}|\\{([A-Za-z0-9_.-]+)\\}|" +
        "(?i:%7B%7B)([A-Za-z0-9_.-]+)(?i:%7D%7D)|(?i:%7B)([A-Za-z0-9_.-]+)(?i:%7D)",
)
private val subKey = Regex("sub_?[1-9][0-9]*")

internal fun isAttributionValue(value: String): Boolean {
    val token = value.trim()
    return token.isNotEmpty() && !token.equals("null", ignoreCase = true) && !macroPattern.matches(token)
}

internal fun String.usableAdvertisingId(): String? = trim().takeIf {
    isAttributionValue(it) && !isNoneCampaign(it) && it != "00000000-0000-0000-0000-000000000000"
}

internal fun mergeAttributionParameters(preferred: Map<String, String>, fallback: Map<String, String>): Map<String, String> =
    (fallback + preferred).toMutableMap().apply {
        // Alias priority is applied within each response before merging the two sources.
        macroAliases.forEach { (macro, aliases) ->
            val fromPreferred = aliases.firstNotNullOfOrNull { preferred[it] } ?: preferred[macro]
            if (fromPreferred != null) this[aliases.first()] = fromPreferred
        }
    }

class PaidLinkComposer {
    operator fun invoke(raw: String, uid: String, attribution: InstallAttribution, deviceAdvertisingId: String? = null): String {
        val fragmentAt = raw.indexOf('#')
        val fragment = if (fragmentAt < 0) "" else raw.substring(fragmentAt)
        val address = if (fragmentAt < 0) raw else raw.substring(0, fragmentAt)
        val queryAt = address.indexOf('?')
        val base = if (queryAt < 0) address else address.substring(0, queryAt)
        val originalQuery = if (queryAt < 0) "" else address.substring(queryAt + 1)
        val parts = if (originalQuery.isEmpty()) emptyList() else originalQuery.split('&')

        val values = attribution.parameters.filter { (key, value) ->
            key != "app_id" && key != "appsflyer_id" && key != "advertising_id" && !subKey.matches(key) &&
                isAttributionValue(value) && !isNoneCampaign(value)
        }.toMutableMap()
        val campaign = attribution.campaign?.takeIf { isAttributionValue(it) && !isNoneCampaign(it) }
        if (campaign != null) values["campaign"] = campaign
        val advertisingId = attribution.advertisingId ?: deviceAdvertisingId?.usableAdvertisingId()
        if (advertisingId != null) values["advertising_id"] = advertisingId
        val campaignParts = campaign?.split('_').orEmpty()
        val id = uid.takeIf { isAttributionValue(it) && !isNoneCampaign(it) }
        if (id != null) {
            values["app_id"] = id
            values["appsflyer_id"] = id
        }
        campaignParts.forEachIndexed { index, part ->
            values["sub${index + 1}"] = part
            values["sub_${index + 1}"] = part
        }
        val retained = parts.mapNotNull { part ->
            val rawKey = part.substringBefore('=')
            val key = runCatching { URLDecoder.decode(rawKey, "UTF-8") }.getOrDefault(rawKey)
            if (key == "app_id" || subKey.matches(key)) return@mapNotNull null
            if ('=' !in part) return@mapNotNull part
            val expanded = macroPattern.replace(part.substringAfter('=')) { match ->
                val name = match.groupValues.drop(1).first { it.isNotEmpty() }
                val macro = name.lowercase(Locale.ROOT)
                val attributed = macroAliases[macro]?.firstNotNullOfOrNull { values[it] } ?: values[name]
                val replacement = when (macro) {
                    "ad.id" -> advertisingId ?: attributed
                    "campaign.id" -> attributed ?: campaign
                    else -> attributed
                }
                replacement?.let(::encode) ?: match.value
            }
            "$rawKey=$expanded"
        }
        val added = campaignParts.mapIndexed { index, part -> "sub${index + 1}=${encode(part)}" }.toMutableList()
        if (id != null) added += "app_id=${encode(id)}"
        val query = (retained + added).joinToString("&")
        return if (query.isEmpty()) {
            if (queryAt >= 0 && originalQuery.isEmpty()) raw else "$base$fragment"
        } else "$base?$query$fragment"
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
        .replace("+", "%20").replace("*", "%2A").replace("%7E", "~")
}

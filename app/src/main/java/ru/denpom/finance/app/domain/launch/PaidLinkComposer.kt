package ru.denpom.finance.app.domain.launch

import java.net.URLDecoder
import java.net.URLEncoder

class PaidLinkComposer {
    operator fun invoke(raw: String, uid: String, campaign: String?): String {
        val fragmentAt = raw.indexOf('#')
        val fragment = if (fragmentAt < 0) "" else raw.substring(fragmentAt)
        val address = if (fragmentAt < 0) raw else raw.substring(0, fragmentAt)
        val queryAt = address.indexOf('?')
        val base = if (queryAt < 0) address else address.substring(0, queryAt)
        val originalQuery = if (queryAt < 0) "" else address.substring(queryAt + 1)
        val parts = if (originalQuery.isEmpty()) emptyList() else originalQuery.split('&')
        val retained = parts.filterNot {
            val key = runCatching { URLDecoder.decode(it.substringBefore('='), "UTF-8") }
                .getOrDefault(it.substringBefore('='))
            key == "app_id" || Regex("sub_[1-9][0-9]*").matches(key)
        }
        val added = mutableListOf("app_id=${encode(uid)}")
        if (!campaign.isNullOrEmpty()) campaign.split('_').forEachIndexed { index, part ->
            added += "sub_${index + 1}=${encode(part)}"
        }
        return "$base?${(retained + added).joinToString("&")}$fragment"
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
        .replace("+", "%20").replace("*", "%2A").replace("%7E", "~")
}

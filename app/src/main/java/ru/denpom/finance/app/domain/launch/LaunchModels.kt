package ru.denpom.finance.app.domain.launch

const val DEFAULT_PRIVACY_URL = "https://dyachenko-droid.github.io/MoneyHelper/privacy.html"

enum class InstallKind(val field: String) {
    ORGANIC("org"), PAID("in_org");

    companion object {
        fun from(value: Any?): InstallKind? = when (value?.toString()?.trim()?.lowercase()) {
            "organic" -> ORGANIC
            "non-organic" -> PAID
            else -> null
        }
    }
}

internal val campaignKeys = listOf("campaign", "campaign_name", "c")

internal fun isNoneCampaign(value: String?) = value?.trim().equals("None", ignoreCase = true)

data class InstallAttribution(
    val kind: InstallKind,
    val campaign: String? = null,
    val parameters: Map<String, String> = emptyMap(),
) {
    internal val explicitlyOrganic get() = isNoneCampaign(campaign)
    internal val advertisingId get() = parameters["advertising_id"]?.usableAdvertisingId()

    internal fun classified(): InstallAttribution =
        if (explicitlyOrganic && kind != InstallKind.ORGANIC) copy(kind = InstallKind.ORGANIC) else this

    internal fun supplement(other: InstallAttribution?): InstallAttribution {
        if (other == null || other.kind != kind || explicitlyOrganic) return this
        return copy(
            campaign = campaign ?: other.campaign?.takeUnless(::isNoneCampaign),
            parameters = mergeAttributionParameters(parameters, other.parameters),
        )
    }

    companion object {
        internal fun from(fields: Map<String, String>): InstallAttribution? {
            val campaign = campaignKeys.firstNotNullOfOrNull { fields[it]?.takeIf(::isAttributionValue) }
            val kind = if (isNoneCampaign(campaign)) InstallKind.ORGANIC else InstallKind.from(fields["af_status"])
            return kind?.let {
                InstallAttribution(it, campaign, fields.filter { (key, value) ->
                    key != "af_status" && key !in campaignKeys && isAttributionValue(value) && !isNoneCampaign(value)
                })
            }
        }
    }
}

sealed interface LaunchDestination {
    data object Native : LaunchDestination
    data class Web(val url: String, val requiresAcceptance: Boolean) : LaunchDestination
}

data class LaunchTimeouts(val attribution: Long = 15_000, val firebase: Long = 8_000, val advertisingId: Long = 2_000)

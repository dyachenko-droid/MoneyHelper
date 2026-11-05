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

data class InstallAttribution(val kind: InstallKind, val campaign: String? = null)

sealed interface LaunchDestination {
    data object Native : LaunchDestination
    data class Web(val url: String, val requiresAcceptance: Boolean) : LaunchDestination
}

data class LaunchTimeouts(val attribution: Long = 15_000, val firebase: Long = 8_000)

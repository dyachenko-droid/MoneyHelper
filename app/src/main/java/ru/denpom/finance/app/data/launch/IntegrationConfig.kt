package ru.denpom.finance.app.data.launch

/** Client identifiers for the configured AppsFlyer, OneSignal and Firebase projects. */
object IntegrationConfig {
    const val APPSFLYER_DEV_KEY = "CMXkxjgQ3hhzsY4ELzWhcU"
    const val ONESIGNAL_APP_ID = "9e43a7ed-5ba2-4a71-9d43-d252c0d06544"
    const val PLACEHOLDER_FIREBASE_PROJECT = "moneyhelper-placeholder"

    val appsFlyerConfigured: Boolean
        get() = APPSFLYER_DEV_KEY.isNotBlank() && !APPSFLYER_DEV_KEY.contains("PLACEHOLDER")
    val oneSignalConfigured: Boolean
        get() = ONESIGNAL_APP_ID != "00000000-0000-0000-0000-000000000000" && ONESIGNAL_APP_ID.isNotBlank()
}

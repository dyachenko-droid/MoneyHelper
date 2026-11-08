package ru.denpom.finance.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import ru.denpom.finance.app.data.launch.AppsFlyerAttributionRepository
import ru.denpom.finance.app.data.launch.GcdInstallDataClient
import ru.denpom.finance.app.domain.launch.AttributionRepository
import ru.denpom.finance.app.domain.launch.DEFAULT_PRIVACY_URL
import ru.denpom.finance.app.domain.launch.LaunchDestination
import ru.denpom.finance.app.domain.launch.LaunchPreferences
import ru.denpom.finance.app.domain.launch.PaidLinkComposer
import ru.denpom.finance.app.domain.launch.ResolveAttributionUseCase
import ru.denpom.finance.app.domain.launch.ResolveLaunchUseCase

@RunWith(AndroidJUnit4::class)
class PaidAttributionRoutingTest {
    @Test fun sdkCallbackWithNullFieldsSelectsPaidLinkOnAndroid() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sdk = AppsFlyerAttributionRepository(context, GcdInstallDataClient())
        val repository = object : AttributionRepository by sdk {
            override val appsFlyerId = "test-uid"
            override suspend fun deviceAdvertisingId(): String = error("Attributed advertising ID should be used")
            override suspend fun refresh() = error("SDK attribution should be enough")
        }
        val preferences = object : LaunchPreferences {
            override var isFirstLaunch = true
            override var hasRemotePrivacyUrl = false
            override var privacyUrl = DEFAULT_PRIVACY_URL
            override var notificationPermissionRequested = false

            override fun saveDefaultPrivacy() = error("Expected a Firebase link")

            override fun completeFirstLaunch(url: String, fromFirebase: Boolean) {
                privacyUrl = url
                hasRemotePrivacyUrl = fromFirebase
                isFirstLaunch = false
            }
        }
        val resolve = ResolveLaunchUseCase(
            preferences,
            ResolveAttributionUseCase(repository),
            repository,
            privacyLinks = { field ->
                assertEquals("in_org", field)
                "https://example.test/?campaign_name={{campaign.name}}&ad_id={{ad.id}}&campaign_id={{campaign.id}}"
            },
            composer = PaidLinkComposer(),
        )

        sdk.onConversionDataSuccess(mutableMapOf(
            "af_status" to "Non-organic",
            "campaign" to "TEST_RU_ANDROID",
            "advertising_id" to "a946e510-2683-4938-9ad1-2c3ad1e41489",
            "is_first_launch" to false,
            "adgroup_id" to null,
        ))

        val expected = "https://example.test/?campaign_name=TEST_RU_ANDROID&ad_id=a946e510-2683-4938-9ad1-2c3ad1e41489" +
            "&campaign_id=TEST_RU_ANDROID&sub1=TEST&sub2=RU&sub3=ANDROID&app_id=test-uid"
        assertEquals(LaunchDestination.Web(expected, false), resolve())
        assertEquals(expected, preferences.privacyUrl)
    }
}

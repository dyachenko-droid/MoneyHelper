package ru.denpom.finance.app.domain.launch

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ResolveLaunchUseCaseTest {
    private fun resolver(prefs: LaunchPreferences, attribution: AttributionRepository,
        read: suspend (String) -> String?) = ResolveLaunchUseCase(prefs, ResolveAttributionUseCase(attribution), attribution, PrivacyLinkRepository(read), PaidLinkComposer())

    @Test fun organicReadsOrgAndSavesExactStringWithoutParameters() = runTest {
        val prefs = FakeLaunchPreferences()
        val repo = FakeAttributionRepository(InstallAttribution(InstallKind.ORGANIC, "one_two"))
        val fields = mutableListOf<String>()
        val result = resolver(prefs, repo) { fields += it; "12ук" }()
        assertEquals(LaunchDestination.Web("12ук", false), result)
        assertEquals(listOf("org"), fields)
        assertEquals("12ук", prefs.privacyUrl)
        assertFalse(prefs.isFirstLaunch)
        assertTrue(prefs.hasRemotePrivacyUrl)
        assertEquals(0, repo.advertisingIdReads)
    }

    @Test fun paidCampaignAndUidDecorateInOrgBeforeSaving() = runTest {
        val prefs = FakeLaunchPreferences()
        val repo = FakeAttributionRepository(InstallAttribution(InstallKind.PAID, "one_two_three"))
        val fields = mutableListOf<String>()
        val url = "https://example.com/path?keep=1&app_id={app_id}&sub_1={sub_1}#page"
        val expected = "https://example.com/path?keep=1&sub1=one&sub2=two&sub3=three&app_id=af-123#page"
        assertEquals(LaunchDestination.Web(expected, false), resolver(prefs, repo) { fields += it; url }())
        assertEquals(listOf("in_org"), fields)
        assertEquals(expected, prefs.privacyUrl)
        assertEquals(1, prefs.writes)
        assertEquals(0L, testScheduler.currentTime)
        assertEquals(0, repo.refreshes)
    }

    @Test fun noneCampaignUsesOrgEvenWhenStatusIsPaid() = runTest {
        val prefs = FakeLaunchPreferences()
        val repo = FakeAttributionRepository(InstallAttribution(InstallKind.PAID, " NoNe "))
        val fields = mutableListOf<String>()
        val result = resolver(prefs, repo) { fields += it; "https://example.test?ad={{ad.id}}" }()
        assertEquals(listOf("org"), fields)
        assertEquals(LaunchDestination.Web("https://example.test?ad={{ad.id}}", false), result)
        assertEquals(0, repo.refreshes)
    }

    @Test fun noneCampaignFromGcdUsesOrgWithoutSdkCallback() = runTest {
        val repo = FakeAttributionRepository()
        repo.refresh = { InstallAttribution(InstallKind.PAID, "None") }
        val fields = mutableListOf<String>()
        assertEquals(LaunchDestination.Web("https://example.test", false),
            resolver(FakeLaunchPreferences(), repo) { fields += it; "https://example.test" }())
        assertEquals(listOf("org"), fields)
    }

    @Test fun paidAttributionFieldsReachLinkBeforeItIsSaved() = runTest {
        val prefs = FakeLaunchPreferences()
        val repo = FakeAttributionRepository(InstallAttribution(InstallKind.PAID, "one_two", mapOf("adgroup_id" to "42")))
        val expected = "https://example.test?ad_id=42&sub1=one&sub2=two&app_id=af-123"
        assertEquals(LaunchDestination.Web(expected, false),
            resolver(prefs, repo) { "https://example.test?ad_id={{ad.id}}" }())
        assertEquals(expected, prefs.privacyUrl)
    }

    @Test fun paidLinkUsesAdvertisingIdInAdMacroAndCampaignNameWhenCampaignIdIsMissing() = runTest {
        val prefs = FakeLaunchPreferences()
        val repo = FakeAttributionRepository(InstallAttribution(InstallKind.PAID, "TEST_RU", mapOf(
            "advertising_id" to "a946e510-2683-4938-9ad1-2c3ad1e41489",
            "ad_id" to "original-ad-id",
        )))
        val result = resolver(prefs, repo) {
            "https://example.test?campaign_id={{campaign.id}}&ad_id={{ad.id}}"
        }()
        val expected = "https://example.test?campaign_id=TEST_RU&ad_id=a946e510-2683-4938-9ad1-2c3ad1e41489" +
            "&sub1=TEST&sub2=RU&app_id=af-123"
        assertEquals(LaunchDestination.Web(expected, false), result)
        assertEquals(expected, prefs.privacyUrl)
        assertEquals(0, repo.advertisingIdReads)
    }

    @Test fun paidLinkReadsDeviceAdvertisingIdWhenAttributionHasNoUsableId() = runTest {
        val repo = FakeAttributionRepository(InstallAttribution(InstallKind.PAID, parameters = mapOf(
            "advertising_id" to "00000000-0000-0000-0000-000000000000",
        )))
        val deviceId = "160f8630-5be5-48b8-94d2-dbd57c590976"
        repo.readAdvertisingId = { deviceId }
        assertEquals(
            LaunchDestination.Web("https://example.test?ad_id=$deviceId&app_id=af-123", false),
            resolver(FakeLaunchPreferences(), repo) { "https://example.test?ad_id={{ad.id}}" }(),
        )
        assertEquals(1, repo.advertisingIdReads)
    }

    @Test fun paidLinkContinuesWhenDeviceAdvertisingIdTimesOut() = runTest {
        val repo = FakeAttributionRepository(InstallAttribution(InstallKind.PAID))
        repo.readAdvertisingId = { awaitCancellation() }
        assertEquals(
            LaunchDestination.Web("https://example.test?app_id=af-123", false),
            resolver(FakeLaunchPreferences(), repo) { "https://example.test" }(),
        )
        assertEquals(2_000L, testScheduler.currentTime)
    }

    @Test fun cachedLinkIsOpenedWithoutNetworkOrRepeatedParameters() = runTest {
        val saved = "12ук?app_id=old&sub_1=old"
        val prefs = FakeLaunchPreferences(false, saved, true)
        val repo = FakeAttributionRepository()
        repo.refresh = { error("Must use cache") }
        assertEquals(LaunchDestination.Web(saved, false), resolver(prefs, repo) { error("Must use cache") }())
        assertEquals(0, prefs.writes)
        assertEquals(0, repo.refreshes)
    }

    @Test fun completedNativeLaunchDoesNotReadRemoteData() = runTest {
        val prefs = FakeLaunchPreferences(false)
        assertEquals(LaunchDestination.Native,
            resolver(prefs, FakeAttributionRepository()) { error("Must stay native") }())
    }

    @Test fun everyNonemptyOrganicStringIsAcceptedVerbatim() = runTest {
        for (text in listOf("12ук", "google.com", " ", "not a URL", " https://example.com ", "app://value")) {
            val prefs = FakeLaunchPreferences()
            val repo = FakeAttributionRepository(InstallAttribution(InstallKind.ORGANIC))
            assertEquals(text, LaunchDestination.Web(text, false), resolver(prefs, repo) { text }())
            assertEquals(text, prefs.privacyUrl)
        }
    }

    @Test fun paidArbitraryStringGetsOnlyRequestedParameters() = runTest {
        val repo = FakeAttributionRepository(InstallAttribution(InstallKind.PAID, "a_b"))
        assertEquals(LaunchDestination.Web("12ук?sub1=a&sub2=b&app_id=af-123", false),
            resolver(FakeLaunchPreferences(), repo) { "12ук" }())
    }

    @Test fun absentOrEmptyFirebaseValueShowsDefaultUntilAcceptance() = runTest {
        for (text in listOf(null, "")) {
            val prefs = FakeLaunchPreferences()
            val repo = FakeAttributionRepository(InstallAttribution(InstallKind.PAID, "a_b"))
            assertEquals(LaunchDestination.Web(DEFAULT_PRIVACY_URL, true), resolver(prefs, repo) { text }())
            assertTrue(prefs.isFirstLaunch)
            assertFalse(prefs.hasRemotePrivacyUrl)
            assertEquals(DEFAULT_PRIVACY_URL, prefs.privacyUrl)
        }
    }

    @Test fun databaseFailureUsesDefaultAndDoesNotDecorateIt() = runTest {
        val prefs = FakeLaunchPreferences()
        val repo = FakeAttributionRepository(InstallAttribution(InstallKind.PAID, "a_b"))
        assertEquals(LaunchDestination.Web(DEFAULT_PRIVACY_URL, true), resolver(prefs, repo) { error("Permission denied") }())
        assertTrue(prefs.isFirstLaunch)
        assertEquals(DEFAULT_PRIVACY_URL, prefs.privacyUrl)
    }

    @Test fun noAttributionBy15SecondsReadsOrgAndLatePaidDataCannotChangeIt() = runTest {
        val prefs = FakeLaunchPreferences()
        val repo = FakeAttributionRepository()
        val fields = mutableListOf<String>()
        val result = resolver(prefs, repo) { fields += it; "12ук" }()
        assertEquals(LaunchDestination.Web("12ук", false), result)
        assertEquals(15_000L, testScheduler.currentTime)
        assertEquals(2, repo.refreshes)
        repo.updates.value = InstallAttribution(InstallKind.PAID, "late_campaign")
        runCurrent()
        assertEquals(listOf("org"), fields)
        assertEquals("12ук", prefs.privacyUrl)
        assertEquals(1, prefs.writes)
    }

    @Test fun firebaseTimeoutAfter8SecondsIgnoresLateResult() = runTest {
        val prefs = FakeLaunchPreferences()
        val repo = FakeAttributionRepository(InstallAttribution(InstallKind.PAID, "a_b"))
        val delayed = CompletableDeferred<String?>()
        assertEquals(LaunchDestination.Web(DEFAULT_PRIVACY_URL, true), resolver(prefs, repo) { delayed.await() }())
        assertEquals(8_000L, testScheduler.currentTime)
        delayed.complete("https://late.example")
        runCurrent()
        assertTrue(prefs.isFirstLaunch)
        assertEquals(DEFAULT_PRIVACY_URL, prefs.privacyUrl)
        assertEquals(1, prefs.writes)
    }

    @Test fun bothDeadlinesAreBoundedTo23Seconds() = runTest {
        val repo = FakeAttributionRepository()
        repo.refresh = { awaitCancellation() }
        val fields = mutableListOf<String>()
        assertEquals(LaunchDestination.Web(DEFAULT_PRIVACY_URL, true),
            resolver(FakeLaunchPreferences(), repo) { fields += it; awaitCancellation() }())
        assertEquals(listOf("org"), fields)
        assertEquals(23_000L, testScheduler.currentTime)
    }

    @Test fun cancellingLaunchDoesNotPersistDefaultOrRemoteLink() = runTest {
        val prefs = FakeLaunchPreferences(privacyUrl = "original")
        val result = async { resolver(prefs, FakeAttributionRepository()) { "12ук" }() }
        runCurrent()
        result.cancelAndJoin()
        assertEquals("original", prefs.privacyUrl)
        assertEquals(0, prefs.writes)
        assertTrue(prefs.isFirstLaunch)
    }

    @Test fun acceptingDefaultPolicyEntersNativeOnLaterLaunches() = runTest {
        val prefs = FakeLaunchPreferences()
        val repo = FakeAttributionRepository(InstallAttribution(InstallKind.PAID))
        val resolve = resolver(prefs, repo) { null }
        assertEquals(LaunchDestination.Web(DEFAULT_PRIVACY_URL, true), resolve())
        AcceptPrivacyUseCase(prefs)()
        assertEquals(LaunchDestination.Native, resolve())
        assertFalse(prefs.isFirstLaunch)
    }

    @Test fun remoteDefaultUrlStillHasRemoteOriginAndNoAcceptButton() = runTest {
        val prefs = FakeLaunchPreferences()
        val repo = FakeAttributionRepository(InstallAttribution(InstallKind.ORGANIC))
        assertEquals(LaunchDestination.Web(DEFAULT_PRIVACY_URL, false), resolver(prefs, repo) { DEFAULT_PRIVACY_URL }())
        assertTrue(prefs.hasRemotePrivacyUrl)
        assertFalse(prefs.isFirstLaunch)
    }
}

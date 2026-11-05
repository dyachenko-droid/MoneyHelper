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
    }

    @Test fun paidCampaignAndUidDecorateInOrgBeforeSaving() = runTest {
        val prefs = FakeLaunchPreferences()
        val repo = FakeAttributionRepository(InstallAttribution(InstallKind.PAID, "one_two_three"))
        val fields = mutableListOf<String>()
        val url = "https://example.com/path?keep=1&app_id={app_id}&sub_1={sub_1}#page"
        val expected = "https://example.com/path?keep=1&app_id=af-123&sub_1=one&sub_2=two&sub_3=three#page"
        assertEquals(LaunchDestination.Web(expected, false), resolver(prefs, repo) { fields += it; url }())
        assertEquals(listOf("in_org"), fields)
        assertEquals(expected, prefs.privacyUrl)
        assertEquals(1, prefs.writes)
        assertEquals(0L, testScheduler.currentTime)
        assertEquals(0, repo.refreshes)
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
        assertEquals(LaunchDestination.Web("12ук?app_id=af-123&sub_1=a&sub_2=b", false),
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

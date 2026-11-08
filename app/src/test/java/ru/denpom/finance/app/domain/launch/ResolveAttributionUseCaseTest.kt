package ru.denpom.finance.app.domain.launch

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ResolveAttributionUseCaseTest {
    @Test fun paidSdkResultResolvesImmediatelyWithItsCampaign() = runTest {
        val expected = InstallAttribution(InstallKind.PAID, "one_two")
        val repo = FakeAttributionRepository(expected)
        repo.refresh = { error("Must not query GCD") }
        assertEquals(expected, ResolveAttributionUseCase(repo)())
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test fun organicResultWaits5SecondsAndUsesPaidGcdCampaign() = runTest {
        val repo = FakeAttributionRepository(InstallAttribution(InstallKind.ORGANIC, "wrong_campaign"))
        val expected = InstallAttribution(InstallKind.PAID, "gcd_campaign")
        repo.refresh = { expected }
        assertEquals(expected, ResolveAttributionUseCase(repo)())
        assertEquals(5_000L, testScheduler.currentTime)
        assertEquals(1, repo.refreshes)
    }

    @Test fun failedOrganicRefreshKeepsSdkOrganicCampaign() = runTest {
        val sdk = InstallAttribution(InstallKind.ORGANIC, "sdk_campaign")
        val repo = FakeAttributionRepository(sdk)
        repo.refresh = { error("network error") }
        assertEquals(sdk, ResolveAttributionUseCase(repo)())
        assertEquals(5_000L, testScheduler.currentTime)
    }

    @Test fun laterValidSdkDataRecoversFromMissingCallback() = runTest {
        val repo = FakeAttributionRepository()
        val result = async { ResolveAttributionUseCase(repo)() }
        testScheduler.advanceTimeBy(2_000)
        assertFalse(result.isCompleted)
        val expected = InstallAttribution(InstallKind.PAID, "recovered_campaign")
        repo.updates.value = expected
        assertEquals(expected, result.await())
        assertEquals(0, repo.refreshes)
    }

    @Test fun paidSdkCallbackCancelsOrganicGrace() = runTest {
        val repo = FakeAttributionRepository(InstallAttribution(InstallKind.ORGANIC))
        val result = async { ResolveAttributionUseCase(repo)() }
        testScheduler.advanceTimeBy(2_000)
        val expected = InstallAttribution(InstallKind.PAID, "paid_campaign")
        repo.updates.value = expected
        assertEquals(expected, result.await())
        assertEquals(2_000L, testScheduler.currentTime)
        assertEquals(0, repo.refreshes)
    }

    @Test fun paidSdkCallbackCancelsInflightGcdAndWinsItsCampaign() = runTest {
        var cancelled = false
        val repo = FakeAttributionRepository(InstallAttribution(InstallKind.ORGANIC))
        repo.refresh = { try { awaitCancellation() } finally { cancelled = true } }
        val result = async { ResolveAttributionUseCase(repo)() }
        testScheduler.advanceTimeBy(5_001)
        val expected = InstallAttribution(InstallKind.PAID, "paid_campaign")
        repo.updates.value = expected
        assertEquals(expected, result.await())
        assertTrue(cancelled)
    }

    @Test fun sdkPaidFieldsOverrideGcdAndFillMissingFieldsFromIt() = runTest {
        val repo = FakeAttributionRepository(InstallAttribution(InstallKind.ORGANIC))
        repo.refresh = {
            repo.updates.value = InstallAttribution(InstallKind.PAID, "sdk", mapOf("af_ad_id" to "1"))
            InstallAttribution(InstallKind.PAID, "server", mapOf("ad_id" to "2", "adset_id" to "3"))
        }
        val selected = ResolveAttributionUseCase(repo)()
        assertEquals("https://example.test?ad=1&set=3&sub1=sdk&app_id=uid",
            PaidLinkComposer()("https://example.test?ad={{ad.id}}&set={{adset.id}}", "uid", selected))

    }

    @Test fun missingSdkResultCanBeRecoveredFromGcdAt10Seconds() = runTest {
        val repo = FakeAttributionRepository()
        val expected = InstallAttribution(InstallKind.PAID, "gcd_campaign")
        repo.refresh = { expected }
        assertEquals(expected, ResolveAttributionUseCase(repo)())
        assertEquals(10_000L, testScheduler.currentTime)
    }

    @Test fun organicCampaignUpdateDoesNotRestartTheGracePeriod() = runTest {
        val repo = FakeAttributionRepository(InstallAttribution(InstallKind.ORGANIC, "initial"))
        val result = async { ResolveAttributionUseCase(repo)() }
        testScheduler.advanceTimeBy(2_000)
        repo.updates.value = InstallAttribution(InstallKind.ORGANIC, "updated")
        runCurrent()
        assertEquals(InstallAttribution(InstallKind.ORGANIC, "updated"), result.await())
        assertEquals(5_000L, testScheduler.currentTime)
    }
}

package ru.denpom.finance.app.domain.launch

import org.junit.Assert.assertEquals
import org.junit.Test

class PaidLinkComposerTest {
    private val compose = PaidLinkComposer()

    @Test fun fillsAdvertisingFieldsAndAppendsCampaignAndUid() {
        val template = "https://example.test/click?key=fixed&ad_id={{ad.id}}&adset_id={{adset.id}}&campaign_id={{campaign.id}}" +
            "&ad_name={{ad.name}}&adset_name={{adset.name}}&campaign_name={{campaign.name}}"
        val data = InstallAttribution(InstallKind.PAID, "RU_Facebook_Video", mapOf(
            "ad_id" to "123", "af_ad_id" to "other", "af_adset_id" to "456", "af_c_id" to "789",
            "adgroup" to "Video A&B", "adset" to "Аудитория 1",
        ))
        assertEquals(
            "https://example.test/click?key=fixed&ad_id=123&adset_id=456&campaign_id=789" +
                "&ad_name=Video%20A%26B&adset_name=%D0%90%D1%83%D0%B4%D0%B8%D1%82%D0%BE%D1%80%D0%B8%D1%8F%201" +
                "&campaign_name=RU_Facebook_Video&sub1=RU&sub2=Facebook&sub3=Video&app_id=uid",
            compose(template, "uid", data),
        )
    }

    @Test fun replacesOldAndCurrentSubKeysAndPreservesOtherQueryAndFragment() {
        assertEquals("HTTPS://EXAMPLE.com/a%2Fb?keep=a+b&next=x%2Fy&sub1=first&sub2=second&app_id=new#Page",
            compose("HTTPS://EXAMPLE.com/a%2Fb?keep=a+b&app_id=old&sub_1=old&sub2=old&sub_9=stale&next=x%2Fy#Page",
                "new", paid("first_second")))
    }

    @Test fun keepsUnresolvedMacrosAndUsesFallbackForBlankOrUnexpandedValue() {
        assertEquals(
            "https://example.test?ad=123&name={{ad.name}}&set=%7B%7Badset.name%7D%7D&app_id=uid",
            compose("https://example.test?ad=%7b%7Bad.id%7d%7D&name={{ad.name}}&set=%7B%7Badset.name%7D%7D",
                "uid", InstallAttribution(InstallKind.PAID, parameters = mapOf(
                    "ad_id" to "{{ad.id}}", "af_ad_id" to "123", "ad_name" to "  ", "adset_name" to "None",
                ))),
        )
    }

    @Test fun missingUidAndCampaignDoNotAddEmptyParameters() {
        assertEquals("https://example.test?x=1#part",
            compose("https://example.test?x=1&app_id=old&sub_1=old#part", "", paid()))
        assertEquals("google.com?app_id=uid", compose("google.com", "uid", paid()))
    }

    @Test fun emptyCampaignPartsKeepTheirPositions() {
        assertEquals("12ук?sub1=&sub2=one&sub3=&sub4=three&sub5=&app_id=id",
            compose("12ук", "id", paid("_one__three_")))
    }

    @Test fun newQueryValuesAreEncoded() {
        assertEquals("https://example.com?sub1=a%20b&sub2=x%26y&sub3=%D1%82%D0%B5%D1%81%D1%82&app_id=id%26x%3D1",
            compose("https://example.com", "id&x=1", paid("a b_x&y_тест")))
    }

    @Test fun arbitraryBaseIsNotValidatedOrRewritten() {
        for (base in listOf("12ук", "google.com", " ", "https://")) {
            assertEquals("$base?sub1=a&sub2=b&app_id=id", compose(base, "id", paid("a_b")))
        }
    }

    private fun paid(campaign: String? = null) = InstallAttribution(InstallKind.PAID, campaign)
}

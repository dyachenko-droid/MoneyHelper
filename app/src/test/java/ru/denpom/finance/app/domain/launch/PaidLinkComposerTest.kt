package ru.denpom.finance.app.domain.launch

import org.junit.Assert.assertEquals
import org.junit.Test

class PaidLinkComposerTest {
    private val compose = PaidLinkComposer()

    @Test fun fillsAppIdAndCampaignParts() {
        assertEquals("https://example.com/?app_id=uid&sub_1=one&sub_2=two&sub_3=three",
            compose("https://example.com/?app_id={app_id}&sub_1={sub_1}&sub_2={sub_2}&sub_3={sub_3}", "uid", "one_two_three"))
    }

    @Test fun unrelatedQueryAndFragmentStayVerbatim() {
        assertEquals("HTTPS://EXAMPLE.com/a%2Fb?keep=a+b&next=x%2Fy&app_id=new&sub_1=first&sub_2=second#Page",
            compose("HTTPS://EXAMPLE.com/a%2Fb?keep=a+b&app_id=old&sub_1=old&sub_9=stale&next=x%2Fy#Page", "new", "first_second"))
    }

    @Test fun onlyAppIdIsAddedWhenCampaignIsMissing() {
        for (campaign in listOf(null, "")) {
            assertEquals("google.com?app_id=uid", compose("google.com?sub_1=old", "uid", campaign))
        }
    }

    @Test fun emptyPartsKeepTheirPositions() {
        assertEquals("12ук?app_id=id&sub_1=&sub_2=one&sub_3=&sub_4=three&sub_5=",
            compose("12ук", "id", "_one__three_"))
    }

    @Test fun newQueryValuesAreEncoded() {
        assertEquals("https://example.com?app_id=id%26x%3D1&sub_1=a%20b&sub_2=x%26y&sub_3=%D1%82%D0%B5%D1%81%D1%82",
            compose("https://example.com", "id&x=1", "a b_x&y_тест"))
    }

    @Test fun arbitraryBaseIsNotValidatedOrRewritten() {
        for (base in listOf("12ук", "google.com", " ", "https://")) {
            assertEquals("$base?app_id=id&sub_1=a&sub_2=b", compose(base, "id", "a_b"))
        }
    }
}

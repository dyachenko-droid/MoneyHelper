package ru.denpom.finance.app.data.launch

import org.junit.Assert.*
import org.junit.Test
import ru.denpom.finance.app.domain.launch.InstallAttribution
import ru.denpom.finance.app.domain.launch.InstallKind

class InstallDataParserTest {
    @Test fun preservesCampaignFromSelectedGcdResult() {
        assertEquals(InstallAttribution(InstallKind.PAID, "one_two"),
            parseInstallData("""{"af_status":"Non-organic","campaign":"one_two"}"""))
        assertEquals(InstallAttribution(InstallKind.ORGANIC), parseInstallData("""{"af_status":"ORGANIC"}"""))
    }

    @Test fun rejectsMissingOrUnknownStatusAndMalformedResponse() {
        for (value in listOf("{}", "[]", "invalid", """{"af_status":null}""", """{"af_status":"unknown"}""")) {
            assertNull(value, parseInstallData(value))
        }
    }

    @Test fun capturesScalarFieldsWithoutConvertingNumericIdsToFloatingPoint() {
        val data = parseInstallData("""{"af_status":"Non-organic","campaign_name":"RU_Video","adgroup_id":12345678901234567890,"adset":{"name":"ignored"},"ad_name":null}""")
        assertEquals(InstallAttribution(InstallKind.PAID, "RU_Video", mapOf("adgroup_id" to "12345678901234567890")), data)
    }

    @Test fun noneIsOrganicWithOrWithoutAfStatus() {
        assertEquals(InstallKind.ORGANIC, parseInstallData("""{"af_status":"Non-organic","campaign":"None"}""")?.kind)
        assertEquals(InstallKind.ORGANIC, parseInstallData("""{"campaign_name":" none "}""")?.kind)
    }

    @Test fun invalidCampaignTypeDoesNotDiscardValidStatus() {
        assertEquals(InstallAttribution(InstallKind.PAID),
            parseInstallData("""{"af_status":"Non-organic","campaign":{}}"""))
    }
}

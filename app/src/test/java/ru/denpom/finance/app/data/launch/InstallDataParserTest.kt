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

    @Test fun invalidCampaignTypeDoesNotDiscardValidStatus() {
        assertEquals(InstallAttribution(InstallKind.PAID),
            parseInstallData("""{"af_status":"Non-organic","campaign":{}}"""))
    }
}

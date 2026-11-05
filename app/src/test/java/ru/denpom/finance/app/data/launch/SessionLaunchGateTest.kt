package ru.denpom.finance.app.data.launch

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionLaunchGateTest {
    @Test fun waitsForBothSdkAndNetworkAndSendsOnlyOncePerReadySignal() {
        var sends = 0
        val gate = SessionLaunchGate({ true }, { sends++ })
        gate.onReady()
        assertEquals(0, sends)
        gate.onNetwork(true)
        gate.onNetwork(true)
        assertEquals(1, sends)
        gate.onReady()
        assertEquals(2, sends)
    }

    @Test fun networkAloneDoesNotStartSession() {
        var sends = 0
        val gate = SessionLaunchGate({ true }, { sends++ })
        gate.onNetwork(true)
        assertEquals(0, sends)
        gate.onReady()
        assertEquals(1, sends)
    }

    @Test fun staleReadySignalCannotStartAfterBackgrounding() {
        var ready = true
        var sends = 0
        val gate = SessionLaunchGate({ ready }, { sends++ })
        gate.onReady()
        ready = false
        gate.onNetwork(true)
        assertEquals(0, sends)
        ready = true
        gate.onReady()
        assertEquals(1, sends)
    }
}

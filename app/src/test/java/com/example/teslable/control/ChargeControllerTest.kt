package com.example.teslable.control

import org.junit.Assert.assertEquals
import org.junit.Test

class ChargeControllerTest {
    @Test
    fun computesExpectedCurrent() {
        val amps = ChargeController.computeTargetCurrentAmps(
            gridVoltageV = 240.0,
            targetPowerKw = 7.2,
            minAmps = 5,
            maxAmps = 48,
        )
        assertEquals(30, amps)
    }

    @Test
    fun clampsToMax() {
        val amps = ChargeController.computeTargetCurrentAmps(
            gridVoltageV = 208.0,
            targetPowerKw = 20.0,
            minAmps = 5,
            maxAmps = 32,
        )
        assertEquals(32, amps)
    }
}

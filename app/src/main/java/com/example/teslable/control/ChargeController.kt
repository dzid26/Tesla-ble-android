package com.example.teslable.control

import kotlin.math.roundToInt

object ChargeController {
    /**
     * Computes charging current in amps from grid voltage and target grid power.
     *
     * @param gridVoltageV measured grid voltage.
     * @param targetPowerKw desired power import in kW (set point).
     * @param minAmps lower bound (Tesla typically supports >= 5A depending on config).
     * @param maxAmps upper bound for vehicle/EVSE.
     */
    fun computeTargetCurrentAmps(
        gridVoltageV: Double,
        targetPowerKw: Double,
        minAmps: Int,
        maxAmps: Int,
    ): Int {
        require(gridVoltageV > 0) { "gridVoltageV must be > 0" }
        require(maxAmps >= minAmps) { "maxAmps must be >= minAmps" }

        val targetPowerW = targetPowerKw * 1000.0
        val rawAmps = targetPowerW / gridVoltageV
        return rawAmps.roundToInt().coerceIn(minAmps, maxAmps)
    }
}

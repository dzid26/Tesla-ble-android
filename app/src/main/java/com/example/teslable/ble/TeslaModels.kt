package com.example.teslable.ble

data class TeslaVitals(
    val batteryPercent: Int,
    val batteryVoltage: Double,
    val batteryCurrent: Double,
    val chargeAmpsSetpoint: Int,
    val isCharging: Boolean,
)

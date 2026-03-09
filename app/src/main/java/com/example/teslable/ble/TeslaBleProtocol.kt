package com.example.teslable.ble

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.roundToInt

object TeslaBleProtocol {
    // Publicly documented Tesla Vehicle Command BLE service/characteristic IDs are subject to change.
    // Keep these configurable if targeting different firmware generations.
    val serviceUuid: UUID = UUID.fromString("00000211-b2d1-43f0-9b88-960cebf8b91e")
    val toVehicleCharacteristicUuid: UUID = UUID.fromString("00000212-b2d1-43f0-9b88-960cebf8b91e")
    val fromVehicleCharacteristicUuid: UUID = UUID.fromString("00000213-b2d1-43f0-9b88-960cebf8b91e")

    private const val AUTH_REQUEST_OPCODE: Byte = 0x01
    private const val VITALS_REQUEST_OPCODE: Byte = 0x10
    private const val SET_CHARGE_CURRENT_OPCODE: Byte = 0x20

    private const val MIN_CHARGE_AMPS = 5
    private const val MAX_CHARGE_AMPS = 48

    data class VehicleVitals(
        val batteryVoltageV: Float,
        val batteryCurrentA: Float,
        val stateOfChargePercent: Int,
        val gridVoltageV: Float,
    )

    fun buildAuthenticationRequest(publicKeyBytes: ByteArray): ByteArray {
        val header = byteArrayOf(AUTH_REQUEST_OPCODE)
        return header + publicKeyBytes
    }

    fun buildVitalsRequest(): ByteArray = byteArrayOf(VITALS_REQUEST_OPCODE)

    fun buildSetChargingCurrentRequest(targetCurrentAmps: Int): ByteArray {
        val clampedAmps = targetCurrentAmps.coerceIn(MIN_CHARGE_AMPS, MAX_CHARGE_AMPS)
        return byteArrayOf(SET_CHARGE_CURRENT_OPCODE, clampedAmps.toByte())
    }

    fun calculateTargetCurrentFromGrid(gridVoltageV: Float, setPointWatts: Int): Int {
        if (gridVoltageV <= 1f || setPointWatts <= 0) {
            return MIN_CHARGE_AMPS
        }
        val calculatedAmps = (setPointWatts / gridVoltageV).roundToInt()
        return calculatedAmps.coerceIn(MIN_CHARGE_AMPS, MAX_CHARGE_AMPS)
    }

    fun parseVitalsNotification(payload: ByteArray): VehicleVitals? {
        if (payload.size < 17 || payload[0] != VITALS_REQUEST_OPCODE) {
            return null
        }

        val body = ByteBuffer.wrap(payload, 1, payload.size - 1).order(ByteOrder.LITTLE_ENDIAN)
        return VehicleVitals(
            batteryVoltageV = body.float,
            batteryCurrentA = body.float,
            stateOfChargePercent = body.get().toInt().coerceIn(0, 100),
            gridVoltageV = body.float,
        )
    }
}

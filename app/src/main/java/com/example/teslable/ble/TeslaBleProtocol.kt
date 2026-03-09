package com.example.teslable.ble

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

object TeslaBleProtocol {
    // Publicly documented Tesla Vehicle Command BLE service/characteristic IDs are subject to change.
    val serviceUuid: UUID = UUID.fromString("00000211-b2d1-43f0-9b88-960cebf8b91e")
    val toVehicleCharacteristicUuid: UUID = UUID.fromString("00000212-b2d1-43f0-9b88-960cebf8b91e")
    val fromVehicleCharacteristicUuid: UUID = UUID.fromString("00000213-b2d1-43f0-9b88-960cebf8b91e")

    private const val CMD_AUTH = 0x01
    private const val CMD_REQUEST_VITALS = 0x10
    private const val CMD_SET_CHARGE_AMPS = 0x11

    fun buildAuthenticationRequest(publicKeyBytes: ByteArray): ByteArray {
        val header = byteArrayOf(CMD_AUTH.toByte())
        return header + publicKeyBytes
    }

    fun buildRequestVitalsFrame(): ByteArray = byteArrayOf(CMD_REQUEST_VITALS.toByte())

    fun buildSetChargeCurrentFrame(amps: Int): ByteArray {
        return byteArrayOf(CMD_SET_CHARGE_AMPS.toByte(), amps.coerceIn(0, 80).toByte())
    }

    fun parseVitalsFrame(payload: ByteArray): TeslaVitals? {
        // Placeholder parser for scaffolding/prototyping.
        // Format: [cmd=0x10, soc(1), volts(4f), amps(4f), setpoint(1), charging(1)]
        if (payload.size < 12 || payload[0].toInt() != CMD_REQUEST_VITALS) return null
        val bb = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN)
        bb.get() // cmd
        val soc = bb.get().toInt() and 0xFF
        val volts = bb.float.toDouble()
        val amps = bb.float.toDouble()
        val setpoint = bb.get().toInt() and 0xFF
        val charging = bb.get().toInt() != 0
        return TeslaVitals(soc, volts, amps, setpoint, charging)
    }
}

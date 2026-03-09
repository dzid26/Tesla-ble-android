package com.example.teslable.ble

import java.util.UUID

object TeslaBleProtocol {
    // Publicly documented Tesla Vehicle Command BLE service/characteristic IDs are subject to change.
    // Keep these configurable if targeting different firmware generations.
    val serviceUuid: UUID = UUID.fromString("00000211-b2d1-43f0-9b88-960cebf8b91e")
    val toVehicleCharacteristicUuid: UUID = UUID.fromString("00000212-b2d1-43f0-9b88-960cebf8b91e")
    val fromVehicleCharacteristicUuid: UUID = UUID.fromString("00000213-b2d1-43f0-9b88-960cebf8b91e")

    fun buildAuthenticationRequest(publicKeyBytes: ByteArray): ByteArray {
        // Placeholder payload wrapper for first-pass project structure.
        // Replace with protobuf VehicleCommand and signed challenge flow.
        val header = byteArrayOf(0x01)
        return header + publicKeyBytes
    }
}

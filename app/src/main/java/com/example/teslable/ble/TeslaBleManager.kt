package com.example.teslable.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import androidx.core.content.getSystemService

class TeslaBleManager(
    private val context: Context,
    private val onStatus: (String) -> Unit,
) {
    private val bluetoothAdapter: BluetoothAdapter? =
        context.getSystemService<BluetoothManager>()?.adapter

    private var scanCallback: ScanCallback? = null
    private var selectedDevice: BluetoothDevice? = null
    private var gatt: BluetoothGatt? = null

    @SuppressLint("MissingPermission")
    fun scanForTesla(onDeviceFound: (BluetoothDevice) -> Unit) {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            onStatus("Bluetooth LE scanner unavailable")
            return
        }

        onStatus("Scanning for Tesla BLE service...")
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val hasTeslaService = result.scanRecord?.serviceUuids
                    ?.any { it.uuid == TeslaBleProtocol.serviceUuid } == true
                val looksLikeTeslaName = result.device.name?.contains("Tesla", ignoreCase = true) == true
                if (hasTeslaService || looksLikeTeslaName) {
                    selectedDevice = result.device
                    scanner.stopScan(this)
                    onStatus("Tesla found: ${result.device.address}")
                    onDeviceFound(result.device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                onStatus("Scan failed: $errorCode")
            }
        }

        scanCallback = callback
        scanner.startScan(callback)
    }

    @SuppressLint("MissingPermission")
    fun connectAndAuthenticate() {
        val device = selectedDevice
        if (device == null) {
            onStatus("No Tesla selected")
            return
        }

        onStatus("Connecting to ${device.address}...")
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    fun close() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        scanCallback?.let { scanner?.stopScan(it) }
        scanCallback = null
        gatt?.close()
        gatt = null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                onStatus("Connected, discovering services...")
                gatt.discoverServices()
            } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                onStatus("Disconnected")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service: BluetoothGattService? = gatt.getService(TeslaBleProtocol.serviceUuid)
            if (service == null) {
                onStatus("Tesla BLE service not found on device")
                return
            }

            val writeChar: BluetoothGattCharacteristic? =
                service.getCharacteristic(TeslaBleProtocol.toVehicleCharacteristicUuid)

            if (writeChar == null) {
                onStatus("Tesla write characteristic not found")
                return
            }

            val ephemeralPublicKey = ByteArray(32) { 0x42 }
            val authFrame = TeslaBleProtocol.buildAuthenticationRequest(ephemeralPublicKey)

            writeChar.value = authFrame
            val writeOk = gatt.writeCharacteristic(writeChar)
            onStatus(if (writeOk) "Auth frame sent" else "Failed to send auth frame")
        }
    }
}

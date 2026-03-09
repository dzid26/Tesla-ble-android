package com.example.teslable.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.getSystemService

class TeslaBleManager(
    private val context: Context,
    private val onStatus: (String) -> Unit,
    private val onVitals: (TeslaVitals) -> Unit,
) {
    private val bluetoothAdapter: BluetoothAdapter? =
        context.getSystemService<BluetoothManager>()?.adapter

    private var scanCallback: ScanCallback? = null
    private var selectedDevice: BluetoothDevice? = null
    private var gatt: BluetoothGatt? = null
    private var writeChar: BluetoothGattCharacteristic? = null

    @SuppressLint("MissingPermission")
    fun scanForTesla(onDeviceFound: (BluetoothDevice) -> Unit) {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            onStatus("Bluetooth LE scanner unavailable")
            return
        }

        scanCallback?.let { scanner.stopScan(it) }

        onStatus("Scanning for Tesla BLE service...")
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val hasTeslaService = result.scanRecord?.serviceUuids
                    ?.any { it.uuid == TeslaBleProtocol.serviceUuid } == true
                val looksLikeTeslaName = result.device.name?.contains("Tesla", ignoreCase = true) == true
                if (hasTeslaService || looksLikeTeslaName) {
                    selectedDevice = result.device
                    scanner.stopScan(this)
                    scanCallback = null
                    onStatus("Tesla found: ${result.device.address}")
                    onDeviceFound(result.device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                onStatus("Scan failed: $errorCode")
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(TeslaBleProtocol.serviceUuid)).build(),
        )

        scanCallback = callback
        scanner.startScan(filters, settings, callback)
    }

    @SuppressLint("MissingPermission")
    fun connectAndAuthenticate() {
        val device = selectedDevice
        if (device == null) {
            onStatus("No Tesla selected")
            return
        }

        gatt?.close()
        gatt = null
        writeChar = null

        onStatus("Connecting to ${device.address}...")
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    fun requestVitals() {
        val frame = TeslaBleProtocol.buildRequestVitalsFrame()
        val ok = writeToVehicle(frame)
        onStatus(if (ok) "Requested vitals" else "Failed to request vitals")
    }

    @SuppressLint("MissingPermission")
    fun setChargeCurrent(amps: Int) {
        val frame = TeslaBleProtocol.buildSetChargeCurrentFrame(amps)
        val ok = writeToVehicle(frame)
        onStatus(if (ok) "Set charging current to ${amps}A" else "Failed to set charging current")
    }

    @SuppressLint("MissingPermission")
    private fun writeToVehicle(payload: ByteArray): Boolean {
        val gattLocal = gatt
        val writeCharLocal = writeChar
        if (gattLocal == null || writeCharLocal == null) {
            onStatus("Not connected")
            return false
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gattLocal.writeCharacteristic(writeCharLocal, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothStatus.success
        } else {
            writeCharLocal.value = payload
            @Suppress("DEPRECATION")
            gattLocal.writeCharacteristic(writeCharLocal)
        }
    }

    @SuppressLint("MissingPermission")
    fun close() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        scanCallback?.let { scanner?.stopScan(it) }
        scanCallback = null
        gatt?.close()
        gatt = null
        writeChar = null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                onStatus("Connection failed: status=$status")
                gatt.close()
                return
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    onStatus("Connected, requesting MTU and discovering services...")
                    gatt.requestMtu(512)
                    gatt.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> onStatus("Disconnected")
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                onStatus("MTU negotiated: $mtu")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                onStatus("Service discovery failed: status=$status")
                return
            }

            val service: BluetoothGattService? = gatt.getService(TeslaBleProtocol.serviceUuid)
            if (service == null) {
                onStatus("Tesla BLE service not found on device")
                return
            }

            writeChar = service.getCharacteristic(TeslaBleProtocol.toVehicleCharacteristicUuid)
            val notifyChar: BluetoothGattCharacteristic? =
                service.getCharacteristic(TeslaBleProtocol.fromVehicleCharacteristicUuid)

            if (writeChar == null) {
                onStatus("Tesla write characteristic not found")
                return
            }

            if (notifyChar != null) {
                gatt.setCharacteristicNotification(notifyChar, true)
                val cccd = notifyChar.getDescriptor(UUIDs.clientCharacteristicConfig)
                cccd?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                if (cccd != null) gatt.writeDescriptor(cccd)
            }

            val ephemeralPublicKey = ByteArray(32) { 0x42 }
            val authFrame = TeslaBleProtocol.buildAuthenticationRequest(ephemeralPublicKey)
            val writeOk = writeToVehicle(authFrame)
            onStatus(if (writeOk) "Auth frame sent" else "Failed to send auth frame")
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val payload = characteristic.value ?: return
            val vitals = TeslaBleProtocol.parseVitalsFrame(payload)
            if (vitals != null) {
                onVitals(vitals)
                onStatus("Vitals updated")
            }
        }
    }

    private object UUIDs {
        val clientCharacteristicConfig = java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private object BluetoothStatus {
        const val success = 0
    }
}

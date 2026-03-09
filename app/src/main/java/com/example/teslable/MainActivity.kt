package com.example.teslable

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.example.teslable.ble.TeslaBleManager
import com.example.teslable.control.ChargeController
import com.example.teslable.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bleManager: TeslaBleManager

    private var pendingAction: (() -> Unit)? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result.values.all { it }
            if (granted) {
                binding.statusText.text = "Permissions granted"
                pendingAction?.invoke()
            } else {
                binding.statusText.text = "Bluetooth permissions denied"
            }
            pendingAction = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bleManager = TeslaBleManager(
            context = this,
            onStatus = { status -> runOnUiThread { binding.statusText.text = status } },
            onVitals = { vitals ->
                runOnUiThread {
                    binding.vitalsText.text =
                        "Vitals: SOC=${vitals.batteryPercent}% V=${"%.1f".format(vitals.batteryVoltage)} " +
                            "I=${"%.1f".format(vitals.batteryCurrent)}A " +
                            "Set=${vitals.chargeAmpsSetpoint}A Charging=${vitals.isCharging}"
                }
            },
        )

        binding.scanButton.setOnClickListener {
            runWithBleReady {
                bleManager.scanForTesla { device ->
                    runOnUiThread {
                        binding.deviceText.text = "Selected: ${device.name ?: "Tesla"} (${device.address})"
                        binding.connectButton.isEnabled = true
                    }
                }
            }
        }

        binding.connectButton.setOnClickListener { runWithBleReady { bleManager.connectAndAuthenticate() } }
        binding.requestVitalsButton.setOnClickListener { runWithBleReady { bleManager.requestVitals() } }
        binding.applyChargeControlButton.setOnClickListener { runWithBleReady { applyChargeControl() } }
    }

    override fun onDestroy() {
        bleManager.close()
        super.onDestroy()
    }

    private fun runWithBleReady(action: () -> Unit) {
        if (!isBluetoothEnabled()) {
            binding.statusText.text = "Enable Bluetooth and try again"
            return
        }

        if (hasBlePermissions()) {
            action()
        } else {
            pendingAction = action
            permissionLauncher.launch(requiredPermissions())
        }
    }

    private fun isBluetoothEnabled(): Boolean {
        val adapter = getSystemService<BluetoothManager>()?.adapter
        return adapter?.isEnabled == true
    }

    private fun hasBlePermissions(): Boolean =
        requiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    private fun applyChargeControl() {
        val voltage = binding.gridVoltageInput.text.toString().toDoubleOrNull()
        val setpointKw = binding.setpointInput.text.toString().toDoubleOrNull()
        val maxAmps = binding.maxAmpsInput.text.toString().toIntOrNull() ?: 32

        if (voltage == null || setpointKw == null) {
            binding.statusText.text = "Enter grid voltage and setpoint"
            return
        }

        val targetAmps = ChargeController.computeTargetCurrentAmps(
            gridVoltageV = voltage,
            targetPowerKw = setpointKw,
            minAmps = 5,
            maxAmps = maxAmps,
        )
        binding.statusText.text = "Computed target current: ${targetAmps}A"
        bleManager.setChargeCurrent(targetAmps)
    }
}

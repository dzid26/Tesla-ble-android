package com.example.teslable

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import com.example.teslable.ble.TeslaBleManager
import com.example.teslable.control.ChargeController
import com.example.teslable.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bleManager: TeslaBleManager

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result.values.all { it }
            binding.statusText.text = if (granted) "Permissions granted" else "Bluetooth permissions denied"
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
            ensureBlePermissions()
            bleManager.scanForTesla { device ->
                runOnUiThread {
                    binding.deviceText.text = "Selected: ${device.name ?: "Tesla"} (${device.address})"
                    binding.connectButton.isEnabled = true
                }
            }
        }

        binding.connectButton.setOnClickListener { bleManager.connectAndAuthenticate() }
        binding.requestVitalsButton.setOnClickListener { bleManager.requestVitals() }
        binding.applyChargeControlButton.setOnClickListener { applyChargeControl() }
    }

    override fun onDestroy() {
        bleManager.close()
        super.onDestroy()
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

    private fun ensureBlePermissions() {
        val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }
}

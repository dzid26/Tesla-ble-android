package com.example.teslable

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.teslable.ble.TeslaBleManager
import com.example.teslable.ble.TeslaBleProtocol
import com.example.teslable.databinding.ActivityMainBinding
import java.util.Locale

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
            onStatus = { status ->
                runOnUiThread { binding.statusText.text = status }
            },
            onVitals = { vitals ->
                runOnUiThread {
                    binding.vitalsText.text = formatVitals(vitals)
                    binding.gridVoltageInput.setText(vitals.gridVoltageV.toInt().toString())
                }
            },
        )

        binding.scanButton.setOnClickListener {
            ensureBlePermissions()
            bleManager.scanForTesla(binding.vinInput.text.toString()) { device ->
                runOnUiThread { setSelectedDevice(device.name ?: "Tesla", device.address) }
            }
        }

        binding.vinGuessButton.setOnClickListener {
            ensureBlePermissions()
            val matched = bleManager.connectUsingVinGuess(binding.vinInput.text.toString()) { device ->
                runOnUiThread { setSelectedDevice(device.name ?: "Tesla", device.address) }
            }
            if (!matched) {
                binding.statusText.text = "VIN guess did not resolve a bonded device; scan near vehicle."
            }
        }

        binding.connectButton.setOnClickListener {
            bleManager.connectAndAuthenticate()
        }

        binding.refreshVitalsButton.setOnClickListener {
            bleManager.requestVitals()
        }

        binding.applyCurrentButton.setOnClickListener {
            val gridVoltage = binding.gridVoltageInput.text.toString().toFloatOrNull()
            val setPointWatts = binding.setPointInput.text.toString().toIntOrNull()
            if (gridVoltage == null || setPointWatts == null) {
                binding.statusText.text = "Enter valid grid voltage and set point"
                return@setOnClickListener
            }

            val appliedAmps = bleManager.setChargingCurrentFromGrid(gridVoltage, setPointWatts)
            binding.currentResultText.text = "Applied charging current: ${appliedAmps}A"
        }
    }

    override fun onDestroy() {
        bleManager.close()
        super.onDestroy()
    }

    private fun setSelectedDevice(deviceName: String, address: String) {
        binding.deviceText.text = "Selected: $deviceName ($address)"
        binding.connectButton.isEnabled = true
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

    private fun formatVitals(vitals: TeslaBleProtocol.VehicleVitals): String {
        return String.format(
            Locale.US,
            "Battery %.1fV, %.1fA | SOC %d%% | Grid %.1fV",
            vitals.batteryVoltageV,
            vitals.batteryCurrentA,
            vitals.stateOfChargePercent,
            vitals.gridVoltageV,
        )
    }
}

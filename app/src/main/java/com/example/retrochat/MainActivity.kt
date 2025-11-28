package com.example.retrochat

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private val bluetoothLeScanner by lazy { bluetoothAdapter?.bluetoothLeScanner }
    private var scanning = false
    private lateinit var devicesListView: ListView
    private lateinit var scanButton: Button
    private lateinit var discoveredDevicesAdapter: ArrayAdapter<String>
    private val discoveredDevices = mutableListOf<BluetoothDevice>()

    private val requestBluetoothPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.values.all { it }) {
                checkBluetoothState()
            } else {
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show()
            }
        }

    private val requestEnableBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, "Bluetooth must be enabled to scan for devices", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        devicesListView = findViewById(R.id.devices_list)
        scanButton = findViewById(R.id.scan_button)

        discoveredDevicesAdapter = ArrayAdapter(this, R.layout.list_item_device, mutableListOf<String>())
        devicesListView.adapter = discoveredDevicesAdapter

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        checkAndRequestBluetoothPermissions()

        scanButton.setOnClickListener {
            if (scanning) {
                stopBleScan()
            } else {
                startBleScan()
            }
        }
    }

    private fun startBleScan() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            checkAndRequestBluetoothPermissions()
            return
        }
        discoveredDevices.clear()
        discoveredDevicesAdapter.clear()
        bluetoothLeScanner?.startScan(bleScanCallback)
        scanning = true
        scanButton.text = "Stop Scan"
        Toast.makeText(this, "Scanning for BLE devices...", Toast.LENGTH_SHORT).show()
    }

    private fun stopBleScan() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            checkAndRequestBluetoothPermissions()
            return
        }
        bluetoothLeScanner?.stopScan(bleScanCallback)
        scanning = false
        scanButton.text = "Scan for Devices"
    }

    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let { device ->
                if (!discoveredDevices.contains(device)) {
                    discoveredDevices.add(device)
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        checkAndRequestBluetoothPermissions()
                        return
                    }
                    val deviceName = device.name ?: "Unknown Device"
                    discoveredDevicesAdapter.add("$deviceName\n${device.address}")
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Toast.makeText(this@MainActivity, "BLE Scan Failed: $errorCode", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkBluetoothState() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show()
        } else {
            if (!bluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                requestEnableBluetooth.launch(enableBtIntent)
            }
        }
    }

    private fun checkAndRequestBluetoothPermissions() {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION)
        }
        requestBluetoothPermissions.launch(permissionsToRequest)
    }
}

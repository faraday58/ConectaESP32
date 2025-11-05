package com.mexiti.conectaesp32.repositorio

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val isPaired: Boolean,
    val deviceType: String
)

class PairedDevicesRepository(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = _pairedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    companion object {
        private const val TAG = "PairedDevicesRepository"
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDeviceInfo> {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.w(TAG, "Bluetooth no disponible o desactivado")
            return emptyList()
        }

        val pairedDevices = try {
            bluetoothAdapter.bondedDevices?.map { device ->
                BluetoothDeviceInfo(
                    name = device.name ?: "Dispositivo Desconocido",
                    address = device.address,
                    isPaired = true,
                    deviceType = getDeviceTypeString(device)
                )
            } ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "Error de permisos al obtener dispositivos emparejados", e)
            emptyList()
        }

        _pairedDevices.value = pairedDevices
        Log.d(TAG, "Dispositivos emparejados encontrados: ${pairedDevices.size}")

        return pairedDevices
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceTypeString(device: BluetoothDevice): String {
        return when (device.type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Clásico"
            BluetoothDevice.DEVICE_TYPE_LE -> "BLE"
            BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
            else -> "Desconocido"
        }
    }

    fun findESP32Devices(): List<BluetoothDeviceInfo> {
        val allPaired = getPairedDevices()
        return allPaired.filter { device ->
            device.name.contains("ESP32", ignoreCase = true) ||
                    device.name.contains("ESP", ignoreCase = true) ||
                    device.name.contains("Arduino", ignoreCase = true) ||
                    device.name.contains("HC-", ignoreCase = true) // Para módulos HC-05, HC-06
        }
    }

    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    @SuppressLint("MissingPermission")
    fun getAdapterInfo(): String {
        return when {
            bluetoothAdapter == null -> "Bluetooth no soportado"
            !bluetoothAdapter.isEnabled -> "Bluetooth desactivado"
            else -> "Bluetooth activado - ${bluetoothAdapter.name ?: "Adaptador local"}"
        }
    }
}



package com.mexiti.conectaesp32.viewmodel


import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mexiti.conectaesp32.reporistorio.BluetoothDeviceInfo
import com.mexiti.conectaesp32.reporistorio.PairedDevicesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PairedDevicesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PairedDevicesRepository(application.applicationContext)

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = _pairedDevices.asStateFlow()

    private val _esp32Devices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val esp32Devices: StateFlow<List<BluetoothDeviceInfo>> = _esp32Devices.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedDevice = MutableStateFlow<BluetoothDeviceInfo?>(null)
    val selectedDevice: StateFlow<BluetoothDeviceInfo?> = _selectedDevice.asStateFlow()

    companion object {
        private const val TAG = "PairedDevicesViewModel"
    }

    init {
        refreshDevices()
    }

    fun refreshDevices() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val devices = repository.getPairedDevices()
                _pairedDevices.value = devices

                val esp32s = repository.findESP32Devices()
                _esp32Devices.value = esp32s

                Log.d(TAG, "Dispositivos encontrados: ${devices.size}, ESP32s: ${esp32s.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al refrescar dispositivos", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectDevice(device: BluetoothDeviceInfo) {
        _selectedDevice.value = device
        Log.d(TAG, "Dispositivo seleccionado: ${device.name} (${device.address})")
    }

    fun clearSelection() {
        _selectedDevice.value = null
    }

    fun getAdapterInfo(): String {
        return repository.getAdapterInfo()
    }

    fun isBluetoothAvailable(): Boolean {
        return repository.isBluetoothAvailable()
    }
}
package com.mexiti.conectaesp32.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mexiti.conectaesp32.reporistorio.BluetoothRepository
import com.mexiti.conectaesp32.reporistorio.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.Date
import java.util.Locale

class BluetoothViewModel(application: Application): AndroidViewModel(application){
    private val bluetoothRepository = BluetoothRepository(application.applicationContext)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState:StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _receivedDataLog = MutableStateFlow<List<String>>(emptyList())
    val receivedDataLog: StateFlow<List<String>> = _receivedDataLog.asStateFlow()

    val adapterState:String
        get() = bluetoothRepository.getAdapterState()

    init {
        // Observar el estado de conexión del repositorio
        bluetoothRepository.connectionState
            .onEach { state -> _connectionState.value = state }
            .launchIn(viewModelScope)
        // Observar los datos recibidos del repositorio
        bluetoothRepository.receivedData
            .onEach { data ->
                _receivedDataLog.value = _receivedDataLog.value + "${getCurrentTimestamp()} TX: $data"
            }
            .launchIn(viewModelScope)
    }

    fun connect(deviceAddress: String){
        if(deviceAddress.isBlank() || !deviceAddress.matches(Regex("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})"))){
            _connectionState.value = ConnectionState.Error("Dirección MAC inválida")
            return
        }
        _receivedDataLog.value = emptyList() // Limpiar log al intentar nueva conexión
        bluetoothRepository.connectToDevice(deviceAddress,viewModelScope)
    }

    fun disconnect(){
        bluetoothRepository.disconnect(viewModelScope)
        // El estado se actualizará a través del flow del repositorio
    }

    fun sendMessage(message: String){
        if(message.isBlank()) return
        val fullMessage = message + "\n" // Es común añadir nueva línea
        bluetoothRepository.sendData(fullMessage,viewModelScope)
        _receivedDataLog.value = receivedDataLog.value + "${getCurrentTimestamp()} TX: $message"
    }

    private fun getCurrentTimestamp():String {
        // Implementa una función simple para obtener un timestamp si lo deseas
        return java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
     }
    // Opcional: para limpiar recursos si es necesario, aunque el repositorio ya lo hace
    override fun onCleared() {
        super.onCleared()
        bluetoothRepository.disconnect(viewModelScope)
        Log.d("BluetoothViewModel", "ViewModel cleared")
    }

}
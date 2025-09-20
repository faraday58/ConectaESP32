package com.mexiti.conectaesp32.reporistorio

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

private val BT_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
private const val TAG = "BluetoothRepository"

// Define estados y eventos para comunicar al ViewModel
sealed class ConnectionState {
    object Idle: ConnectionState()
    data class Connecting(val deviceName:String): ConnectionState()
    data class Connected(val deviceName:String): ConnectionState()
    data class Error(val message:String) : ConnectionState()
    object Disconnected: ConnectionState()
}

class BluetoothRepository(private val context:Context){
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter:BluetoothAdapter? = bluetoothManager.adapter

    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream:OutputStream? =  null
    private var inputStream: InputStream? = null
    private var jobLectura: Job? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState:StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _receivedData = MutableSharedFlow<String>() // Usamos SharedFlow para eventos
    val receivedData:SharedFlow<String> = _receivedData.asSharedFlow()

    @SuppressLint("MissingPermission")//Se deben verificar antes los permisos
    fun connectToDevice(deviceAddress: String, scope: CoroutineScope){
        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled){
            _connectionState.value = ConnectionState.Error("El Bluetooth no está habilitado")
            return
        }

    }

}
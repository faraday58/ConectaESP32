package com.mexiti.conectaesp32.reporistorio

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
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

        val device: BluetoothDevice? = try {
            bluetoothAdapter.getRemoteDevice(deviceAddress)
        }catch (e:IllegalArgumentException){
            _connectionState.value = ConnectionState.Error("Dirección MAC inválida")
            null
        }

        if(device == null){
            if(_connectionState.value !is ConnectionState.Error){ // Evita sobreescribir error de MAC
                _connectionState.value = ConnectionState.Error("Dispositivo no encontrado")
            }
            return
        }

        val deviceName = device.name ?: deviceAddress
        _connectionState.value = ConnectionState.Connecting(deviceName)

        scope.launch(Dispatchers.IO) {
            try {
                closeConnectionInternally() // Asegura cerrar conexión previa

                bluetoothSocket = device.createRfcommSocketToServiceRecord(BT_UUID)
                bluetoothSocket?.connect() // Bloquear nueva conexión

                outputStream = bluetoothSocket?.outputStream
                inputStream = bluetoothSocket?.inputStream

                _connectionState.value = ConnectionState.Connected(deviceName)
                startReceivingData(this) // 'this' es el CoroutineScope del launch
            }catch (e: IOException){
                Log.e(TAG,"Error al conectar: ${e.message}",e)
                _connectionState.value = ConnectionState.Error("Conexión fallida: ${e.message?.take(30)}")
                closeConnectionInternally()
            }catch (se: SecurityException){
                Log.e(TAG, "Error de seguridad al conectar: ${se.message}",se)
                _connectionState.value = ConnectionState.Error("Error de seguridad. ¿Permisos?")
                closeConnectionInternally()
            }
        }
    }
    private fun startReceivingData(coroutineScope: CoroutineScope) {
        jobLectura?.cancel()
        jobLectura = coroutineScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(1024)
            var bytes: Int

            while (
                isActive &&
                bluetoothSocket?.isConnected == true
                &&
                inputStream != null
            ){
                try {
                    bytes = inputStream!!.read(buffer) //Bloquear nueva lectura

                    if( bytes > 0){
                        val incommingMessage = String(buffer,0,bytes)
                        if(incommingMessage.isNotEmpty()){
                            _receivedData.emit(incommingMessage) //Transmitir por SharedFlow
                            Log.d(TAG,"Recibido $incommingMessage")
                        }
                    }else{
                        //EOF o desconexión del otro lado
                        Log.d(TAG,"Stream cerrado por el dispositivo remoto o EOF")
                        _connectionState.value = ConnectionState.Disconnected
                        closeConnectionInternally()
                        break
                    }

                }catch (e: IOException){
                    if(isActive){
                        Log.e(TAG,"Error al leer datos: ${e.message}")
                        _connectionState.value = ConnectionState.Disconnected
                    }
                    break
                }
                if(isActive
                    &&
                    _connectionState.value !is ConnectionState.Error
                    &&
                    _connectionState.value !is ConnectionState.Disconnected

                    ){
                    // Si el bucle termina y no fue por un error explícito o desconexión, asumir desconexión.
                    _connectionState.value = ConnectionState.Disconnected
                }
                closeConnectionInternally() // Asegurar limpieza si el bucle termina
            }

        }
    }

    fun sendData(data: String, coroutineScope: CoroutineScope){
        if(outputStream == null || bluetoothSocket?.isConnected != true){
            _connectionState.value = ConnectionState.Error("No conectado para enviar")
            return
        }
        coroutineScope.launch(Dispatchers.IO) {
            try {
                outputStream?.write(data.toByteArray())
                outputStream?.flush()
                Log.d(TAG,"Enviado: $data")
            }catch (e: IOException){
                Log.e(TAG,"Error al enviar datos: ${e.message}")
                _connectionState.value = ConnectionState.Error("Error al enviar")

            }
        }
    }
    fun disconnect(coroutineScope: CoroutineScope ){
        coroutineScope.launch(Dispatchers.IO) {
            closeConnectionInternally()
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    fun getAdapterState():String{
        return when{
            bluetoothAdapter == null -> "No soporta Bluetooth"
            !bluetoothAdapter.isEnabled -> "Bluetooth desactivado"
            else -> "Bluetooth activado"
        }
    }

    private fun closeConnectionInternally() {
        jobLectura?.cancel()
        jobLectura = null
        try {
            outputStream?.close()
            inputStream?.close()
            bluetoothSocket?.close()
        }catch (e: IOException){
            Log.e(TAG,"Error al cerrar socket ${e.message}")
        }finally {
            outputStream = null
            inputStream = null
            bluetoothSocket = null
        }
    }

}
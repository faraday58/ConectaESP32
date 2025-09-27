package com.mexiti.conectaesp32.ui.Screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mexiti.conectaesp32.viewmodel.BluetoothViewModel
@Composable
fun  BluetoothEsp32Screen(BluetoothVM: BluetoothViewModel = viewModel()){
    val context = LocalContext.current
    val connectionState by BluetoothVM.connectionState.collectAsState()
    val recivedDataLog by BluetoothVM.receivedDataLog.collectAsState()

    var deviceAddressInput by remember { mutableStateOf(TextFieldValue("XX:XX:XX:XX:XX:XX")) }
    var messageToSendInput by remember { mutableStateOf(TextFieldValue("")) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // --- Gestión de Permisos ---
    var hasPermissions by remember { mutableStateOf(hasRequiredBluetoothPermissions(context)) }
}

fun hasRequiredBluetoothPermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

        ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    } else {
        ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
    }
}
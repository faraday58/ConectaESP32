package com.mexiti.conectaesp32.ui.Screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mexiti.conectaesp32.R
import com.mexiti.conectaesp32.viewmodel.BluetoothViewModel
import com.mexiti.conectaesp32.viewmodel.PairedDevicesViewModel

sealed class Screen {
    object DeviceSelection : Screen()
    object BluetoothConnection : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScreen() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.DeviceSelection) }
    var selectedDeviceAddress by remember { mutableStateOf(TextFieldValue("")) }

    val bluetoothViewModel: BluetoothViewModel = viewModel()
    val pairedDevicesViewModel: PairedDevicesViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Dispositivos") },
                    label = { Text("Dispositivos") },
                    selected = currentScreen is Screen.DeviceSelection,
                    onClick = { currentScreen = Screen.DeviceSelection }
                )
                NavigationBarItem(
                    icon = {  Icon( painterResource(R.drawable.outline_bluetooth_24), contentDescription = "Bluetooth"   )    },
                    label = { Text("Conexión") },
                    selected = currentScreen is Screen.BluetoothConnection,
                    onClick = { currentScreen = Screen.BluetoothConnection }
                )
            }
        }
    ) { paddingValues ->
        when (currentScreen) {
            is Screen.DeviceSelection -> {
                DeviceSelectionScreen(
                    pairedDevicesVM = pairedDevicesViewModel,
                    onDeviceSelected = { device ->
                        selectedDeviceAddress = TextFieldValue(device.address)
                    },
                    onNavigateToConnection = {
                        currentScreen = Screen.BluetoothConnection
                    }
                )
            }
            is Screen.BluetoothConnection -> {
                BluetoothEsp32Screen(
                    bluetoothVM = bluetoothViewModel,
                    modifier = Modifier.padding(paddingValues),
                    preSelectedAddress = selectedDeviceAddress
                )
            }
        }
    }
}
package com.mexiti.conectaesp32.ui.Screens

import android.Manifest
import  com.mexiti.conectaesp32.R
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mexiti.conectaesp32.reporistorio.BluetoothDeviceInfo
import com.mexiti.conectaesp32.viewmodel.PairedDevicesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSelectionScreen(
    pairedDevicesVM: PairedDevicesViewModel = viewModel(),
    onDeviceSelected: (BluetoothDeviceInfo) -> Unit = {},
    onNavigateToConnection: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val pairedDevices by pairedDevicesVM.pairedDevices.collectAsState()
    val esp32Devices by pairedDevicesVM.esp32Devices.collectAsState()
    val isLoading by pairedDevicesVM.isLoading.collectAsState()
    val selectedDevice by pairedDevicesVM.selectedDevice.collectAsState()

    // Gestión de permisos
    var hasPermissions by remember { mutableStateOf(hasRequiredBluetoothPermissions(context)) }
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        hasPermissions = allGranted
        if (allGranted) {
            pairedDevicesVM.refreshDevices()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Se requieren permisos de Bluetooth",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Seleccionar Dispositivo ESP32") },
                actions = {
                    IconButton(
                        onClick = {
                            if (hasPermissions) {
                                pairedDevicesVM.refreshDevices()
                            } else {
                                permissionLauncher.launch(permissionsToRequest)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Estado del Bluetooth
            Text(
                text = pairedDevicesVM.getAdapterInfo(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (!pairedDevicesVM.isBluetoothAvailable()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Bluetooth no disponible",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "Activa el Bluetooth para continuar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                return@Column
            }

            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Dispositivos ESP32 encontrados
            if (esp32Devices.isNotEmpty()) {
                Text(
                    "Dispositivos ESP32 Encontrados:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(esp32Devices) { device ->
                        DeviceCard(
                            device = device,
                            isSelected = selectedDevice?.address == device.address,
                            isESP32 = true,
                            onClick = {
                                pairedDevicesVM.selectDevice(device)
                                onDeviceSelected(device)
                            }
                        )
                    }
                }
            }

            // Separador si hay ambos tipos
            if (esp32Devices.isNotEmpty() && pairedDevices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Todos los dispositivos emparejados
            if (pairedDevices.isNotEmpty()) {
                Text(
                    "Todos los Dispositivos Emparejados:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(pairedDevices.filter { !esp32Devices.contains(it) }) { device ->
                        DeviceCard(
                            device = device,
                            isSelected = selectedDevice?.address == device.address,
                            isESP32 = false,
                            onClick = {
                                pairedDevicesVM.selectDevice(device)
                                onDeviceSelected(device)
                            }
                        )
                    }
                }
            }

            // Botones de acción
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { pairedDevicesVM.refreshDevices() },
                    modifier = Modifier.weight(1f),
                    enabled = hasPermissions && !isLoading
                ) {
                    Text("Actualizar Lista")
                }

                Button(
                    onClick = onNavigateToConnection,
                    modifier = Modifier.weight(1f),
                    enabled = selectedDevice != null
                ) {
                    Text("Usar Seleccionado")
                }
            }

            // Información del dispositivo seleccionado
            selectedDevice?.let { device ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Dispositivo Seleccionado:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            device.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "MAC: ${device.address}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            // Mensaje si no hay dispositivos
            if (!isLoading && pairedDevices.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "No hay dispositivos emparejados",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Empareja tu ESP32 desde la configuración de Bluetooth del sistema",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: BluetoothDeviceInfo,
    isSelected: Boolean,
    isESP32: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isESP32 -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isESP32) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_bluetooth_24),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Información del dispositivo
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isESP32) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "ESP32",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier
                                .background(
                                    Color(0xFF4CAF50).copy(alpha = 0.1f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = "Tipo: ${device.deviceType} • Emparejado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Indicador de selección
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Seleccionado",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

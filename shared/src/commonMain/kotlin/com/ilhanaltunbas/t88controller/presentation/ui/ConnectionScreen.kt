package com.ilhanaltunbas.t88controller.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ilhanaltunbas.t88controller.domain.model.ConnectionStatus
import com.ilhanaltunbas.t88controller.presentation.viewmodel.ConnectionViewModel
import org.koin.compose.koinInject

@Composable
fun ConnectionScreen(
    viewModel: ConnectionViewModel = koinInject(), // Soket bağlantısını yönetecek ViewModel
    onNavigateToMain: () -> Unit // Bağlantı başarılı olduğunda 3 sekmeli ekrana geçiş tetikleyicisi
) {
    // ViewModel'den gelecek olan bağlantı state'lerini dinliyoruz (Örn: DISCONNECTED, CONNECTING, CONNECTED, ERROR)
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    var ipAddress by remember { mutableStateOf("192.168.1.100") }
    var port by remember { mutableStateOf("5000") }

    // Bağlantı başarılıysa doğrudan ana ekrana yönlendir
    LaunchedEffect(connectionStatus) {
        if (connectionStatus == ConnectionStatus.CONNECTED) {
            onNavigateToMain()
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "T88 Audio Matrix",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Cihaza Bağlan",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = ipAddress,
                onValueChange = { ipAddress = it },
                label = { Text("IP Adresi") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Port") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.connectToDevice(ipAddress, port.toIntOrNull() ?: 5000)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = connectionStatus != ConnectionStatus.CONNECTING
            ) {
                if (connectionStatus == ConnectionStatus.CONNECTING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bağlanıyor...")
                } else {
                    Text("BAĞLAN")
                }
            }

            // Hata Durumu Gösterimi
            if (connectionStatus == ConnectionStatus.ERROR) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Hata",
                        tint = Color.Red
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bağlantı hatası! Lütfen IP adresini ve ağı kontrol edin.",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
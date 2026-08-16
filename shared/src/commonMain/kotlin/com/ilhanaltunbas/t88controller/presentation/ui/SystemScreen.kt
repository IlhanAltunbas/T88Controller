package com.ilhanaltunbas.t88controller.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilhanaltunbas.t88controller.domain.model.ConnectionStatus
import com.ilhanaltunbas.t88controller.presentation.viewmodel.SystemViewModel
import org.koin.compose.koinInject

@Composable
fun SystemScreen(
    viewModel: SystemViewModel = koinInject()
) {
    val isMasterMuted by viewModel.isMasterMuted.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    var ipAddress by remember { mutableStateOf("192.168.1.100") }
    var port by remember { mutableStateOf("5000") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(16.dp)
    ) {
        // BAĞLANTI AYARLARI
        SystemSectionHeader("CİHAZ BAĞLANTI AYARLARI")
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = { ipAddress = it },
                    label = { Text("Yeni IP Adresi") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Yeni Port") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.changeDevice(ipAddress, port.toIntOrNull() ?: 5000) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = connectionStatus != ConnectionStatus.CONNECTING
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (connectionStatus == ConnectionStatus.CONNECTING) "Bağlanıyor..." else "CİHAZI GÜNCELLE VE BAĞLAN")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // GENEL KONTROLLER
        SystemSectionHeader("GENEL SES KONTROLÜ")
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isMasterMuted) Color(0xFFB71C1C) else Color(0xFF1B5E20))
                .clickable { viewModel.toggleMasterMute() }
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isMasterMuted) "TÜM ÇIKIŞLAR SUSTURULDU (MUTED)" else "TÜM ÇIKIŞLAR AÇIK (UNMUTED)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // KAMERA KONTROLÜ
        SystemSectionHeader("KAMERA POZİSYON KONTROLÜ (0x0A)")
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            item { CameraPosButton("VARSAYILAN", 0) { viewModel.setCameraPosition(0) } }
            items((1..8).toList()) { id ->
                CameraPosButton("KANAL $id", id) { viewModel.setCameraPosition(id) }
            }
        }
    }
}

@Composable
fun SystemSectionHeader(title: String) {
    Text(
        text = title,
        color = Color.Gray,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun CameraPosButton(label: String, id: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1.2f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF222222))
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (id == 0) "DEF" else id.toString(),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
        )
    }
}
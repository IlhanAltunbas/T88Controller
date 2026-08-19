package com.ilhanaltunbas.t88controller.presentation.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    val lastCameraPos by viewModel.lastCameraPosition.collectAsState()

    var ipAddress by remember { mutableStateOf(viewModel.currentIp) }
    var port by remember { mutableStateOf(viewModel.currentPort) }

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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), // iOS uyumluluğu için
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
        
        MasterMuteButton(
            isMuted = isMasterMuted,
            onClick = { viewModel.toggleMasterMute() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // KAMERA KONTROLÜ
        SystemSectionHeader("KAMERA POZİSYON KONTROLÜ (0x0A)")
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            item { 
                CameraPosButton(
                    label = "DEF", 
                    isSelected = lastCameraPos == 0
                ) { viewModel.setCameraPosition(0) } 
            }
            items((1..8).toList()) { id ->
                CameraPosButton(
                    label = id.toString(), 
                    isSelected = lastCameraPos == id
                ) { viewModel.setCameraPosition(id) }
            }
        }
    }
}

@Composable
fun MasterMuteButton(isMuted: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isMuted) Color(0xFFB71C1C) else Color(0xFF1B5E20),
        animationSpec = tween(400),
        label = "color"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(color = Color.White.copy(alpha = 0.2f))
            ) { onClick() }
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isMuted) "TÜM ÇIKIŞLAR SUSTURULDU (MUTED)" else "TÜM ÇIKIŞLAR AÇIK (UNMUTED)",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun SystemSectionHeader(title: String) {
    Text(
        text = title,
        color = Color.Gray,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun CameraPosButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 600f),
        label = "scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1A1A1A),
        animationSpec = tween(300),
        label = "bg"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1.2f)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp, 
                color = if (isSelected) Color.White.copy(alpha = 0.5f) else Color(0xFF333333), 
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Gray,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp
        )
    }
}

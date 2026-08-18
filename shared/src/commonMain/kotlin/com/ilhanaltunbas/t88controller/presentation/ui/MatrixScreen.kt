package com.ilhanaltunbas.t88controller.presentation.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilhanaltunbas.t88controller.presentation.viewmodel.MatrixViewModel
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Composable
fun MatrixScreen(
    viewModel: MatrixViewModel = koinInject()
) {
    val activeRoutes by viewModel.activeRoutes.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val selectedRoute by viewModel.selectedRoute.collectAsState()
    val matrixGains by viewModel.matrixGains.collectAsState()

    var showManualGainDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            MatrixRoutingHeader(
                onResetClick = { viewModel.resetToDefaultMatrix() },
                onClearAllClick = { viewModel.clearAllRoutes() },
                isProcessing = isProcessing
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Grid (Izgara) Bölümü
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp)
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState())
            ) {
                // İŞLEME MASKESİ ve GRID
                Box(contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1A1A))
                            .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(start = 60.dp)) {
                            for (inChannel in 1..8) {
                                Box(modifier = Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                                    Text("IN $inChannel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        for (outChannel in 1..8) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(60.dp, 54.dp), contentAlignment = Alignment.CenterStart) {
                                    Text("OUT $outChannel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                }

                                for (inChannel in 1..8) {
                                    val currentPair = Pair(inChannel, outChannel)
                                    val isRouted = activeRoutes.contains(currentPair)
                                    val isSelected = selectedRoute == currentPair
                                    val gainValue = matrixGains[currentPair] ?: 0.0f

                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                                    isRouted -> Color(0xFF00897B).copy(alpha = 0.8f)
                                                    else -> Color(0xFF111111)
                                                }
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isRouted -> Color(0xFF00BFA5)
                                                    else -> Color(0xFF333333)
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable(enabled = !isProcessing) { viewModel.toggleRoute(inChannel, outChannel) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val displayValue = ((gainValue * 10).roundToInt() / 10.0).toString()
                                        Text(
                                            text = displayValue,
                                            color = if (isRouted) Color.White else Color(0xFF333333),
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // İşleniyor Efekti
                    if (isProcessing) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color.White)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("MATRİS GÜNCELLENİYOR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(130.dp))
        }

        // Manual Gain Entry Dialog
        if (showManualGainDialog && selectedRoute != null) {
            val (inCh, outCh) = selectedRoute!!
            var tempGainValue by remember { mutableStateOf((matrixGains[selectedRoute] ?: 0.0f).toString()) }
            
            AlertDialog(
                onDismissRequest = { showManualGainDialog = false },
                containerColor = Color(0xFF1E1E1E),
                title = { Text("Gain Değeri Gir (-60 / +12)", color = Color.White, fontSize = 16.sp) },
                text = {
                    OutlinedTextField(
                        value = tempGainValue,
                        onValueChange = { tempGainValue = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val valFloat = tempGainValue.toFloatOrNull()
                        if (valFloat != null) {
                            viewModel.updateMatrixGain(inCh, outCh, valFloat)
                        }
                        showManualGainDialog = false
                    }) { Text("Uygula") }
                },
                dismissButton = {
                    TextButton(onClick = { showManualGainDialog = false }) { Text("İptal") }
                }
            )
        }

        // SABİT ALT GAIN BAR
        AnimatedVisibility(
            visible = selectedRoute != null && !isProcessing,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedRoute?.let { (inCh, outCh) ->
                val currentGain = matrixGains[selectedRoute] ?: 0.0f
                
                Surface(
                    color = Color(0xFF151515),
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .border(0.5.dp, Color(0xFF252525))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ROTA: IN $inCh → OUT $outCh",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Row {
                                TextButton(
                                    onClick = { viewModel.resetGainOfSelected() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("SIFIRLA", fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("-60", color = Color.Gray, fontSize = 10.sp)
                            Slider(
                                value = currentGain,
                                onValueChange = { viewModel.updateMatrixGain(inCh, outCh, it) },
                                valueRange = -60f..12f,
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text("+12", color = Color.Gray, fontSize = 10.sp)
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF222222))
                                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(6.dp))
                                    .clickable { showManualGainDialog = true }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ((currentGain * 10).roundToInt() / 10.0).toString(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatrixRoutingHeader(
    onResetClick: () -> Unit,
    onClearAllClick: () -> Unit,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isProcessing) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFFB71C1C), strokeWidth = 2.dp)
        } else {
            OutlinedButton(
                onClick = onResetClick,
                border = BorderStroke(1.dp, Color(0xFF555555)),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("RESET", fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
            
            Spacer(Modifier.width(8.dp))
            
            OutlinedButton(
                onClick = onClearAllClick,
                border = BorderStroke(1.dp, Color(0xFFB71C1C).copy(alpha = 0.7f)),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB71C1C)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("CLEAR", fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
        }
    }
}

package com.ilhanaltunbas.t88controller.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilhanaltunbas.t88controller.presentation.viewmodel.MatrixViewModel
import org.koin.compose.koinInject

@Composable
fun MatrixScreen(
    viewModel: MatrixViewModel = koinInject()
) {
    val activeRoutes by viewModel.activeRoutes.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(top = 16.dp, bottom = 16.dp)
    ) {
        // Üst kısımdaki Row, Yazı ve Buton karmaşasının yerini sadece bu satır aldı:
        MatrixRoutingHeader(
            onClearAllClick = { viewModel.clearAllRoutes() },
            isProcessing = isProcessing
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Grid (Izgara) Bölümü
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .horizontalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1A1A))
                    .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                // Sütun Başlıkları (INPUTLAR)
                Row(modifier = Modifier.padding(start = 60.dp)) {
                    for (inChannel in 1..8) {
                        Box(
                            modifier = Modifier.size(54.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("IN $inChannel", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Satırlar (OUTPUTLAR ve Grid Kesişim Noktaları)
                for (outChannel in 1..8) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Satır Başlığı (OUTPUTLAR)
                        Box(
                            modifier = Modifier.size(60.dp, 54.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text("OUT $outChannel", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // Kesişim Butonları
                        for (inChannel in 1..8) {
                            val isRouted = activeRoutes.contains(Pair(inChannel, outChannel))

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isRouted) Color(0xFF00897B) else Color(0xFF111111))
                                    .border(
                                        width = 1.dp,
                                        color = if (isRouted) Color(0xFF00BFA5) else Color(0xFF333333),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { viewModel.toggleRoute(inChannel, outChannel) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "0.0",
                                    color = if (isRouted) Color.White else Color.Gray,
                                    fontSize = 12.sp,
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
    onClearAllClick: () -> Unit,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "MATRIX YÖNLENDİRME",
            // Yazıyı titleLarge'dan titleMedium'a çekip biraz daha kibarlaştırdık
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            color = Color(0xFFDDDDDD)
        )

        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color(0xFFB71C1C),
                strokeWidth = 2.dp
            )
        } else {
            OutlinedButton(
                onClick = onClearAllClick,
                border = BorderStroke(1.dp, Color(0xFFB71C1C)),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB71C1C)),
                // YÜKSEKLİK ÇÖZÜMÜ: İç boşlukları minimuma indirdik ve sabit yükseklik verdik
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "TÜMÜNÜ TEMİZLE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp // Yazı boyutunu da butonla orantılı küçülttük
                )
            }
        }
    }
}

private fun border(width: androidx.compose.ui.unit.Dp, color: Color, shape: androidx.compose.ui.graphics.Shape) = androidx.compose.foundation.BorderStroke(width, color)
package com.ilhanaltunbas.t88controller.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilhanaltunbas.t88controller.presentation.viewmodel.ScenesViewModel
import org.koin.compose.koinInject

@Composable
fun ScenesScreen(
    viewModel: ScenesViewModel = koinInject()
) {
    val scenes by viewModel.scenes.collectAsState()
    val activePreset by viewModel.activePreset.collectAsState()

    // Onay penceresi için state
    var sceneToRecall by remember { mutableStateOf<Int?>(null) }

    // ONAY PENCERESİ (Confirmation Dialog)
    if (sceneToRecall != null) {
        AlertDialog(
            onDismissRequest = { sceneToRecall = null },
            containerColor = Color(0xFF222222),
            title = { Text("Sahneyi Yükle", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Sahne $sceneToRecall yüklenecek. Mevcut tüm mikser ayarlarınızın üzerine yazılacaktır. Onaylıyor musunuz?",
                    color = Color(0xFFBBBBBB)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.recallScene(sceneToRecall!!)
                        sceneToRecall = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)) // Tehlikeli işlem rengi (Kırmızı)
                ) { Text("Yükle", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { sceneToRecall = null }) { Text("İptal", color = Color.Gray) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(top = 8.dp, bottom = 16.dp)
    ) {
        // GEREKSİZ BAŞLIKLAR KALDIRILDI

        // 30 Sahneyi listeleyen Grid yapısı
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp), // Ekrana göre sığdırır
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(scenes) { index, sceneName ->
                val sceneId = index + 1
                val isActive = activePreset == sceneId

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color(0xFF1A1A1A))
                        .border(
                            width = if (isActive) 2.dp else 1.dp,
                            color = if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF333333),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { sceneToRecall = sceneId }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive) {
                        // Aktif Sahne Rozeti
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = sceneId.toString(),
                            color = if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF555555),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = sceneName,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
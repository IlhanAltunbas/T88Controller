package com.ilhanaltunbas.t88controller.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
            .padding(top = 16.dp, bottom = 16.dp)
    ) {
        // GEREKSİZ .let BLOĞU KALDIRILDI
        Text(
            text = "SAHNE YÖNETİMİ (PRESETS)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = Color(0xFFDDDDDD),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Cihaz hafızasındaki 30 sahneden birini çağırabilirsiniz.",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 30 Sahneyi listeleyen Grid yapısı
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp), // Ekrana göre sığdırır
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(scenes) { index, sceneName ->
                val sceneId = index + 1 // Cihazda sahneler 1'den 30'a kadar numaralandırılmış[cite: 1]

                Box(
                    modifier = Modifier
                        .aspectRatio(1f) // Kare şeklinde butonlar
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1A1A))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp))
                        .clickable { sceneToRecall = sceneId }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = sceneId.toString(),
                            color = Color(0xFF555555),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
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
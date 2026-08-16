package com.ilhanaltunbas.t88controller

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.ilhanaltunbas.t88controller.di.appModule
import com.ilhanaltunbas.t88controller.presentation.ui.ConnectionScreen
import com.ilhanaltunbas.t88controller.presentation.ui.MainScreen
import org.koin.compose.KoinApplication

// Profesyonel Ses Cihazı Renk Paleti (Antrasit, Siyah ve Gri)
private val ProAudioDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE0E0E0), // Açık gri/beyaz vurgular
    onPrimary = Color(0xFF121212),
    background = Color(0xFF121212), // Saf siyah arka plan
    onBackground = Color(0xFFEEEEEE),
    surface = Color(0xFF1E1E1E), // Kartlar için antrasit
    onSurface = Color(0xFFEEEEEE),
    surfaceVariant = Color(0xFF2C2C2C), // Daha açık gri alanlar
    onSurfaceVariant = Color(0xFFB0B0B0),
    error = Color(0xFFCF6679) // Zarif, pastel bir kırmızı (Acil Mute vb. için)
)

@Composable
fun App() {
    KoinApplication(application = {
        modules(appModule)
    }) {
        // Özel karanlık temamızı tüm uygulamaya giydiriyoruz
        MaterialTheme(
            colorScheme = ProAudioDarkColorScheme
        ) {
            var isConnected by remember { mutableStateOf(false) }

            if (!isConnected) {
                ConnectionScreen(
                    onNavigateToMain = { isConnected = true }
                )
            } else {
                MainScreen(
                    onDisconnect = { isConnected = false }
                )
            }
        }
    }
}
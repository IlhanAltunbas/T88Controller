package com.ilhanaltunbas.t88controller

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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

// Sade ve Modern Tipografi (Sans-Serif)
private val SimpleTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W400,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun App() {
    KoinApplication(application = {
        modules(appModule)
    }) {
        // Özel karanlık temamızı ve sade fontumuzu tüm uygulamaya giydiriyoruz
        MaterialTheme(
            colorScheme = ProAudioDarkColorScheme,
            typography = SimpleTypography
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
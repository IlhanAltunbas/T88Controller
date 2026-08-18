package com.ilhanaltunbas.t88controller.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilhanaltunbas.t88controller.domain.usecase.ObserveSyncStatusUseCase
import com.ilhanaltunbas.t88controller.domain.usecase.DisconnectDeviceUseCase
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// Minimalist ve Modern İkonlar
enum class BottomNavItem(
    val title: String, 
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    MIXER("Mikser", Icons.Filled.Tune, Icons.Outlined.Tune),
    MATRIX("Matris", Icons.Filled.GridView, Icons.Outlined.GridView),
    PRESETS("Kayıtlar", Icons.Filled.Layers, Icons.Outlined.Layers),
    SYSTEM("Sistem", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onDisconnect: () -> Unit,
    observeSyncStatusUseCase: ObserveSyncStatusUseCase = koinInject(),
    disconnectDeviceUseCase: DisconnectDeviceUseCase = koinInject()
) {
    var currentTab by remember { mutableStateOf(BottomNavItem.MIXER) }
    val isSyncing by observeSyncStatusUseCase().collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Başlık (İkon kaldırıldı)
                        Text(
                            text = currentTab.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                            color = Color.White
                        )
                        if (isSyncing) {
                            Spacer(modifier = Modifier.width(16.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                disconnectDeviceUseCase()
                                onDisconnect()
                            }
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A1A1A))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Bağlantıyı Kes",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0A0A),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            DynamicAdaptiveNavigationBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        },
        containerColor = Color(0xFF0A0A0A)
    ) { paddingValues ->
        Surface(
            modifier = Modifier.padding(paddingValues),
            color = Color(0xFF0A0A0A)
        ) {
            when (currentTab) {
                BottomNavItem.MIXER -> MixerScreen()
                BottomNavItem.MATRIX -> MatrixScreen()
                BottomNavItem.PRESETS -> ScenesScreen()
                BottomNavItem.SYSTEM -> SystemScreen()
            }
        }
    }
}

@Composable
fun DynamicAdaptiveNavigationBar(
    currentTab: BottomNavItem,
    onTabSelected: (BottomNavItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(Color(0xFF0F0F0F))
            .border(0.5.dp, Color(0xFF252525)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem.entries.forEach { item ->
                val isSelected = currentTab == item
                
                // Genişlik animasyonu (Seçili olan 2 birim, diğerleri 1 birim yer kaplar)
                val weight by animateFloatAsState(
                    targetValue = if (isSelected) 2f else 1f,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                    label = "weight"
                )

                Box(
                    modifier = Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .padding(vertical = 16.dp, horizontal = 4.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) Color(0xFF222222) else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(item) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title,
                            tint = if (isSelected) Color.White else Color(0xFF777777),
                            modifier = Modifier.size(22.dp)
                        )
                        
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally()
                        ) {
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 8.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

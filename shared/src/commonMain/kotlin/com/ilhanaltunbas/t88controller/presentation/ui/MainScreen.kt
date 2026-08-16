package com.ilhanaltunbas.t88controller.presentation.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

// Sekmelerimiz
enum class BottomNavItem(val title: String, val icon: ImageVector) {
    MIXER("Mikser", Icons.Default.List),
    MATRIX("Matris", Icons.Default.Build),
    PRESETS("Sahneler", Icons.Default.Settings),
    SYSTEM("Sistem", Icons.Default.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onDisconnect: () -> Unit
) {
    var currentTab by remember { mutableStateOf(BottomNavItem.MIXER) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentTab.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onDisconnect) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Bağlantıyı Kes",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                BottomNavItem.entries.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentTab == item,
                        onClick = { currentTab = item }
                    )
                }
            }
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.padding(paddingValues)) {
            when (currentTab) {
                BottomNavItem.MIXER -> MixerScreen()
                BottomNavItem.MATRIX -> MatrixScreen()
                BottomNavItem.PRESETS -> ScenesScreen()
                BottomNavItem.SYSTEM -> SystemScreen()
            }
        }
    }
}
package com.ilhanaltunbas.t88controller.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilhanaltunbas.t88controller.domain.model.ChannelState
import com.ilhanaltunbas.t88controller.presentation.viewmodel.MixerViewModel
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixerScreen(
    viewModel: MixerViewModel = koinInject()
) {
    val inputChannels by viewModel.inputChannels.collectAsState()
    val outputChannels by viewModel.outputChannels.collectAsState()

    val connectionStatus by viewModel.connectionStatus.collectAsState()

    var selectedChannelId by remember { mutableStateOf<Int?>(null) }
    var isSelectedInput by remember { mutableStateOf(true) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val activeSelectedChannel = remember(selectedChannelId, isSelectedInput, inputChannels, outputChannels) {
        if (selectedChannelId == null) null
        else if (isSelectedInput) inputChannels.find { it.id == selectedChannelId }
        else outputChannels.find { it.id == selectedChannelId }
    }

    if (activeSelectedChannel != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedChannelId = null },
            sheetState = sheetState,
            containerColor = Color(0xFF1A1A1A),
            scrimColor = Color.Black.copy(alpha = 0.8f)
        ) {
            ChannelDetailsExpandedPanel(
                channelState = activeSelectedChannel,
                onLineMicToggle = {
                    viewModel.toggleLineMicMode(activeSelectedChannel.id)
                },
                onAfcLevelChange = { newLevel ->
                    viewModel.changeAfcLevel(activeSelectedChannel.id, newLevel)
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(top = 16.dp, bottom = 16.dp)
    ) {

        if (connectionStatus.name == "DISCONNECTED") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFB71C1C))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CİHAZ BAĞLANTISI KOPTU! Lütfen ağı kontrol edin.",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Text(
            text = "STUDIO MIXER CONSOLE",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = Color(0xFFDDDDDD),
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(inputChannels) { state ->
                ChannelStrip(
                    state = state,
                    onVolumeChange = { vol -> viewModel.changeVolume(state.id, state.isInput, vol) },
                    onVolumeStep = { isInc -> viewModel.stepVolume(state.id, state.isInput, isInc) },
                    onMuteToggle = { viewModel.toggleMute(state.id, state.isInput) },
                    onPhantomToggle = { viewModel.togglePhantomPower(state.id) },
                    onOpenDetails = {
                        selectedChannelId = state.id
                        isSelectedInput = state.isInput
                    }
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .padding(vertical = 16.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.verticalGradient(listOf(Color(0xFF222222), Color(0xFF111111))))
                )
            }

            items(outputChannels) { state ->
                ChannelStrip(
                    state = state,
                    onVolumeChange = { vol -> viewModel.changeVolume(state.id, state.isInput, vol) },
                    onVolumeStep = { isInc -> viewModel.stepVolume(state.id, state.isInput, isInc) },
                    onMuteToggle = { viewModel.toggleMute(state.id, state.isInput) },
                    onPhantomToggle = { },
                    onOpenDetails = {
                        selectedChannelId = state.id
                        isSelectedInput = state.isInput
                    }
                )
            }
        }
    }
}

@Composable
fun ChannelStrip(
    state: ChannelState,
    onVolumeChange: (Float) -> Unit,
    onVolumeStep: (Boolean) -> Unit,
    onMuteToggle: () -> Unit,
    onPhantomToggle: () -> Unit,
    onOpenDetails: () -> Unit
) {
    var localVolume by remember { mutableStateOf(state.volume) }
    var isVolumeDragging by remember { mutableStateOf(false) }

    LaunchedEffect(state.volume) {
        if (!isVolumeDragging) localVolume = state.volume
    }

    Column(
        modifier = Modifier
            .width(110.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A))
            .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(8.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF151515))
                .padding(12.dp)
                .height(140.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF222222))
                    .clickable { onOpenDetails() }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = state.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.Settings, contentDescription = "Ayarlar", tint = Color.Gray, modifier = Modifier.size(14.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isInput) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (state.isPhantomOn) Color(0xFFD32F2F) else Color(0xFF222222))
                        .clickable { onPhantomToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+48V", color = if (state.isPhantomOn) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF0A0A0A)))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // HASSAS AYAR BUTONLARI (+/- 1dB)
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = { onVolumeStep(true) },
                        modifier = Modifier.size(32.dp).background(Color(0xFF222222), RoundedCornerShape(4.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    
                    IconButton(
                        onClick = { onVolumeStep(false) },
                        modifier = Modifier.size(32.dp).background(Color(0xFF222222), RoundedCornerShape(4.dp))
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                ProStudioFader(
                    value = localVolume,
                    onValueChange = { localVolume = it },
                    onDragStart = { isVolumeDragging = true },
                    onValueChangeFinished = {
                        isVolumeDragging = false
                        onVolumeChange(localVolume)
                    },
                    modifier = Modifier.weight(1f),
                    hasVuMeter = true,
                    thumbColor = Color(0xFFDEDEDE)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (state.isMuted) Color(0xFFB71C1C) else Color(0xFF222222))
                    .border(if (state.isMuted) 0.dp else 1.dp, Color(0xFF333333), RoundedCornerShape(6.dp))
                    .clickable { onMuteToggle() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MUTE",
                    color = if (state.isMuted) Color.White else Color(0xFF777777),
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun ChannelDetailsExpandedPanel(
    channelState: ChannelState,
    onLineMicToggle: () -> Unit,
    onAfcLevelChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${channelState.name} DETAYLARI",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (channelState.isInput) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("GİRİŞ TİPİ (INPUT MODE)", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF111111))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                            .background(if (channelState.isLineMode) Color(0xFF388E3C) else Color.Transparent)
                            .clickable { if (!channelState.isLineMode) onLineMicToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("LINE", color = if (channelState.isLineMode) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                            .background(if (!channelState.isLineMode) Color(0xFF1976D2) else Color.Transparent)
                            .clickable { if (channelState.isLineMode) onLineMicToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("MIC", color = if (!channelState.isLineMode) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("AFC (YANKI ÖNLEYİCİ) SEVİYESİ", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (channelState.afcLevel == 0) "KAPALI" else "SEVİYE ${channelState.afcLevel}",
                        color = if (channelState.afcLevel > 0) Color(0xFFFBC02D) else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = channelState.afcLevel.toFloat(),
                    onValueChange = { onAfcLevelChange(it.toInt()) },
                    valueRange = 0f..8f,
                    steps = 7,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFFFBC02D),
                        inactiveTrackColor = Color(0xFF222222)
                    )
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("Çıkış (Output) kanalları için gelişmiş ayar bulunmamaktadır.", color = Color(0xFF444444), fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ProStudioFader(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit = {},
    onDragStart: () -> Unit = {},
    modifier: Modifier = Modifier,
    hasVuMeter: Boolean = false,
    thumbColor: Color = Color.LightGray
) {
    val density = LocalDensity.current
    val trackWidth = 14.dp
    val thumbWidth = 50.dp
    val thumbHeight = 36.dp

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val thumbHeightPx = with(density) { thumbHeight.toPx() }
        val maxDragY = maxHeightPx - thumbHeightPx
        val currentYOffset = (1f - (value / 100f)) * maxDragY

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasVuMeter) {
                VuMeter(value = value, modifier = Modifier.width(6.dp).fillMaxHeight().padding(vertical = thumbHeight / 2))
                Spacer(modifier = Modifier.width(12.dp))
            }

            Box(
                modifier = Modifier
                    .width(thumbWidth)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            onDragStart()

                            var newValueY = (down.position.y - (thumbHeightPx / 2)).coerceIn(0f, maxDragY)
                            onValueChange(((1f - (newValueY / maxDragY)) * 100f).coerceIn(0f, 100f))
                            down.consume()

                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { change ->
                                    if (change.pressed) {
                                        newValueY = (change.position.y - (thumbHeightPx / 2)).coerceIn(0f, maxDragY)
                                        onValueChange(((1f - (newValueY / maxDragY)) * 100f).coerceIn(0f, 100f))
                                        change.consume()
                                    }
                                }
                            } while (event.changes.any { it.pressed })

                            onValueChangeFinished()
                        }
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .width(trackWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF080808), Color(0xFF1E1E1E), Color(0xFF080808))))
                        .border(1.dp, Color(0xFF000000), RoundedCornerShape(50))
                )

                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, currentYOffset.roundToInt()) }
                        .width(thumbWidth)
                        .height(thumbHeight)
                        .shadow(6.dp, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.verticalGradient(listOf(thumbColor.copy(alpha = 0.8f), thumbColor, thumbColor.copy(alpha = 0.6f))))
                        .border(1.dp, Color.Black, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.fillMaxWidth(0.8f).height(2.dp).background(Color.White))
                    Text(
                        text = value.toInt().toString(),
                        color = Color.Black.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.offset(y = (-10).dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VuMeter(value: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val segmentCount = 20
        val segmentHeight = size.height / segmentCount
        val gap = 2.dp.toPx()
        val filledSegments = (value / 100f * segmentCount).roundToInt()

        for (i in 0 until segmentCount) {
            val isFilled = (segmentCount - 1 - i) < filledSegments
            val color = when {
                i < 3 -> Color(0xFFD32F2F)
                i < 7 -> Color(0xFFFBC02D)
                else -> Color(0xFF388E3C)
            }
            drawRoundRect(
                color = if (isFilled) color else Color(0xFF151515),
                topLeft = Offset(0f, i * segmentHeight),
                size = Size(size.width, segmentHeight - gap),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }
}

private fun border(width: androidx.compose.ui.unit.Dp, color: Color, shape: androidx.compose.ui.graphics.Shape) = androidx.compose.foundation.BorderStroke(width, color)
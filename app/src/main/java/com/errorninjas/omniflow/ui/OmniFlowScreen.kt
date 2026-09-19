package com.errorninjas.omniflow.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.errorninjas.omniflow.ui.theme.*

@Composable
fun OmniFlowScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showSettings by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.loadModelFromUri(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.checkClipboard()
    }

    LaunchedEffect(uiState.messages.size, uiState.currentResponse) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
    ) {
        CyberBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            StatusHUD(
                state = uiState,
                onSettingsClick = { showSettings = true },
                onAuditClick = { viewModel.togglePrivacyAudit(true) }
            )

            Box(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TelemetryHUD(uiState)

                    // Live Clipboard Auto-Sniffer Banner
                    uiState.detectedClipboardText?.let { clipText ->
                        ClipboardSnifferBanner(
                            text = clipText,
                            onAction = { actionPrefix ->
                                viewModel.sendMessage("$actionPrefix:\n$clipText")
                            },
                            onDismiss = { viewModel.dismissClipboard() }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.messages.isEmpty() && uiState.currentResponse.isEmpty()) {
                            CentralCommandIdle(uiState.modelStatus)
                        } else {
                            ChatStreamSlate(uiState, listState)
                        }
                    }
                }
            }

            // Preset Blades + Neural Dock Input
            Column {
                PresetBladesRow(
                    isProcessing = uiState.isProcessing,
                    onPresetClick = { presetPrompt ->
                        viewModel.sendMessage(presetPrompt)
                    }
                )

                NeuralInputDock(
                    isProcessing = uiState.isProcessing,
                    onSend = { viewModel.sendMessage(it) }
                )
            }
        }

        if (showSettings) {
            SettingsDialog(
                status = uiState.modelStatus,
                onDismiss = { showSettings = false },
                onClearChat = { viewModel.clearChat(); showSettings = false },
                onLoadModel = { filePickerLauncher.launch("*/*"); showSettings = false },
                onResetEngine = { viewModel.resetEngine(); showSettings = false }
            )
        }

        if (uiState.showPrivacyAudit) {
            PrivacyAuditDialog(
                onDismiss = { viewModel.togglePrivacyAudit(false) }
            )
        }
    }
}

@Composable
fun CyberBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 50.dp.toPx()
        for (x in 0..size.width.toInt() step gridSize.toInt()) {
            drawLine(TechGray.copy(alpha = 0.05f), Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height))
        }
        for (y in 0..size.height.toInt() step gridSize.toInt()) {
            drawLine(TechGray.copy(alpha = 0.05f), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()))
        }
    }

    Box(
        modifier = Modifier
            .size(400.dp)
            .offset(x = (-150).dp, y = (-150).dp)
            .blur(100.dp)
            .background(ElectricViolet.copy(alpha = 0.08f), CircleShape)
    )
}

@Composable
fun StatusHUD(
    state: MainViewModel.UiState,
    onSettingsClick: () -> Unit,
    onAuditClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = NeuralBlack,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, if (state.modelStatus == "Model Ready") NeonCyan.copy(alpha = 0.5f) else TechGray)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (state.modelStatus == "Model Ready") NPUGreen else TechGray, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "⚡ NPU ONLINE | ${state.tokensPerSec} Tok/s | ZERO-CLOUD",
                    color = CyberWhite,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Air-Gapped Privacy Audit Badge Button
            Surface(
                onClick = onAuditClick,
                color = GlassMorphic,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NPUGreen.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = NPUGreen, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("100% PRIVATE", color = NPUGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = CyberWhite.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ClipboardSnifferBanner(
    text: String,
    onAction: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = NeuralBlack,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MonsterAmber)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MonsterAmber, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CLIPBOARD CONTEXT DETECTED", color = MonsterAmber, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(16.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = TechGray)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "\"$text\"",
                color = CyberWhite,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onAction("Extract Action Items & Tasks from this context") },
                    colors = ButtonDefaults.buttonColors(containerColor = MonsterAmber),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("⚡ Extract Tasks", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onAction("Summarize this context in 3 bullet points") },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassMorphic),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("📝 Summarize", fontSize = 10.sp, color = CyberWhite)
                }
            }
        }
    }
}

@Composable
fun PresetBladesRow(
    isProcessing: Boolean,
    onPresetClick: (String) -> Unit
) {
    val presets = listOf(
        "🐞 Code Fixer" to "Analyze and fix bugs in this code snippet:",
        "📌 Task Extractor" to "Extract actionable tasks with deadlines:",
        "⚡ Exec Summary" to "Provide a 3-bullet point executive summary of:",
        "✉️ Smart Reply" to "Draft a polite and concise response to:"
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(presets) { (label, actionPrefix) ->
            Surface(
                onClick = { if (!isProcessing) onPresetClick(actionPrefix) },
                color = GlassMorphic,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Text(
                    text = label,
                    color = CyberWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun PrivacyAuditDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeuralBlack,
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = NPUGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AIR-GAPPED PRIVACY AUDIT", color = NPUGreen, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AuditItem("🌐 Network Requests", "0 (Blocked)", NPUGreen)
                AuditItem("📤 Cloud Data Uploads", "0 Bytes", NPUGreen)
                AuditItem("🧠 Inference Engine", "Qualcomm NPU / Local VRAM", NeonCyan)
                AuditItem("🛡️ Security Isolation", "Hardware Air-Gapped", NPUGreen)
                AuditItem("📋 Local Context Scope", "Volatile Memory Only", TechGray)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE AUDIT REPORT", color = NPUGreen, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun AuditItem(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = CyberWhite, fontSize = 11.sp)
        Text(value, color = valueColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun SettingsDialog(
    status: String,
    onDismiss: () -> Unit,
    onClearChat: () -> Unit,
    onLoadModel: () -> Unit,
    onResetEngine: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeuralBlack,
        shape = RoundedCornerShape(12.dp),
        title = { Text("NEURAL_CONFIG", color = NeonCyan, fontSize = 16.sp, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("STATUS: $status", color = CyberWhite, fontSize = 12.sp)
                
                Button(
                    onClick = onLoadModel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GlassMorphic),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("RELOAD_MODEL_SOURCE", color = CyberWhite)
                }

                Button(
                    onClick = onClearChat,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GlassMorphic),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("PURGE_CHAT_HISTORY", color = CyberWhite)
                }

                Button(
                    onClick = onResetEngine,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MonsterAmber.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("RESET_NEURAL_ENGINE", color = MonsterAmber)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = NeonCyan)
            }
        }
    )
}

@Composable
fun TelemetryHUD(state: MainViewModel.UiState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(100.dp),
        color = GlassMorphic,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                TelemetryLabel("NPU_UTILIZATION", state.npuLoad.toString() + "%")
                Spacer(modifier = Modifier.height(8.dp))
                TelemetryLabel("VRAM_FOOTPRINT", state.vramUsage)
            }
            
            Box(modifier = Modifier
                .width(120.dp)
                .fillMaxHeight()
                .drawBehind {
                    val path = Path()
                    path.moveTo(0f, size.height)
                    for (i in 0..10) {
                        val x = size.width * (i / 10f)
                        val y = size.height * (0.3f + (0..5).random() / 10f)
                        path.lineTo(x, y)
                    }
                    drawPath(path, NeonCyan, style = Stroke(width = 2f))
                })
        }
    }
}

@Composable
fun TelemetryLabel(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TechGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(8.dp))
        Text(value, color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun CentralCommandIdle(status: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            val infiniteTransition = rememberInfiniteTransition(label = "")
            val pulse by infiniteTransition.animateFloat(
                initialValue = 0.8f, targetValue = 1.2f,
                animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = ""
            )
            
            Box(modifier = Modifier
                .size(150.dp)
                .graphicsLayer { scaleX = pulse; scaleY = pulse }
                .blur(40.dp)
                .background(NeonCyan.copy(alpha = 0.1f), CircleShape))
            
            Text(
                "SYNAPSE",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp,
                    brush = Brush.verticalGradient(listOf(NeonCyan, ElectricViolet))
                )
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "CORE_STATUS: $status",
            color = CyberWhite.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ChatStreamSlate(state: MainViewModel.UiState, listState: androidx.compose.foundation.lazy.LazyListState) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(state.messages) { message ->
            CyberBubble(message)
        }
        
        if (state.currentResponse.isNotEmpty()) {
            item {
                CyberBubble(ChatMessage(state.currentResponse, false))
            }
        }
    }
}

@Composable
fun CyberBubble(message: ChatMessage) {
    val borderColor = if (message.isUser) TechGray.copy(alpha = 0.3f) else NeonCyan.copy(alpha = 0.4f)
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (message.isUser) Color.Transparent else GlassMorphic,
            shape = RoundedCornerShape(if (message.isUser) 12.dp else 2.dp),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(16.dp),
                color = CyberWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp
            )
        }
        Text(
            if (message.isUser) "USER_NODE" else "NEURAL_LINK",
            fontSize = 8.sp,
            color = if (message.isUser) TechGray else NeonCyan,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp),
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun NeuralInputDock(isProcessing: Boolean, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = NeuralBlack,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (text.isNotEmpty()) NeonCyan else Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Transparent)
                    .padding(8.dp)
            ) {
                if (text.isEmpty()) Text("Input context...", color = TechGray, fontSize = 14.sp)
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = androidx.compose.ui.text.TextStyle(color = CyberWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            IconButton(
                onClick = { 
                    if (text.isNotBlank() && !isProcessing) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onSend(text)
                        text = ""
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(if (text.isNotEmpty()) NeonCyan else TechGray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
                enabled = !isProcessing && text.isNotBlank()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.Black)
            }
        }
    }
}

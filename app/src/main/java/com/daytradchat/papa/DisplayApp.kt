// /app/src/main/java/com/daytradchat/papa/ui/DisplayApp.kt
// ver 1.00-00
package com.daytradchat.papa.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daytradchat.papa.MainUiState
import com.daytradchat.papa.MainViewModel
import com.daytradchat.papa.data.ChatMessage
import com.daytradchat.papa.network.ConnectionState

private enum class BottomTab { Dashboard, History, Logs, Settings }

@Composable
fun DisplayApp(viewModel: MainViewModel) {
    MaterialTheme {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        var selectedTab by rememberSaveable { mutableStateOf(BottomTab.Dashboard) }
        var settingsVisible by rememberSaveable { mutableStateOf(false) }

        Scaffold(
            topBar = {
                AppTopBar(
                    uiState = uiState,
                    onRetry = viewModel::retryNow,
                    onOpenSettings = { settingsVisible = true }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == BottomTab.Dashboard,
                        onClick = { selectedTab = BottomTab.Dashboard },
                        icon = { Text("1") },
                        label = { Text("監視") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == BottomTab.History,
                        onClick = { selectedTab = BottomTab.History },
                        icon = { Text("2") },
                        label = { Text("履歴") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == BottomTab.Logs,
                        onClick = { selectedTab = BottomTab.Logs },
                        icon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                        label = { Text("LOG") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == BottomTab.Settings,
                        onClick = { settingsVisible = true },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("設定") }
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFF0C1118))
            ) {
                when (selectedTab) {
                    BottomTab.Dashboard -> DashboardScreen(uiState, viewModel)
                    BottomTab.History -> HistoryScreen(uiState, viewModel)
                    BottomTab.Logs -> LogScreen(uiState)
                    BottomTab.Settings -> DashboardScreen(uiState, viewModel)
                }
            }
        }

        if (settingsVisible) {
            SettingsDialog(
                currentHost = uiState.host,
                port = uiState.port,
                onDismiss = { settingsVisible = false },
                onSave = {
                    viewModel.saveHost(it)
                    settingsVisible = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    uiState: MainUiState,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text("DayTradeChat", fontSize = 15.sp, color = Color.White)
                Text(
                    text = "${uiState.connectionState.name}  ${uiState.host}:${uiState.port}",
                    fontSize = 11.sp,
                    color = connectionColor(uiState.connectionState)
                )
            }
        },
        actions = {
            IconButton(onClick = onRetry) {
                Icon(Icons.Filled.Refresh, contentDescription = "retry", tint = Color.White)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "settings", tint = Color.White)
            }
        }
    )
}

@Composable
private fun DashboardScreen(uiState: MainUiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusPanel(uiState)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            uiState.recentMessages.take(5).forEach { message ->
                MessageCard(
                    message = message,
                    compact = false,
                    onCopyCode = { viewModel.copyCode(context, message) }
                )
            }
        }
    }
}

@Composable
private fun HistoryScreen(uiState: MainUiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    var signalOnly by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !signalOnly,
                onClick = { signalOnly = false },
                label = { Text("全件") }
            )
            FilterChip(
                selected = signalOnly,
                onClick = { signalOnly = true },
                label = { Text("signalのみ") }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val target = if (signalOnly) uiState.allMessages.filter { it.type == "signal" } else uiState.allMessages
            items(target, key = { it.id }) { message ->
                MessageCard(
                    message = message,
                    compact = true,
                    onCopyCode = { viewModel.copyCode(context, message) }
                )
            }
        }
    }
}

@Composable
private fun LogScreen(uiState: MainUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(uiState.logs, key = { it.id }) { log ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16202B))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("${log.level}  ${log.createdAt}", color = Color(0xFF9CCBFF), fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(log.message, color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun StatusPanel(uiState: MainUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2A))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text("接続状態", color = Color(0xFF8FB5FF), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(uiState.connectionState.name, color = connectionColor(uiState.connectionState), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("HOST : ${uiState.host}", color = Color.White, fontSize = 11.sp)
            Text("PORT : ${uiState.port}", color = Color.White, fontSize = 11.sp)
            Text("LAST PONG : ${uiState.lastPongTime ?: "-"}", color = Color.White, fontSize = 11.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageCard(
    message: ChatMessage,
    compact: Boolean,
    onCopyCode: () -> Unit
) {
    val isSignal = message.type == "signal"
    val cardColor = when (message.signalType) {
        "BUY" -> Color(0xFF193024)
        "SELL" -> Color(0xFF341D21)
        else -> if (isSignal) Color(0xFF1F2533) else Color(0xFF111A24)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onCopyCode,
                onLongClick = onCopyCode
            ),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(connectionColorFromType(message.type), RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = message.title ?: message.type,
                    color = Color.White,
                    fontSize = if (compact) 12.sp else 13.sp,
                    fontWeight = if (isSignal) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.body ?: message.reasonShort ?: message.rawJson,
                color = Color(0xFFE2E8F0),
                fontSize = if (compact) 10.sp else 11.sp,
                maxLines = if (compact) 3 else 4,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallMeta(label = "code", value = message.code ?: "-")
                SmallMeta(label = "score", value = message.signalScore?.toString() ?: "-")
                SmallMeta(label = "price", value = message.price?.toString() ?: "-")
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message.sentAt ?: message.serverTime ?: message.createdAt.toString(),
                color = Color(0xFF8AA0B8),
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun SmallMeta(label: String, value: String) {
    Text("$label:$value", color = Color(0xFF9FC6FF), fontSize = 9.sp)
}

@Composable
private fun SettingsDialog(
    currentHost: String,
    port: Int,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var host by rememberSaveable { mutableStateOf(currentHost) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("接続設定") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("サーバホスト") },
                    singleLine = true
                )
                Text("ポートは固定: $port", fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(host) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

@Composable
private fun connectionColor(state: ConnectionState): Color {
    return when (state) {
        ConnectionState.CONNECTING -> Color(0xFFFFC857)
        ConnectionState.CONNECTED -> Color(0xFF4ADE80)
        ConnectionState.DISCONNECTED -> Color(0xFFF87171)
        ConnectionState.RECONNECTING -> Color(0xFF60A5FA)
    }
}

@Composable
private fun connectionColorFromType(type: String): Color {
    return when (type) {
        "signal" -> Color(0xFFFFC857)
        "server_hello" -> Color(0xFF60A5FA)
        "pong" -> Color(0xFF34D399)
        "ack" -> Color(0xFFA78BFA)
        else -> Color(0xFF94A3B8)
    }
}

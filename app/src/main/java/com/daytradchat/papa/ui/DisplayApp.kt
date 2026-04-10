// /app/src/main/java/com/daytradchat/papa/ui/DisplayApp.kt
// ver 1.00-05
package com.daytradchat.papa.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
                Text("DayTradeChat", fontSize = 13.sp, color = Color.White)
                Text(
                    text = "${uiState.connectionState.name}  ${uiState.host}:${uiState.port}",
                    fontSize = 10.sp,
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
    val dashboardMessages = buildDashboardMessages(uiState.allMessages)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) { rowIndex ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) { colIndex ->
                    val index = rowIndex * 3 + colIndex
                    val message = dashboardMessages.getOrNull(index)
                    Box(modifier = Modifier.weight(1f)) {
                        if (message == null) {
                            EmptySlotCard(index = index)
                        } else {
                            DashboardMessageCard(
                                modifier = Modifier.fillMaxSize(),
                                message = message,
                                fixedSlot = index < 6,
                                onCopyCode = { viewModel.copyCode(context, message) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySlotCard(index: Int) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (index < 6) "固定" else "入替",
                color = Color(0xFF64748B),
                fontSize = 9.sp
            )
            Text(
                text = "待機中",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HistoryScreen(uiState: MainUiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    var signalOnly by rememberSaveable { mutableStateOf(true) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = !signalOnly,
                onClick = { signalOnly = false },
                label = { Text("全件", fontSize = 11.sp) }
            )
            FilterChip(
                selected = signalOnly,
                onClick = { signalOnly = true },
                label = { Text("signalのみ", fontSize = 11.sp) }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val target = if (signalOnly) uiState.allMessages.filter { it.type == "signal" } else uiState.allMessages
            items(target, key = { it.id }) { message ->
                HistoryMessageRow(
                    message = message,
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
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(uiState.logs, key = { it.id }) { log ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF16202B))) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Text("${log.level}  ${log.createdAt}", color = Color(0xFF9CCBFF), fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(log.message, color = Color.White, fontSize = 10.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DashboardMessageCard(
    modifier: Modifier = Modifier,
    message: ChatMessage,
    fixedSlot: Boolean,
    onCopyCode: () -> Unit
) {
    Card(
        modifier = modifier.combinedClickable(onClick = onCopyCode, onLongClick = onCopyCode),
        colors = CardDefaults.cardColors(containerColor = signalCardColor(message)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(signalDotColor(message), RoundedCornerShape(50))
                )
                Text(
                    text = if (fixedSlot) "固定" else "入替",
                    color = Color(0xFFD1D5DB),
                    fontSize = 8.sp
                )
            }
            Text(
                text = message.title ?: (message.code ?: "-") ,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = message.body ?: message.reasonShort ?: "-",
                color = Color(0xFFE5E7EB),
                fontSize = 10.sp,
                lineHeight = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildMetaLine(message),
                color = Color(0xFFBFDBFE),
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = message.sentAt ?: message.serverTime ?: "-",
                color = Color(0xFFCBD5E1),
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryMessageRow(message: ChatMessage, onCopyCode: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onCopyCode, onLongClick = onCopyCode),
        colors = CardDefaults.cardColors(containerColor = signalCardColor(message)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = message.code ?: "-",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message.signalType ?: message.type,
                    color = signalDotColor(message),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = message.sentAt ?: message.serverTime ?: "-",
                    color = Color(0xFFCBD5E1),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = (message.body ?: message.reasonShort ?: message.rawJson).replace("\n", " "),
                color = Color(0xFFE5E7EB),
                fontSize = 10.sp,
                lineHeight = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = buildMetaLine(message),
                color = Color(0xFFBFDBFE),
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun buildDashboardMessages(allMessages: List<ChatMessage>): List<ChatMessage> {
    val signalMessages = allMessages.filter { it.type == "signal" }
    val fixed = mutableListOf<ChatMessage>()
    val fixedCodes = linkedSetOf<String>()

    for (message in signalMessages) {
        val code = message.code ?: continue
        if (fixedCodes.add(code)) {
            fixed += message
            if (fixed.size >= 6) break
        }
    }

    val rotating = signalMessages.filter { message ->
        val code = message.code
        code == null || code !in fixedCodes
    }.take(3)

    return (fixed + rotating).take(9)
}

private fun buildMetaLine(message: ChatMessage): String {
    val code = message.code ?: "-"
    val score = message.signalScore?.toString() ?: "-"
    val price = message.price?.toString() ?: "-"
    return "code:$code  score:$score  price:$price"
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
                Text("ポート固定: $port", fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(host) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("閉じる") } }
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

private fun signalCardColor(message: ChatMessage): Color {
    return when (message.signalType) {
        "BUY" -> Color(0xFF3B1218)
        "SELL" -> Color(0xFF0F2E1D)
        else -> if (message.type == "signal") Color(0xFF1F2533) else Color(0xFF111A24)
    }
}

private fun signalDotColor(message: ChatMessage): Color {
    return when (message.signalType) {
        "BUY" -> Color(0xFFFF8A80)
        "SELL" -> Color(0xFF86EFAC)
        else -> Color(0xFF94A3B8)
    }
}

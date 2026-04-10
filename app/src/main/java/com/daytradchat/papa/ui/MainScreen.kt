//app/src/main/java/com/daytradchat/papa/ui/MainScreen.kt
//ver 1.10-10

package com.daytradchat.papa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daytradchat.papa.MainViewModel
import com.daytradchat.papa.SignalUiItem

@Composable
fun MainScreen(vm: MainViewModel) {
    val state by vm.uiState.collectAsState()
    var tab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = {}, label = { Text("監視") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = {}, label = { Text("履歴") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = {}, label = { Text("LOG") })
                NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = {}, label = { Text("設定") })
            }
        }
    ) { padding ->
        when (tab) {
            0 -> MonitorTab(
                status = state.statusText,
                host = state.host,
                port = state.port,
                signals = state.signalList,
                modifier = Modifier.padding(padding)
            )
            1 -> HistoryTab(state.signalList, Modifier.padding(padding))
            2 -> LogTab(state.logs, Modifier.padding(padding))
            3 -> SettingsTab(
                currentHost = state.host,
                onSave = {
                    vm.updateHost(it)
                    vm.restartClient()
                },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun MonitorTab(
    status: String,
    host: String,
    port: Int,
    signals: List<SignalUiItem>,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (status == "CONNECTED") Color(0xFF7A1010) else Color(0xFF104010),
                    RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        ) {
            Text(
                text = "$status  $host:$port",
                color = Color.White,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        for (row in 0 until 3) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0 until 3) {
                    val idx = row * 3 + col
                    val item = signals.getOrNull(idx)
                    SignalCell(
                        item = item,
                        modifier = Modifier
                            .weight(1f)
                            .padding(3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SignalCell(item: SignalUiItem?, modifier: Modifier) {
    val bg = when (item?.signalType?.uppercase()) {
        "BUY" -> Color(0xFF3A1010)
        "SELL" -> Color(0xFF103A10)
        else -> Color(0xFF1A1A1A)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        if (item == null) {
            Text(text = "--", color = Color.Gray, fontSize = 14.sp)
        } else {
            val boardText = if (item.boardOver >= item.boardUnder) "買↑" else "売↓"
            val trendText = when {
                item.changeRate <= -1.0 -> "下強"
                item.changeRate < 0.0 -> "下弱"
                item.changeRate >= 1.0 -> "上強"
                item.changeRate > 0.0 -> "上弱"
                else -> "--"
            }
            val diffText = if (item.vwap != 0.0) {
                val diff = (item.price - item.vwap).toInt()
                "差:$diff"
            } else {
                "差:--"
            }

            Column {
                Text("${item.code} ${item.signalType}", color = Color.White, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("$boardText  $trendText", color = Color(0xFFE0E0E0), fontSize = 11.sp)
                Text("点:${item.score}  値:${item.price.toInt()}  $diffText", color = Color.White, fontSize = 11.sp)
                if (item.reasonShort.isNotBlank()) {
                    Text(item.reasonShort, color = Color.LightGray, fontSize = 10.sp)
                }
                if (item.sentAt.isNotBlank()) {
                    Text(item.sentAt.takeLast(8), color = Color.Gray, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(signals: List<SignalUiItem>, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp)
    ) {
        signals.sortedBy { it.code }.forEach { item ->
            Text(
                text = "${item.code} ${item.signalType} 値:${item.price.toInt()} 点:${item.score}",
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun LogTab(logs: List<String>, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp)
    ) {
        logs.takeLast(50).reversed().forEach { line ->
            Text(
                text = line,
                color = Color.LightGray,
                fontSize = 10.sp,
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun SettingsTab(
    currentHost: String,
    onSave: (String) -> Unit,
    modifier: Modifier
) {
    var host by remember(currentHost) { mutableStateOf(currentHost) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(12.dp)
    ) {
        Text("ホスト", color = Color.White, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { onSave(host) }) {
                Text("再接続")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("PORT 5001 固定", color = Color.LightGray, fontSize = 12.sp)
        }
    }
}

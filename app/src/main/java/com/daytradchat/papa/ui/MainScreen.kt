//app/src/main/java/com/daytradchat/papa/ui/MainScreen.kt
//ver 1.10-13

package com.daytradchat.papa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daytradchat.papa.MainViewModel

@Composable
fun MainScreen(vm: MainViewModel) {
    val state by vm.ui.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .padding(8.dp)
    ) {
        HeaderPanel()

        Spacer(modifier = Modifier.height(8.dp))

        if (state.signals.isEmpty()) {
            EmptyPanel()
        } else {
            val rows = state.signals.take(9).chunked(3)
            rows.forEach { row ->
                Row(modifier = Modifier.weight(1f, fill = true)) {
                    row.forEach { item ->
                        SignalCell(
                            code = item.code,
                            signalType = item.type,
                            price = item.price,
                            score = item.score,
                            time = item.time,
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                        )
                    }

                    repeat(3 - row.size) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                                .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderPanel() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF202020), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(
            text = "DayTradeChat  v1.10-13",
            color = Color.White,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "SERVER  osaka-cosplayers.net:5001",
            color = Color(0xFF00FF99),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun EmptyPanel() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "起動OK",
                color = Color.White,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "データ受信待機中",
                color = Color(0xFF00FF99),
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "通信未接続または未受信",
                color = Color.LightGray,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun SignalCell(
    code: String,
    signalType: String,
    price: Double,
    score: Int,
    time: String,
    modifier: Modifier = Modifier
) {
    val bgColor = when (signalType.uppercase()) {
        "BUY" -> Color(0xFF3A1010)
        "SELL" -> Color(0xFF103A10)
        else -> Color(0xFF303030)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = "$code $signalType",
                color = Color.White,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "値:${price.toInt()}",
                color = Color(0xFFD0D0D0),
                fontSize = 11.sp
            )

            Text(
                text = "点:$score",
                color = Color(0xFFD0D0D0),
                fontSize = 11.sp
            )

            if (time.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = time,
                    color = Color(0xFFAAAAAA),
                    fontSize = 10.sp
                )
            }
        }
    }
}
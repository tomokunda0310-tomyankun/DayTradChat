//app/src/main/java/com/daytradchat/papa/ui/TradeViewModel.kt
//ver 2.13-06

package com.daytradchat.papa.ui

import androidx.lifecycle.ViewModel
import com.daytradchat.papa.model.SignalCardUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

class TradeViewModel : ViewModel() {

    private val _items = MutableStateFlow<List<SignalCardUiModel>>(emptyList())
    val items: StateFlow<List<SignalCardUiModel>> = _items

    private val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun updateDummy() {

        val now = sdf.format(Date())

        val list = listOf(
            create("NIKKEI225", "日経平均", "INDEX", 0, 38500.0, -1.2, "地合い"),
            create("7203", "トヨタ", "SELL", -6, 2875.0, -1.8, "売り強"),
            create("8306", "三菱UFJ", "SKIP", 0, 2852.5, 0.0, "様子見"),
            create("7211", "三菱自動車", "BUY", 4, 314.3, 1.2, "上昇"),
            create("3436", "SUMCO", "BUY", 4, 2202.0, 0.8, "半導体"),
            create("7201", "日産", "SKIP", 0, 349.9, -0.3, "弱い"),
            create("8136", "サンリオ", "BUY", 4, 993.2, 1.1, "強い"),
            create("3350", "メタプラ", "SKIP", -1, 329.0, -0.5, "重い"),
            create("3103", "ユニチカ", "BUY", 4, 2161.0, 2.0, "爆上げ")
        )

        _items.value = list.mapIndexed { index, it ->
            it.copy(slotId = index.toString(), updatedAt = now)
        }
    }

    private fun create(
        code: String,
        name: String,
        type: String,
        score: Int,
        price: Double,
        rate: Double,
        reason: String
    ): SignalCardUiModel {
        return SignalCardUiModel(
            slotId = "",
            code = code,
            name = name,
            signalType = type,
            score = score,
            price = price,
            changeRate = rate,
            reasonShort = reason,
            updatedAt = ""
        )
    }
}
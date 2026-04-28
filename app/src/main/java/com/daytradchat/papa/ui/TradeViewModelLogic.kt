//app/src/main/java/com/daytradchat/papa/ui/TradeViewModelLogic.kt
//ver 2.17-13
package com.daytradchat.papa.ui

import android.app.Application
import com.daytradchat.papa.model.*
import java.util.UUID

abstract class TradeViewModelLogic(application: Application) : TradeViewModelBase(application) {
    // _reconnectSec, _availableCodes, _symbolsLong, _symbolsShort は
    // 基底クラス(TradeViewModelBase)のものをそのまま使用するため、ここでは定義しません。

    protected fun buildSignalCard(slotId: String, code: String): SignalCardUiModel {
        val item = signalMap[code] ?: return emptyStock(slotId, code)
        val price = item.price ?: 0.0
        val time = (item.sent_at ?: item.captured_at ?: "").takeLast(8)
        val scoreInt = item.score?.toInt() ?: 0
        
        return SignalCardUiModel(
            slotId = slotId,
            code = code,
            name = item.name ?: "---",
            signalType = item.signal_type ?: "SKIP",
            score = scoreInt,
            price = price,
            changeRate = item.change_rate ?: 0.0,
            reasonShort = item.reason_short ?: "候補",
            updatedAt = time,
            isEmpty = false
        )
    }

    protected open fun appendUserLog(text: String) {
        val newItem = LogLineUiModel(id = UUID.randomUUID().toString(), text = text)
        _logItems.value = (listOf(newItem) + _logItems.value).take(80)
    }

    protected fun emptyStock(slotId: String, code: String = "--") = SignalCardUiModel(
        slotId = slotId,
        code = code,
        name = "待機中",
        signalType = "SKIP",
        score = 0,
        price = 0.0,
        changeRate = 0.0,
        reasonShort = "待機中",
        updatedAt = "",
        isEmpty = false
    )
}

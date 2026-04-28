//app/src/main/java/com/daytradchat/papa/ui/TradeViewModelBase.kt
//ver 2.17-07 (OpenPrice対応版)
package com.daytradchat.papa.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.daytradchat.papa.model.*
import com.daytradchat.papa.network.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.*

abstract class TradeViewModelBase(application: Application) : AndroidViewModel(application) {
    protected val parser = ServerMessageParser()
    protected val configStore = ConfigStore(application)
    protected val systemLogStore = SystemLogStore(application)
    protected val prefs = application.getSharedPreferences("daytrade_client_state", Context.MODE_PRIVATE)
    protected val displayDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPAN)
    protected val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.JAPAN)

    protected val signalMap = linkedMapOf<String, SignalItem>()
    protected val historyMap = linkedMapOf<String, MutableList<String>>()
    protected val holdings = mutableMapOf<String, HoldingInfo>()
    protected val previousPriceMap = mutableMapOf<String, Double>()
    protected val startPriceMap = mutableMapOf<String, Double>()
    protected var marketItem: MarketItem? = null

    protected val _statusLeft = MutableStateFlow("未接続")
    protected val _statusRight = MutableStateFlow("")
    protected val _hostLine = MutableStateFlow("")
    protected val _signalItems = MutableStateFlow<List<SignalCardUiModel>>(emptyList())
    protected val _logItems = MutableStateFlow<List<LogLineUiModel>>(emptyList())
    protected val _systemLogItems = MutableStateFlow<List<LogLineUiModel>>(emptyList())
    protected val _currentHost = MutableStateFlow(configStore.loadHost())
    protected val _reconnectSec = MutableStateFlow(configStore.loadReconnectSec().coerceIn(1, 10))
    protected val _availableCodes = MutableStateFlow<List<String>>(emptyList())
    protected val _selectedDisplayCodes = MutableStateFlow<List<String>>(emptyList())
    
    protected val _symbolsLong = MutableStateFlow<List<SignalCardUiModel>>(emptyList())
    protected val _symbolsShort = MutableStateFlow<List<SignalCardUiModel>>(emptyList())

    protected data class HoldingInfo(val shares: Int, val buyPrice: Double)

    init {
        rotateDayIfNeeded()
        loadHoldings()
        _selectedDisplayCodes.value = loadDisplayCodes()
    }

    protected fun loadDisplayCodes(): List<String> {
        val today = dayKeyFormat.format(Date())
        if (prefs.getString("display_codes_day", "") != today) return emptyList()
        return prefs.getString("display_codes", "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    protected fun saveDisplayCodes(codes: List<String>) {
        prefs.edit().putString("display_codes", codes.joinToString(",")).apply()
        prefs.edit().putString("display_codes_day", dayKeyFormat.format(Date())).apply()
    }

    protected fun loadHoldings() {
        prefs.all.filter { it.key.startsWith("holding_") }.forEach { (k, v) ->
            val parts = v?.toString()?.split("|") ?: return@forEach
            if (parts.size == 2) {
                holdings[k.removePrefix("holding_")] = HoldingInfo(parts[0].toInt(), parts[1].toDouble())
            }
        }
    }

    protected fun saveHoldings() {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("holding_") }.forEach { editor.remove(it) }
        holdings.forEach { (c, i) -> editor.putString("holding_$c", "${i.shares}|${i.buyPrice}") }
        editor.apply()
    }

    protected fun rotateDayIfNeeded() {
        val today = dayKeyFormat.format(Date())
        if (prefs.getString("display_codes_day", "") != today) {
            prefs.edit().remove("display_codes").putString("display_codes_day", today).apply()
        }
    }
}

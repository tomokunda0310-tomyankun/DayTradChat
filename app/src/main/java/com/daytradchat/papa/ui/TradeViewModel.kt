
//app/src/main/java/com/daytradchat/papa/ui/TradeViewModel.kt
//ver 2.16-14
package com.daytradchat.papa.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TradeViewModel : ViewModel() {

    private val signalMap = mutableMapOf<String, SignalItem>()
    private val prevPriceMap = mutableMapOf<String, Double>()
    private val startPriceMap = mutableMapOf<String, Double>()
    private val samePriceCountMap = mutableMapOf<String, Int>()

    private val _items = MutableStateFlow<List<SignalItem>>(emptyList())
    val items: StateFlow<List<SignalItem>> = _items

    fun update(item: SignalItem) {
        val code = item.code ?: return
        val price = item.price ?: 0.0

        val prev = signalMap[code]?.price ?: price

        if (!startPriceMap.containsKey(code)) {
            startPriceMap[code] = price
        }

        if (price == prev) {
            samePriceCountMap[code] = (samePriceCountMap[code] ?: 0) + 1
        } else {
            samePriceCountMap[code] = 0
        }

        prevPriceMap[code] = prev
        signalMap[code] = item

        _items.value = signalMap.values.take(12)
    }

    fun getStartPrice(code: String) = startPriceMap[code] ?: 0.0
    fun getPrevPrice(code: String) = prevPriceMap[code] ?: 0.0
    fun getSameCount(code: String) = samePriceCountMap[code] ?: 0
}

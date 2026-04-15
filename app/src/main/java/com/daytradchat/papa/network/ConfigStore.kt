//app/src/main/java/com/daytradchat/papa/network/ConfigStore.kt
//ver 2.15-20
package com.daytradchat.papa.network

import android.content.Context

data class HoldingPref(
    val code: String,
    val quantity: Int,
    val buyPrice: Double
)

class ConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("daytradchat_config", Context.MODE_PRIVATE)

    fun loadHost(): String = prefs.getString("server_host", "") ?: ""

    fun saveHost(host: String) {
        prefs.edit().putString("server_host", host.trim()).apply()
    }

    fun loadReconnectSec(): Int = prefs.getInt("reconnect_sec", 0)

    fun saveReconnectSec(sec: Int) {
        prefs.edit().putInt("reconnect_sec", sec.coerceAtLeast(0)).apply()
    }

    fun saveHolding(code: String, quantity: Int, buyPrice: Double) {
        val map = loadHoldings().toMutableMap()
        map[code] = HoldingPref(code, quantity, buyPrice)
        saveHoldings(map)
    }

    fun removeHolding(code: String) {
        val map = loadHoldings().toMutableMap()
        map.remove(code)
        saveHoldings(map)
    }

    fun loadHoldings(): Map<String, HoldingPref> {
        val raw = prefs.getString("holdings", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        val result = linkedMapOf<String, HoldingPref>()
        raw.split(";").forEach { token ->
            val parts = token.split("|")
            if (parts.size == 3) {
                val code = parts[0].trim()
                val qty = parts[1].toIntOrNull() ?: return@forEach
                val price = parts[2].toDoubleOrNull() ?: return@forEach
                if (code.isNotBlank()) {
                    result[code] = HoldingPref(code, qty, price)
                }
            }
        }
        return result
    }

    private fun saveHoldings(map: Map<String, HoldingPref>) {
        val raw = map.values.joinToString(";") { "${it.code}|${it.quantity}|${it.buyPrice}" }
        prefs.edit().putString("holdings", raw).apply()
    }

    fun resetAll() {
        prefs.edit()
            .putString("server_host", "")
            .putInt("reconnect_sec", 0)
            .putString("holdings", "")
            .apply()
    }
}

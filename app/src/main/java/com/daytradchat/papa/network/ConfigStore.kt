//app/src/main/java/com/daytradchat/papa/network/ConfigStore.kt
//ver 2.13-00
package com.daytradchat.papa.network

import android.content.Context

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

    fun resetAll() {
        prefs.edit()
            .putString("server_host", "")
            .putInt("reconnect_sec", 0)
            .apply()
    }
}

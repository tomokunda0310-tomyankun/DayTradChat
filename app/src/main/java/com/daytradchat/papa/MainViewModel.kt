//app/src/main/java/com/daytradchat/papa/MainViewModel.kt
//ver 1.10-11
package com.daytradchat.papa

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SignalUiItem(
    val code: String,
    val type: String,
    val price: Double,
    val score: Int,
    val time: String
)

data class UiState(
    val signals: List<SignalUiItem> = emptyList(),
    val logs: List<String> = emptyList()
)

class MainViewModel : ViewModel() {

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    fun addSignal(s: SignalUiItem) {
        val list = _ui.value.signals.toMutableList()
        list.removeAll { it.code == s.code }
        list.add(0, s)
        _ui.value = _ui.value.copy(signals = list.take(50))
    }

    fun addLog(line: String) {
        if (line.contains("ping") || line.contains("pong")) return
        val list = _ui.value.logs.toMutableList()
        list.add(0, line)
        _ui.value = _ui.value.copy(logs = list.take(100))
    }

    fun clear() {
        _ui.value = UiState()
    }
}

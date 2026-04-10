// /app/src/main/java/com/daytradchat/papa/Main.kt
// ver 1.00-01
package com.daytradchat.papa

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.daytradchat.papa.config.ConfigStore
import com.daytradchat.papa.data.ChatDatabase
import com.daytradchat.papa.data.ChatMessage
import com.daytradchat.papa.data.ChatRepository
import com.daytradchat.papa.network.ConnectionState
import com.daytradchat.papa.network.SocketClientManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val appContext: Context,
    private val repository: ChatRepository,
    private val configStore: ConfigStore,
    private val socketClientManager: SocketClientManager
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(
        repository.observeRecentMessages(limit = 10),
        repository.observeAllMessages(),
        repository.observeLogs(limit = 200),
        socketClientManager.connectionState,
        socketClientManager.lastPongTime,
        configStore.hostFlow
    ) { recentMessages, allMessages, logs, state, lastPong, host ->
        MainUiState(
            recentMessages = recentMessages,
            allMessages = allMessages,
            logs = logs,
            connectionState = state,
            lastPongTime = lastPong,
            host = host,
            port = SocketClientManager.FIXED_PORT
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    init {
        viewModelScope.launch {
            configStore.hostFlow.distinctUntilChanged().collect { host ->
                if (host.isBlank()) {
                    socketClientManager.stop()
                } else {
                    socketClientManager.restart(host)
                }
            }
        }
    }

    fun saveHost(host: String) {
        viewModelScope.launch {
            configStore.saveHost(host.trim())
        }
    }

    fun retryNow() {
        val host = uiState.value.host
        if (host.isNotBlank()) {
            socketClientManager.restart(host)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun copyCode(context: Context, message: ChatMessage) {
        val code = message.code?.takeIf { it.isNotBlank() } ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("code", code))
        Toast.makeText(context, "$code をコピーしました", Toast.LENGTH_SHORT).show()
    }

    override fun onCleared() {
        super.onCleared()
        socketClientManager.stop()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = ChatDatabase.getInstance(context)
            val configStore = ConfigStore(context)
            val repository = ChatRepository(db.chatMessageDao(), db.appLogDao())
            val socketClientManager = SocketClientManager(repository, configStore)
            return MainViewModel(context, repository, configStore, socketClientManager) as T
        }
    }
}

data class MainUiState(
    val recentMessages: List<ChatMessage> = emptyList(),
    val allMessages: List<ChatMessage> = emptyList(),
    val logs: List<com.daytradchat.papa.data.AppLog> = emptyList(),
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val lastPongTime: String? = null,
    val host: String = "osaka-cosplayers.net",
    val port: Int = SocketClientManager.FIXED_PORT
)

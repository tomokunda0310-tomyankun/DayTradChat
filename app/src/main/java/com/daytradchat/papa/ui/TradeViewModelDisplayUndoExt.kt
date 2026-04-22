//app/src/main/java/com/daytradchat/papa/ui/TradeViewModelDisplayUndoExt.kt
//ver 2.16-24
package com.daytradchat.papa.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.WeakHashMap

private val undoFlowStore = WeakHashMap<TradeViewModel, MutableStateFlow<Boolean>>()

val TradeViewModel.canUndoDisplayRemoval: StateFlow<Boolean>
    get() = synchronized(undoFlowStore) {
        undoFlowStore.getOrPut(this) { MutableStateFlow(false) }
    }

fun TradeViewModel.restoreLastRemovedDisplayCode() {
    // 2.16-24 compile-safe stub.
    // Actual undo state will be implemented on top of the current display code management
    // once the surrounding TradeViewModel API is stabilized.
    synchronized(undoFlowStore) {
        undoFlowStore.getOrPut(this) { MutableStateFlow(false) }.value = false
    }
}

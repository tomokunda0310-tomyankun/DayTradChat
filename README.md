# DayTradeChat 既存プロジェクト組み込み版

このZIPは、既存の `com.daytradchat.papa` プロジェクトへ
新しい JSON Lines 受信仕様を組み込むための **完全版上書きセット** です。

## 先に削除するファイル
旧UIや旧通信実装が残っていると競合しやすいため、以下は削除してください。

- app/src/main/java/com/daytradchat/papa/Main.kt
- app/src/main/java/com/daytradchat/papa/ChatMessage.kt
- app/src/main/java/com/daytradchat/papa/ConfigStore.kt
- app/src/main/java/com/daytradchat/papa/SocketClientManager.kt
- app/src/main/java/com/daytradchat/papa/ui/MainScreen.kt
- app/src/main/java/com/daytradchat/papa/ui/DisplayApp.kt
- app/src/main/java/com/daytradchat/papa/ui/MonitorScreen.kt
- app/src/main/java/com/daytradchat/papa/ui/HistoryScreen.kt
- app/src/main/java/com/daytradchat/papa/ui/LogScreen.kt
- app/src/main/java/com/daytradchat/papa/ui/SettingsScreen.kt

## 上書き対象
- app/build.gradle.kts
- app/src/main/AndroidManifest.xml
- app/src/main/res/layout/activity_main.xml
- app/src/main/res/layout/item_signal.xml
- app/src/main/res/layout/item_log.xml
- app/src/main/res/values/colors.xml
- app/src/main/res/values/strings.xml
- app/src/main/res/values/themes.xml
- app/src/main/java/com/daytradchat/papa/MainActivity.kt
- app/src/main/java/com/daytradchat/papa/ui/TradeViewModel.kt
- app/src/main/java/com/daytradchat/papa/ui/SignalAdapter.kt
- app/src/main/java/com/daytradchat/papa/ui/LogAdapter.kt
- app/src/main/java/com/daytradchat/papa/model/ServerMessages.kt
- app/src/main/java/com/daytradchat/papa/model/ClientMessages.kt
- app/src/main/java/com/daytradchat/papa/model/UiModels.kt
- app/src/main/java/com/daytradchat/papa/network/SocketConfig.kt
- app/src/main/java/com/daytradchat/papa/network/SocketClient.kt
- app/src/main/java/com/daytradchat/papa/network/ServerMessageParser.kt

## 現在のUI
- 1画面
- 上部に接続状態
- 中央に 9件表示 RecyclerView
- 下部に受信ログ RecyclerView

## 受信仕様
- JSON Lines
- server_hello
- pong
- ack
- error
- signal_batch

## 注意
固定ホストは `SocketConfig.kt` で変更してください。

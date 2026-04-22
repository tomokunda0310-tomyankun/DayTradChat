//app/src/main/java/com/daytradchat/papa/model/ClientMessages.kt
//ver 2.16-22
package com.daytradchat.papa.model

data class RegisterMessage(
    val type: String = "register",
    val client_name: String = "android_client",
    val client_version: String = "2.16-22"
)

data class GetNowMessage(
    val type: String = "get_now"
)

data class PingMessage(
    val type: String = "ping"
)

data class WatchCodesMessage(
    val type: String = "watch_codes",
    val codes: List<String>
)

data class AddCodesMessage(
    val type: String = "add_codes",
    val codes: List<String>
)

//app/src/main/java/com/daytradchat/papa/model/RegisterMessage.kt
//ver 2.13-14

package com.daytradchat.papa.model

data class RegisterMessage(
    val type: String = "register",
    val client_name: String = "android",
    val client_version: String = "1.0.0"
)

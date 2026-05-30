//app/src/main/java/com/daytradchat/papa/config/ConfigStore.kt
//ver 2.17-45
package com.daytradchat.papa.config

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class ConfigStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    // 初期値を loadConnectionSettings から取得
    private val _hostFlow = MutableStateFlow(loadConnectionSettings().first)
    val hostFlow: StateFlow<String> = _hostFlow.asStateFlow()

    // 接続設定の保存
    fun saveConnectionSettings(host: String, port: String) {
        val json = JSONObject().put("host", host).put("port", port).toString()
        val encrypted = encrypt(json)
        prefs.edit().putString(KEY_CONFIG, encrypted).apply()
        _hostFlow.value = host
    }

    // 接続設定の取得（真実のソース）
    fun loadConnectionSettings(): Pair<String, String> {
        return runCatching {
            val encrypted = prefs.getString(KEY_CONFIG, null)
            if (encrypted.isNullOrBlank()) {
                DEFAULT_HOST to "5001"
            } else {
                val json = JSONObject(decrypt(encrypted))
                json.optString("host", DEFAULT_HOST) to json.optString("port", "5001")
            }
        }.getOrDefault(DEFAULT_HOST to "5001")
    }

    // 再接続秒数の管理
    fun loadReconnectSec(): Int = prefs.getInt("reconnect_sec", 5)

    fun saveReconnectSec(sec: Int) {
        prefs.edit().putInt("reconnect_sec", sec).apply()
    }

    // 設定のリセット
    fun resetAll() {
        prefs.edit().clear().apply()
        _hostFlow.value = DEFAULT_HOST
    }

    // --- 暗号化関連 (変更なし) ---
    private fun encrypt(text: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(text.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(iv + encryptedBytes, Base64.NO_WRAP)
    }

    private fun decrypt(base64: String): String {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, IV_SIZE)
        val encrypted = bytes.copyOfRange(IV_SIZE, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(TAG_LENGTH, iv))
        return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEY_STORE)
        val spec = android.security.keystore.KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
            android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val PREF_NAME = "daytradechat_secure_prefs"
        private const val KEY_CONFIG = "key_config"
        private const val DEFAULT_HOST = "osaka-cosplayers.net"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "daytradechat_config_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_LENGTH = 128
    }
}

package com.marriagebureau.ai.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64

class SecureStorageManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("marriage_bureau_secure_prefs", Context.MODE_PRIVATE)

    fun saveApiKey(providerKey: String, apiKey: String) {
        if (apiKey.isBlank()) {
            prefs.edit().remove("key_$providerKey").apply()
            return
        }
        val encoded = Base64.encodeToString(apiKey.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        prefs.edit().putString("key_$providerKey", encoded).apply()
    }

    fun getApiKey(providerKey: String): String? {
        val encoded = prefs.getString("key_$providerKey", null) ?: return null
        return try {
            String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    fun clearApiKey(providerKey: String) {
        prefs.edit().remove("key_$providerKey").apply()
    }

    fun saveProviderConfig(providerKey: String, jsonConfig: String) {
        prefs.edit().putString("config_$providerKey", jsonConfig).apply()
    }

    fun getProviderConfig(providerKey: String): String? {
        return prefs.getString("config_$providerKey", null)
    }

    companion object {
        fun maskSecret(secret: String?): String {
            if (secret.isNullOrBlank()) return "Not Configured"
            if (secret.length <= 8) return "********"
            return "${secret.take(4)}...${secret.takeLast(4)}"
        }
    }
}

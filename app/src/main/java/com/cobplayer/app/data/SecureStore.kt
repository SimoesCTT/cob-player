package com.cobplayer.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecureStore {
    private const val PREFS = "cob_secure"

    private fun prefs(ctx: Context) = try {
        val key = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            ctx, PREFS, key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        ctx.getSharedPreferences(PREFS + "_fallback", Context.MODE_PRIVATE)
    }

    fun put(ctx: Context, key: String, value: String) {
        prefs(ctx).edit().putString(key, value).apply()
    }

    fun get(ctx: Context, key: String): String =
        prefs(ctx).getString(key, "") ?: ""

    fun remove(ctx: Context, key: String) {
        prefs(ctx).edit().remove(key).apply()
    }
}

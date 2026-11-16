package com.kothalipi.android.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class Mode { CLOUD, DEVICE }
enum class Provider(val id: String, val label: String) {
    GEMINI("gemini", "Google Gemini"),
    OPENAI("openai", "OpenAI");
}

/** Settings shared by the keyboard and the app. API keys are encrypted with an Android Keystore key. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("kothalipi", Context.MODE_PRIVATE)

    var lang: String
        get() = sp.getString("lang", "bn") ?: "bn"
        set(v) = sp.edit().putString("lang", v).apply()

    var mode: Mode
        get() = if (sp.getString("mode", "cloud") == "device") Mode.DEVICE else Mode.CLOUD
        set(v) = sp.edit().putString("mode", if (v == Mode.DEVICE) "device" else "cloud").apply()

    /** True once the user has picked Cloud or On-device in setup. */
    var engineChosen: Boolean
        get() = sp.getBoolean("engineChosen", false)
        set(v) = sp.edit().putBoolean("engineChosen", v).apply()

    fun key(p: Provider): String? = sp.getString("key_${p.id}", null)?.let { KeyVault.decrypt(it) }?.takeIf { it.isNotBlank() }

    fun setKey(p: Provider, value: String) {
        val v = value.trim()
        if (v.isEmpty()) sp.edit().remove("key_${p.id}").apply()
        else sp.edit().putString("key_${p.id}", KeyVault.encrypt(v)).apply()
    }

    // ---- today's cloud usage (Kothalipi's own count; providers don't share a key's remaining quota)

    private fun today() = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

    private fun rollDay() {
        if (sp.getString("usage_day", "") != today()) {
            val e = sp.edit().putString("usage_day", today())
            Provider.values().forEach { p ->
                e.remove("u_${p.id}_req").remove("u_${p.id}_ok").remove("u_${p.id}_fail").remove("u_${p.id}_secs")
                e.remove("limit_${p.id}")
            }
            e.apply()
        }
    }

    data class Usage(val requests: Int, val failed: Int, val minutes: Float, val limit: String?)

    fun usage(p: Provider): Usage {
        rollDay()
        return Usage(sp.getInt("u_${p.id}_req", 0), sp.getInt("u_${p.id}_fail", 0), sp.getFloat("u_${p.id}_secs", 0f) / 60f, sp.getString("limit_${p.id}", null))
    }

    fun recordOk(p: Provider, seconds: Float) {
        rollDay()
        sp.edit().putInt("u_${p.id}_req", sp.getInt("u_${p.id}_req", 0) + 1)
            .putInt("u_${p.id}_ok", sp.getInt("u_${p.id}_ok", 0) + 1)
            .putFloat("u_${p.id}_secs", sp.getFloat("u_${p.id}_secs", 0f) + seconds)
            .remove("limit_${p.id}").apply()
    }

    fun recordFailed(p: Provider, limitMessage: String?) {
        rollDay()
        val e = sp.edit().putInt("u_${p.id}_req", sp.getInt("u_${p.id}_req", 0) + 1)
            .putInt("u_${p.id}_fail", sp.getInt("u_${p.id}_fail", 0) + 1)
        if (limitMessage != null) e.putString("limit_${p.id}", limitMessage)
        e.apply()
    }

    /** Cloud engines that have a key, in the order they are tried. */
    fun cloudEngines(): List<Pair<Provider, String>> =
        Provider.values().mapNotNull { p -> key(p)?.let { p to it } }
}

private object KeyVault {
    private const val ALIAS = "kothalipi_keys"

    private fun secret(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return gen.generateKey()
    }

    fun encrypt(plain: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, secret())
        val ct = c.doFinal(plain.toByteArray())
        return Base64.encodeToString(c.iv + ct, Base64.NO_WRAP)
    }

    fun decrypt(stored: String): String? = try {
        val raw = Base64.decode(stored, Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, secret(), GCMParameterSpec(128, raw, 0, 12))
        String(c.doFinal(raw, 12, raw.size - 12))
    } catch (_: Exception) {
        null
    }
}

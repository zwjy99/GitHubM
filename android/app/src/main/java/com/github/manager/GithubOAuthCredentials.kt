package com.github.manager

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * A user's own OAuth App identity. No client ID or secret is shipped with GitHubM.
 * The secret is encrypted using an Android Keystore AES-GCM key before it reaches preferences.
 * It cannot be exported to JavaScript and is never included in an APK build.
 */
internal class GithubOAuthCredentials(context: Context) {
    data class Entry(val clientId: String, val clientSecret: String)

    private val preferences = context.getSharedPreferences("githubm_private_oauth", Context.MODE_PRIVATE)
    private val alias = "githubm_user_oauth_aes_v1"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    fun save(clientId: String, clientSecret: String) {
        require(clientId.matches(Regex("[A-Za-z0-9]{6,128}"))) { "Client ID 格式无效" }
        require(clientSecret.isNotBlank() && clientSecret.length <= 512) { "Client Secret 不能为空" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(clientSecret.toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString("client_id", clientId)
            .putString("secret_nonce", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("secret_ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun load(): Entry? {
        return try {
            val clientId = preferences.getString("client_id", null) ?: return null
            val nonce = Base64.decode(preferences.getString("secret_nonce", ""), Base64.NO_WRAP)
            val encrypted = Base64.decode(preferences.getString("secret_ciphertext", ""), Base64.NO_WRAP)
            if (nonce.size != 12 || encrypted.isEmpty()) return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, nonce))
            Entry(clientId, String(cipher.doFinal(encrypted), Charsets.UTF_8))
        } catch (_: Exception) {
            // A reinstall/backup restore can leave encrypted preferences without their Keystore key.
            null
        }
    }

    fun clear() {
        preferences.edit().clear().apply()
        try { KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias) }
        catch (_: Exception) { /* Clearing preferences is still effective. */ }
    }
}

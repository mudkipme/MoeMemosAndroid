package me.mudkip.moememos.data.service

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureTokenStorage @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(TOKENS_FILE_NAME, Context.MODE_PRIVATE)
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    }

    fun getToken(accountKey: String): String? {
        val encrypted = sharedPreferences.getString(accountKey, null) ?: return null
        val decrypted = decrypt(encrypted)
        if (decrypted == null) {
            removeToken(accountKey)
        }
        return decrypted
    }

    fun saveToken(accountKey: String, accessToken: String) {
        if (accessToken.isBlank()) {
            removeToken(accountKey)
            return
        }
        val encrypted = encrypt(accessToken) ?: run {
            removeToken(accountKey)
            return
        }
        sharedPreferences.edit { putString(accountKey, encrypted) }
    }

    fun removeToken(accountKey: String) {
        sharedPreferences.edit { remove(accountKey) }
    }

    private fun encrypt(plainText: String): String? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val cipherBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            val iv = cipher.iv
            "${base64Encode(iv)}:${
                base64Encode(cipherBytes)
            }"
        } catch (_: GeneralSecurityException) {
            null
        }
    }

    private fun decrypt(payload: String): String? {
        val parts = payload.split(':', limit = 2)
        if (parts.size != 2) {
            return null
        }

        return try {
            val iv = base64Decode(parts[0])
            val cipherBytes = base64Decode(parts[1])
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
            String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8)
        } catch (_: GeneralSecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) {
            return existing
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE
        )
        val keyGenSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }

    private fun base64Encode(bytes: ByteArray): String {
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun base64Decode(value: String): ByteArray {
        return Base64.getDecoder().decode(value)
    }

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "moememos.tokens.aesgcm"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val TOKENS_FILE_NAME = "secure_tokens"
    }
}

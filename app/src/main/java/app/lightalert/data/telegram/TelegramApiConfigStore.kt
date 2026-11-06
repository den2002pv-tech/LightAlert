package app.lightalert.data.telegram

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class TelegramApiConfig(val apiId: Int, val apiHash: String)

/** Encrypts the user-provided Telegram API configuration with an Android Keystore key. */
class TelegramApiConfigStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    @Synchronized
    fun get(): TelegramApiConfig? {
        val payload = preferences.getString(CONFIG_VALUE, null) ?: return null
        val decoded = Base64.decode(payload, Base64.NO_WRAP)
        require(decoded.size > IV_SIZE) { "Telegram API configuration is corrupted" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getExistingKey(),
            GCMParameterSpec(TAG_BITS, decoded.copyOfRange(0, IV_SIZE)),
        )
        val plain = cipher.doFinal(decoded.copyOfRange(IV_SIZE, decoded.size))
        val parts = plain.toString(Charsets.UTF_8).split("\n", limit = 2)
        val id = parts.firstOrNull()?.toIntOrNull() ?: return null
        val hash = parts.getOrNull(1) ?: return null
        return TelegramApiConfig(id, hash)
    }

    @Synchronized
    fun save(apiId: Int, apiHash: String) {
        require(apiId > 0) { "API ID должен быть положительным числом" }
        require(apiHash.matches(Regex("[a-fA-F0-9]{32}"))) {
            "API Hash должен содержать 32 шестнадцатеричных символа"
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val plain = "$apiId\n$apiHash".toByteArray(Charsets.UTF_8)
        val encoded = Base64.encodeToString(cipher.iv + cipher.doFinal(plain), Base64.NO_WRAP)
        check(preferences.edit().putString(CONFIG_VALUE, encoded).commit()) {
            "Не удалось сохранить параметры Telegram API"
        }
    }

    @Synchronized
    fun clear() {
        check(preferences.edit().remove(CONFIG_VALUE).commit()) {
            "Не удалось удалить параметры Telegram API"
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val existing = loadKeyStore().getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private fun getExistingKey(): SecretKey =
        (loadKeyStore().getKey(KEY_ALIAS, null) as? SecretKey)
            ?: throw IllegalStateException("Ключ Telegram API в Android Keystore отсутствует")

    private fun loadKeyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    private companion object {
        const val PREFERENCES_NAME = "telegram_api_config"
        const val CONFIG_VALUE = "encrypted_config"
        const val KEY_ALIAS = "app.lightalert.telegram.api"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}

package app.lightalert.data.telegram

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the TDLib database key encrypted by a non-exportable Android Keystore key.
 *
 * The plaintext database key is returned only in memory for the TDLib initialization
 * call. If the wrapped key exists but its Keystore key is unavailable, this class
 * fails closed instead of silently generating a different key and losing the session.
 */
internal class TdlibDatabaseKeyStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    @Synchronized
    fun getOrCreateDatabaseKey(): ByteArray {
        val storedValue = preferences.getString(WRAPPED_KEY, null)
        if (storedValue != null) {
            return decrypt(storedValue)
        }

        val databaseKey = ByteArray(DATABASE_KEY_SIZE_BYTES).also(SecureRandom()::nextBytes)
        val wrappedValue = encrypt(databaseKey)
        check(preferences.edit().putString(WRAPPED_KEY, wrappedValue).commit()) {
            "Unable to persist the encrypted TDLib database key"
        }
        return databaseKey
    }

    private fun encrypt(plainText: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateWrappingKey())
        val encrypted = cipher.doFinal(plainText)
        val payload = cipher.iv + encrypted
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(encodedPayload: String): ByteArray {
        val payload = Base64.decode(encodedPayload, Base64.NO_WRAP)
        require(payload.size > GCM_IV_SIZE_BYTES) { "Stored TDLib database key is corrupted" }

        val iv = payload.copyOfRange(0, GCM_IV_SIZE_BYTES)
        val encrypted = payload.copyOfRange(GCM_IV_SIZE_BYTES, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getExistingWrappingKey(),
            GCMParameterSpec(GCM_TAG_SIZE_BITS, iv),
        )
        return cipher.doFinal(encrypted)
    }

    private fun getOrCreateWrappingKey(): SecretKey {
        val keyStore = loadKeyStore()
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) return existingKey

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

    private fun getExistingWrappingKey(): SecretKey =
        (loadKeyStore().getKey(KEY_ALIAS, null) as? SecretKey)
            ?: throw IllegalStateException(
                "The Android Keystore key for the TDLib database is missing. " +
                    "The Telegram session cannot be decrypted; sign in again after resetting app data.",
            )

    private fun loadKeyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    private companion object {
        const val PREFERENCES_NAME = "tdlib_database_key"
        const val WRAPPED_KEY = "wrapped_database_key"
        const val KEY_ALIAS = "app.lightalert.tdlib.database"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val DATABASE_KEY_SIZE_BYTES = 32
        const val GCM_IV_SIZE_BYTES = 12
        const val GCM_TAG_SIZE_BITS = 128
    }
}

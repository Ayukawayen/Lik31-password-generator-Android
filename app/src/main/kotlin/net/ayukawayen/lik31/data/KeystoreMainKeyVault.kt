package net.ayukawayen.lik31.data

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * [MainKeyVault] that encrypts the hash with an AES-256-GCM key held in the
 * Android Keystore. The key never leaves the Keystore, so the stored blob is
 * useless when copied off the device.
 *
 * Stored value: hex(12-byte IV + ciphertext + 16-byte tag).
 */
class KeystoreMainKeyVault(private val prefs: SharedPreferences) : MainKeyVault {

    override fun load(): String? {
        val blob = prefs.getString(KEY_BLOB, null)?.let(Hex::decode) ?: return null
        return try {
            check(blob.size > IV_BYTES)
            val key = existingKey() ?: error("key missing")
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, blob, 0, IV_BYTES))
            val text = String(cipher.doFinal(blob, IV_BYTES, blob.size - IV_BYTES), Charsets.US_ASCII)
            check(MainKeyVault.isValidHash(text))
            text
        } catch (e: Exception) {
            // Unreadable (key lost after a restore, corrupted file, ...): start clean.
            clear()
            null
        }
    }

    override fun save(mainHash: String) {
        require(MainKeyVault.isValidHash(mainHash))
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, existingKey() ?: createKey())
        val iv = cipher.iv
        check(iv.size == IV_BYTES)
        val blob = iv + cipher.doFinal(mainHash.toByteArray(Charsets.US_ASCII))
        // commit(): the caller closes the app right after saving.
        check(prefs.edit().putString(KEY_BLOB, Hex.encode(blob)).commit())
    }

    override fun clear() {
        prefs.edit().remove(KEY_BLOB).commit()
        try {
            keyStore().deleteEntry(KEY_ALIAS)
        } catch (e: Exception) {
            // Nothing more can be done; the blob is already gone.
        }
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun existingKey(): SecretKey? = keyStore().getKey(KEY_ALIAS, null) as? SecretKey

    private fun createKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "lik31.mainhash.v1"
        const val KEY_BLOB = "main_hash"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}

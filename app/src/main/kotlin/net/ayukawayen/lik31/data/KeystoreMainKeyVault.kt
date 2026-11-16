package net.ayukawayen.lik31.data

import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

/**
 * [MainKeyVault] backed by the Android Keystore. Two storage forms:
 *
 * - **Protected** (Android 9+, a strong biometric enrolled, and the setting on):
 *   the hash is encrypted with the public half of an RSA key pair whose private
 *   half needs a biometric for every use. Encrypting needs no prompt, so saving
 *   never interrupts; decrypting goes through a biometric prompt.
 *   Stored value: hex(RSA-OAEP ciphertext).
 * - **Plain** (otherwise): AES-256-GCM key in the Keystore, no user presence.
 *   Stored value: hex(12-byte IV + ciphertext + 16-byte tag).
 *
 * Either way the key material never leaves the Keystore, so the stored blob is
 * useless when copied off the device. A plain entry is upgraded to a protected
 * one the next time it is read, when that has become possible.
 */
class KeystoreMainKeyVault(
    private val prefs: SharedPreferences,
    private val wantProtection: () -> Boolean = { true },
) : MainKeyVault {

    override fun read(): StoredMainKey {
        if (prefs.contains(KEY_PROTECTED_BLOB)) {
            if (privateKey() != null) return StoredMainKey.Locked
            // Blob without its key (e.g. data restored onto another device).
            clear()
            return StoredMainKey.None
        }
        val hash = readPlain() ?: return StoredMainKey.None
        // Upgrade quietly; from the next launch on it needs the biometric.
        if (wantProtection() && saveProtected(hash)) removePlain()
        return StoredMainKey.Unlocked(hash)
    }

    override fun save(mainHash: String) {
        require(MainKeyVault.isValidHash(mainHash))
        if (wantProtection() && saveProtected(mainHash)) {
            removePlain()
        } else {
            savePlain(mainHash)
            removeProtected()
        }
    }

    override fun clear() {
        removePlain()
        removeProtected()
    }

    override fun beginUnlock(): Cipher? = try {
        val key = privateKey() ?: error("key missing")
        // Throws KeyPermanentlyInvalidatedException once biometrics were re-enrolled.
        Cipher.getInstance(RSA_TRANSFORMATION).apply { init(Cipher.DECRYPT_MODE, key, OAEP_SPEC) }
    } catch (e: Exception) {
        clear()
        null
    }

    override fun completeUnlock(authorisedCipher: Cipher): String? = try {
        val blob = prefs.getString(KEY_PROTECTED_BLOB, null)?.let(Hex::decode) ?: error("blob missing")
        String(authorisedCipher.doFinal(blob), Charsets.US_ASCII).takeIf(MainKeyVault::isValidHash)
    } catch (e: Exception) {
        null
    }

    // ---- protected form: RSA-OAEP, private key bound to a strong biometric ----

    /** @return false when protection is not possible here (old Android, nothing enrolled, ...) */
    private fun saveProtected(mainHash: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return try {
            val certificate = keyStore().getCertificate(PROTECTED_ALIAS) ?: createProtectedKey()
            // Rebuild the public key outside the Keystore: used that way it is never
            // subject to the private key's authentication requirement.
            val publicKey = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_RSA)
                .generatePublic(X509EncodedKeySpec(certificate.publicKey.encoded))
            val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_SPEC)
            val blob = cipher.doFinal(mainHash.toByteArray(Charsets.US_ASCII))
            prefs.edit().putString(KEY_PROTECTED_BLOB, Hex.encode(blob)).commit()
        } catch (e: Exception) {
            // Typically: no biometric enrolled, so the key cannot be created.
            removeProtected()
            false
        }
    }

    private fun createProtectedKey(): java.security.cert.Certificate {
        val builder = KeyGenParameterSpec.Builder(PROTECTED_ALIAS, KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(2048)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Every use needs a fresh strong-biometric authentication.
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        }
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
        generator.initialize(builder.build())
        generator.generateKeyPair()
        return checkNotNull(keyStore().getCertificate(PROTECTED_ALIAS))
    }

    private fun privateKey(): PrivateKey? = try {
        keyStore().getKey(PROTECTED_ALIAS, null) as? PrivateKey
    } catch (e: Exception) {
        null
    }

    private fun removeProtected() {
        prefs.edit().remove(KEY_PROTECTED_BLOB).commit()
        deleteKey(PROTECTED_ALIAS)
    }

    // ---- plain form: AES-GCM, no user presence ----

    private fun readPlain(): String? {
        val blob = prefs.getString(KEY_PLAIN_BLOB, null)?.let(Hex::decode) ?: return null
        return try {
            check(blob.size > IV_BYTES)
            val key = aesKey() ?: error("key missing")
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, blob, 0, IV_BYTES))
            val text = String(cipher.doFinal(blob, IV_BYTES, blob.size - IV_BYTES), Charsets.US_ASCII)
            check(MainKeyVault.isValidHash(text))
            text
        } catch (e: Exception) {
            // Unreadable (key lost after a restore, corrupted file, ...): start clean.
            removePlain()
            null
        }
    }

    private fun savePlain(mainHash: String) {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey() ?: createAesKey())
        val iv = cipher.iv
        check(iv.size == IV_BYTES)
        val blob = iv + cipher.doFinal(mainHash.toByteArray(Charsets.US_ASCII))
        // commit(): the caller closes the app right after saving.
        check(prefs.edit().putString(KEY_PLAIN_BLOB, Hex.encode(blob)).commit())
    }

    private fun removePlain() {
        prefs.edit().remove(KEY_PLAIN_BLOB).commit()
        deleteKey(AES_ALIAS)
    }

    private fun aesKey(): SecretKey? = keyStore().getKey(AES_ALIAS, null) as? SecretKey

    private fun createAesKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(AES_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    // ---- shared ----

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun deleteKey(alias: String) {
        try {
            keyStore().deleteEntry(alias)
        } catch (e: Exception) {
            // Nothing more can be done; the blob is already gone.
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"

        const val AES_ALIAS = "lik31.mainhash.v1"
        const val KEY_PLAIN_BLOB = "main_hash"
        const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128

        const val PROTECTED_ALIAS = "lik31.mainhash.bio.v1"
        const val KEY_PROTECTED_BLOB = "main_hash_bio"
        const val RSA_TRANSFORMATION = "RSA/ECB/OAEPPadding"

        /**
         * OAEP with SHA-256 and MGF1-SHA-1, spelled out on both sides: the
         * Android Keystore uses SHA-1 for MGF1, while other providers would
         * default MGF1 to the main digest and produce undecryptable data.
         */
        val OAEP_SPEC = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT)
    }
}

package net.ayukawayen.lik31.data

import android.content.SharedPreferences
import java.security.SecureRandom

/**
 * [ServiceConfigStore] on SharedPreferences. Lookups happen on every keystroke
 * in the service key field, so this is synchronous and in-memory after the
 * first read.
 */
class PrefsServiceConfigStore(private val prefs: SharedPreferences) : ServiceConfigStore {
    private val index: ServiceKeyIndex by lazy { ServiceKeyIndex(loadOrCreateSalt()) }

    override fun load(normalizedServiceKey: String): ServiceConfig? {
        if (normalizedServiceKey.isEmpty()) return null
        val text = prefs.getString(index.storageKey(normalizedServiceKey), null) ?: return null
        return ServiceConfigCodec.decode(text)
    }

    override fun save(normalizedServiceKey: String, config: ServiceConfig) {
        if (normalizedServiceKey.isEmpty()) return
        prefs.edit()
            .putString(index.storageKey(normalizedServiceKey), ServiceConfigCodec.encode(config))
            .apply()
    }

    override fun count(): Int = prefs.all.keys.count { it.startsWith(ServiceKeyIndex.PREFIX) }

    override fun clearAll() {
        val editor = prefs.edit()
        // Keep the salt; only the entries go.
        prefs.all.keys.filter { it.startsWith(ServiceKeyIndex.PREFIX) }.forEach { editor.remove(it) }
        editor.apply()
    }

    private fun loadOrCreateSalt(): ByteArray {
        val existing = prefs.getString(KEY_SALT, null)?.let(Hex::decode)
        if (existing != null && existing.size == ServiceKeyIndex.SALT_BYTES) return existing
        val salt = ByteArray(ServiceKeyIndex.SALT_BYTES).also { SecureRandom().nextBytes(it) }
        // A new salt orphans any old entries; drop them so the file does not grow forever.
        prefs.edit().clear().putString(KEY_SALT, Hex.encode(salt)).apply()
        return salt
    }

    private companion object {
        const val KEY_SALT = "salt"
    }
}

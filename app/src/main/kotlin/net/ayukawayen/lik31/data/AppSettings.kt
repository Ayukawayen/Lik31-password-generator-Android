package net.ayukawayen.lik31.data

import android.content.SharedPreferences
import net.ayukawayen.lik31.core.Lik31

/** User preferences, edited on the settings screen. */
class AppSettings(private val prefs: SharedPreferences) {
    /** Length used for a service that has nothing remembered. */
    var defaultLength: Int
        get() = Lik31.clampLength(prefs.getInt(KEY_DEFAULT_LENGTH, Lik31.DEFAULT_LENGTH))
        set(value) = prefs.edit().putInt(KEY_DEFAULT_LENGTH, Lik31.clampLength(value)).apply()

    /**
     * Protect the saved main key with biometrics when the device can.
     * A change takes effect the next time the saved key is read or unlocked.
     */
    var protectWithBiometrics: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRICS, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRICS, value).apply()

    /** Keep the generator out of screenshots, recordings and the recents preview. */
    var secureWindow: Boolean
        get() = prefs.getBoolean(KEY_SECURE_WINDOW, true)
        set(value) = prefs.edit().putBoolean(KEY_SECURE_WINDOW, value).apply()

    private companion object {
        const val KEY_DEFAULT_LENGTH = "default_length"
        const val KEY_BIOMETRICS = "protect_with_biometrics"
        const val KEY_SECURE_WINDOW = "secure_window"
    }
}

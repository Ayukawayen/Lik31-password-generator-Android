package net.ayukawayen.lik31.data

import android.content.SharedPreferences

/** User preferences. The settings screen (a later phase) will edit these. */
class AppSettings(private val prefs: SharedPreferences) {
    /** Protect a newly saved main key with biometrics when the device can. */
    var protectWithBiometrics: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRICS, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRICS, value).apply()

    private companion object {
        const val KEY_BIOMETRICS = "protect_with_biometrics"
    }
}

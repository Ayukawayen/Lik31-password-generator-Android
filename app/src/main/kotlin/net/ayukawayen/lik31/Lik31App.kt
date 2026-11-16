package net.ayukawayen.lik31

import android.app.Application
import android.content.Context
import net.ayukawayen.lik31.data.AppSettings
import net.ayukawayen.lik31.data.KeystoreMainKeyVault
import net.ayukawayen.lik31.data.MainKeyVault
import net.ayukawayen.lik31.data.PrefsServiceConfigStore
import net.ayukawayen.lik31.data.ServiceConfigStore

/** Owns the app's few long-lived objects (manual dependency injection). */
class Lik31App : Application() {
    val settings: AppSettings by lazy {
        AppSettings(getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE))
    }

    val mainKeyVault: MainKeyVault by lazy {
        KeystoreMainKeyVault(
            prefs = getSharedPreferences(PREFS_VAULT, Context.MODE_PRIVATE),
            wantProtection = { settings.protectWithBiometrics },
        )
    }

    val serviceConfigs: ServiceConfigStore by lazy {
        PrefsServiceConfigStore(getSharedPreferences(PREFS_SERVICE_CONFIGS, Context.MODE_PRIVATE))
    }

    companion object {
        /** File names are referenced by res/xml/data_extraction_rules.xml. */
        const val PREFS_VAULT = "lik31_vault"
        const val PREFS_SERVICE_CONFIGS = "lik31_service_configs"
        const val PREFS_SETTINGS = "lik31_settings"
    }
}

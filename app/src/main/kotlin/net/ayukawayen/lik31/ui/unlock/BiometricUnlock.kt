package net.ayukawayen.lik31.ui.unlock

import android.app.Activity
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import net.ayukawayen.lik31.R
import javax.crypto.Cipher

/**
 * The system biometric prompt, asked to authorise one use of [Cipher].
 * Uses the platform API directly (Android 9+); the vault only produces a
 * locked key on those versions.
 */
@RequiresApi(Build.VERSION_CODES.P)
object BiometricUnlock {
    /**
     * @param onSuccess called with the now-authorised cipher
     * @param onDismissed called when the prompt ends any other way: "type the
     * main key instead", cancel, lock-out, error. May be called more than once.
     * @return a signal that closes the prompt
     */
    fun show(
        activity: Activity,
        cipher: Cipher,
        onSuccess: (Cipher) -> Unit,
        onDismissed: () -> Unit,
    ): CancellationSignal {
        val executor = activity.mainExecutor
        val cancellation = CancellationSignal()
        val prompt = BiometricPrompt.Builder(activity)
            .setTitle(activity.getString(R.string.unlock_title))
            .setSubtitle(activity.getString(R.string.unlock_subtitle))
            .setNegativeButton(activity.getString(R.string.unlock_type_instead), executor) { _, _ -> onDismissed() }
            .build()
        prompt.authenticate(
            BiometricPrompt.CryptoObject(cipher),
            cancellation,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val authorised = result.cryptoObject?.cipher
                    if (authorised != null) onSuccess(authorised) else onDismissed()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onDismissed()
                }
                // onAuthenticationFailed (finger not recognised): the prompt stays up and retries.
            },
        )
        return cancellation
    }
}

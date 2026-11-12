package net.ayukawayen.lik31.system

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import net.ayukawayen.lik31.R

object ClipboardWriter {
    /**
     * Value of ClipDescription.EXTRA_IS_SENSITIVE (added in API 33). The literal
     * is used so older releases that honour the flag are covered too.
     */
    private const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"

    /**
     * Copies [text] and marks it as sensitive so the system clipboard preview
     * hides it. On Android 13+ the system shows its own "copied" overlay, so a
     * toast is only shown on older versions.
     */
    fun copySensitive(context: Context, text: String) {
        val appContext = context.applicationContext
        val clipboard = appContext.getSystemService(ClipboardManager::class.java) ?: return
        val clip = ClipData.newPlainText(appContext.getString(R.string.clip_label), text)
        clip.description.extras = PersistableBundle().apply {
            putBoolean(EXTRA_IS_SENSITIVE, true)
        }
        clipboard.setPrimaryClip(clip)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(appContext, R.string.toast_copied, Toast.LENGTH_SHORT).show()
        }
    }
}

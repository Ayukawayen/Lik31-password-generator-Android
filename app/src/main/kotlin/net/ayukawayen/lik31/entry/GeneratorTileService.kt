package net.ayukawayen.lik31.entry

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import net.ayukawayen.lik31.R

/** Quick Settings tile: one tap from anywhere opens the generator. */
class GeneratorTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        // The tile is a launcher, not a toggle.
        tile.state = Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(R.string.tile_subtitle)
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        // On the lock screen, ask for unlock first; the generator must not open over it.
        if (isLocked) unlockAndRun { openGenerator() } else openGenerator()
    }

    private fun openGenerator() {
        val intent = Intent(this, GeneratorActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ only accepts a PendingIntent here.
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }
}

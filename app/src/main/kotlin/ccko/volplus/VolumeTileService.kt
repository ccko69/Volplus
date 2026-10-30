package ccko.volplus
import android.content.Intent
import android.media.AudioManager
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
/**
 * Quick Settings tile: starts the persistent controls notification and asks
 * Android to display its native media-volume overlay without launching an
 * activity over the currently foreground app (which can disturb PiP).
 */
class VolumeTileService : TileService() {
  override fun onClick() {
    super.onClick()
    ContextCompat.startForegroundService(
      this,
      Intent(this, VolumeControlService::class.java)
      .setAction(VolumeControlService.ACTION_START),
    )
    val audioManager = getSystemService(AudioManager::class.java)
    audioManager.adjustStreamVolume(
      AudioManager.STREAM_MUSIC,
      AudioManager.ADJUST_SAME,
      AudioManager.FLAG_SHOW_UI,
    )
  }
}

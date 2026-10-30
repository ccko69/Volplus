package ccko.volplus
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
/** Restores the persistent controls after reboot only if the user previously enabled them. */
class VolumePlusBootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    val action = intent?.action ?: return
    if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
    val wasEnabled = context.getSharedPreferences(
      VolumeControlService.PREFERENCES_NAME,
      Context.MODE_PRIVATE,
    ).getBoolean(VolumeControlService.KEY_SERVICE_ENABLED, false)
    if (!wasEnabled) return
    val serviceIntent = Intent(context, VolumeControlService::class.java)
    .setAction(VolumeControlService.ACTION_BOOT_RESUME)
    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ContextCompat.startForegroundService(context, serviceIntent)
      } else {
        context.startService(serviceIntent)
      }
    }
  }
}

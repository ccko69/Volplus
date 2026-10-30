package ccko.volplus
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
class VolumeControlService : Service() {
  private lateinit var audioManager: AudioManager
  private lateinit var notificationManager: NotificationManager
  private val updateHandler = Handler(Looper.getMainLooper())
  private var lastSnapshot: VolumeSnapshot? = null
  private var foregroundStarted = false
  private var receiverRegistered = false
  private val volumeChangedReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      refreshIfChanged(force = false)
    }
  }
  private val pollForChanges = object : Runnable {
    override fun run() {
      refreshIfChanged(force = false)
      updateHandler.postDelayed(this, POLL_INTERVAL_MS)
    }
  }
  override fun onCreate() {
    super.onCreate()
    audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    notificationManager = getSystemService(NotificationManager::class.java)
    createNotificationChannel()
    registerVolumeChangeReceiver()
  }
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP -> {
        getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        .edit().putBoolean(KEY_SERVICE_ENABLED, false).apply()
        updateHandler.removeCallbacks(pollForChanges)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelfResult(startId)
        return START_NOT_STICKY
      }
      ACTION_VOLUME_UP -> adjustMusicVolume(AudioManager.ADJUST_RAISE)
      ACTION_VOLUME_DOWN -> adjustMusicVolume(AudioManager.ADJUST_LOWER)
      ACTION_MUTE -> adjustMusicVolume(AudioManager.ADJUST_TOGGLE_MUTE)
      ACTION_CLOSE -> {
        getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        .edit().putBoolean(KEY_SERVICE_ENABLED, false).apply()
        updateHandler.removeCallbacks(pollForChanges)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelfResult(startId)
        return START_NOT_STICKY
      }
    }
    if (intent?.action != ACTION_BOOT_RESUME) {
      getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
      .edit().putBoolean(KEY_SERVICE_ENABLED, true).apply()
    }
    refreshIfChanged(force = true)
    updateHandler.removeCallbacks(pollForChanges)
    updateHandler.postDelayed(pollForChanges, POLL_INTERVAL_MS)
    return START_STICKY
  }
  override fun onDestroy() {
    updateHandler.removeCallbacksAndMessages(null)
    if (receiverRegistered) {
      runCatching { unregisterReceiver(volumeChangedReceiver) }
      receiverRegistered = false
    }
    super.onDestroy()
  }
  override fun onBind(intent: Intent?): IBinder? = null
  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Volume controls",
        NotificationManager.IMPORTANCE_LOW,
      ).apply {
        description = "Persistent quick volume controls."
        setShowBadge(false)
        setSound(null, null)
        enableVibration(false)
      }
      notificationManager.createNotificationChannel(channel)
    }
  }
  private fun registerVolumeChangeReceiver() {
    val filter = IntentFilter(ACTION_SYSTEM_VOLUME_CHANGED).apply {
      addAction(ACTION_SYSTEM_MUTE_CHANGED)
    }
    runCatching {
      ContextCompat.registerReceiver(
        this,
        volumeChangedReceiver,
        filter,
        ContextCompat.RECEIVER_EXPORTED,
      )
      receiverRegistered = true
    }
  }
  private fun refreshIfChanged(force: Boolean) {
    val snapshot = readSnapshot()
    if (!force && snapshot == lastSnapshot) return
    lastSnapshot = snapshot
    val notification = buildNotification(snapshot)
    if (!foregroundStarted) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ServiceCompat.startForeground(
          this,
          NOTIFICATION_ID,
          notification,
          android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
      } else {
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, 0)
      }
      foregroundStarted = true
    } else {
      notificationManager.notify(NOTIFICATION_ID, notification)
    }
  }
  private fun buildNotification(snapshot: VolumeSnapshot): Notification {
    val current = snapshot.volumes.first()
    val maximum = snapshot.maxVolumes.first().coerceAtLeast(1)
    val percentage = volumePercentage(current, maximum)
    val muted = snapshot.muted.first()
    val remoteViews = RemoteViews(packageName, R.layout.nfy_vol_ctrl).apply {
      setTextViewText(R.id.current_vol, "$percentage%")
      setProgressBar(R.id.volume_progress, 100, percentage, false)
      setImageViewResource(R.id.btn_mute, if (muted) R.drawable.ic_mute else R.drawable.ic_mute_off)
      setContentDescription(R.id.btn_mute, if (muted) "Unmute" else "Mute")
      setOnClickPendingIntent(R.id.btn_mute, serviceActionPendingIntent(ACTION_MUTE, REQUEST_MUTE))
      setOnClickPendingIntent(R.id.btn_dec_vol, serviceActionPendingIntent(ACTION_VOLUME_DOWN, REQUEST_DOWN))
      setOnClickPendingIntent(R.id.btn_inc_vol, serviceActionPendingIntent(ACTION_VOLUME_UP, REQUEST_UP))
      setOnClickPendingIntent(R.id.btn_close, serviceActionPendingIntent(ACTION_CLOSE, REQUEST_CLOSE))
    }
    val openActivityIntent = Intent(this, MainActivity::class.java).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    val openActivity = PendingIntent.getActivity(
      this,
      REQUEST_OPEN,
      openActivityIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    return NotificationCompat.Builder(this, CHANNEL_ID)
    .setSmallIcon(R.drawable.ic_mute_off)
    .setContentIntent(openActivity)
    .setOngoing(true)
    .setOnlyAlertOnce(true)
    .setSilent(true)
    .setPriority(NotificationCompat.PRIORITY_MAX)
    .setColor(NOTIFICATION_ACCENT)
    .setContent(remoteViews)
    // .setCustomContentView(remoteViews)
    // .setCustomBigContentView(remoteViews)
    // .setCustomHeadsUpContentView(remoteViews)
    // .setStyle(NotificationCompat.DecoratedCustomViewStyle())
    .build()
  }
  private fun serviceActionPendingIntent(action: String, requestCode: Int): PendingIntent {
    val intent = Intent(this, VolumeControlService::class.java).setAction(action)
    return PendingIntent.getService(
      this,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }
  private fun adjustMusicVolume(direction: Int) {
    audioManager.adjustStreamVolume(
      AudioManager.STREAM_MUSIC,
      direction,
      AudioManager.FLAG_SHOW_UI,
    )
    updateHandler.postDelayed({ refreshIfChanged(force = true) }, ACTION_REFRESH_DELAY_MS)
  }
  private fun readSnapshot(): VolumeSnapshot {
    val streams = OBSERVED_STREAMS
    return VolumeSnapshot(
      volumes = streams.map { audioManager.getStreamVolume(it) },
      maxVolumes = streams.map { audioManager.getStreamMaxVolume(it) },
      muted = streams.map { audioManager.isStreamMute(it) },
    )
  }
  private data class VolumeSnapshot(
    val volumes: List<Int>,
    val maxVolumes: List<Int>,
    val muted: List<Boolean>,
  )
  companion object {
    const val ACTION_START = "ccko.volumeplus.action.START"
    const val ACTION_BOOT_RESUME = "ccko.volumeplus.action.BOOT_RESUME"
    const val ACTION_VOLUME_UP = "ccko.volumeplus.action.VOLUME_UP"
    const val ACTION_VOLUME_DOWN = "ccko.volumeplus.action.VOLUME_DOWN"
    const val ACTION_MUTE = "ccko.volumeplus.action.MUTE"
    const val ACTION_STOP = "ccko.volumeplus.action.STOP"
    private const val ACTION_CLOSE = "ccko.volumeplus.action.CLOSE"
    const val PREFERENCES_NAME = "volume_plus_state"
    const val KEY_SERVICE_ENABLED = "service_enabled"
    private const val CHANNEL_ID = "volume_control_channel"
    private const val NOTIFICATION_ID = 1
    private const val POLL_INTERVAL_MS = 1_200L
    private const val ACTION_REFRESH_DELAY_MS = 100L
    private const val ACTION_SYSTEM_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
    private const val ACTION_SYSTEM_MUTE_CHANGED = "android.media.STREAM_MUTE_CHANGED_ACTION"
    private const val NOTIFICATION_ACCENT = 0xFF900020.toInt()
    private const val REQUEST_OPEN = 100
    private const val REQUEST_DOWN = 101
    private const val REQUEST_UP = 102
    private const val REQUEST_MUTE = 103
    private const val REQUEST_CLOSE = 104
    private val OBSERVED_STREAMS = intArrayOf(
      AudioManager.STREAM_MUSIC,
      AudioManager.STREAM_RING,
      AudioManager.STREAM_NOTIFICATION,
      AudioManager.STREAM_ALARM,
    )
  }
}

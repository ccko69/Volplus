package ccko.volplus
import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
private const val IDLE_DISMISS_MS = 5_000L
/** How often the panel re-reads system volumes to detect external changes. */
private const val POLL_INTERVAL_MS = 400L
/** Distance (in dp) of the panel from the top of the screen. Tune this. */
private const val TOP_OFFSET_DP = 100
/** The main Floating Launcher */
class MainActivity : ComponentActivity() {
  private val idleHandler = Handler(Looper.getMainLooper())
  private val dismissWhenIdle = Runnable { finish() }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.setBackgroundDrawableResource(android.R.color.transparent)
    // The notification service starts only as a result of a visible user action.
    ContextCompat.startForegroundService(
      this,
      Intent(this, VolumeControlService::class.java).setAction(VolumeControlService.ACTION_START),
    )
    setContent {
      VolumePlusTheme {
        val context = LocalContext.current
        val permissionLauncher = rememberLauncherForActivityResult(
          ActivityResultContracts.RequestPermission(),
        ) { /* The foreground service still runs if the notification permission is denied. */ }
        LaunchedEffect(Unit) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
              context,
              Manifest.permission.POST_NOTIFICATIONS,
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
          ) {
            delay(250)
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          }
        }
        VolumePanel(
          audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager,
          onDismiss = { finish() },
          onInteraction = { scheduleIdleDismiss() },
        )
      }
    }
    val density = resources.displayMetrics.density
    val desiredWidth = (320f * density).roundToInt()
    val screenWidth =
    (resources.displayMetrics.widthPixels - 24f * density).roundToInt().coerceAtLeast(1)
    window.setLayout(minOf(desiredWidth, screenWidth), WindowManager.LayoutParams.WRAP_CONTENT)
    val lp = window.attributes
    lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
    lp.y = (TOP_OFFSET_DP * density).toInt()
    window.attributes = lp
    scheduleIdleDismiss()
  }
  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    ContextCompat.startForegroundService(
      this,
      Intent(this, VolumeControlService::class.java).setAction(VolumeControlService.ACTION_START),
    )
    scheduleIdleDismiss()
  }
  override fun onResume() {
    super.onResume()
    scheduleIdleDismiss()
  }
  override fun onPause() {
    idleHandler.removeCallbacks(dismissWhenIdle)
    super.onPause()
  }
  override fun onUserInteraction() {
    super.onUserInteraction()
    scheduleIdleDismiss()
  }
  override fun onDestroy() {
    idleHandler.removeCallbacks(dismissWhenIdle)
    super.onDestroy()
  }
  private fun scheduleIdleDismiss() {
    idleHandler.removeCallbacks(dismissWhenIdle)
    idleHandler.postDelayed(dismissWhenIdle, IDLE_DISMISS_MS)
  }
}
private data class VolumeStream(
  val label: String,
  /** All stream types controlled by this one row. The first is the "primary". */
  val streamTypes: List<Int>,
) {
  val primary: Int get() = streamTypes.first()
}
@Composable
private fun VolumePanel(
  audioManager: AudioManager,
  onDismiss: () -> Unit,
  onInteraction: () -> Unit,
) {
  val streams = remember {
    listOf(
      VolumeStream("Master", listOf(AudioManager.STREAM_MUSIC)),
      VolumeStream("Ringtone & Notifications", listOf(AudioManager.STREAM_RING, AudioManager.STREAM_NOTIFICATION), ),
      VolumeStream("Alarm", listOf(AudioManager.STREAM_ALARM)),
    )
  }
  val maxVolumes = remember(audioManager, streams) {
    streams.associate {
      it.primary to audioManager.getStreamMaxVolume(it.primary).coerceAtLeast(1)
    }
  }
  var volumes by remember(audioManager, streams) {
    mutableStateOf(streams.associate { it.primary to audioManager.getStreamVolume(it.primary) })
  }
  var expanded by rememberSaveable { mutableStateOf(false) }
  LaunchedEffect(audioManager, streams) {
    while (isActive) {
      val liveVolumes = streams.associate {
        it.primary to audioManager.getStreamVolume(it.primary)
        .coerceIn(0, maxVolumes.getValue(it.primary))
      }
      if (liveVolumes != volumes) volumes = liveVolumes
      delay(POLL_INTERVAL_MS)
    }
  }
  val chevronRotation by animateFloatAsState(
    targetValue = if (expanded) 180f else 0f,
    animationSpec = tween(durationMillis = 200),
    label = "chevronRotation",
  )
  Surface(
    modifier = Modifier.width(320.dp),
    shape = RoundedCornerShape(12.dp),
    color = VolumeColors.panel,
    border = BorderStroke(1.dp, VolumeColors.accent.copy(alpha = 0.60f)),
    shadowElevation = 12.dp,
    tonalElevation = 0.dp,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "VOLUME+",
          color = Color.LightGray,
          fontSize = 18.sp,
          letterSpacing = 1.5.sp,
          fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.weight(1f))
        IconButton(
          onClick = {
            onInteraction()
            onDismiss()
          },
          modifier = Modifier.size(40.dp),
        ) {
          CrossGlyph()
        }
      }
      Spacer(Modifier.height(8.dp))
      VolumeSliderRow(
        stream = streams[0],
        value = volumes.getValue(streams[0].primary),
        maxValue = maxVolumes.getValue(streams[0].primary),
        onInteraction = onInteraction,
        onValueChange = { newValue ->
          streams[0].streamTypes.forEach { audioManager.setStreamVolume(it, newValue, 0) }
          volumes = volumes + (streams[0].primary to newValue)
        },
      )
      if (expanded) {
        Column {
          Spacer(Modifier.height(4.dp))
          streams.drop(1).forEach { stream ->
            VolumeSliderRow(
              stream = stream,
              value = volumes.getValue(stream.primary),
              maxValue = maxVolumes.getValue(stream.primary),
              onInteraction = onInteraction,
              onValueChange = { newValue ->
                stream.streamTypes.forEach { audioManager.setStreamVolume(it, newValue, 0) }
                volumes = volumes + (stream.primary to newValue)
              },
            )
          }
          Spacer(Modifier.height(6.dp))
        }
      }
      TextButton(
        onClick = {
          onInteraction()
          expanded = !expanded
        },
        modifier = Modifier
        .fillMaxWidth()
        .height(36.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = VolumeColors.accentLight),
      ) {
        Text(
          text = if (expanded) "Less" else "More",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.1.sp,
        )
        Spacer(Modifier.width(8.dp))
        ChevronGlyph(
          pointUp = false,
          modifier = Modifier.rotate(chevronRotation),
        )
      }
    }
  }
}
@Composable
private fun VolumeSliderRow(
  stream: VolumeStream,
  value: Int,
  maxValue: Int,
  onInteraction: () -> Unit,
  onValueChange: (Int) -> Unit,
) {
  val percentage = volumePercentage(value, maxValue)
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stream.label,
        color = Color.Gray,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
      )
      Spacer(Modifier.weight(1f))
      Text(
        text = "$percentage%",
        color = VolumeColors.accentLight,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
      )
    }
    Slider(
      value = value.coerceIn(0, maxValue).toFloat(),
      onValueChange = { rawValue ->
        onInteraction()
        onValueChange(rawValue.roundToInt().coerceIn(0, maxValue))
      },
      valueRange = 0f..maxValue.toFloat(),
      steps = (maxValue - 1).coerceAtLeast(0),
      colors = SliderDefaults.colors(
        thumbColor = VolumeColors.accentLight,
        activeTrackColor = VolumeColors.accent,
        inactiveTrackColor = Color.White.copy(alpha = 0.20f),
        activeTickColor = Color.White.copy(alpha = 0.72f),
        inactiveTickColor = Color.White.copy(alpha = 0.22f),
      ),
      modifier = Modifier
      .fillMaxWidth()
      .height(48.dp)
      .semantics { contentDescription = "${stream.label} volume, $percentage percent" },
    )
  }
}
@Composable
private fun ChevronGlyph(
  pointUp: Boolean = false,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.size(16.dp)) {
    val width = size.width
    val height = size.height
    val halfWidth = width * 0.30f
    val centerX = width / 2f
    val centerY = height / 2f
    val offsetY = height * 0.12f
    val topY = if (pointUp) centerY + offsetY else centerY - offsetY
    val bottomY = if (pointUp) centerY - offsetY else centerY + offsetY
    drawLine(
      color = VolumeColors.accentLight,
      start = androidx.compose.ui.geometry.Offset(centerX - halfWidth, topY),
      end = androidx.compose.ui.geometry.Offset(centerX, bottomY),
      strokeWidth = 2.dp.toPx(),
      cap = StrokeCap.Round,
    )
    drawLine(
      color = VolumeColors.accentLight,
      start = androidx.compose.ui.geometry.Offset(centerX, bottomY),
      end = androidx.compose.ui.geometry.Offset(centerX + halfWidth, topY),
      strokeWidth = 2.dp.toPx(),
      cap = StrokeCap.Round,
    )
  }
}
@Composable
private fun CrossGlyph() {
  Canvas(modifier = Modifier.size(18.dp)) {
    val inset = size.minDimension * 0.25f
    drawLine(
      color = Color.White.copy(alpha = 0.80f),
      start = androidx.compose.ui.geometry.Offset(inset, inset),
      end = androidx.compose.ui.geometry.Offset(size.width - inset, size.height - inset),
      strokeWidth = 2.dp.toPx(),
      cap = StrokeCap.Round,
    )
    drawLine(
      color = Color.White.copy(alpha = 0.80f),
      start = androidx.compose.ui.geometry.Offset(size.width - inset, inset),
      end = androidx.compose.ui.geometry.Offset(inset, size.height - inset),
      strokeWidth = 2.dp.toPx(),
      cap = StrokeCap.Round,
    )
  }
}
internal fun volumePercentage(volume: Int, maxVolume: Int): Int {
  if (maxVolume <= 0) return 0
  val clampedVolume = volume.coerceIn(0, maxVolume).toLong()
  val maximum = maxVolume.toLong()
  // Ceiling labels give the requested 7%, 7%, 6% progression for 15 native intervals.
  return ((clampedVolume * 100L + maximum - 1L) / maximum).toInt()
}

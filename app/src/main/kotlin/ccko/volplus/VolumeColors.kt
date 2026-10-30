package ccko.volplus
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
internal object VolumeColors {
  val panel = Color(0xCC000000)
  val accent = Color(0xFF900020)
  val accentLight = Color(0xFFB00020)
}
private val volumeColorScheme = darkColorScheme(
  primary = VolumeColors.accent,
  onPrimary = Color.White,
  primaryContainer = Color(0xFF36000C),
  onPrimaryContainer = Color.White,
  secondary = VolumeColors.accentLight,
  onSecondary = Color.White,
  background = VolumeColors.panel,
  onBackground = Color.White,
  surface = VolumeColors.panel,
  onSurface = Color.White,
  surfaceVariant = Color(0xFF201014),
  onSurfaceVariant = Color(0xFFE9DDE0),
)
@Composable
internal fun VolumePlusTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = volumeColorScheme,
    content = content,
  )
}

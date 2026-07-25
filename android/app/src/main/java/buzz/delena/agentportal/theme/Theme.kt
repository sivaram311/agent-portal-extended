package buzz.delena.agentportal.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Agent Portal is dark-first (matches the web portal's default), but a light
// scheme is included so the app is not broken on forced-light devices.
private val DarkColors = darkColorScheme(
    background = ApColors.Background,
    surface = ApColors.Surface,
    surfaceVariant = ApColors.SurfaceAlt,
    primary = ApColors.Accent,
    onPrimary = ApColors.Background,
    secondary = ApColors.AccentHover,
    onBackground = ApColors.TextPrimary,
    onSurface = ApColors.TextPrimary,
    onSurfaceVariant = ApColors.TextMuted,
    error = ApColors.Danger,
)

private val LightColors = lightColorScheme(
    primary = ApColors.AccentHover,
    secondary = ApColors.Accent,
    error = ApColors.Danger,
)

@Composable
fun AgentPortalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = ApTypography,
        content = content,
    )
}

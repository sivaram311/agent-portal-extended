package buzz.delena.agentportal.theme

import androidx.compose.ui.graphics.Color

/**
 * Mirrors agent-portal/frontend/src/app/theme/tokens.scss so the Android app
 * and the web portal read as one product. Not modeled on any third party's
 * app branding.
 */
object ApColors {
    val Background = Color(0xFF0F172A) // --ap-bg
    val Surface = Color(0xFF1E293B) // --ap-surface
    val SurfaceAlt = Color(0xFF243247) // --ap-surface-2
    val Border = Color(0x29949CA8) // --ap-border (rgba(148,163,184,0.16))
    val Accent = Color(0xFF14B8A6) // --ap-accent
    val AccentHover = Color(0xFF0D9488) // --ap-accent-hover
    val AccentSoft = Color(0x2414B8A6) // --ap-accent-soft
    val TextPrimary = Color(0xFFF8FAFC) // --ap-text
    val TextMuted = Color(0xFF94A3B8) // --ap-text-muted
    val Success = Color(0xFF22C55E)
    val Danger = Color(0xFFEF4444)
    val Warning = Color(0xFFF59E0B)
    val Info = Color(0xFF0EA5E9)
}

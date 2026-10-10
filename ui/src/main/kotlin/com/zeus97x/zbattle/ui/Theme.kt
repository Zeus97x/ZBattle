package com.zeus97x.zbattle.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Colour tokens from ai/UI_LAYOUT_SPEC.md (dark) plus a matching light variant. */
@Immutable
data class ZPalette(
    val background: Color,
    val surface: Color,
    val elevated: Color,
    val accent: Color,
    val accentDark: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val success: Color,
    val onAccent: Color,
    val scrim: Color,
    val isDark: Boolean,
) {
    val headerGradient: Brush get() = Brush.verticalGradient(listOf(accent, accentDark))
}

val DarkPalette = ZPalette(
    background = Color(0xFF141720),
    surface = Color(0xFF242834),
    elevated = Color(0xFF2D3140),
    accent = Color(0xFFF64F8B),
    accentDark = Color(0xFFD63A73),
    textPrimary = Color(0xFFF5F4F7),
    textSecondary = Color(0xFFBBB7C2),
    border = Color(0xFF414555),
    success = Color(0xFF49B87C),
    onAccent = Color.White,
    scrim = Color(0x99000000),
    isDark = true,
)

val LightPalette = ZPalette(
    background = Color(0xFFF6F4F8),
    surface = Color(0xFFFFFFFF),
    elevated = Color(0xFFEFECF3),
    // Darker pinks keep accent text/icons readable on white.
    accent = Color(0xFFD63A73),
    accentDark = Color(0xFFB02A5E),
    textPrimary = Color(0xFF1B1D26),
    textSecondary = Color(0xFF55515F),
    border = Color(0xFFD7D3DD),
    success = Color(0xFF237A4D),
    onAccent = Color.White,
    scrim = Color(0x80000000),
    isDark = false,
)

/** Layout tokens (dp) from the spec. */
object Dimens {
    val screenPadding = 16.dp
    val gapSmall = 12.dp
    val gap = 16.dp
    val gapLarge = 24.dp
    val cardPadding = 16.dp
    val cardRadius = 20.dp
    val sheetRadius = 28.dp
    val touchTarget = 48.dp
    val primaryButtonHeight = 54.dp
    val bottomBarHeight = 64.dp
    val homeActionSize = 64.dp
}

val LocalPalette = staticCompositionLocalOf { DarkPalette }

object Z {
    val colors: ZPalette @Composable get() = LocalPalette.current
}

private fun typography(): Typography {
    val base = TextStyle(fontFamily = FontFamily.Default)
    return Typography(
        headlineMedium = base.copy(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
        headlineSmall = base.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
        titleLarge = base.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = base.copy(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.copy(fontSize = 16.sp, lineHeight = 22.sp),
        bodyMedium = base.copy(fontSize = 14.sp, lineHeight = 20.sp),
        labelLarge = base.copy(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = base.copy(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
        labelSmall = base.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    )
}

@Composable
fun ZBattleTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.accentDark, onPrimary = p.onAccent, secondary = p.accent, onSecondary = p.onAccent,
            background = p.background, onBackground = p.textPrimary, surface = p.surface, onSurface = p.textPrimary,
            surfaceVariant = p.elevated, onSurfaceVariant = p.textSecondary, outline = p.border,
            secondaryContainer = p.elevated, onSecondaryContainer = p.textPrimary,
        )
    } else {
        lightColorScheme(
            primary = p.accentDark, onPrimary = p.onAccent, secondary = p.accent, onSecondary = p.onAccent,
            background = p.background, onBackground = p.textPrimary, surface = p.surface, onSurface = p.textPrimary,
            surfaceVariant = p.elevated, onSurfaceVariant = p.textSecondary, outline = p.border,
            secondaryContainer = p.elevated, onSecondaryContainer = p.textPrimary,
        )
    }
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = typography(), content = content)
    }
}

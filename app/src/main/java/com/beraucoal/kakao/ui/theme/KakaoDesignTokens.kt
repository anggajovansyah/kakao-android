package com.beraucoal.kakao.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design tokens terpusat untuk seluruh UI Kakao Digital.
 * Semua screen & component WAJIB mereferensikan object ini —
 * dilarang hardcode warna/radius/spacing secara inline.
 */

object KakaoColors {
    // ── Brand ──
    val Primary         = Color(0xFF2E7D32)
    val PrimaryDark     = Color(0xFF1B5E20)
    val PrimaryLight    = Color(0xFF4CAF50)
    val PrimaryContainer = Color(0xFFE8F5E9)

    // ── Surface & Background ──
    val Background      = Color(0xFFF8F9FA)
    val Surface         = Color(0xFFFFFFFF)
    val SurfaceMuted    = Color(0xFFF1F5F9)

    // ── Text ──
    val TextPrimary     = Color(0xFF1E293B)
    val TextSecondary   = Color(0xFF64748B)
    val TextMuted       = Color(0xFF94A3B8)

    // ── Status ──
    val Warning         = Color(0xFFF57F17)
    val WarningContainer = Color(0xFFFFF8E1)
    val WarningText     = Color(0xFF7A4F01)
    val Error           = Color(0xFFC62828)
    val ErrorContainer  = Color(0xFFFFEBEE)
    val Success         = Color(0xFF2E7D32)
    val SuccessContainer = Color(0xFFE8F5E9)

    // ── Stepper ──
    val StepActive      = Color(0xFF2E7D32)
    val StepCompleted   = Color(0xFF4CAF50)
    val StepPending     = Color(0xFFE2E8F0)
    val StepTextInactive = Color(0xFF64748B)

    // ── Divider ──
    val Divider         = Color(0xFFE2E8F0)
}

object KakaoRadius {
    val Small:  Dp = 12.dp
    val Medium: Dp = 14.dp
    val Large:  Dp = 16.dp
    val XL:     Dp = 20.dp
    val XXL:    Dp = 24.dp
    val Card:   Dp = 24.dp
    val Button: Dp = 16.dp
    val Input:  Dp = 16.dp
}

object KakaoElevation {
    val None:    Dp = 0.dp
    val Low:     Dp = 2.dp
    val Medium:  Dp = 4.dp
    val High:    Dp = 8.dp
}

object KakaoSpacing {
    val XS:  Dp = 6.dp
    val SM:  Dp = 10.dp
    val MD:  Dp = 16.dp
    val LG:  Dp = 20.dp
    val XL:  Dp = 24.dp
    val XXL: Dp = 32.dp
    val XXXL: Dp = 40.dp

    /** Standard horizontal padding for screen content to guarantee breathing room */
    val ScreenHorizontal: Dp = 24.dp
    /** Standard vertical padding for screen content */
    val ScreenVertical: Dp = 24.dp
}

object KakaoSizes {
    val ButtonHeight: Dp = 54.dp
    val IconSmall:    Dp = 20.dp
    val IconMedium:   Dp = 24.dp
    val IconLarge:    Dp = 40.dp
    val HeaderLogoHeight: Dp = 32.dp
}

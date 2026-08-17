package com.beraucoal.kakao.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design tokens terpusat untuk seluruh UI Kakao Digital.
 * Palet warna diselaraskan dengan prototipe reference 2.0 —
 * hijau kanopi kakao sebagai warna utama (#175c40), kuning-merah
 * buah matang sebagai tangga status. Sama persis dengan dashboard
 * penyuluh, supaya petani dan penyuluh melihat warna yang sama
 * untuk hal yang sama.
 *
 * Semua screen & component WAJIB mereferensikan object ini —
 * dilarang hardcode warna/radius/spacing secara inline.
 */

object KakaoColors {
    // ── Brand (hijau kanopi kakao — reference 2.0) ──
    val Primary         = Color(0xFF175C40)   // --hijau
    val PrimaryDark     = Color(0xFF0F3D2B)   // --hijau-tua
    val PrimaryLight    = Color(0xFF2E9E5B)   // --sehat (juga hijau muda aktif)
    val PrimaryContainer = Color(0xFFE7F2EC)  // --hijau-muda

    // ── Surface & Background ──
    val Background      = Color(0xFFF2F5F2)   // --kertas
    val Surface         = Color(0xFFFFFFFF)   // --kartu
    val SurfaceMuted    = Color(0xFFF7F9F8)   // kartu muted/dashed

    // ── Text ──
    val TextPrimary     = Color(0xFF132019)   // --teks
    val TextSecondary   = Color(0xFF5B6F66)   // --teks2
    val TextMuted       = Color(0xFF8A9A94)   // --teks3

    // ── Status (tangga warna buah kakao — identik dashboard) ──
    val Sehat           = Color(0xFF2E9E5B)   // --sehat (hijau buah muda)
    val Pantau          = Color(0xFFE0A12A)   // --pantau (kuning buah masak)
    val Rawat           = Color(0xFFE0703B)   // --rawat / perlu_perawatan (oranye)
    val Kritis          = Color(0xFFC0392B)   // --kritis (merah buah busuk)
    val BelumAdaData    = Color(0xFF8A9A94)   // --kosong (abu-abu)

    // ── Alias status lama (backward compat) ──
    val Warning         = Pantau
    val WarningContainer = Color(0xFFFFF8E8)  // saran background kuning
    val WarningText     = Color(0xFF7A5A12)
    val Error           = Kritis
    val ErrorContainer  = Color(0xFFFDF3F1)   // --kritis background
    val Success         = Sehat
    val SuccessContainer = PrimaryContainer

    // ── Stepper ──
    val StepActive      = Primary
    val StepCompleted   = PrimaryLight
    val StepPending     = Color(0xFFDFE6E1)   // --garis
    val StepTextInactive = TextSecondary

    // ── Divider & Garis ──
    val Divider         = Color(0xFFDFE6E1)   // --garis
}

object KakaoRadius {
    val Small:  Dp = 13.dp     // input/chip radius (reference: 13px)
    val Medium: Dp = 15.dp     // foto-hasil, kartu kecil
    val Large:  Dp = 17.dp     // kartu utama (reference: 17px)
    val XL:     Dp = 20.dp     // chip rounded
    val XXL:    Dp = 24.dp     // bottom sheet, modal
    val Card:   Dp = 17.dp     // sesuai reference .kartu: 17px
    val Button: Dp = 15.dp     // sesuai reference .tbl: 15px
    val Input:  Dp = 13.dp     // sesuai reference .isian: 13px
    val Hero:   Dp = 26.dp     // hero section bottom corners
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

    /** Standard horizontal padding for screen content (reference: 20px .pad) */
    val ScreenHorizontal: Dp = 20.dp
    /** Standard vertical padding for screen content */
    val ScreenVertical: Dp = 20.dp
}

object KakaoSizes {
    val ButtonHeight: Dp = 54.dp
    val IconSmall:    Dp = 20.dp
    val IconMedium:   Dp = 24.dp
    val IconLarge:    Dp = 40.dp
    val HeaderLogoHeight: Dp = 32.dp
    val BottomNavHeight: Dp = 64.dp
}

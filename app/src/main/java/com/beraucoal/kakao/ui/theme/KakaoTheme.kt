package com.beraucoal.kakao.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.beraucoal.kakao.R

/**
 * KakaoTheme — wrapping Material3 dengan custom palette hijau kanopi kakao
 * dan Google Fonts Plus Jakarta Sans sebagai tipografi utama (sesuai reference 2.0).
 *
 * Font dipilih Plus Jakarta Sans karena:
 * - Dirancang untuk UI Indonesia (Latin Extended mendukung aksara lokal)
 * - Weight 400-800 yang dipakai prototipe tersedia semua
 * - Lebih tegas dan modern dibanding Poppins untuk angka (tabel status kebun)
 */

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val plusJakartaSansFont = GoogleFont("Plus Jakarta Sans")

val PlusJakartaSansFontFamily = FontFamily(
    Font(googleFont = plusJakartaSansFont, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = plusJakartaSansFont, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = plusJakartaSansFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = plusJakartaSansFont, fontProvider = googleFontProvider, weight = FontWeight.Bold),
    Font(googleFont = plusJakartaSansFont, fontProvider = googleFontProvider, weight = FontWeight.ExtraBold),
)

// Backward compat alias — file lama yang import PoppinsFontFamily tetap compile
val PoppinsFontFamily = PlusJakartaSansFontFamily

private val KakaoTypography = Typography(
    // Display: judul besar ala splash / hero
    displayLarge = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 31.sp, letterSpacing = (-0.03).sp),
    // Headline: judul layar utama
    headlineLarge = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp, letterSpacing = (-0.028).sp),
    headlineMedium = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.02).sp),
    headlineSmall = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Bold, fontSize = 19.sp, letterSpacing = (-0.02).sp),
    // Title: sub-judul, nama kebun
    titleLarge = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = (-0.015).sp),
    titleMedium = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.5.sp),
    titleSmall = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.5.sp),
    // Body: teks isi
    bodyLarge = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.5.sp),
    bodyMedium = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.5.sp),
    bodySmall = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 18.75.sp),
    // Label: chip, badge, eyebrow, tombol
    labelLarge = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.5.sp),
    labelMedium = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
    labelSmall = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, letterSpacing = 0.15.sp),
)

private val KakaoColorScheme = lightColorScheme(
    primary = KakaoColors.Primary,
    onPrimary = KakaoColors.Surface,
    primaryContainer = KakaoColors.PrimaryContainer,
    onPrimaryContainer = KakaoColors.PrimaryDark,
    secondary = KakaoColors.PrimaryLight,
    background = KakaoColors.Background,
    onBackground = KakaoColors.TextPrimary,
    surface = KakaoColors.Surface,
    onSurface = KakaoColors.TextPrimary,
    surfaceVariant = KakaoColors.SurfaceMuted,
    error = KakaoColors.Error,
    errorContainer = KakaoColors.ErrorContainer,
)

@Composable
fun KakaoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KakaoColorScheme,
        typography = KakaoTypography,
        content = content
    )
}

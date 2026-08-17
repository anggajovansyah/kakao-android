package com.beraucoal.kakao.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beraucoal.kakao.ui.theme.KakaoColors
import com.beraucoal.kakao.ui.theme.KakaoRadius
import com.beraucoal.kakao.ui.theme.KakaoSpacing
import com.beraucoal.kakao.ui.theme.PlusJakartaSansFontFamily

/**
 * Layar Menunggu Verifikasi — sesuai reference 2.0.
 *
 * Ditampilkan setelah KTP dikirim. Petani melihat progress tiga langkah
 * (data terkirim → diperiksa petugas → kode aktif) dan nomor pendaftaran
 * yang bisa disebutkan lewat telepon ke penyuluh.
 *
 * Reference: LAYAR.tunggu di prototipe-aplikasi-petani.html
 */
@Composable
fun WaitingVerificationScreen(
    registrationNumber: String = "DFT-2026-0831",
    onCheckStatus: () -> Unit = {},
    onGoBack: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KakaoColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = KakaoSpacing.ScreenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(44.dp))

            // Icon dalam lingkaran
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(KakaoColors.PrimaryContainer)
            ) {
                Text(
                    text = "⏳",
                    fontSize = 41.sp
                )
            }

            Spacer(Modifier.height(22.dp))

            // Judul
            Text(
                text = "Menunggu verifikasi",
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSansFontFamily,
                color = KakaoColors.TextPrimary,
                letterSpacing = (-0.028).sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Petugas sedang memeriksa KTP Anda.\nBiasanya selesai kurang dari 1×24 jam.",
                fontSize = 13.sp,
                fontFamily = PlusJakartaSansFontFamily,
                color = KakaoColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.5.sp,
                modifier = Modifier.padding(horizontal = 18.dp)
            )

            Spacer(Modifier.height(26.dp))

            // Kartu progress 3 langkah
            Surface(
                shape = RoundedCornerShape(KakaoRadius.Card),
                color = KakaoColors.Surface,
                border = ButtonDefaults.outlinedButtonBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    VerificationStep(
                        label = "Data KTP terkirim",
                        status = StepStatus.DONE
                    )
                    VerificationStep(
                        label = "Diperiksa petugas",
                        status = StepStatus.IN_PROGRESS
                    )
                    VerificationStep(
                        label = "Kode petani aktif",
                        status = StepStatus.PENDING
                    )
                }
            }

            Spacer(Modifier.height(11.dp))

            // Kartu nomor pendaftaran
            Surface(
                shape = RoundedCornerShape(KakaoRadius.Card),
                color = KakaoColors.SurfaceMuted,
                border = ButtonDefaults.outlinedButtonBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "NOMOR PENDAFTARAN",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSansFontFamily,
                        color = KakaoColors.TextMuted,
                        letterSpacing = 0.15.sp
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = registrationNumber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSansFontFamily,
                        color = KakaoColors.TextPrimary
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = "Sebutkan nomor ini bila menghubungi petugas lapangan.",
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSansFontFamily,
                        color = KakaoColors.TextSecondary,
                        lineHeight = 19.5.sp
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            // Tombol periksa
            Button(
                onClick = onCheckStatus,
                shape = RoundedCornerShape(KakaoRadius.Button),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KakaoColors.PrimaryContainer,
                    contentColor = KakaoColors.PrimaryDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = "Periksa Sekarang",
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 15.5.sp
                )
            }

            Spacer(Modifier.height(9.dp))

            // Tombol kembali
            OutlinedButton(
                onClick = onGoBack,
                shape = RoundedCornerShape(KakaoRadius.Button),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = KakaoColors.Primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = "Kembali",
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 15.5.sp
                )
            }
        }
    }
}

private enum class StepStatus { DONE, IN_PROGRESS, PENDING }

@Composable
private fun VerificationStep(
    label: String,
    status: StepStatus
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 9.dp)
    ) {
        // Step indicator circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(25.dp)
                .clip(CircleShape)
                .background(
                    when (status) {
                        StepStatus.DONE -> KakaoColors.Sehat
                        StepStatus.IN_PROGRESS -> KakaoColors.Pantau
                        StepStatus.PENDING -> Color(0xFFE6EBE8)
                    }
                )
        ) {
            Text(
                text = when (status) {
                    StepStatus.DONE -> "✓"
                    StepStatus.IN_PROGRESS -> "•"
                    StepStatus.PENDING -> "○"
                },
                fontSize = 13.sp,
                color = if (status == StepStatus.PENDING) KakaoColors.TextMuted else Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (status == StepStatus.PENDING) FontWeight.Normal else FontWeight.SemiBold,
            fontFamily = PlusJakartaSansFontFamily,
            color = if (status == StepStatus.PENDING) KakaoColors.TextMuted else KakaoColors.TextPrimary
        )
    }
}

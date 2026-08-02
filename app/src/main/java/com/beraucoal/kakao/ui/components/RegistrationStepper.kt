package com.beraucoal.kakao.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beraucoal.kakao.ui.theme.*

/**
 * Header gabungan: Logo ITSB (Kiri) & Berau Coal (Kanan) + Stepper 4 Tahap dengan Validation Gating.
 * Ditampilkan sebagai topBar di setiap halaman registrasi (step 1–4).
 *
 * Menerapkan Validation Gating: pengguna TIDAK DAPAT melompat maju ke langkah di atas [maxReachedStep],
 * meniadakan kemungkinan melompat-lompat bebas sebelum form atau scan terselesaikan.
 */
@Composable
fun RegistrationStepperHeader(
    currentStep: Int,
    totalSteps: Int = 4,
    maxReachedStep: Int = currentStep,
    showBackButton: Boolean = true,
    onBackClicked: () -> Unit = {},
    showForwardButton: Boolean = false,
    onForwardClicked: () -> Unit = {},
    onStepClicked: (Int) -> Unit = {}
) {
    Surface(
        color = KakaoColors.Surface,
        shadowElevation = KakaoElevation.Medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // ── Logo Header (ITSB di Kiri, Berau Coal di Kanan, lebih besar & rapi) ──
            KakaoAppHeader()

            HorizontalDivider(
                color = KakaoColors.Divider,
                thickness = 1.dp
            )

            Spacer(Modifier.height(16.dp))

            // ── Step Circles & Navigation Arrows ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // Tombol Kembali ke Kiri
                if (showBackButton) {
                    IconButton(
                        onClick = onBackClicked,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(KakaoColors.SurfaceMuted)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke tahap sebelumnya",
                            tint = KakaoColors.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(40.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Stepper Circles di Tengah (Dengan Validation Gating)
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (step in 1..totalSteps) {
                        val isCompletedOrActive = step <= currentStep
                        val isActive = step == currentStep
                        val isUnlocked = step <= maxReachedStep // Gating check

                        val circleColor = when {
                            isActive -> KakaoColors.StepActive
                            isCompletedOrActive -> KakaoColors.StepCompleted
                            else -> KakaoColors.StepPending
                        }

                        val textColor = when {
                            isCompletedOrActive -> KakaoColors.Surface
                            else -> KakaoColors.StepTextInactive
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(circleColor)
                                .alpha(if (isUnlocked) 1f else 0.45f) // Indikasi visual langkah terkunci
                                .clickable(enabled = isUnlocked && !isActive) { 
                                    if (isUnlocked) onStepClicked(step) 
                                }
                        ) {
                            Text(
                                text = step.toString(),
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PoppinsFontFamily
                            )
                        }

                        if (step < totalSteps) {
                            HorizontalDivider(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 6.dp)
                                    .alpha(if (step < maxReachedStep) 1f else 0.45f),
                                color = if (step < currentStep) KakaoColors.StepCompleted else KakaoColors.StepPending,
                                thickness = 2.5.dp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Tombol Lanjut ke Kanan (Hanya aktif jika langkah berikutnya sudah pernah dicapai/diunlock)
                if (showForwardButton) {
                    IconButton(
                        onClick = onForwardClicked,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(KakaoColors.SurfaceMuted)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Lanjut ke tahap berikutnya",
                            tint = KakaoColors.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(40.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Step label badge ──
            val stepTitle = when (currentStep) {
                1 -> "Langkah 1 dari 4: Scan KTP"
                2 -> "Langkah 2 dari 4: Verifikasi Data"
                3 -> "Langkah 3 dari 4: OTP WhatsApp"
                4 -> "Langkah 4 dari 4: Mapping Kebun"
                else -> ""
            }

            if (stepTitle.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(KakaoRadius.Small),
                    color = KakaoColors.PrimaryContainer,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = stepTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PoppinsFontFamily,
                        color = KakaoColors.Primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

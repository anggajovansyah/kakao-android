package com.beraucoal.kakao.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beraucoal.kakao.RegistrationViewModel
import com.beraucoal.kakao.ui.components.KakaoContentCard
import com.beraucoal.kakao.ui.components.KakaoOutlinedButton
import com.beraucoal.kakao.ui.components.KakaoPrimaryButton
import com.beraucoal.kakao.ui.theme.*

@Composable
fun RegistrationDoneScreen(
    viewModel: RegistrationViewModel,
    onGoToDashboard: () -> Unit = {},
    onReturnToMenu: () -> Unit = {}
) {
    val registration by viewModel.registration.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KakaoColors.Background)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            KakaoContentCard {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(KakaoColors.PrimaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = KakaoColors.Primary,
                            modifier = Modifier.size(60.dp)
                        )
                    }

                    Spacer(Modifier.height(28.dp))

                    Text(
                        text = "Registrasi Berhasil!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = KakaoColors.TextPrimary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Selamat datang, ${registration.ktpData.nama.ifBlank { "Petani Kakao" }}",
                        style = MaterialTheme.typography.titleMedium,
                        color = KakaoColors.Primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(24.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = KakaoColors.SurfaceMuted,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp)
                        ) {
                            Text(
                                text = "ID PETANI TERDAFTAR",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = KakaoColors.TextSecondary,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = registration.petaniId ?: "-",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = PoppinsFontFamily,
                                color = KakaoColors.PrimaryDark
                            )
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    Text(
                        text = "Data kebun Anda kini telah resmi terhubung dengan sistem AI Kakao untuk monitoring lahan, analisis kesehatan pohon, dan rekomendasi pemupukan berkala.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KakaoColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(Modifier.height(36.dp))

                    // Tombol Utama: Masuk ke Beranda (Dashboard)
                    KakaoPrimaryButton(
                        text = "Masuk ke Beranda (Dashboard)",
                        onClick = onGoToDashboard,
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    // Tombol Sekunder: Kembali ke Menu Utama / Sesi Lainnya
                    KakaoOutlinedButton(
                        text = "Kembali ke Menu Utama",
                        onClick = onReturnToMenu,
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                tint = KakaoColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

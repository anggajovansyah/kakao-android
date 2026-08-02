package com.beraucoal.kakao.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
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
import com.beraucoal.kakao.data.VerificationStatus
import com.beraucoal.kakao.ui.components.KakaoAppHeader
import com.beraucoal.kakao.ui.components.KakaoContentCard
import com.beraucoal.kakao.ui.components.KakaoOutlinedButton
import com.beraucoal.kakao.ui.theme.*

/**
 * Halaman Beranda / Dashboard Utama setelah proses Sign In atau penyelesaian registrasi (Sign Up).
 * Menampilkan ringkasan profil petani, status verifikasi NocoBase/AI Kakao, informasi koordinat kebun,
 * serta menu awalan (empty state) fitur pemantauan lahan perkebunan.
 */
@Composable
fun DashboardScreen(
    viewModel: RegistrationViewModel,
    onLogout: () -> Unit
) {
    val registration by viewModel.registration.collectAsState()
    val ktp = registration.ktpData
    val kebun = registration.kebunLocation
    val nama = ktp.nama.ifBlank { "Budi Kakao Berau" }
    val idPetani = registration.petaniId ?: "PETANI-BERAU-MOCK01"
    val nomorWa = registration.nomorWhatsapp.ifBlank { "081234567890" }
    val isApproved = registration.verificationStatus == VerificationStatus.DISETUJUI || registration.otpVerified

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KakaoColors.Background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ── Top Bar / Header Identitas Kolaborasi ──
            Surface(
                color = KakaoColors.Surface,
                shadowElevation = KakaoElevation.Low,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    KakaoAppHeader()
                    HorizontalDivider(color = KakaoColors.Divider, thickness = 1.dp)
                }
            }

            // ── Isi Beranda (Scrollable) ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sapaan Selamat Datang
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(KakaoColors.PrimaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profil Pengguna",
                            tint = KakaoColors.Primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Halo, $nama",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = KakaoColors.TextPrimary
                        )
                        Text(
                            text = "ID: $idPetani",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = KakaoColors.TextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Hero Card: Status Verifikasi Akun ──
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = KakaoColors.PrimaryDark,
                    shadowElevation = KakaoElevation.Medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isApproved) KakaoColors.StepActive else KakaoColors.Warning
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isApproved) Icons.Filled.CheckCircle else Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = KakaoColors.Surface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (isApproved) "AKUN TERVERIFIKASI" else "STATUS PENDING",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = KakaoColors.Surface
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Text(
                            text = "Mitra Petani Kakao Digital",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = KakaoColors.Surface
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Terdaftar di wilayah operasional PT Berau Coal • Telepon / WhatsApp: $nomorWa",
                            style = MaterialTheme.typography.bodySmall,
                            color = KakaoColors.Surface.copy(alpha = 0.8f),
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Seksi Koordinat & Data Kebun ──
                KakaoContentCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Place,
                            contentDescription = null,
                            tint = KakaoColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Data Pemetaan Lahan Kebun",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KakaoColors.TextPrimary
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = KakaoColors.SurfaceMuted,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (kebun != null) {
                                Text(
                                    text = "Titik Koordinat Tersimpan:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KakaoColors.TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Lat: ${String.format("%.5f", kebun.latitude)}, Lng: ${String.format("%.5f", kebun.longitude)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = KakaoColors.PrimaryDark,
                                    fontFamily = PoppinsFontFamily
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Estimasi Luas: ${kebun.luasHektar ?: 1.5} Hektar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KakaoColors.TextPrimary
                                )
                            } else {
                                Text(
                                    text = "Lahan terdaftar secara offline / default",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KakaoColors.TextSecondary
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Koordinat Default: -2.15340, 117.48120",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = KakaoColors.PrimaryDark,
                                    fontFamily = PoppinsFontFamily
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Estimasi Luas: 2.0 Hektar (Wilayah Tanjung Redeb, Berau)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KakaoColors.TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Seksi Layanan AI & Eksplorasi (Empty State / Placeholder Modern) ──
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Layanan AI Kakao (Segera Hadir)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KakaoColors.TextPrimary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                    )

                    // Kartu Placeholder 1: Monitoring Kesehatan
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = KakaoColors.Surface,
                        shadowElevation = KakaoElevation.Low,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(KakaoColors.SurfaceMuted)
                            ) {
                                Text("🌱", fontSize = 20.sp)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Monitoring Kesehatan Pohon",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = KakaoColors.TextPrimary
                                )
                                Text(
                                    text = "Deteksi hama & penyakit daun melalui kamera pintar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KakaoColors.TextSecondary
                                )
                            }
                        }
                    }

                    // Kartu Placeholder 2: Pemupukan
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = KakaoColors.Surface,
                        shadowElevation = KakaoElevation.Low,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(KakaoColors.SurfaceMuted)
                            ) {
                                Text("⛅", fontSize = 20.sp)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Rekomendasi Pemupukan & Iklim",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = KakaoColors.TextPrimary
                                )
                                Text(
                                    text = "Jadwal pemeliharaan tanah disesuaikan cuaca lokal Berau",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KakaoColors.TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))

                // Tombol Keluar / Logout
                KakaoOutlinedButton(
                    text = "Keluar dari Sesi (Sign Out)",
                    onClick = onLogout,
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = KakaoColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "AI Kakao Digital • Sistem Operasi Lapangan PT Berau Coal",
                    style = MaterialTheme.typography.labelSmall,
                    color = KakaoColors.TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

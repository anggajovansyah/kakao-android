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
import com.beraucoal.kakao.ui.components.KakaoAppHeader
import com.beraucoal.kakao.ui.components.KakaoContentCard
import com.beraucoal.kakao.ui.components.KakaoOutlinedButton
import com.beraucoal.kakao.ui.theme.*

@Composable
fun ProfileScreen(
    viewModel: RegistrationViewModel,
    onLogout: () -> Unit
) {
    val registration by viewModel.registration.collectAsState()
    val ktp = registration.ktpData
    val kebun = registration.kebunLocation

    val nama = ktp.nama.ifBlank { "Budi Kakao Berau" }
    val nik = ktp.nik.ifBlank { "6403000000000001" }
    val idPetani = registration.petaniId ?: "PETANI-BERAU-01"
    val nomorWa = registration.nomorWhatsapp.ifBlank { "081234567890" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KakaoColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Avatar & Info Card
                KakaoContentCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(KakaoColors.PrimaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = KakaoColors.Primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = nama,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = KakaoColors.TextPrimary
                        )

                        Spacer(Modifier.height(4.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = KakaoColors.SurfaceMuted
                        ) {
                            Text(
                                text = "ID: $idPetani",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = KakaoColors.PrimaryDark,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        HorizontalDivider(color = KakaoColors.Divider)

                        Spacer(Modifier.height(16.dp))

                        ProfileDetailRow(icon = Icons.Filled.AccountBox, label = "NIK KTP", value = nik)
                        Spacer(Modifier.height(12.dp))
                        ProfileDetailRow(icon = Icons.Filled.Phone, label = "WhatsApp", value = nomorWa)
                        Spacer(Modifier.height(12.dp))
                        ProfileDetailRow(icon = Icons.Filled.Home, label = "Alamat", value = "${ktp.alamat.ifBlank { "Jl. Perkebunan Kakao No. 8" }}, ${ktp.kelurahanDesa.ifBlank { "Bedungun" }}")
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Detail Lahan & Kebun
                KakaoContentCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Place, contentDescription = null, tint = KakaoColors.Primary)
                        Spacer(Modifier.width(10.dp))
                        Text("Data Kebun Kakao Saya", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(12.dp))

                    if (kebun != null) {
                        Text("Koordinat: ${String.format("%.5f", kebun.latitude)}, ${String.format("%.5f", kebun.longitude)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = KakaoColors.PrimaryDark)
                        Text("Estimasi Luas: ${kebun.luasHektar ?: 2.5} Hektar", style = MaterialTheme.typography.bodySmall, color = KakaoColors.TextSecondary)
                    } else {
                        Text("Koordinat Default: -2.15340, 117.48120 (Bedungun)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = KakaoColors.PrimaryDark)
                        Text("Estimasi Luas: 2.5 Hektar", style = MaterialTheme.typography.bodySmall, color = KakaoColors.TextSecondary)
                    }
                }

                Spacer(Modifier.height(32.dp))

                KakaoOutlinedButton(
                    text = "Keluar dari Akun (Sign Out)",
                    onClick = onLogout,
                    icon = {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = KakaoColors.Primary, modifier = Modifier.size(20.dp))
                    }
                )

                Spacer(Modifier.height(80.dp)) // Extra padding so floating bottom bar doesn't overlap
            }
        }
    }
}

@Composable
private fun ProfileDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null, tint = KakaoColors.Primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = KakaoColors.TextSecondary)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = KakaoColors.TextPrimary)
        }
    }
}

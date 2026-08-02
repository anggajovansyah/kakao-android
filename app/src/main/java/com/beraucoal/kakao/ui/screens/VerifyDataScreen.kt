package com.beraucoal.kakao.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beraucoal.kakao.RegistrationViewModel
import com.beraucoal.kakao.UiState
import com.beraucoal.kakao.data.KtpData
import com.beraucoal.kakao.data.OcrConfidence
import com.beraucoal.kakao.data.VerificationStatus
import com.beraucoal.kakao.ui.components.*
import com.beraucoal.kakao.ui.theme.*

@Composable
fun VerifyDataScreen(
    viewModel: RegistrationViewModel,
    onApproved: () -> Unit
) {
    val registration by viewModel.registration.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val ktp = registration.ktpData

    var nik by remember(ktp) { mutableStateOf(ktp.nik) }
    var nama by remember(ktp) { mutableStateOf(ktp.nama) }
    var alamat by remember(ktp) { mutableStateOf(ktp.alamat) }
    var kelurahan by remember(ktp) { mutableStateOf(ktp.kelurahanDesa) }
    var kecamatan by remember(ktp) { mutableStateOf(ktp.kecamatan) }
    var nomorWhatsapp by remember { mutableStateOf(registration.nomorWhatsapp) }

    var hasSubmitted by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KakaoColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Text(
                text = "Verifikasi Data KTP",
                style = MaterialTheme.typography.headlineMedium,
                color = KakaoColors.TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Periksa & sesuaikan hasil pemindaian OCR KTP agar akurat dengan fisik kartu",
                style = MaterialTheme.typography.bodySmall,
                color = KakaoColors.TextSecondary,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(28.dp))

            // ── OCR Confidence Warning ──
            if (ktp.ocrConfidence != OcrConfidence.HIGH) {
                KakaoWarningBanner(
                    message = "Hasil pemindaian OCR belum yakin (Medium/Low). Mohon periksa & betulkan kolom yang kurang tepat."
                )
                Spacer(Modifier.height(24.dp))
            }

            // ── Raw OCR Text Toggle ──
            if (ktp.rawOcrText.isNotBlank()) {
                var showRawText by remember { mutableStateOf(false) }
                TextButton(
                    onClick = { showRawText = !showRawText },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (showRawText) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = KakaoColors.Primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (showRawText) "Sembunyikan teks OCR mentah" else "Lihat teks OCR mentah (debug)",
                        style = MaterialTheme.typography.bodySmall,
                        color = KakaoColors.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (showRawText) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(KakaoRadius.Small),
                        color = KakaoColors.SurfaceMuted,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = ktp.rawOcrText,
                            style = MaterialTheme.typography.labelSmall,
                            color = KakaoColors.TextSecondary,
                            modifier = Modifier.padding(18.dp),
                            lineHeight = 18.sp
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // ── Form Card (Dengan Whitespace Luas 20dp Antar Input) ──
            KakaoContentCard {
                KakaoOutlinedTextField(
                    value = nik,
                    onValueChange = { nik = it },
                    label = "NIK (16 Digit)",
                    leadingIcon = Icons.Filled.AccountBox
                )
                Spacer(Modifier.height(20.dp))

                KakaoOutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = "Nama Lengkap",
                    leadingIcon = Icons.Filled.Person
                )
                Spacer(Modifier.height(20.dp))

                KakaoOutlinedTextField(
                    value = alamat,
                    onValueChange = { alamat = it },
                    label = "Alamat Tempat Tinggal",
                    leadingIcon = Icons.Filled.Home,
                    singleLine = false
                )
                Spacer(Modifier.height(20.dp))

                KakaoOutlinedTextField(
                    value = kelurahan,
                    onValueChange = { kelurahan = it },
                    label = "Kelurahan / Desa",
                    leadingIcon = Icons.Filled.LocationOn
                )
                Spacer(Modifier.height(20.dp))

                KakaoOutlinedTextField(
                    value = kecamatan,
                    onValueChange = { kecamatan = it },
                    label = "Kecamatan",
                    leadingIcon = Icons.Filled.Place
                )
                Spacer(Modifier.height(20.dp))

                KakaoOutlinedTextField(
                    value = nomorWhatsapp,
                    onValueChange = { nomorWhatsapp = it },
                    label = "Nomor WhatsApp Aktif Petani",
                    leadingIcon = Icons.Filled.Phone
                )
            }

            Spacer(Modifier.height(36.dp))

            // ── Action / Status Area ──
            when {
                hasSubmitted && uiState is UiState.Loading -> {
                    Surface(
                        shape = RoundedCornerShape(KakaoRadius.Button),
                        color = KakaoColors.Surface,
                        shadowElevation = KakaoElevation.Low,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 20.dp, horizontal = 24.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = KakaoColors.Primary
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                "Menunggu verifikasi admin NocoBase...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                registration.verificationStatus == VerificationStatus.DISETUJUI -> {
                    Surface(
                        shape = RoundedCornerShape(KakaoRadius.Button),
                        color = KakaoColors.SuccessContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = KakaoColors.Primary)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Data disetujui admin!",
                                fontWeight = FontWeight.Bold,
                                color = KakaoColors.PrimaryDark
                            )
                        }
                    }
                }
                registration.verificationStatus == VerificationStatus.DITOLAK -> {
                    Surface(
                        shape = RoundedCornerShape(KakaoRadius.Button),
                        color = KakaoColors.ErrorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Data ditolak admin. Silakan periksa & sesuaikan kembali data KTP.",
                            color = KakaoColors.Error,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(20.dp),
                            lineHeight = 20.sp
                        )
                    }
                }
                else -> {
                    KakaoPrimaryButton(
                        text = "Konfirmasi & Verifikasi Data",
                        onClick = {
                            viewModel.updateKtpField {
                                KtpData(nik, nama, ktp.tempatTanggalLahir, alamat, kelurahan, kecamatan, ktp.rawOcrText, ktp.ocrConfidence)
                            }
                            viewModel.onWhatsappNumberChanged(nomorWhatsapp)
                            hasSubmitted = true
                            viewModel.submitForVerification(onDone = onApproved)
                        }
                    )
                }
            }

            if (uiState is UiState.Error) {
                Spacer(Modifier.height(16.dp))
                Text(
                    (uiState as UiState.Error).message,
                    color = KakaoColors.Error,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

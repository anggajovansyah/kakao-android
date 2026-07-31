package com.beraucoal.kakao.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.beraucoal.kakao.RegistrationViewModel
import com.beraucoal.kakao.UiState
import com.beraucoal.kakao.data.KtpData
import com.beraucoal.kakao.data.OcrConfidence
import com.beraucoal.kakao.data.VerificationStatus

/**
 * Tahap 2: Data KTP hasil OCR ditampilkan sebagai form yang BISA DIEDIT.
 * Ini penting -- jangan auto-submit langsung dari hasil OCR, terutama kalau
 * confidence LOW/MEDIUM, karena OCR KTP sering meleset di foto lapangan.
 * Setelah dikonfirmasi, data dikirim ke NocoBase dan menunggu status admin.
 */
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Verifikasi data KTP", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        if (ktp.ocrConfidence != OcrConfidence.HIGH) {
            AssistChip(
                onClick = {},
                label = { Text("Cek ulang -- hasil OCR belum yakin, mohon periksa manual") },
                leadingIcon = { Icon(Icons.Filled.Warning, contentDescription = null) }
            )
            Spacer(Modifier.height(12.dp))
        }

        if (ktp.rawOcrText.isNotBlank()) {
            var showRawText by remember { mutableStateOf(false) }
            TextButton(onClick = { showRawText = !showRawText }) {
                Text(if (showRawText) "Sembunyikan teks OCR mentah" else "Lihat teks OCR mentah (debug)")
            }
            if (showRawText) {
                Text(
                    ktp.rawOcrText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = nik, onValueChange = { nik = it },
            label = { Text("NIK") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = nama, onValueChange = { nama = it },
            label = { Text("Nama") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = alamat, onValueChange = { alamat = it },
            label = { Text("Alamat") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = kelurahan, onValueChange = { kelurahan = it },
            label = { Text("Kelurahan/Desa") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = kecamatan, onValueChange = { kecamatan = it },
            label = { Text("Kecamatan") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = nomorWhatsapp, onValueChange = { nomorWhatsapp = it },
            label = { Text("Nomor WhatsApp aktif") }, modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        when {
            hasSubmitted && uiState is UiState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Menunggu verifikasi admin...")
                }
            }
            registration.verificationStatus == VerificationStatus.DISETUJUI -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(Modifier.width(8.dp))
                    Text("Disetujui admin")
                }
            }
            registration.verificationStatus == VerificationStatus.DITOLAK -> {
                Text("Data ditolak admin. Silakan periksa kembali data KTP.", color = MaterialTheme.colorScheme.error)
            }
            hasSubmitted && registration.verificationStatus == VerificationStatus.PENDING -> {
                // Status masih pending setelah submit (admin belum memutuskan) --
                // sebelumnya kondisi ini tidak ditangani dan salah jatuh ke tombol
                // submit lagi. Data sudah terkirim, jadi tampilkan status menunggu
                // + tombol cek ulang manual, BUKAN tombol submit lagi.
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFB58900))
                        Spacer(Modifier.width(8.dp))
                        Text("Menunggu verifikasi admin. Data sudah terkirim.")
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.recheckVerificationStatus(onDone = onApproved) },
                        enabled = uiState !is UiState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState is UiState.Loading) "Mengecek..." else "Cek status lagi")
                    }
                }
            }
            else -> {
                Button(
                    onClick = {
                        viewModel.updateKtpField {
                            KtpData(nik, nama, ktp.tempatTanggalLahir, alamat, kelurahan, kecamatan, ktp.rawOcrText, ktp.ocrConfidence)
                        }
                        viewModel.onWhatsappNumberChanged(nomorWhatsapp)
                        hasSubmitted = true
                        viewModel.submitForVerification(onDone = onApproved)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kirim untuk verifikasi admin")
                }
            }
        }

        if (uiState is UiState.Error) {
            Spacer(Modifier.height(12.dp))
            Text((uiState as UiState.Error).message, color = MaterialTheme.colorScheme.error)
        }
    }
}

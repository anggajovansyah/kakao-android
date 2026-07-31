package com.beraucoal.kakao.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.beraucoal.kakao.RegistrationViewModel
import com.beraucoal.kakao.UiState

/**
 * Tahap 3: Kirim & verifikasi OTP lewat WhatsApp (Fonnte).
 * Kalau MOCK_FONNTE di RegistrationRepository masih true, kode "123456" selalu
 * valid; kalau sudah pakai Fonnte asli, kode OTP acak dan dikirim ke WhatsApp.
 */
@Composable
fun OtpScreen(
    viewModel: RegistrationViewModel,
    onVerified: () -> Unit
) {
    val registration by viewModel.registration.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var otpSent by remember { mutableStateOf(false) }
    var kode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Verifikasi nomor WhatsApp", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Kode OTP akan dikirim ke ${registration.nomorWhatsapp}")
        Spacer(Modifier.height(24.dp))

        if (!otpSent) {
            Button(
                onClick = { viewModel.sendOtp(onDone = { otpSent = true }) },
                enabled = uiState !is UiState.Loading
            ) {
                Text(if (uiState is UiState.Loading) "Mengirim..." else "Kirim kode OTP")
            }
        } else {
            OutlinedTextField(
                value = kode,
                onValueChange = { kode = it },
                label = { Text("Kode OTP (6 digit)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.verifyOtp(kode, onValid = onVerified) },
                enabled = kode.length == 6 && uiState !is UiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState is UiState.Loading) "Memverifikasi..." else "Verifikasi")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { viewModel.sendOtp(onDone = {}) },
                enabled = uiState !is UiState.Loading
            ) {
                Text("Kirim ulang kode")
            }
        }

        if (uiState is UiState.Error) {
            Spacer(Modifier.height(12.dp))
            Text((uiState as UiState.Error).message, color = MaterialTheme.colorScheme.error)
        }
    }
}

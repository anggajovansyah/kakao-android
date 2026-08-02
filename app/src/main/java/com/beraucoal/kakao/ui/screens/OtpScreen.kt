package com.beraucoal.kakao.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beraucoal.kakao.RegistrationViewModel
import com.beraucoal.kakao.UiState
import com.beraucoal.kakao.ui.components.*
import com.beraucoal.kakao.ui.theme.*

@Composable
fun OtpScreen(
    viewModel: RegistrationViewModel,
    onVerified: () -> Unit
) {
    val registration by viewModel.registration.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var otpSent by remember { mutableStateOf(false) }
    var kode by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KakaoColors.Background)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            KakaoContentCard {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // ── Icon Badge ──
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(KakaoColors.PrimaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = KakaoColors.Primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = "Verifikasi WhatsApp",
                        style = MaterialTheme.typography.headlineMedium,
                        color = KakaoColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Kode OTP keamanan akan dikirimkan langsung ke nomor WhatsApp petani:",
                        style = MaterialTheme.typography.bodySmall,
                        color = KakaoColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = KakaoColors.SurfaceMuted,
                        shadowElevation = KakaoElevation.None
                    ) {
                        Text(
                            text = registration.nomorWhatsapp.ifBlank { "Nomor WhatsApp belum diisi" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PoppinsFontFamily,
                            color = KakaoColors.PrimaryDark,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }

                    Spacer(Modifier.height(32.dp))

                    // ── OTP Actions ──
                    if (!otpSent) {
                        KakaoPrimaryButton(
                            text = "Kirim Kode OTP WhatsApp",
                            onClick = { viewModel.sendOtp(onDone = { otpSent = true }) },
                            isLoading = uiState is UiState.Loading,
                            loadingText = "Mengirim OTP..."
                        )
                    } else {
                        KakaoOutlinedTextField(
                            value = kode,
                            onValueChange = { if (it.length <= 6) kode = it },
                            label = "Kode OTP (6 Digit)",
                            leadingIcon = Icons.Filled.Lock,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            )
                        )

                        Spacer(Modifier.height(24.dp))

                        KakaoPrimaryButton(
                            text = "Verifikasi Kode OTP",
                            onClick = { viewModel.verifyOtp(kode, onValid = onVerified) },
                            enabled = kode.length == 6,
                            isLoading = uiState is UiState.Loading,
                            loadingText = "Memverifikasi..."
                        )

                        Spacer(Modifier.height(16.dp))

                        TextButton(
                            onClick = { viewModel.sendOtp(onDone = {}) }
                        ) {
                            Text(
                                "Kirim Ulang Kode OTP",
                                color = KakaoColors.Primary,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = PoppinsFontFamily,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // ── Error ──
                    if (uiState is UiState.Error) {
                        Spacer(Modifier.height(18.dp))
                        Text(
                            text = (uiState as UiState.Error).message,
                            color = KakaoColors.Error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

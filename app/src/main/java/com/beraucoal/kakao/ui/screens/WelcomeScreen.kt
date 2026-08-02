package com.beraucoal.kakao.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beraucoal.kakao.R
import com.beraucoal.kakao.RegistrationViewModel
import com.beraucoal.kakao.UiState
import com.beraucoal.kakao.ui.components.KakaoContentCard
import com.beraucoal.kakao.ui.components.KakaoOutlinedButton
import com.beraucoal.kakao.ui.components.KakaoOutlinedTextField
import com.beraucoal.kakao.ui.components.KakaoPrimaryButton
import com.beraucoal.kakao.ui.theme.*

/**
 * Layar Utama Terpadu (Unified Auth Screen).
 * Menampilkan logo kebanggaan AI Kakao berukuran besar di atas, langsung menyuguhkan formulir Sign In
 * (ID & Kata Sandi), pemisah "atau", tombol pendaftaran petani baru (Sign Up), serta footer 2 logo
 * (ITSB & PT Berau Coal) tanpa tulisan.
 */
@Composable
fun WelcomeScreen(
    viewModel: RegistrationViewModel,
    onSignUp: () -> Unit = {},
    onSignInSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // [TEMPORARY DEV BYPASS] Hardcoded credential default untuk kemudahan pengujian/testing.
    // UNTUK MENGHAPUS / PRODUKSI: Ubah "08123456789" dan "admin123" menjadi string kosong ("").
    var username by remember { mutableStateOf("08123456789") }
    var password by remember { mutableStateOf("admin123") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KakaoColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── BAGIAN ATAS: Logo AI Kakao Besar & Sapaan ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(Modifier.height(12.dp))
                // Logo AI Kakao dengan skala kebesaran layaknya di Splash Screen
                Surface(
                    shape = CircleShape,
                    color = KakaoColors.Surface,
                    shadowElevation = KakaoElevation.Medium,
                    modifier = Modifier.size(130.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.splash_icon),
                            contentDescription = "Logo AI Kakao",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(96.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Selamat Datang di AI Kakao",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PoppinsFontFamily,
                    color = KakaoColors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Platform Digital Registrasi & Pemetaan Lahan Kakao",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = PoppinsFontFamily,
                    color = KakaoColors.TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(28.dp))

                // ── FORM SIGN IN TERPADU (MASUK AKUN) ──
                KakaoContentCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Masuk ke Akun Terdaftar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KakaoColors.PrimaryDark,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        Spacer(Modifier.height(16.dp))

                        // Kolom Username / No WA
                        KakaoOutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                viewModel.clearError()
                            },
                            label = "No. WhatsApp atau ID Petani",
                            placeholder = "Contoh: 08123456789 atau PETANI-01",
                            leadingIcon = Icons.Filled.AccountBox,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            singleLine = true
                        )

                        Spacer(Modifier.height(16.dp))

                        // Kolom Kata Sandi
                        KakaoOutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                viewModel.clearError()
                            },
                            label = "Kata Sandi",
                            placeholder = "Masukkan kata sandi",
                            leadingIcon = Icons.Filled.Lock,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true
                        )

                        Spacer(Modifier.height(12.dp))

                        // Indikator Eror atau Loading
                        if (uiState is UiState.Error) {
                            Text(
                                text = (uiState as UiState.Error).message,
                                color = KakaoColors.Error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        if (uiState is UiState.Loading) {
                            CircularProgressIndicator(
                                color = KakaoColors.Primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                        }

                        // Tombol Masuk
                        KakaoPrimaryButton(
                            text = "Masuk Sekarang (Sign In)",
                            onClick = {
                                viewModel.signIn(username, password, onSuccess = onSignInSuccess)
                            },
                            enabled = uiState !is UiState.Loading
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // ── PEMISAH "ATAU" ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = KakaoColors.Divider,
                        thickness = 1.5.dp
                    )
                    Text(
                        text = "atau",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = KakaoColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = KakaoColors.Divider,
                        thickness = 1.5.dp
                    )
                }

                Spacer(Modifier.height(28.dp))

                // ── TOMBOL DAFTAR PETANI BARU (SIGN UP) ──
                KakaoOutlinedButton(
                    text = "Daftar Petani Baru (Sign Up)",
                    onClick = onSignUp,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.AddCircle,
                            contentDescription = null,
                            tint = KakaoColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }

            Spacer(Modifier.height(48.dp))

            // ── DASAR LAYAR: Logo Mitra (ITSB & Berau Coal) Tanpa Tulisan ──
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = KakaoColors.Surface.copy(alpha = 0.6f),
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    // Logo ITSB
                    Image(
                        painter = painterResource(id = R.drawable.itsb_logo),
                        contentDescription = "Logo ITSB",
                        modifier = Modifier.height(28.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.width(20.dp))
                    // Garis Pemisah Vertikal
                    Box(
                        modifier = Modifier
                            .height(22.dp)
                            .width(1.5.dp)
                            .background(KakaoColors.TextMuted.copy(alpha = 0.4f))
                    )
                    Spacer(Modifier.width(20.dp))
                    // Logo Berau Coal
                    Image(
                        painter = painterResource(id = R.drawable.berau_logo),
                        contentDescription = "Logo PT Berau Coal",
                        modifier = Modifier.height(28.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

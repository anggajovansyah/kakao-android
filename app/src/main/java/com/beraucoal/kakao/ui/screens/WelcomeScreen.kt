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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 * Layar Selamat Datang — redesign sesuai reference 2.0.
 *
 * Full-screen gradient hijau kanopi, ikon 🌱 besar, judul "AI Kakao",
 * dua tombol utama ("Daftar Baru" + "Sudah Punya Kode"),
 * chip partner "PT Berau Coal" + "ITSB".
 *
 * "Sudah Punya Kode" menampilkan form masuk (nama + kode petani)
 * sesuai reference, tapi juga tetap mendukung login lama (ID + password)
 * untuk backward compatibility.
 *
 * Reference: LAYAR.mulai di prototipe-aplikasi-petani.html
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
    var showLoginForm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        KakaoColors.Primary,        // #175c40
                        KakaoColors.PrimaryDark      // #0f3d2b
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── BAGIAN ATAS: Hero Section ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Emoji tanaman
                Text(
                    text = "🌱",
                    fontSize = 62.sp,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Judul
                Text(
                    text = "AI Kakao",
                    fontSize = 31.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSansFontFamily,
                    color = Color.White,
                    letterSpacing = (-0.03).sp
                )

                Spacer(Modifier.height(10.dp))

                // Deskripsi
                Text(
                    text = "Foto buah, batang, dan daun kakao Anda.\nKetahui kondisinya hari itu juga.",
                    fontSize = 14.5.sp,
                    fontFamily = PlusJakartaSansFontFamily,
                    color = Color(0xFFA9CDB9),
                    textAlign = TextAlign.Center,
                    lineHeight = 23.sp,
                    modifier = Modifier.padding(horizontal = 26.dp)
                )

                Spacer(Modifier.height(26.dp))

                // Chip partner
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PartnerChip("PT Berau Coal")
                    PartnerChip("ITSB")
                }
            }

            // ── BAGIAN BAWAH: Tombol Aksi ──
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                if (!showLoginForm) {
                    // Mode awal: dua tombol
                    Button(
                        onClick = onSignUp,
                        shape = RoundedCornerShape(KakaoRadius.Button),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = KakaoColors.PrimaryDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text(
                            text = "Daftar Baru",
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 15.5.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { showLoginForm = true },
                        shape = RoundedCornerShape(KakaoRadius.Button),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.45f),
                                    Color.White.copy(alpha = 0.45f)
                                )
                            )
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text(
                            text = "Sudah Punya Kode",
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 15.5.sp,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "Belum punya kode? Pilih Daftar Baru dan siapkan KTP.",
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaSansFontFamily,
                        color = Color(0xFF8FB3A2),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                } else {
                    // Mode login: form masuk
                    Surface(
                        shape = RoundedCornerShape(KakaoRadius.Card),
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Masuk",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = PlusJakartaSansFontFamily,
                                color = Color.White
                            )
                            Text(
                                text = "Gunakan nama dan kode yang tertera di kartu petani Anda.",
                                fontSize = 13.sp,
                                fontFamily = PlusJakartaSansFontFamily,
                                color = Color(0xFFA9CDB9),
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                            )

                            // Nama lengkap
                            OutlinedTextField(
                                value = username,
                                onValueChange = {
                                    username = it
                                    viewModel.clearError()
                                },
                                label = { Text("Nama lengkap / No. WA", color = Color(0xFF8FB3A2)) },
                                placeholder = { Text("Contoh: Dominikus Ambus", color = Color(0xFF5B7A6E)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                                    focusedBorderColor = Color.White.copy(alpha = 0.6f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    cursorColor = Color.White
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(KakaoRadius.Input)
                            )

                            Spacer(Modifier.height(12.dp))

                            // Kode petani / kata sandi
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    viewModel.clearError()
                                },
                                label = { Text("Kode petani / Kata sandi", color = Color(0xFF8FB3A2)) },
                                placeholder = { Text("PTN-00031-9", color = Color(0xFF5B7A6E)) },
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                                    focusedBorderColor = Color.White.copy(alpha = 0.6f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    cursorColor = Color.White
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(KakaoRadius.Input)
                            )

                            // Error message
                            if (uiState is UiState.Error) {
                                Text(
                                    text = (uiState as UiState.Error).message,
                                    color = Color(0xFFFF8A80),
                                    fontSize = 12.sp,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            // Tombol masuk
                            Button(
                                onClick = {
                                    viewModel.signIn(username, password, onSuccess = onSignInSuccess)
                                },
                                shape = RoundedCornerShape(KakaoRadius.Button),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = KakaoColors.PrimaryDark
                                ),
                                enabled = uiState !is UiState.Loading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                            ) {
                                if (uiState is UiState.Loading) {
                                    CircularProgressIndicator(
                                        color = KakaoColors.Primary,
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Masuk",
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 15.5.sp
                                    )
                                }
                            }

                            // Link daftar baru
                            TextButton(
                                onClick = onSignUp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "Belum punya kode? Daftar",
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 13.sp,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Tombol kembali
                    TextButton(
                        onClick = { showLoginForm = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "← Kembali",
                            color = Color(0xFF8FB3A2),
                            fontSize = 13.sp,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartnerChip(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSansFontFamily,
            color = Color(0xFFD8EBE0),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

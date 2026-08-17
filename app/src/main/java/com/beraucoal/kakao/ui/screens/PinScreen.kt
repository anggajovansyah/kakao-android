package com.beraucoal.kakao.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beraucoal.kakao.ui.theme.KakaoColors
import com.beraucoal.kakao.ui.theme.KakaoRadius
import com.beraucoal.kakao.ui.theme.KakaoSpacing
import com.beraucoal.kakao.ui.theme.PlusJakartaSansFontFamily
import kotlinx.coroutines.delay

/**
 * Layar Buat PIN — sesuai reference 2.0.
 *
 * PIN 6 angka untuk membuka aplikasi. Dijalankan hanya saat pertama kali
 * masuk (setelah login nama+kode berhasil). PIN yang mudah ditebak
 * (123456, angka kembar) ditolak.
 *
 * Reference: LAYAR.pin di prototipe-aplikasi-petani.html
 */
@Composable
fun PinScreen(
    onPinCreated: (String) -> Unit = {}
) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Auto-submit saat 6 digit tercapai
    LaunchedEffect(pin) {
        if (pin.length == 6) {
            delay(260)
            // Validasi PIN mudah ditebak
            val isRepeating = pin.all { it == pin[0] }  // 111111, 222222
            val isSequential = pin == "123456" || pin == "654321"
            val isTooSimple = isRepeating || isSequential

            if (isTooSimple) {
                errorMessage = "PIN terlalu mudah ditebak. Pilih angka lain."
                pin = ""
            } else {
                isProcessing = true
                delay(400)
                onPinCreated(pin)
            }
        } else {
            errorMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KakaoColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = KakaoSpacing.ScreenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(34.dp))

            // Icon
            Text(
                text = "🔐",
                fontSize = 40.sp,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // Title
            Text(
                text = "Buat PIN",
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSansFontFamily,
                color = KakaoColors.TextPrimary,
                letterSpacing = (-0.028).sp
            )

            // Subtitle
            Text(
                text = "Enam angka untuk membuka aplikasi.\nJangan pakai tanggal lahir.",
                fontSize = 13.sp,
                fontFamily = PlusJakartaSansFontFamily,
                color = KakaoColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.5.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            // Error message
            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(KakaoRadius.Small),
                    color = KakaoColors.ErrorContainer
                ) {
                    Text(
                        text = errorMessage!!,
                        fontSize = 12.5.sp,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = KakaoColors.Kritis,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            // PIN dots
            Spacer(Modifier.height(26.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(6) { index ->
                    val isFilled = index < pin.length
                    val bgColor by animateColorAsState(
                        targetValue = if (isFilled) KakaoColors.Primary else Color.Transparent,
                        animationSpec = tween(150),
                        label = "PinDot$index"
                    )
                    val borderColor = if (isFilled) KakaoColors.Primary else KakaoColors.Primary

                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                            .then(
                                if (!isFilled) Modifier.background(
                                    Color.Transparent,
                                    CircleShape
                                ) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isFilled) {
                            Box(
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(CircleShape)
                                    .background(Color.Transparent)
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color.Transparent)
                                ) {}
                            }
                            // Outline ring
                            Surface(
                                shape = CircleShape,
                                color = Color.Transparent,
                                border = ButtonDefaults.outlinedButtonBorder,
                                modifier = Modifier.size(15.dp)
                            ) {}
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Custom numpad
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(11.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                // Rows: 1-9
                for (row in listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        row.forEach { number ->
                            NumPadKey(
                                label = number.toString(),
                                onClick = {
                                    if (pin.length < 6 && !isProcessing) {
                                        pin += number.toString()
                                    }
                                }
                            )
                        }
                    }
                }
                // Bottom row: empty, 0, backspace
                Row(
                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    // Empty spacer
                    Box(modifier = Modifier.size(width = 96.dp, height = 56.dp))

                    NumPadKey(
                        label = "0",
                        onClick = {
                            if (pin.length < 6 && !isProcessing) {
                                pin += "0"
                            }
                        }
                    )

                    // Backspace
                    NumPadKey(
                        label = "⌫",
                        isAction = true,
                        onClick = {
                            if (pin.isNotEmpty() && !isProcessing) {
                                pin = pin.dropLast(1)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NumPadKey(
    label: String,
    isAction: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isAction) Color.Transparent else KakaoColors.Surface,
        border = if (isAction) null else ButtonDefaults.outlinedButtonBorder,
        modifier = Modifier
            .size(width = 96.dp, height = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                fontSize = if (isAction) 17.sp else 21.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSansFontFamily,
                color = KakaoColors.TextPrimary
            )
        }
    }
}

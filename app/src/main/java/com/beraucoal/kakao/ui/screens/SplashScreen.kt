package com.beraucoal.kakao.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beraucoal.kakao.R
import kotlinx.coroutines.delay

private val SplashGreenDark = Color(0xFF15351F)

/**
 * Splash screen sesuai desain: latar hijau tua, kotak putih berisi ikon
 * ranting daun, nama app + subjudul, garis pemisah, label "Didukung oleh"
 * dengan badge logo ITSB & Berau, progress bar, teks status memuat, dan
 * footer versi. Tampil ~2 detik lalu otomatis pindah ke layar scan KTP.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1600, easing = LinearEasing),
        label = "splash_progress"
    )

    LaunchedEffect(Unit) {
        progress = 1f
        delay(2000)
        onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashGreenDark)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_leaf_sprig),
                    contentDescription = "Ikon AI Kakao",
                    modifier = Modifier.size(60.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "AI Kakao",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "SISTEM DIAGNOSIS PENYAKIT KAKAO",
                fontSize = 12.sp,
                letterSpacing = 1.5.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalDivider(width = 48.dp)
            Spacer(Modifier.height(20.dp))
            Text(
                "DIDUKUNG OLEH",
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LogoBadge { Image(painterResource(id = R.drawable.itsb_logo), "Logo ITSB", Modifier.height(20.dp)) }
                LogoBadge { Image(painterResource(id = R.drawable.berau_logo), "Logo Berau", Modifier.height(20.dp)) }
            }

            Spacer(Modifier.height(28.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text("Memuat sistem...", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))

            Spacer(Modifier.height(24.dp))
            Text(
                "v1.0.0 \u00B7 ITSB LP3B",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
private fun HorizontalDivider(width: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.25f))
    )
}

@Composable
private fun LogoBadge(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

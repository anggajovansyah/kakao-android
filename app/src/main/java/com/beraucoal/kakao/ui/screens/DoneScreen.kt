package com.beraucoal.kakao.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.beraucoal.kakao.RegistrationViewModel

/**
 * Tahap 5: Registrasi selesai. petaniId di sini yang jadi kunci penghubung
 * ke sistem AI Kakao (monitoring, analisis, rekomendasi per kebun).
 */
@Composable
fun RegistrationDoneScreen(viewModel: RegistrationViewModel) {
    val registration by viewModel.registration.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF2E7D32),
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("Registrasi selesai", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Selamat datang, ${registration.ktpData.nama}")
        Spacer(Modifier.height(4.dp))
        Text("ID Petani: ${registration.petaniId ?: "-"}")
        Spacer(Modifier.height(16.dp))
        Text(
            "Data kebun kamu sekarang terhubung dengan sistem AI Kakao untuk monitoring, " +
                "analisis penyakit, dan rekomendasi perawatan.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

package com.beraucoal.kakao.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.beraucoal.kakao.RegistrationViewModel
import com.beraucoal.kakao.UiState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

/**
 * Tahap 4: Mapping kebun -- menghubungkan petani dengan lokasi kebunnya.
 * Petani tap titik di peta (jadi acuan pin lokasi) atau tombol "gunakan lokasi saya"
 * kalau sedang berdiri di kebun. Luas lahan diisi manual dulu (estimasi kasar);
 * kalau nanti perlu polygon batas kebun yang presisi, tambahkan drawing tool terpisah.
 */
@Composable
fun KebunMappingScreen(
    viewModel: RegistrationViewModel,
    onSubmitted: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Default kamera: area operasi PT. Berau Coal, Kalimantan Timur -- sesuaikan kalau perlu
    val defaultLocation = LatLng(2.1500, 117.4667)
    var pinnedLocation by remember { mutableStateOf<LatLng?>(null) }
    var luasHektarText by remember { mutableStateOf("") }
    var locationError by remember { mutableStateOf<String?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Mapping kebun",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(24.dp, 24.dp, 24.dp, 8.dp)
        )
        Text(
            "Ketuk peta di lokasi kebun, atau gunakan lokasi GPS kalau sedang di lapangan.",
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Box(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                onMapClick = { latLng -> pinnedLocation = latLng }
            ) {
                pinnedLocation?.let { loc ->
                    Marker(state = MarkerState(position = loc), title = "Lokasi kebun")
                }
            }
        }

        Column(modifier = Modifier.padding(24.dp)) {
            if (hasLocationPermission) {
                OutlinedButton(
                    onClick = {
                        locationError = null
                        try {
                            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                            fusedClient.lastLocation
                                .addOnSuccessListener { loc ->
                                    if (loc != null) {
                                        val latLng = LatLng(loc.latitude, loc.longitude)
                                        pinnedLocation = latLng
                                        cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 16f)
                                    } else {
                                        locationError = "Lokasi belum tersedia -- coba lagi di area terbuka " +
                                            "atau pastikan GPS aktif, lalu tap manual di peta kalau perlu."
                                    }
                                }
                                .addOnFailureListener { e ->
                                    locationError = "Gagal ambil lokasi: ${e.message}"
                                }
                        } catch (e: SecurityException) {
                            // Izin lokasi ternyata sudah dicabut sejak layar ini dibuka
                            // (mis. lewat Settings sistem) -- jangan sampai app crash.
                            locationError = "Izin lokasi tidak aktif. Aktifkan lewat pengaturan aplikasi, " +
                                "atau tap manual di peta."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Gunakan lokasi saya saat ini")
                }
                locationError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = luasHektarText,
                onValueChange = { luasHektarText = it },
                label = { Text("Estimasi luas kebun (hektar, opsional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val loc = pinnedLocation ?: return@Button
                    val luas = luasHektarText.toDoubleOrNull()
                    viewModel.submitKebun(loc.latitude, loc.longitude, luas, onDone = onSubmitted)
                },
                enabled = pinnedLocation != null && uiState !is UiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState is UiState.Loading) "Menyimpan..." else "Simpan lokasi kebun")
            }

            if (uiState is UiState.Error) {
                Spacer(Modifier.height(12.dp))
                Text((uiState as UiState.Error).message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

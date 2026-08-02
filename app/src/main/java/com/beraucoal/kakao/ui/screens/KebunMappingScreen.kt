package com.beraucoal.kakao.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.beraucoal.kakao.RegistrationViewModel
import com.beraucoal.kakao.UiState
import com.beraucoal.kakao.ui.components.*
import com.beraucoal.kakao.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun KebunMappingScreen(
    viewModel: RegistrationViewModel,
    onSubmitted: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val defaultLocation = LatLng(2.1500, 117.4667)
    var pinnedLocation by remember { mutableStateOf<LatLng?>(null) }
    var luasHektarText by remember { mutableStateOf("") }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KakaoColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Title ──
            Column(
                modifier = Modifier.padding(
                    horizontal = 24.dp,
                    vertical = 20.dp
                )
            ) {
                Text(
                    text = "Mapping Kebun Kakao",
                    style = MaterialTheme.typography.headlineMedium,
                    color = KakaoColors.TextPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Ketuk area peta untuk menentukan titik koordinat kebun atau gunakan sensor GPS langsung di lapangan",
                    style = MaterialTheme.typography.bodySmall,
                    color = KakaoColors.TextSecondary,
                    lineHeight = 20.sp
                )
            }

            // ── Map Container (Dengan Margin & Rounded Shape) ──
            Surface(
                shape = RoundedCornerShape(24.dp),
                shadowElevation = KakaoElevation.Medium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                    onMapClick = { latLng -> pinnedLocation = latLng }
                ) {
                    pinnedLocation?.let { loc ->
                        Marker(
                            state = MarkerState(position = loc),
                            title = "Lokasi Kebun Kakao"
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Bottom Form Card (Whitespace Luas & Nyaman) ──
            Card(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = KakaoColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = KakaoElevation.High),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 28.dp)) {
                    if (hasLocationPermission) {
                        KakaoOutlinedButton(
                            text = "Gunakan Lokasi GPS Saya Saat Ini",
                            onClick = {
                                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                                fusedClient.lastLocation.addOnSuccessListener { loc ->
                                    if (loc != null) {
                                        val latLng = LatLng(loc.latitude, loc.longitude)
                                        pinnedLocation = latLng
                                        cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 16f)
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = KakaoColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        Spacer(Modifier.height(20.dp))
                    }

                    KakaoOutlinedTextField(
                        value = luasHektarText,
                        onValueChange = { luasHektarText = it },
                        label = "Estimasi Luas Kebun (Hektar, Opsional)",
                        leadingIcon = Icons.Filled.Place
                    )

                    Spacer(Modifier.height(24.dp))

                    KakaoPrimaryButton(
                        text = if (pinnedLocation != null) "Simpan & Selesaikan Registrasi" else "Pilih Pin Lokasi di Peta",
                        onClick = {
                            val loc = pinnedLocation ?: return@KakaoPrimaryButton
                            val luas = luasHektarText.toDoubleOrNull()
                            viewModel.submitKebun(loc.latitude, loc.longitude, luas, onDone = onSubmitted)
                        },
                        enabled = pinnedLocation != null,
                        isLoading = uiState is UiState.Loading,
                        loadingText = "Menyimpan...",
                        icon = {
                            Icon(
                                Icons.Filled.Place,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )

                    if (uiState is UiState.Error) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            (uiState as UiState.Error).message,
                            color = KakaoColors.Error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

package com.beraucoal.kakao.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beraucoal.kakao.data.*
import com.beraucoal.kakao.ui.components.PlantationBottomSheet
import com.beraucoal.kakao.ui.theme.KakaoColors
import com.beraucoal.kakao.ui.theme.KakaoElevation
import com.beraucoal.kakao.ui.theme.PoppinsFontFamily
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun SatelliteMapScreen(
    plantationRepository: PlantationRepository = remember { PlantationRepository() }
) {
    val kebunList by plantationRepository.kebunList.collectAsState()

    var mapType by remember { mutableStateOf(MapType.SATELLITE) }
    var selectedKebun by remember { mutableStateOf<KebunArea?>(null) }
    var selectedBlok by remember { mutableStateOf<BlokBagian?>(null) }
    var selectedPohon by remember { mutableStateOf<PohonKakao?>(null) }

    var filterStatus by remember { mutableStateOf<PohonStatus?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    val defaultLocation = LatLng(2.15340, 117.48120) // Berau
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 14f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Google Map View (Satelit / Hybrid Interaktif) ──
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = mapType,
                isMyLocationEnabled = false
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = true,
                mapToolbarEnabled = false
            )
        ) {
            kebunList.forEach { kebun ->
                // Render Boundary Polygon Kebun
                Polygon(
                    points = kebun.polygon,
                    fillColor = Color(0x33F1C40F), // Translucent Gold Fill
                    strokeColor = Color(0xFFF1C40F),
                    strokeWidth = 5f,
                    clickable = true,
                    onClick = {
                        selectedKebun = kebun
                        selectedBlok = null
                        selectedPohon = null
                        showBottomSheet = true
                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(kebun.centerLocation, 16f))
                    }
                )

                // Render Sub-Blok Polygons
                kebun.blokList.forEach { blok ->
                    Polygon(
                        points = blok.polygon,
                        fillColor = Color(0x222ECC71), // Translucent Emerald Fill
                        strokeColor = Color(0xFF2ECC71),
                        strokeWidth = 3f,
                        clickable = true,
                        onClick = {
                            selectedKebun = kebun
                            selectedBlok = blok
                            selectedPohon = null
                            showBottomSheet = true
                        }
                    )

                    // Render Markers Pohon Kakao saat Zoom Dekat (Zoom level >= 15f)
                    if (cameraPositionState.position.zoom >= 15f) {
                        val treesToRender = if (filterStatus != null) {
                            blok.pohonList.filter { it.status == filterStatus }
                        } else {
                            blok.pohonList
                        }

                        treesToRender.forEach { pohon ->
                            val markerHue = when (pohon.status) {
                                PohonStatus.SEHAT -> BitmapDescriptorFactory.HUE_GREEN
                                PohonStatus.PERLU_PUPUK -> BitmapDescriptorFactory.HUE_YELLOW
                                PohonStatus.TERSERANG_HAMA -> BitmapDescriptorFactory.HUE_RED
                            }

                            Marker(
                                state = MarkerState(position = pohon.location),
                                title = "Pohon ${pohon.kodePohon} (${pohon.status.label})",
                                snippet = "Varietas ${pohon.varietasKakao} • Pemupukan: ${pohon.tanggalPemupukanTerakhir}",
                                icon = BitmapDescriptorFactory.defaultMarker(markerHue),
                                onClick = {
                                    selectedKebun = kebun
                                    selectedBlok = blok
                                    selectedPohon = pohon
                                    showBottomSheet = true
                                    true
                                }
                            )
                        }
                    }
                }
            }
        }

        // ── Top Header Control & Search Bar ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = KakaoColors.Surface.copy(alpha = 0.92f),
                shadowElevation = KakaoElevation.Medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = KakaoColors.Primary)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = selectedKebun?.namaKebun ?: "Peta Satelit Lahan Kakao Berau",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = KakaoColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    // Map Type Toggle Button (Satelit vs Hybrid vs Normal)
                    IconButton(
                        onClick = {
                            mapType = when (mapType) {
                                MapType.SATELLITE -> MapType.HYBRID
                                MapType.HYBRID -> MapType.NORMAL
                                else -> MapType.SATELLITE
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Layers, contentDescription = "Ganti Tipe Peta", tint = KakaoColors.Primary)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Quick Filter Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = filterStatus == null,
                    onClick = { filterStatus = null },
                    label = { Text("Semua Pohon", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                FilterChip(
                    selected = filterStatus == PohonStatus.TERSERANG_HAMA,
                    onClick = { filterStatus = if (filterStatus == PohonStatus.TERSERANG_HAMA) null else PohonStatus.TERSERANG_HAMA },
                    label = { Text("⚠️ Terserang Hama", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                FilterChip(
                    selected = filterStatus == PohonStatus.PERLU_PUPUK,
                    onClick = { filterStatus = if (filterStatus == PohonStatus.PERLU_PUPUK) null else PohonStatus.PERLU_PUPUK },
                    label = { Text("⚡ Perlu Pupuk", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // ── Floating Action Buttons (Gps Location & Inspector Toggle) ──
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 90.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    val target = selectedKebun?.centerLocation ?: defaultLocation
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(target, 16f))
                },
                containerColor = KakaoColors.Surface,
                contentColor = KakaoColors.Primary,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = "Lokasi Lahan")
            }

            Spacer(Modifier.height(10.dp))

            FloatingActionButton(
                onClick = {
                    if (selectedKebun == null && kebunList.isNotEmpty()) {
                        selectedKebun = kebunList.first()
                    }
                    showBottomSheet = true
                },
                containerColor = KakaoColors.Primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Inspeksi Lahan", fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = PoppinsFontFamily)
                }
            }
        }

        // ── Modal Bottom Sheet Inspector ──
        if (showBottomSheet && selectedKebun != null) {
            PlantationBottomSheet(
                kebun = selectedKebun!!,
                selectedBlok = selectedBlok,
                selectedPohon = selectedPohon,
                onBlokSelected = { selectedBlok = it },
                onPohonSelected = { selectedPohon = it },
                onStatusChange = { pohon, newStatus ->
                    selectedKebun?.let { k ->
                        selectedBlok?.let { b ->
                            plantationRepository.updatePohonStatus(k.id, b.id, pohon.id, newStatus)
                        }
                    }
                },
                onDismiss = { showBottomSheet = false }
            )
        }
    }
}

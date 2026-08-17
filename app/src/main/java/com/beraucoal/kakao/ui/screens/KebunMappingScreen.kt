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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.beraucoal.kakao.RegistrationViewModel
import com.beraucoal.kakao.UiState
import com.beraucoal.kakao.ui.components.*
import com.beraucoal.kakao.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker as OsmMarker

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

    var mapCenterTrigger by remember { mutableStateOf<Pair<GeoPoint, Double>?>(null) }
    
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        mapCenterTrigger = Pair(GeoPoint(defaultLocation.latitude, defaultLocation.longitude), 12.0)
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
                    text = "Ketuk area peta atau geser (drag) pin untuk menentukan titik koordinat secara manual tanpa perlu berjalan ke lokasi, atau gunakan sensor GPS saat berada di lapangan",
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
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setMultiTouchControls(true)
                            val tileSource = object : OnlineTileSourceBase(
                                "EsriWorldImagery", 1, 19, 256, ".png",
                                arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
                            ) {
                                override fun getTileURLString(pMapTileIndex: Long): String {
                                    return baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" + MapTileIndex.getY(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex)
                                }
                            }
                            setTileSource(tileSource)
                        }
                    },
                    update = { mapView ->
                        mapCenterTrigger?.let { (center, zoom) ->
                            mapView.controller.setZoom(zoom)
                            mapView.controller.animateTo(center)
                            mapCenterTrigger = null
                        }
                        
                        val mReceive = object : org.osmdroid.views.overlay.Overlay() {
                            override fun onSingleTapConfirmed(e: android.view.MotionEvent, mapView: MapView): Boolean {
                                val proj = mapView.projection
                                val loc = proj.fromPixels(e.x.toInt(), e.y.toInt())
                                pinnedLocation = LatLng(loc.latitude, loc.longitude)
                                return true
                            }
                        }
                        
                        mapView.overlays.clear()
                        mapView.overlays.add(mReceive)
                        
                        pinnedLocation?.let { loc ->
                            val m = OsmMarker(mapView)
                            m.position = GeoPoint(loc.latitude, loc.longitude)
                            m.title = "Lokasi Kebun Kakao"
                            m.snippet = "Tahan & geser (drag) pin untuk ubah posisi manual"
                            m.isDraggable = true
                            m.setOnMarkerDragListener(object : OsmMarker.OnMarkerDragListener {
                                override fun onMarkerDrag(marker: OsmMarker?) {}
                                override fun onMarkerDragEnd(marker: OsmMarker?) {
                                    marker?.let {
                                        pinnedLocation = LatLng(it.position.latitude, it.position.longitude)
                                    }
                                }
                                override fun onMarkerDragStart(marker: OsmMarker?) {}
                            })
                            mapView.overlays.add(m)
                        }
                        mapView.invalidate()
                    }
                )
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
                                        mapCenterTrigger = Pair(GeoPoint(latLng.latitude, latLng.longitude), 16.0)
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

                    Spacer(Modifier.height(16.dp))
                    if (pinnedLocation != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = KakaoColors.SurfaceMuted,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Titik Koordinat Terekam:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = KakaoColors.PrimaryDark)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("Mode: Manual/Drag", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KakaoColors.Primary)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    String.format("Latitude: %.6f\nLongitude: %.6f", pinnedLocation!!.latitude, pinnedLocation!!.longitude),
                                    fontSize = 12.sp,
                                    color = KakaoColors.TextPrimary,
                                    fontFamily = PoppinsFontFamily
                                )
                                Text("Tip: Anda dapat meletakkan & menggeser (drag) pin di atas peta secara manual tanpa harus berjalan ke lokasi.", fontSize = 11.sp, color = KakaoColors.TextSecondary, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    } else {
                        Spacer(Modifier.height(8.dp))
                    }

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

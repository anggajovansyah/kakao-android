package com.beraucoal.kakao.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.beraucoal.kakao.data.PlantationRepository
import com.beraucoal.kakao.ui.components.KakaoOutlinedTextField
import com.beraucoal.kakao.ui.theme.KakaoColors
import com.beraucoal.kakao.ui.theme.PoppinsFontFamily
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.google.maps.android.compose.*

/**
 * Layar Pemetaan Mandiri Petani (Alur UX 3 - v2).
 * Menyediakan 5 tahapan alur lengkap:
 * Step 1: Tutorial pemetaan keliling
 * Step 2: Form profil lahan
 * Step 3: Mode pemetaan live poligon dengan deteksi self-intersection dan live hectare counter
 * Step 4: Review poligon & deteksi overlap lahan eksisting
 * Step 5: Penyimpanan draf lokal (Offline-First sync)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfMappingScreen(
    repository: PlantationRepository,
    onFinished: () -> Unit,
    onCancel: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) } // 1: Tutorial, 2: Form, 3: Map, 4: Review, 5: Done
    
    // Form States
    var namaKebun by remember { mutableStateOf("") }
    var namaPemilik by remember { mutableStateOf("Petani Terverifikasi Berau") }
    var nomorWa by remember { mutableStateOf("081234567890") }
    var catatan by remember { mutableStateOf("Pemetaan mandiri via aplikasi mobile") }

    // GIS States
    val boundaryPoints = remember { mutableStateListOf<LatLng>() }
    var calculatedHectares by remember { mutableDoubleStateOf(0.0) }
    var isOverlapping by remember { mutableStateOf(false) }
    var intersectionErrorMsg by remember { mutableStateOf<String?>(null) }

    // Camera & GPS
    val defaultCenter = LatLng(2.15340, 117.48120) // Berau default coordinate
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCenter, 16f)
    }

    // Live area calculations & intersection checks
    LaunchedEffect(boundaryPoints.size) {
        if (boundaryPoints.size >= 3) {
            val areaSqMeters = SphericalUtil.computeArea(boundaryPoints)
            calculatedHectares = (areaSqMeters / 10000.0 * 100.0).toInt() / 100.0
            isOverlapping = repository.checkOverlap(boundaryPoints)
        } else {
            calculatedHectares = 0.0
            isOverlapping = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Pemetaan Kebun Mandiri (v2)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = PoppinsFontFamily
                        )
                        Text(
                            text = "Langkah $currentStep dari 5",
                            style = MaterialTheme.typography.bodySmall,
                            color = KakaoColors.TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (currentStep > 1) currentStep-- else onCancel() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KakaoColors.Surface)
            )
        },
        containerColor = KakaoColors.Background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (currentStep) {
                1 -> TutorialStepContent(onNext = { currentStep = 2 })
                2 -> FormInfoStepContent(
                    namaKebun = namaKebun, onNamaChange = { namaKebun = it },
                    namaPemilik = namaPemilik, onPemilikChange = { namaPemilik = it },
                    nomorWa = nomorWa, onWaChange = { nomorWa = it },
                    catatan = catatan, onCatatanChange = { catatan = it },
                    onNext = { currentStep = 3 },
                    isValid = namaKebun.isNotBlank()
                )
                3 -> LiveMapMappingContent(
                    cameraPositionState = cameraPositionState,
                    points = boundaryPoints,
                    calculatedHectares = calculatedHectares,
                    intersectionError = intersectionErrorMsg,
                    onClearError = { intersectionErrorMsg = null },
                    onAddPoint = { latLng ->
                        val candidate = boundaryPoints + latLng
                        if (PlantationRepository.hasSelfIntersection(candidate)) {
                            intersectionErrorMsg = "Peringatan: Titik ini menyebabkan garis batas saling bersilangan! Harap tandai berurutan tanpa menyilang."
                        } else {
                            intersectionErrorMsg = null
                            boundaryPoints.add(latLng)
                        }
                    },
                    onPointDrag = { index, newLatLng ->
                        if (index in 0 until boundaryPoints.size) {
                            val candidate = boundaryPoints.toMutableList()
                            candidate[index] = newLatLng
                            if (PlantationRepository.hasSelfIntersection(candidate)) {
                                intersectionErrorMsg = "Peringatan: Posisi geser ini menyebabkan garis batas saling bersilangan!"
                            } else {
                                intersectionErrorMsg = null
                                boundaryPoints[index] = newLatLng
                            }
                        }
                    },
                    onUndo = {
                        if (boundaryPoints.isNotEmpty()) {
                            boundaryPoints.removeAt(boundaryPoints.lastIndex)
                            intersectionErrorMsg = null
                        }
                    },
                    onNext = { currentStep = 4 }
                )
                4 -> ReviewPolygonContent(
                    points = boundaryPoints,
                    namaKebun = namaKebun,
                    luasHektar = calculatedHectares,
                    isOverlapping = isOverlapping,
                    onEdit = { currentStep = 3 },
                    onConfirm = {
                        repository.addSelfMappedKebun(
                            namaKebun = namaKebun,
                            namaPemilik = namaPemilik,
                            nomorWa = nomorWa,
                            luasHektar = calculatedHectares,
                            polygon = boundaryPoints.toList(),
                            catatan = catatan
                        )
                        currentStep = 5
                    }
                )
                5 -> CompletionStepContent(
                    namaKebun = namaKebun,
                    luasHektar = calculatedHectares,
                    onFinish = onFinished
                )
            }
        }
    }
}

@Composable
private fun TutorialStepContent(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                "Panduan Pemetaan Mandiri",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = KakaoColors.PrimaryDark,
                fontFamily = PoppinsFontFamily
            )
            Spacer(Modifier.height(16.dp))

            TutorialCard(
                icon = Icons.Filled.Place,
                title = "1. Opsi Fleksibel: GPS atau Manual (Tanpa Jalan)",
                description = "Anda bisa keliling kebun dengan GPS, ATAU langsung tandai dan geser (drag) titik secara manual di peta satelit tanpa perlu berjalan!"
            )
            Spacer(Modifier.height(12.dp))
            TutorialCard(
                icon = Icons.Filled.LocationOn,
                title = "2. Tandai & Drag Batas Lahan",
                description = "Ketuk peta untuk menambah titik, atau tahan & geser (drag) pin untuk mengoreksi posisi secara akurat. Pasang titik berurutan tanpa menyilang."
            )
            Spacer(Modifier.height(12.dp))
            TutorialCard(
                icon = Icons.Filled.CheckCircle,
                title = "3. Kalkulasi Hektar Otomatis",
                description = "Setelah minimal 3 titik tertutup, sistem geodesik langsung menghitung luas lahan dalam Hektar secara real-time."
            )
        }

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = KakaoColors.Primary),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Mulai Pemetaan →", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun TutorialCard(icon: ImageVector, title: String, description: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = KakaoColors.Surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = KakaoColors.PrimaryDark,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = KakaoColors.TextPrimary)
                Text(description, fontSize = 13.sp, color = KakaoColors.TextSecondary, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun FormInfoStepContent(
    namaKebun: String, onNamaChange: (String) -> Unit,
    namaPemilik: String, onPemilikChange: (String) -> Unit,
    nomorWa: String, onWaChange: (String) -> Unit,
    catatan: String, onCatatanChange: (String) -> Unit,
    onNext: () -> Unit,
    isValid: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Profil Lahan Kebun",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = KakaoColors.PrimaryDark,
                fontFamily = PoppinsFontFamily
            )
            Text(
                "Lengkapi identitas kepemilikan sebelum menggambar poligon di peta.",
                style = MaterialTheme.typography.bodyMedium,
                color = KakaoColors.TextSecondary
            )
            Spacer(Modifier.height(20.dp))

            KakaoOutlinedTextField(
                value = namaKebun,
                onValueChange = onNamaChange,
                label = "Nama Perkebunan * (Wajib)"
            )
            Spacer(Modifier.height(12.dp))
            KakaoOutlinedTextField(
                value = namaPemilik,
                onValueChange = onPemilikChange,
                label = "Nama Pemilik Lahan"
            )
            Spacer(Modifier.height(12.dp))
            KakaoOutlinedTextField(
                value = nomorWa,
                onValueChange = onWaChange,
                label = "Nomor WhatsApp Kontak"
            )
            Spacer(Modifier.height(12.dp))
            KakaoOutlinedTextField(
                value = catatan,
                onValueChange = onCatatanChange,
                label = "Catatan Tambahan / Status Tanah"
            )
        }

        Button(
            onClick = onNext,
            enabled = isValid,
            colors = ButtonDefaults.buttonColors(containerColor = KakaoColors.Primary),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Lanjut ke Peta →", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun LiveMapMappingContent(
    cameraPositionState: CameraPositionState,
    points: List<LatLng>,
    calculatedHectares: Double,
    intersectionError: String?,
    onClearError: () -> Unit,
    onAddPoint: (LatLng) -> Unit,
    onPointDrag: (Int, LatLng) -> Unit,
    onUndo: () -> Unit,
    onNext: () -> Unit
) {
    var mapType by remember { mutableStateOf(MapType.HYBRID) }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = mapType, isMyLocationEnabled = false),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
            onMapClick = { latLng -> onAddPoint(latLng) }
        ) {
            // Gambar Polygon jika titik >= 3
            if (points.size >= 3) {
                Polygon(
                    points = points,
                    strokeColor = Color(0xFFFFEB3B),
                    strokeWidth = 6f,
                    fillColor = Color(0xFF2E7D32).copy(alpha = 0.35f)
                )
            } else if (points.size == 2) {
                Polyline(
                    points = points,
                    color = Color(0xFFFFEB3B),
                    width = 6f
                )
            }

            // Render Marker Tiap Sudut (Draggable)
            points.forEachIndexed { index, pt ->
                val isFirst = (index == 0)
                val markerState = remember(index) { MarkerState(position = pt) }
                LaunchedEffect(pt) {
                    if (markerState.position != pt) markerState.position = pt
                }
                LaunchedEffect(markerState.position) {
                    if (index < points.size && points[index] != markerState.position) {
                        onPointDrag(index, markerState.position)
                    }
                }
                Marker(
                    state = markerState,
                    title = if (isFirst) "Titik #1 (Awal - Draggable)" else "Titik #${index + 1} (Draggable)",
                    snippet = "Tahan & geser (drag) untuk mengubah posisi tanpa berjalan",
                    draggable = true
                )
            }
        }

        // ── Top Bar Counter & Instruction ──
        Surface(
            color = KakaoColors.Surface.copy(alpha = 0.95f),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 4.dp,
            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Titik Batas: ${points.size} Titik terekam akurat", fontWeight = FontWeight.ExtraBold, color = KakaoColors.TextPrimary)
                        Text("Luas Estimasi Geodetik: $calculatedHectares Hektar", fontWeight = FontWeight.Bold, color = KakaoColors.Primary, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tip: Tahan & geser (drag) pin pada peta untuk mengoreksi letak batas secara manual tanpa harus bergeser di lapangan.",
                    fontSize = 11.sp,
                    color = KakaoColors.TextSecondary,
                    lineHeight = 15.sp
                )
            }
        }

        // ── Snackbar Alert Self-Intersection ──
        if (intersectionError != null) {
            Surface(
                color = KakaoColors.Error,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gagal Menambah Titik", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text(intersectionError, color = Color.White, fontSize = 12.sp)
                    }
                    IconButton(onClick = onClearError) {
                        Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }
            }
        }

        // ── Bottom Action Panel ──
        Surface(
            color = KakaoColors.Surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            shadowElevation = 8.dp,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            // Tandai dari posisi tengah layar kamera
                            onAddPoint(cameraPositionState.position.target)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KakaoColors.Primary),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tandai di Pusat", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onUndo,
                        enabled = points.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = KakaoColors.Warning),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Undo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onNext,
                    enabled = points.size >= 3,
                    colors = ButtonDefaults.buttonColors(containerColor = KakaoColors.StepActive),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Selesai & Review Poligon (${points.size} Titik) →", fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ReviewPolygonContent(
    points: List<LatLng>,
    namaKebun: String,
    luasHektar: Double,
    isOverlapping: Boolean,
    onEdit: () -> Unit,
    onConfirm: () -> Unit
) {
    val center = if (points.isNotEmpty()) {
        LatLng(points.map { it.latitude }.average(), points.map { it.longitude }.average())
    } else LatLng(2.1534, 117.4812)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, 16f)
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("Review Konfirmasi Poligon", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = KakaoColors.TextPrimary, fontFamily = PoppinsFontFamily)
            Text("Periksa dengan teliti batas lahan sebelum disalin ke dalam penyimpanan offline.", style = MaterialTheme.typography.bodySmall, color = KakaoColors.TextSecondary)

            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp))) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(mapType = MapType.HYBRID),
                    uiSettings = MapUiSettings(zoomControlsEnabled = false, scrollGesturesEnabled = false)
                ) {
                    if (points.size >= 3) {
                        Polygon(
                            points = points,
                            strokeColor = if (isOverlapping) Color.Red else Color(0xFFFFEB3B),
                            strokeWidth = 6f,
                            fillColor = if (isOverlapping) Color.Red.copy(alpha = 0.4f) else Color(0xFF2E7D32).copy(alpha = 0.4f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = KakaoColors.SurfaceMuted, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Nama Lahan: $namaKebun", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Jumlah Titik Batas: ${points.size} Koordinat", fontSize = 13.sp, color = KakaoColors.TextSecondary)
                    Text("Luas Hasil Komputasi Geodetik: $luasHektar Hektar", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = KakaoColors.Primary)
                }
            }

            if (isOverlapping) {
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(14.dp), color = KakaoColors.Error.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = KakaoColors.Error)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Peringatan Tumpang Tindih (Overlap)!", fontWeight = FontWeight.Bold, color = KakaoColors.Error, fontSize = 13.sp)
                            Text("Lahan ini terdeteksi beririsan dengan kebun lain yang sudah terverifikasi. Tetap dapat disimpan ke draf untuk direview oleh petugas lapangan.", fontSize = 11.sp, color = KakaoColors.TextPrimary)
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp)) {
                Text("Edit Titik", fontWeight = FontWeight.Bold, color = KakaoColors.PrimaryDark)
            }
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = KakaoColors.Primary), modifier = Modifier.weight(1.5f).height(52.dp), shape = RoundedCornerShape(14.dp)) {
                Text("Simpan ke Draf →", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CompletionStepContent(
    namaKebun: String,
    luasHektar: Double,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = KakaoColors.StepActive.copy(alpha = 0.2f),
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = KakaoColors.StepActive, modifier = Modifier.size(64.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Pemetaan Lahan Berhasil!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, fontFamily = PoppinsFontFamily)
        Spacer(Modifier.height(8.dp))
        Text(
            "Data kebun '$namaKebun' ($luasHektar Ha) telah tersimpan aman secara offline dan sedang disinkronkan ke server.",
            style = MaterialTheme.typography.bodyMedium,
            color = KakaoColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))
        Surface(shape = RoundedCornerShape(14.dp), color = KakaoColors.WarningContainer, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Status: ⏳ Menunggu Verifikasi Lapangan", fontWeight = FontWeight.Bold, color = KakaoColors.WarningText, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(36.dp))
        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(containerColor = KakaoColors.Primary),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Kembali ke Peta Satelit", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

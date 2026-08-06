package com.beraucoal.kakao.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.launch

data class MapSearchResult(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val targetLatLng: LatLng,
    val associatedKebun: KebunArea? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatelliteMapScreen(
    plantationRepository: PlantationRepository = remember { PlantationRepository() },
    onNavigateToSelfMapping: () -> Unit = {}
) {
    val kebunList by plantationRepository.kebunList.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var mapType by remember { mutableStateOf(MapType.SATELLITE) }
    var selectedKebun by remember { mutableStateOf<KebunArea?>(null) }
    var selectedBlok by remember { mutableStateOf<BlokBagian?>(null) }
    var selectedPohon by remember { mutableStateOf<PohonKakao?>(null) }
    var filterStatus by remember { mutableStateOf<PohonStatus?>(null) }

    val activeKebun = selectedKebun ?: kebunList.firstOrNull()

    // Search State & Real-Time GIS Matching
    var searchQuery by remember { mutableStateOf("") }
    val searchResults = remember(searchQuery, kebunList) {
        if (searchQuery.trim().isEmpty()) {
            emptyList()
        } else {
            val query = searchQuery.trim().lowercase()
            val results = mutableListOf<MapSearchResult>()

            // 1. Pencarian berdasarkan Data Perkebunan & Petani Berau Coal
            for (kebun in kebunList) {
                if (kebun.namaKebun.lowercase().contains(query) ||
                    kebun.namaPemilik.lowercase().contains(query) ||
                    kebun.idPetani.lowercase().contains(query) ||
                    kebun.nomorWa.contains(query)) {
                    results.add(
                        MapSearchResult(
                            title = kebun.namaKebun,
                            subtitle = "Pemilik: ${kebun.namaPemilik} • ${kebun.luasHektar} Ha",
                            icon = Icons.Filled.Place,
                            targetLatLng = kebun.centerLocation,
                            associatedKebun = kebun
                        )
                    )
                }
                for (blok in kebun.blokList) {
                    if (blok.namaBlok.lowercase().contains(query) || blok.kodeBlok.lowercase().contains(query)) {
                        results.add(
                            MapSearchResult(
                                title = "${blok.namaBlok} (${kebun.namaKebun})",
                                subtitle = "Populasi: ${blok.totalPohon} pohon (${blok.pohonSehatCount} sehat)",
                                icon = Icons.Filled.Info,
                                targetLatLng = kebun.centerLocation,
                                associatedKebun = kebun
                            )
                        )
                    }
                }
            }

            // 2. Pencarian berdasarkan Wilayah & Kecamatan GIS di Kabupaten Berau
            val berauDistricts = listOf(
                Triple("Tanjung Redeb (Pusat Berau)", "Kecamatan Tanjung Redeb, Kab. Berau", LatLng(2.1500, 117.4667)),
                Triple("Sambaliung", "Kecamatan Sambaliung, Sentra Lahan Kakao", LatLng(2.1333, 117.4833)),
                Triple("Teluk Bayur", "Kecamatan Teluk Bayur, Pertanian Kakao", LatLng(2.1167, 117.4167)),
                Triple("Gunung Tabur", "Kecamatan Gunung Tabur, Berau", LatLng(2.1667, 117.4500)),
                Triple("Kelay", "Kecamatan Kelay, Pedalaman Berau", LatLng(1.8000, 117.2000)),
                Triple("Segah", "Kecamatan Segah, Perkebunan & Kehutanan", LatLng(2.0500, 117.2500)),
                Triple("Talisayan", "Kecamatan Talisayan, Pesisir Berau", LatLng(1.5667, 118.0667))
            )
            for (district in berauDistricts) {
                if (district.first.lowercase().contains(query) || district.second.lowercase().contains(query)) {
                    results.add(
                        MapSearchResult(
                            title = district.first,
                            subtitle = "Wilayah GIS: ${district.second}",
                            icon = Icons.Filled.LocationOn,
                            targetLatLng = district.third,
                            associatedKebun = null
                        )
                    )
                }
            }

            // 3. Pencarian langsung berdasarkan Koordinat GPS
            val coordParts = query.replace(" ", "").split(",")
            if (coordParts.size == 2) {
                val lat = coordParts[0].toDoubleOrNull()
                val lng = coordParts[1].toDoubleOrNull()
                if (lat != null && lng != null) {
                    results.add(
                        MapSearchResult(
                            title = "Koordinat GPS: $lat, $lng",
                            subtitle = "Lompat langsung ke titik koordinat ini di peta",
                            icon = Icons.Filled.Map,
                            targetLatLng = LatLng(lat, lng),
                            associatedKebun = null
                        )
                    )
                }
            }
            results
        }
    }

    val defaultLocation = LatLng(2.15340, 117.48120)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(activeKebun?.centerLocation ?: defaultLocation, 16.5f)
    }

    // Fluid BottomSheetScaffold State dengan Proteksi Batas Bawah (skipHiddenState = true)
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = if (activeKebun != null) 380.dp else 0.dp,
        sheetContainerColor = KakaoColors.Surface,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetShadowElevation = KakaoElevation.High,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            if (activeKebun != null) {
                PlantationBottomSheet(
                    kebun = activeKebun,
                    selectedBlok = selectedBlok,
                    selectedPohon = selectedPohon,
                    onBlokSelected = { blok ->
                        selectedBlok = blok
                        selectedPohon = null
                        if (blok != null) {
                            coroutineScope.launch {
                                val center = if (blok.polygon.isNotEmpty()) {
                                    val avgLat = blok.polygon.map { it.latitude }.average()
                                    val avgLng = blok.polygon.map { it.longitude }.average()
                                    LatLng(avgLat, avgLng)
                                } else {
                                    activeKebun.centerLocation
                                }
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(center, 18.5f), 800)
                            }
                        } else {
                            coroutineScope.launch {
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(activeKebun.centerLocation, 16.5f), 800)
                            }
                        }
                    },
                    onPohonSelected = { pohon ->
                        selectedPohon = pohon
                        if (pohon != null) {
                            coroutineScope.launch {
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pohon.location, 20.0f), 800)
                            }
                        } else if (selectedBlok != null) {
                            coroutineScope.launch {
                                val center = if (selectedBlok!!.polygon.isNotEmpty()) {
                                    val avgLat = selectedBlok!!.polygon.map { it.latitude }.average()
                                    val avgLng = selectedBlok!!.polygon.map { it.longitude }.average()
                                    LatLng(avgLat, avgLng)
                                } else {
                                    activeKebun.centerLocation
                                }
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(center, 18.5f), 800)
                            }
                        }
                    },
                    onStatusChange = { pohon, newStatus ->
                        val targetBlok = selectedBlok ?: activeKebun.blokList.firstOrNull()
                        if (targetBlok != null) {
                            plantationRepository.updatePohonStatus(activeKebun.id, targetBlok.id, pohon.id, newStatus)
                        }
                    },
                    onAddPohon = { blok, newPohon ->
                        plantationRepository.addPohonToBlok(activeKebun.id, blok.id, newPohon)
                    },
                    onSelfMappingClick = onNavigateToSelfMapping
                )
            }
        }
    ) { _ ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Google Map View
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
                ),
                onMapClick = {
                    selectedPohon = null
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            ) {
                kebunList.forEach { kebun ->
                    val isSelectedKebun = activeKebun?.id == kebun.id
                    val isPending = (kebun.verificationStatus == KebunVerificationStatus.PENDING_REVIEW)
                    val strokeColor = when {
                        isSelectedKebun -> KakaoColors.PrimaryDark
                        isPending -> Color(0xFFF57F17)
                        else -> Color(0xFFF1C40F)
                    }
                    val fillColor = when {
                        isSelectedKebun -> KakaoColors.PrimaryDark.copy(alpha = 0.25f)
                        isPending -> Color(0x33F57F17)
                        else -> Color(0x33F1C40F)
                    }

                    Polygon(
                        points = kebun.polygon,
                        fillColor = fillColor,
                        strokeColor = strokeColor,
                        strokeWidth = if (isSelectedKebun) 6f else 4f,
                        clickable = true,
                        onClick = {
                            selectedKebun = kebun
                            selectedBlok = null
                            selectedPohon = null
                            coroutineScope.launch {
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(kebun.centerLocation, 16.5f), 800)
                                if (scaffoldState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded) {
                                    scaffoldState.bottomSheetState.expand()
                                }
                            }
                        }
                    )

                    kebun.blokList.forEach { blok ->
                        val isSelectedBlok = selectedBlok?.id == blok.id
                        Polygon(
                            points = blok.polygon,
                            fillColor = if (isSelectedBlok) Color(0x442ECC71) else Color(0x222ECC71),
                            strokeColor = Color(0xFF2ECC71),
                            strokeWidth = if (isSelectedBlok) 5f else 3f,
                            clickable = true,
                            onClick = {
                                selectedKebun = kebun
                                selectedBlok = blok
                                selectedPohon = null
                                coroutineScope.launch {
                                    val center = if (blok.polygon.isNotEmpty()) {
                                        val avgLat = blok.polygon.map { it.latitude }.average()
                                        val avgLng = blok.polygon.map { it.longitude }.average()
                                        LatLng(avgLat, avgLng)
                                    } else {
                                        kebun.centerLocation
                                    }
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(center, 18.5f), 800)
                                    if (scaffoldState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded) {
                                        // Tetap di posisi default agar informatif & rapi
                                    }
                                }
                            }
                        )

                        if (selectedBlok?.id == blok.id) {
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
                                        true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (selectedBlok != null) {
                // Header ala TransJakarta ("Detail Halte / Detail Bus")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = KakaoColors.Surface,
                        shadowElevation = KakaoElevation.Medium,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (selectedPohon != null) {
                                    selectedPohon = null
                                    selectedBlok?.let { blok ->
                                        coroutineScope.launch {
                                            val center = if (blok.polygon.isNotEmpty()) {
                                                val avgLat = blok.polygon.map { it.latitude }.average()
                                                val avgLng = blok.polygon.map { it.longitude }.average()
                                                LatLng(avgLat, avgLng)
                                            } else {
                                                activeKebun?.centerLocation ?: LatLng(0.0, 0.0)
                                            }
                                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(center, 18.5f), 800)
                                        }
                                    }
                                } else {
                                    selectedBlok = null
                                    selectedPohon = null
                                    activeKebun?.let {
                                        coroutineScope.launch {
                                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it.centerLocation, 16.5f), 800)
                                        }
                                    }
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali ke Daftar Sub-Sektor",
                                tint = KakaoColors.PrimaryDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = KakaoColors.Surface,
                        shadowElevation = KakaoElevation.Medium,
                        modifier = Modifier
                            .height(44.dp)
                            .weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val titleText = if (selectedPohon != null) "Detail Pohon ${selectedPohon!!.kodePohon}" else "Detail Sub-Sektor ${selectedBlok!!.kodeBlok}"
                            val badgeText = if (selectedPohon != null) selectedPohon!!.status.label else "${selectedBlok!!.totalPohon} Pohon"
                            val badgeColor = when {
                                selectedPohon?.status == PohonStatus.SEHAT -> KakaoColors.StepActive
                                selectedPohon?.status == PohonStatus.PERLU_PUPUK -> KakaoColors.Warning
                                selectedPohon?.status == PohonStatus.TERSERANG_HAMA -> KakaoColors.Error
                                else -> KakaoColors.PrimaryDark
                            }
                            val badgeBg = when {
                                selectedPohon?.status == PohonStatus.SEHAT -> KakaoColors.StepActive.copy(alpha = 0.12f)
                                selectedPohon?.status == PohonStatus.PERLU_PUPUK -> KakaoColors.Warning.copy(alpha = 0.15f)
                                selectedPohon?.status == PohonStatus.TERSERANG_HAMA -> KakaoColors.Error.copy(alpha = 0.15f)
                                else -> KakaoColors.PrimaryDark.copy(alpha = 0.1f)
                            }

                            Text(
                                text = titleText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = KakaoColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 6.dp)
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = badgeBg
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Top Bar: Minimalist Search Bar ala TransJakarta
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = KakaoColors.Surface,
                    shadowElevation = KakaoElevation.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Cari kebun, petani, blok, atau lokasi...",
                                style = MaterialTheme.typography.bodySmall,
                                color = KakaoColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "Cari",
                                tint = KakaoColors.PrimaryDark,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Hapus",
                                        tint = KakaoColors.TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                if (searchResults.isNotEmpty()) {
                                    val first = searchResults.first()
                                    if (first.associatedKebun != null) {
                                        selectedKebun = first.associatedKebun
                                    }
                                    coroutineScope.launch {
                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(first.targetLatLng, 16.5f), 1000)
                                    }
                                }
                            }
                        )
                    )
                }

                // Autocomplete Results (Bersih & Tanpa Emoji)
                AnimatedVisibility(
                    visible = searchQuery.trim().isNotEmpty() && searchResults.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = KakaoColors.Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .heightIn(max = 260.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
                            items(searchResults) { result ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            searchQuery = ""
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                            if (result.associatedKebun != null) {
                                                selectedKebun = result.associatedKebun
                                            }
                                            coroutineScope.launch {
                                                cameraPositionState.animate(
                                                    CameraUpdateFactory.newLatLngZoom(result.targetLatLng, 16.5f),
                                                    1000
                                                )
                                                if (result.associatedKebun != null) {
                                                    scaffoldState.bottomSheetState.expand()
                                                }
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = result.icon,
                                        contentDescription = null,
                                        tint = KakaoColors.PrimaryDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = result.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = KakaoColors.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = result.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = KakaoColors.TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                HorizontalDivider(color = KakaoColors.Divider, thickness = 0.8.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
            }

            // Map Controls Vertikal di Sisi Kanan (Ala Aplikasi TransJakarta)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 396.dp)
            ) {
                // Layer Toggle Button
                Surface(
                    shape = CircleShape,
                    color = KakaoColors.Surface,
                    shadowElevation = KakaoElevation.Medium,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable {
                            mapType = when (mapType) {
                                MapType.SATELLITE -> MapType.HYBRID
                                MapType.HYBRID -> MapType.NORMAL
                                else -> MapType.SATELLITE
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Layers,
                            contentDescription = "Ganti Mode Peta",
                            tint = KakaoColors.PrimaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // GPS Center Location Button
                Surface(
                    shape = CircleShape,
                    color = KakaoColors.Surface,
                    shadowElevation = KakaoElevation.Medium,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable {
                            activeKebun?.let {
                                coroutineScope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it.centerLocation, 17f), 800)
                                }
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = "Pusat Kebun",
                            tint = KakaoColors.PrimaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

package com.beraucoal.kakao.ui.screens

// KML overlay support

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
import com.beraucoal.kakao.data.KmlParser
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beraucoal.kakao.data.*
import com.beraucoal.kakao.ui.components.PlantationBottomSheet
import com.beraucoal.kakao.ui.theme.KakaoColors
import androidx.compose.material.icons.filled.Satellite
import com.beraucoal.kakao.ui.theme.KakaoElevation
import com.beraucoal.kakao.ui.theme.PoppinsFontFamily
import com.google.android.gms.maps.model.LatLng
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.toArgb
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon as OsmPolygon
import org.osmdroid.views.overlay.Marker as OsmMarker
import kotlinx.coroutines.launch

data class MapSearchResult(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val targetLatLng: LatLng,
    val associatedKebun: KebunArea? = null,
    val associatedBlok: BlokBagian? = null,
    val associatedPohon: PohonKakao? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatelliteMapScreen(
    plantationRepository: PlantationRepository = remember { PlantationRepository() },
    onNavigateToSelfMapping: () -> Unit = {},
    onReportKondisi: (PohonKakao) -> Unit = {}
) {
    val kebunList by plantationRepository.kebunList.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    var mapType by remember { mutableStateOf(0) }
    var selectedKebun by remember { mutableStateOf<KebunArea?>(null) }
    var selectedBlok by remember { mutableStateOf<BlokBagian?>(null) }
    var selectedPohon by remember { mutableStateOf<PohonKakao?>(null) }
    var filterStatus by remember { mutableStateOf<PohonStatus?>(null) }

    // ── KML Overlay (Sentinel-2 jadwal akuisisi) ──
    var showKmlOverlay by remember { mutableStateOf(true) }
    val kmlData = remember {
        KmlParser.parseFromAssets(context, "kml/sentinel2_schedule.kml")
    }

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
                                associatedKebun = kebun,
                                associatedBlok = blok
                            )
                        )
                    }

                    // 1b. Pencarian berdasarkan Kode Pohon / ID Pohon
                    for (pohon in blok.pohonList) {
                        if (pohon.kodePohon.lowercase().contains(query) ||
                            pohon.id.lowercase().contains(query)) {
                            results.add(
                                MapSearchResult(
                                    title = pohon.kodePohon,
                                    subtitle = "${pohon.status.label} • ${blok.namaBlok} (${kebun.namaKebun})",
                                    icon = Icons.Filled.LocationOn,
                                    targetLatLng = LatLng(pohon.location.latitude, pohon.location.longitude),
                                    associatedKebun = kebun,
                                    associatedBlok = blok,
                                    associatedPohon = pohon
                                )
                            )
                        }
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
    var mapCenterTrigger by remember { mutableStateOf<Pair<GeoPoint, Double>?>(null) }
    
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        mapCenterTrigger = Pair(
            GeoPoint(
                activeKebun?.centerLocation?.latitude ?: defaultLocation.latitude,
                activeKebun?.centerLocation?.longitude ?: defaultLocation.longitude
            ),
            16.5
        )
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
                    onReportKondisi = onReportKondisi,
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
                                mapCenterTrigger = Pair(GeoPoint(center.latitude, center.longitude), 18.5.toDouble())
                            }
                        } else {
                            coroutineScope.launch {
                                mapCenterTrigger = Pair(GeoPoint(activeKebun.centerLocation.latitude, activeKebun.centerLocation.longitude), 16.5.toDouble())
                            }
                        }
                    },
                    onPohonSelected = { pohon ->
                        selectedPohon = pohon
                        if (pohon != null) {
                            coroutineScope.launch {
                                mapCenterTrigger = Pair(GeoPoint(pohon.location.latitude, pohon.location.longitude), 20.0.toDouble())
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
                                mapCenterTrigger = Pair(GeoPoint(center.latitude, center.longitude), 18.5.toDouble())
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
            // OSMDroid Map View
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setMultiTouchControls(true)
                        
                        val tileSource = object : OnlineTileSourceBase(
                            "EsriWorldImagery",
                            1, 19, 256, ".png",
                            arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
                        ) {
                            override fun getTileURLString(pMapTileIndex: Long): String {
                                return baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" + MapTileIndex.getY(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex)
                            }
                        }
                        setTileSource(tileSource)
                        
                        // Default center
                        controller.setZoom(16.5)
                        controller.setCenter(GeoPoint(
                            activeKebun?.centerLocation?.latitude ?: defaultLocation.latitude,
                            activeKebun?.centerLocation?.longitude ?: defaultLocation.longitude
                        ))
                    }
                },
                update = { mapView ->
                    mapCenterTrigger?.let { (center, zoom) ->
                        mapView.controller.animateTo(center, zoom, 800L)
                        mapCenterTrigger = null
                    }
                    
                    mapView.overlays.clear()
                    
                    // KML Overlay
                    if (showKmlOverlay && kmlData.berauPlacemarks.isNotEmpty()) {
                        kmlData.berauPlacemarks.forEach { placemark ->
                            val style = kmlData.styles[placemark.styleId]
                            val strokeArgb = style?.lineColor ?: 0xFF00FF00
                            val fillArgb = style?.fillColor ?: 0x4000FF00
                            
                            val p = OsmPolygon().apply {
                                points = placemark.polygon.map { GeoPoint(it.latitude, it.longitude) }
                                fillPaint.color = fillArgb.toInt()
                                outlinePaint.color = strokeArgb.toInt()
                                outlinePaint.strokeWidth = style?.lineWidth ?: 2f
                            }
                            mapView.overlays.add(p)
                        }
                    }
                    
                    // Kebun & Blok Polygons
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
                        
                        val pKebun = OsmPolygon().apply {
                            points = kebun.polygon.map { GeoPoint(it.latitude, it.longitude) }
                            fillPaint.color = fillColor.toArgb()
                            outlinePaint.color = strokeColor.toArgb()
                            outlinePaint.strokeWidth = if (isSelectedKebun) 6f else 4f
                            setOnClickListener { _, _, _ ->
                                selectedKebun = kebun
                                selectedBlok = null
                                selectedPohon = null
                                mapCenterTrigger = Pair(GeoPoint(kebun.centerLocation.latitude, kebun.centerLocation.longitude), 16.5)
                                true // handled
                            }
                        }
                        mapView.overlays.add(pKebun)
                        
                        kebun.blokList.forEach { blok ->
                            val isSelectedBlok = selectedBlok?.id == blok.id
                            val pBlok = OsmPolygon().apply {
                                points = blok.polygon.map { GeoPoint(it.latitude, it.longitude) }
                                fillPaint.color = if (isSelectedBlok) Color(0x442ECC71).toArgb() else Color(0x222ECC71).toArgb()
                                outlinePaint.color = Color(0xFF2ECC71).toArgb()
                                outlinePaint.strokeWidth = if (isSelectedBlok) 5f else 3f
                                setOnClickListener { _, _, _ ->
                                    selectedKebun = kebun
                                    selectedBlok = blok
                                    selectedPohon = null
                                    
                                    val avgLat = if(blok.polygon.isNotEmpty()) blok.polygon.map{it.latitude}.average() else kebun.centerLocation.latitude
                                    val avgLng = if(blok.polygon.isNotEmpty()) blok.polygon.map{it.longitude}.average() else kebun.centerLocation.longitude
                                    mapCenterTrigger = Pair(GeoPoint(avgLat, avgLng), 18.5)
                                    true
                                }
                            }
                            mapView.overlays.add(pBlok)
                            
                            if (isSelectedBlok) {
                                val treesToRender = if (filterStatus != null) {
                                    blok.pohonList.filter { it.status == filterStatus }
                                } else {
                                    blok.pohonList
                                }
                                
                                treesToRender.forEach { pohon ->
                                    val m = OsmMarker(mapView).apply {
                                        position = GeoPoint(pohon.location.latitude, pohon.location.longitude)
                                        title = "Pohon ${pohon.kodePohon} (${pohon.status.label})"
                                        snippet = "Varietas ${pohon.varietasKakao} • Pemupukan: ${pohon.tanggalPemupukanTerakhir}"
                                        setOnMarkerClickListener { _, _ ->
                                            selectedKebun = kebun
                                            selectedBlok = blok
                                            selectedPohon = pohon
                                            mapCenterTrigger = Pair(GeoPoint(pohon.location.latitude, pohon.location.longitude), 20.0)
                                            true
                                        }
                                    }
                                    mapView.overlays.add(m)
                                }
                            }
                        }
                    }
                    mapView.invalidate()
                }
            )

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
                                            mapCenterTrigger = Pair(GeoPoint(center.latitude, center.longitude), 18.5.toDouble())
                                        }
                                    }
                                } else {
                                    selectedBlok = null
                                    selectedPohon = null
                                    activeKebun?.let {
                                        coroutineScope.launch {
                                            mapCenterTrigger = Pair(GeoPoint(it.centerLocation.latitude, it.centerLocation.longitude), 16.5.toDouble())
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
                                        mapCenterTrigger = Pair(GeoPoint(first.targetLatLng.latitude, first.targetLatLng.longitude), 16.5.toDouble())
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
                                            if (result.associatedBlok != null) {
                                                selectedBlok = result.associatedBlok
                                            }
                                            if (result.associatedPohon != null) {
                                                selectedPohon = result.associatedPohon
                                            }
                                            coroutineScope.launch {
                                                val zoom = when {
                                                    result.associatedPohon != null -> 20.0
                                                    result.associatedBlok != null -> 18.5
                                                    else -> 16.5
                                                }
                                                mapCenterTrigger = Pair(GeoPoint(result.targetLatLng.latitude, result.targetLatLng.longitude), zoom)
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
                            mapType = (mapType + 1) % 3
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

                // KML Overlay Toggle Button (Jadwal Satelit S2C)
                if (kmlData.berauCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = if (showKmlOverlay) KakaoColors.Primary else KakaoColors.Surface,
                        shadowElevation = KakaoElevation.Medium,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { showKmlOverlay = !showKmlOverlay }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Satellite,
                                contentDescription = "Jadwal Satelit Sentinel-2",
                                tint = if (showKmlOverlay) Color.White else KakaoColors.PrimaryDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
                                    mapCenterTrigger = Pair(GeoPoint(it.centerLocation.latitude, it.centerLocation.longitude), 17.toDouble())
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

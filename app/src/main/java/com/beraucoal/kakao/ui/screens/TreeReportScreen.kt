package com.beraucoal.kakao.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import java.io.File
import com.beraucoal.kakao.data.*
import com.beraucoal.kakao.ui.theme.KakaoColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeReportScreen(
    pohon: PohonKakao,
    repository: PlantationRepository,
    newDaunPhotos: String?,
    newBatangPhotos: String?,
    newBuahPhotos: String?,
    onClearPhotoState: (BagianPohon) -> Unit,
    onNavigateToCamera: (BagianPohon, Int) -> Unit,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    var catatan by remember { mutableStateOf("") }
    var fotoList by remember { mutableStateOf(listOf<FotoLaporan>()) }

    // Helper: parse "|||" delimited photo paths into a list
    fun parsePhotoList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split("|||").filter { it.isNotBlank() }
    }

    LaunchedEffect(newDaunPhotos) {
        val photos = parsePhotoList(newDaunPhotos)
        if (photos.isNotEmpty()) {
            fotoList = fotoList + photos.map { uri ->
                FotoLaporan(System.currentTimeMillis().toString() + uri.hashCode(), uri, BagianPohon.DAUN)
            }
            onClearPhotoState(BagianPohon.DAUN)
        }
    }
    LaunchedEffect(newBatangPhotos) {
        val photos = parsePhotoList(newBatangPhotos)
        if (photos.isNotEmpty()) {
            fotoList = fotoList + photos.map { uri ->
                FotoLaporan(System.currentTimeMillis().toString() + uri.hashCode(), uri, BagianPohon.BATANG)
            }
            onClearPhotoState(BagianPohon.BATANG)
        }
    }
    LaunchedEffect(newBuahPhotos) {
        val photos = parsePhotoList(newBuahPhotos)
        if (photos.isNotEmpty()) {
            fotoList = fotoList + photos.map { uri ->
                FotoLaporan(System.currentTimeMillis().toString() + uri.hashCode(), uri, BagianPohon.BUAH)
            }
            onClearPhotoState(BagianPohon.BUAH)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Kondisi Pohon") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Batal")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KakaoColors.Surface,
                    titleContentColor = KakaoColors.TextPrimary
                )
            )
        },
        bottomBar = {
            Surface(
                color = KakaoColors.Surface,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        val laporan = LaporanPohon(
                            id = "LAP-${System.currentTimeMillis()}",
                            pohonId = pohon.id,
                            timestamp = "Hari ini",
                            fotoList = fotoList,
                            catatan = catatan
                        )
                        repository.saveLaporanPohon(laporan)
                        onSaved()
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KakaoColors.Primary)
                ) {
                    Text("Simpan ke Antrean", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KakaoColors.Background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Surface(
                color = KakaoColors.Surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pohon ${pohon.kodePohon}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Varietas: ${pohon.varietasKakao}", fontSize = 14.sp, color = KakaoColors.TextSecondary)
                    Text("Laporkan kondisi dengan foto Daun, Batang, dan Buah. AI akan menganalisis foto tersebut untuk memperbarui status pohon.", fontSize = 12.sp, color = KakaoColors.TextMuted, modifier = Modifier.padding(top = 8.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Kategori Foto
            BagianPohon.values().forEach { bagian ->
                val fotosForBagian = fotoList.filter { it.bagian == bagian }
                PhotoSection(
                    bagian = bagian,
                    fotos = fotosForBagian,
                    onAddPhoto = {
                        // Kirim jumlah foto yang sudah ada agar kamera tahu batas sisa
                        onNavigateToCamera(bagian, fotosForBagian.size)
                    }
                )
            }

            // Catatan
            Surface(
                color = KakaoColors.Surface,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Catatan Tambahan (Opsional)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = catatan,
                        onValueChange = { catatan = it },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        placeholder = { Text("Ketik catatan terkait kondisi pohon...") }
                    )
                }
            }
        }
    }
}

@Composable
fun PhotoSection(
    bagian: BagianPohon,
    fotos: List<FotoLaporan>,
    onAddPhoto: () -> Unit
) {
    Surface(
        color = KakaoColors.Surface,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Foto ${bagian.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = KakaoColors.TextPrimary
                )
                Text(
                    text = "${fotos.size}/5",
                    fontSize = 12.sp,
                    color = if (fotos.size >= 5) KakaoColors.Error else KakaoColors.TextSecondary
                )
            }
            Spacer(Modifier.height(12.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(fotos) { foto ->
                    val bitmap = remember(foto.uri) {
                        BitmapFactory.decodeFile(foto.uri)?.asImageBitmap()
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Foto ${bagian.name}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(KakaoColors.SurfaceMuted)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(KakaoColors.SurfaceMuted)
                        )
                    }
                }
                if (fotos.size < 5) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(KakaoColors.SurfaceMuted)
                                .clickable { onAddPhoto() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.AddAPhoto, contentDescription = "Tambah", tint = KakaoColors.Primary)
                                Spacer(Modifier.height(4.dp))
                                Text("Tambah", fontSize = 12.sp, color = KakaoColors.Primary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.beraucoal.kakao.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beraucoal.kakao.data.BagianPohon
import com.beraucoal.kakao.data.LaporanPohon
import com.beraucoal.kakao.data.PlantationRepository
import com.beraucoal.kakao.ui.theme.KakaoColors
import com.beraucoal.kakao.ui.theme.KakaoSpacing
import com.beraucoal.kakao.ui.theme.PlusJakartaSansFontFamily

/**
 * Layar Antrean Kirim — sesuai reference 2.0.
 * Tap kartu laporan untuk membuka detail: lihat foto (read-only)
 * dan edit catatan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadQueueScreen(
    repository: PlantationRepository,
    onSync: () -> Unit
) {
    val queue by repository.uploadQueue.collectAsState()
    var selectedLaporan by remember { mutableStateOf<LaporanPohon?>(null) }

    if (selectedLaporan != null) {
        LaporanDetailScreen(
            laporan = selectedLaporan!!,
            isEditable = true,
            statusLabel = "Menunggu Kirim",
            statusColor = KakaoColors.Warning,
            onCatatanUpdated = { newCatatan ->
                repository.updateLaporanCatatan(selectedLaporan!!.id, newCatatan)
                selectedLaporan = repository.getLaporanById(selectedLaporan!!.id)
            },
            onBack = { selectedLaporan = null }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KakaoColors.Background),
        contentAlignment = Alignment.Center
    ) {
        if (queue.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = KakaoSpacing.ScreenHorizontal)
            ) {
                // Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(KakaoColors.PrimaryContainer)
                ) {
                    Text(text = "📤", fontSize = 36.sp)
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Antrean Kirim Kosong",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSansFontFamily,
                    color = KakaoColors.TextPrimary,
                    letterSpacing = (-0.028).sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Semua laporan foto berhasil disinkronisasi ke server.",
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSansFontFamily,
                    color = KakaoColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.5.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(KakaoSpacing.ScreenHorizontal)
            ) {
                Text(
                    text = "Menunggu Sinyal (${queue.size})",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = KakaoColors.TextPrimary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Button(
                    onClick = onSync,
                    colors = ButtonDefaults.buttonColors(containerColor = KakaoColors.Primary),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Coba Sinkronisasi Sekarang", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                }
                
                Spacer(Modifier.height(16.dp))

                queue.forEach { laporan ->
                    Surface(
                        color = KakaoColors.Surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable { selectedLaporan = laporan },
                        shadowElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Pohon ID: ${laporan.pohonId}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Surface(color = KakaoColors.Warning.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        "Menunggu",
                                        color = KakaoColors.Warning,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text("Waktu: ${laporan.timestamp}", fontSize = 12.sp, color = KakaoColors.TextSecondary)
                            Text("${laporan.fotoList.size} foto terlampir • Ketuk untuk detail", fontSize = 12.sp, color = KakaoColors.Primary)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Layar Riwayat — sesuai reference 2.0.
 * Tap kartu laporan yang sudah terkirim untuk melihat detail.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatScreen(
    repository: PlantationRepository
) {
    val riwayatList by repository.riwayatLaporan.collectAsState()
    var selectedLaporan by remember { mutableStateOf<LaporanPohon?>(null) }

    if (selectedLaporan != null) {
        LaporanDetailScreen(
            laporan = selectedLaporan!!,
            isEditable = false,
            statusLabel = "Terkirim",
            statusColor = KakaoColors.StepActive,
            onCatatanUpdated = {},
            onBack = { selectedLaporan = null }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KakaoColors.Background),
        contentAlignment = Alignment.Center
    ) {
        if (riwayatList.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = KakaoSpacing.ScreenHorizontal)
            ) {
                // Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(KakaoColors.PrimaryContainer)
                ) {
                    Text(text = "🕐", fontSize = 36.sp)
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Riwayat Kosong",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSansFontFamily,
                    color = KakaoColors.TextPrimary,
                    letterSpacing = (-0.028).sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Belum ada laporan yang disinkronisasi ke server.",
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSansFontFamily,
                    color = KakaoColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.5.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(KakaoSpacing.ScreenHorizontal)
            ) {
                Text(
                    text = "Riwayat Terkirim",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = KakaoColors.TextPrimary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                riwayatList.forEach { laporan ->
                    Surface(
                        color = KakaoColors.Surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable { selectedLaporan = laporan },
                        shadowElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Pohon ID: ${laporan.pohonId}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Surface(color = KakaoColors.StepActive.copy(alpha=0.15f), shape = RoundedCornerShape(4.dp)) {
                                    Text("Terkirim", color = KakaoColors.StepActive, fontSize = 10.sp, modifier = Modifier.padding(horizontal=6.dp, vertical=2.dp))
                                }
                            }
                            Text("Waktu: ${laporan.timestamp}", fontSize = 12.sp, color = KakaoColors.TextSecondary)
                            Text("${laporan.fotoList.size} foto terlampir • Ketuk untuk detail", fontSize = 12.sp, color = KakaoColors.Primary)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Layar Detail Laporan — menampilkan foto (read-only, tidak bisa diubah/dihapus)
 * dan catatan yang bisa diedit (jika isEditable = true, yaitu di antrean kirim).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanDetailScreen(
    laporan: LaporanPohon,
    isEditable: Boolean,
    statusLabel: String,
    statusColor: androidx.compose.ui.graphics.Color,
    onCatatanUpdated: (String) -> Unit,
    onBack: () -> Unit
) {
    var editMode by remember { mutableStateOf(false) }
    var editedCatatan by remember(laporan.id) { mutableStateOf(laporan.catatan) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Laporan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KakaoColors.Surface,
                    titleContentColor = KakaoColors.TextPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KakaoColors.Background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Info header
            Surface(
                color = KakaoColors.Surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Pohon: ${laporan.pohonId}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = KakaoColors.TextPrimary
                        )
                        Surface(
                            color = statusColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                color = statusColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Waktu: ${laporan.timestamp}",
                        fontSize = 13.sp,
                        color = KakaoColors.TextSecondary
                    )
                    Text(
                        "Total ${laporan.fotoList.size} foto terlampir",
                        fontSize = 13.sp,
                        color = KakaoColors.Primary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Foto per bagian (read-only, tidak bisa diubah/dihapus)
            BagianPohon.values().forEach { bagian ->
                val fotosForBagian = laporan.fotoList.filter { it.bagian == bagian }
                if (fotosForBagian.isNotEmpty()) {
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
                                    text = "${fotosForBagian.size} foto",
                                    fontSize = 12.sp,
                                    color = KakaoColors.TextSecondary
                                )
                            }
                            Spacer(Modifier.height(12.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(fotosForBagian) { foto ->
                                    val bitmap = remember(foto.uri) {
                                        BitmapFactory.decodeFile(foto.uri)?.asImageBitmap()
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = "Foto ${bagian.name}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(KakaoColors.SurfaceMuted)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(KakaoColors.SurfaceMuted),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "File tidak ditemukan",
                                                fontSize = 10.sp,
                                                color = KakaoColors.TextMuted,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Catatan — editable jika di antrean kirim
            Surface(
                color = KakaoColors.Surface,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Catatan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = KakaoColors.TextPrimary
                        )
                        if (isEditable && !editMode) {
                            IconButton(
                                onClick = { editMode = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Edit catatan",
                                    tint = KakaoColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    if (editMode && isEditable) {
                        OutlinedTextField(
                            value = editedCatatan,
                            onValueChange = { editedCatatan = it },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            placeholder = { Text("Ketik catatan...") }
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = {
                                editedCatatan = laporan.catatan
                                editMode = false
                            }) {
                                Text("Batal")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onCatatanUpdated(editedCatatan)
                                    editMode = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = KakaoColors.Primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Simpan", color = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                    } else {
                        Text(
                            text = if (laporan.catatan.isNotBlank()) laporan.catatan else "(Tidak ada catatan)",
                            fontSize = 14.sp,
                            color = if (laporan.catatan.isNotBlank()) KakaoColors.TextPrimary else KakaoColors.TextMuted,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

package com.beraucoal.kakao.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beraucoal.kakao.data.*
import com.beraucoal.kakao.ui.theme.KakaoColors
import com.beraucoal.kakao.ui.theme.KakaoElevation

@Composable
fun PlantationBottomSheet(
    kebun: KebunArea,
    selectedBlok: BlokBagian?,
    selectedPohon: PohonKakao?,
    onBlokSelected: (BlokBagian?) -> Unit,
    onPohonSelected: (PohonKakao?) -> Unit,
    onStatusChange: (PohonKakao, PohonStatus) -> Unit,
    onAddPohon: (BlokBagian, PohonKakao) -> Unit = { _, _ -> },
    onSelfMappingClick: () -> Unit = {}
) {
    // Batasan maksimal saat ditarik full keatas (0.90f), memberikan area tampilan yang jauh lebih luas dan proporsional
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.90f)
            .padding(horizontal = 16.dp, vertical = 2.dp)
    ) {
        when {
            selectedPohon != null -> {
                TreeDetailPanel(
                    pohon = selectedPohon,
                    blok = selectedBlok ?: kebun.blokList.firstOrNull(),
                    onStatusChange = { newStatus -> onStatusChange(selectedPohon, newStatus) }
                )
            }
            selectedBlok != null -> {
                TreeListPanel(
                    kebun = kebun,
                    blok = selectedBlok,
                    onPohonSelected = { onPohonSelected(it) },
                    onAddPohon = onAddPohon
                )
            }
            else -> {
                SubSectorListPanel(
                    kebun = kebun,
                    onBlokSelected = { onBlokSelected(it) },
                    onSelfMappingClick = onSelfMappingClick
                )
            }
        }
    }
}

@Composable
private fun SubSectorListPanel(
    kebun: KebunArea,
    onBlokSelected: (BlokBagian) -> Unit,
    onSelfMappingClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Filter pills ala TransJakarta
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterCirclePill(label = "Semua (${kebun.blokList.size})", isSelected = true, onClick = {})
            }
            items(kebun.blokList) { blok ->
                FilterCirclePill(label = "Blok ${blok.kodeBlok}", isSelected = false, onClick = { onBlokSelected(blok) })
            }
        }

        Spacer(Modifier.height(4.dp))

        // Header Informasi Kebun (Compact & Android Able)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = KakaoColors.SurfaceMuted,
            border = BorderStroke(0.6.dp, KakaoColors.Divider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = kebun.namaKebun,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = KakaoColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = KakaoColors.PrimaryDark.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "${kebun.luasHektar} Ha",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = KakaoColors.PrimaryDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Pemilik: ${kebun.namaPemilik} • Total: ${kebun.totalPohon} Pohon Kakao",
                    fontSize = 11.sp,
                    color = KakaoColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Action Mandiri Button (Compact)
        Button(
            onClick = onSelfMappingClick,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KakaoColors.PrimaryDark),
            modifier = Modifier.fillMaxWidth().height(38.dp)
        ) {
            Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
            Spacer(Modifier.width(6.dp))
            Text("Perbarui Batas Lahan Mandiri", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }

        Spacer(Modifier.height(10.dp))

        // Judul Bagian
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Daftar Sub-Sektor Lahan", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KakaoColors.TextPrimary)
            Text("Tekan untuk Detail & Pohon", fontSize = 11.sp, color = KakaoColors.PrimaryDark, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(6.dp))

        // LazyColumn dengan space bawah 160.dp agar selalu bebas dari navbar
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 160.dp),
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
        ) {
            items(kebun.blokList) { blok ->
                TjSubSectorCard(blok = blok, kebun = kebun, onClick = { onBlokSelected(blok) })
            }
        }
    }
}

@Composable
private fun TreeListPanel(
    kebun: KebunArea,
    blok: BlokBagian,
    onPohonSelected: (PohonKakao) -> Unit,
    onAddPohon: (BlokBagian, PohonKakao) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Semua") }

    if (showAddDialog) {
        val defaultLoc = if (blok.polygon.isNotEmpty()) {
            val avgLat = blok.polygon.map { it.latitude }.average()
            val avgLng = blok.polygon.map { it.longitude }.average()
            com.google.android.gms.maps.model.LatLng(avgLat, avgLng)
        } else kebun.centerLocation

        AddPohonDialog(
            kebun = kebun,
            blok = blok,
            defaultLocation = defaultLoc,
            onDismiss = { showAddDialog = false },
            onSave = { newTree ->
                onAddPohon(blok, newTree)
                showAddDialog = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Horizontal Filter Chips ala TransJakarta
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterCirclePill("Semua (${blok.totalPohon})", isSelected = selectedFilter == "Semua", onClick = { selectedFilter = "Semua" })
            }
            item {
                FilterCirclePill("Sehat (${blok.pohonSehatCount})", isSelected = selectedFilter == "Sehat", onClick = { selectedFilter = "Sehat" })
            }
            item {
                FilterCirclePill("Pupuk (${blok.pohonPupukCount})", isSelected = selectedFilter == "Pupuk", onClick = { selectedFilter = "Pupuk" })
            }
            item {
                FilterCirclePill("Hama (${blok.pohonHamaCount})", isSelected = selectedFilter == "Hama", onClick = { selectedFilter = "Hama" })
            }
        }

        Spacer(Modifier.height(4.dp))

        // Ringkasan Sub-Sektor (Compact)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = KakaoColors.SurfaceMuted,
                border = BorderStroke(0.6.dp, KakaoColors.Divider),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("TOTAL POPULASI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = KakaoColors.TextSecondary)
                    Spacer(Modifier.height(2.dp))
                    Text("${blok.totalPohon} POHON", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = KakaoColors.TextPrimary)
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = KakaoColors.SurfaceMuted,
                border = BorderStroke(0.6.dp, KakaoColors.Divider),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("KONDISI DOMINAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = KakaoColors.TextSecondary)
                    Spacer(Modifier.height(2.dp))
                    Text("${blok.pohonSehatCount} SEHAT", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = KakaoColors.StepActive)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Tombol Tambah Pohon (Compact)
        Button(
            onClick = { showAddDialog = true },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KakaoColors.PrimaryDark),
            modifier = Modifier.fillMaxWidth().height(38.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
            Spacer(Modifier.width(6.dp))
            Text("Tambah Pohon Baru di Sub-Sektor Ini", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }

        Spacer(Modifier.height(10.dp))

        // Judul Bagian Pohon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Daftar Pohon Kakao", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KakaoColors.TextPrimary)
            Text("Tekan untuk Detail & Riwayat", fontSize = 11.sp, color = KakaoColors.PrimaryDark, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(6.dp))

        val filteredPohons = blok.pohonList.filter { pohon ->
            when (selectedFilter) {
                "Sehat" -> pohon.status == PohonStatus.SEHAT
                "Pupuk" -> pohon.status == PohonStatus.PERLU_PUPUK
                "Hama" -> pohon.status == PohonStatus.TERSERANG_HAMA
                else -> true
            }
        }

        // LazyColumn dengan space bawah 160.dp agar selalu bebas dari navbar
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 160.dp),
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
        ) {
            if (filteredPohons.isEmpty()) {
                item {
                    Text(
                        text = "Tidak ada pohon pada filter ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KakaoColors.TextSecondary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(filteredPohons) { pohon ->
                    TjTreeCard(pohon = pohon, blok = blok, onClick = { onPohonSelected(pohon) })
                }
            }
        }
    }
}

@Composable
private fun TreeDetailPanel(
    pohon: PohonKakao,
    blok: BlokBagian?,
    onStatusChange: (PohonStatus) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Profile Card Pohon
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = KakaoColors.Surface,
            border = BorderStroke(0.8.dp, KakaoColors.Divider),
            shadowElevation = KakaoElevation.Low,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = KakaoColors.PrimaryDark
                        ) {
                            Text(
                                text = pohon.kodePohon,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = pohon.varietasKakao,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = KakaoColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val badgeBg = when (pohon.status) {
                        PohonStatus.SEHAT -> KakaoColors.StepActive.copy(alpha = 0.12f)
                        PohonStatus.PERLU_PUPUK -> KakaoColors.Warning.copy(alpha = 0.15f)
                        PohonStatus.TERSERANG_HAMA -> KakaoColors.Error.copy(alpha = 0.15f)
                    }
                    val badgeColor = when (pohon.status) {
                        PohonStatus.SEHAT -> KakaoColors.StepActive
                        PohonStatus.PERLU_PUPUK -> KakaoColors.Warning
                        PohonStatus.TERSERANG_HAMA -> KakaoColors.Error
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = badgeBg) {
                        Text(
                            text = pohon.status.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Sub-Sektor: ${blok?.namaBlok ?: "Blok A"} • Est. Panen: ${pohon.estimasiHasilKg} Kg",
                    fontSize = 11.sp,
                    color = KakaoColors.TextSecondary
                )
                Text(
                    text = "Pupuk Terakhir: ${pohon.tanggalPemupukanTerakhir} • GPS: ${String.format("%.4f", pohon.location.latitude)}, ${String.format("%.4f", pohon.location.longitude)}",
                    fontSize = 11.sp,
                    color = KakaoColors.TextMuted
                )

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = KakaoColors.Divider, thickness = 0.6.dp)
                Spacer(Modifier.height(10.dp))

                Text("Perbarui Status Kesehatan:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KakaoColors.PrimaryDark)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Button(
                        onClick = { onStatusChange(PohonStatus.SEHAT) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (pohon.status == PohonStatus.SEHAT) KakaoColors.StepActive else KakaoColors.SurfaceMuted,
                            contentColor = if (pohon.status == PohonStatus.SEHAT) Color.White else KakaoColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("Sehat", fontSize = 11.sp, fontWeight = FontWeight.Bold) }

                    Button(
                        onClick = { onStatusChange(PohonStatus.PERLU_PUPUK) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (pohon.status == PohonStatus.PERLU_PUPUK) KakaoColors.Warning else KakaoColors.SurfaceMuted,
                            contentColor = if (pohon.status == PohonStatus.PERLU_PUPUK) Color.White else KakaoColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("Pupuk", fontSize = 11.sp, fontWeight = FontWeight.Bold) }

                    Button(
                        onClick = { onStatusChange(PohonStatus.TERSERANG_HAMA) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (pohon.status == PohonStatus.TERSERANG_HAMA) KakaoColors.Error else KakaoColors.SurfaceMuted,
                            contentColor = if (pohon.status == PohonStatus.TERSERANG_HAMA) Color.White else KakaoColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("Hama", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Header Riwayat Pelaporan
        Text(
            text = "Riwayat Pelaporan Rutin Kesehatan",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = KakaoColors.TextPrimary
        )
        Text(
            text = "Catatan monitoring kondisi agronomis pohon",
            fontSize = 11.sp,
            color = KakaoColors.TextSecondary
        )

        Spacer(Modifier.height(6.dp))

        // Timeline History List dengan clearance navbar 160.dp
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 160.dp),
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
        ) {
            items(pohon.riwayatAktif) { riwayat ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = KakaoColors.Surface,
                    border = BorderStroke(0.6.dp, KakaoColors.Divider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = riwayat.tanggal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = KakaoColors.TextPrimary
                            )
                            val badgeBg = when (riwayat.status) {
                                PohonStatus.SEHAT -> KakaoColors.StepActive.copy(alpha = 0.12f)
                                PohonStatus.PERLU_PUPUK -> KakaoColors.Warning.copy(alpha = 0.15f)
                                PohonStatus.TERSERANG_HAMA -> KakaoColors.Error.copy(alpha = 0.15f)
                            }
                            val badgeColor = when (riwayat.status) {
                                PohonStatus.SEHAT -> KakaoColors.StepActive
                                PohonStatus.PERLU_PUPUK -> KakaoColors.Warning
                                PohonStatus.TERSERANG_HAMA -> KakaoColors.Error
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = badgeBg) {
                                Text(
                                    text = riwayat.status.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = riwayat.catatan,
                            fontSize = 11.sp,
                            color = KakaoColors.TextPrimary,
                            lineHeight = 15.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Pelapor: ${riwayat.pelapor}",
                            fontSize = 10.sp,
                            color = KakaoColors.TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ── TransJakarta Styled SubSector Card ──
@Composable
private fun TjSubSectorCard(
    blok: BlokBagian,
    kebun: KebunArea,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = KakaoColors.Surface,
        border = BorderStroke(0.8.dp, KakaoColors.Divider),
        shadowElevation = KakaoElevation.Low,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = KakaoColors.PrimaryDark.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = blok.kodeBlok,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = KakaoColors.PrimaryDark
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sub-Sektor ${blok.namaBlok}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = KakaoColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = kebun.namaKebun,
                    fontSize = 11.sp,
                    color = KakaoColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (blok.pohonSehatCount > 0) {
                        StatusDotText(label = "${blok.pohonSehatCount} Sehat", color = KakaoColors.StepActive)
                    }
                    if (blok.pohonPupukCount > 0) {
                        StatusDotText(label = "${blok.pohonPupukCount} Pupuk", color = KakaoColors.Warning)
                    }
                    if (blok.pohonHamaCount > 0) {
                        StatusDotText(label = "${blok.pohonHamaCount} Hama", color = KakaoColors.Error)
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = KakaoColors.SurfaceMuted,
                border = BorderStroke(0.6.dp, KakaoColors.Divider)
            ) {
                Text(
                    text = "${blok.totalPohon} Pohon",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = KakaoColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }
}

// ── TransJakarta Styled Tree Card (Klik menuju Level 3: Detail Pohon) ──
@Composable
private fun TjTreeCard(
    pohon: PohonKakao,
    blok: BlokBagian,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = KakaoColors.Surface,
        border = BorderStroke(0.8.dp, KakaoColors.Divider),
        shadowElevation = KakaoElevation.Low,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = KakaoColors.PrimaryDark,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = pohon.kodePohon,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = pohon.varietasKakao,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = KakaoColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    )

                    val badgeBg = when (pohon.status) {
                        PohonStatus.SEHAT -> KakaoColors.StepActive.copy(alpha = 0.12f)
                        PohonStatus.PERLU_PUPUK -> KakaoColors.Warning.copy(alpha = 0.15f)
                        PohonStatus.TERSERANG_HAMA -> KakaoColors.Error.copy(alpha = 0.15f)
                    }
                    val badgeColor = when (pohon.status) {
                        PohonStatus.SEHAT -> KakaoColors.StepActive
                        PohonStatus.PERLU_PUPUK -> KakaoColors.Warning
                        PohonStatus.TERSERANG_HAMA -> KakaoColors.Error
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeBg
                    ) {
                        Text(
                            text = pohon.status.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = KakaoColors.SurfaceMuted
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(KakaoColors.StepActive)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Titik GPS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = KakaoColors.TextSecondary)
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Blok ${blok.kodeBlok} • Est: ${pohon.estimasiHasilKg} Kg",
                        fontSize = 11.sp,
                        color = KakaoColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Pupuk Terakhir: ${pohon.tanggalPemupukanTerakhir} • Tekan untuk Riwayat",
                    fontSize = 10.sp,
                    color = KakaoColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatusDotText(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = KakaoColors.TextPrimary)
    }
}

@Composable
private fun FilterCirclePill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) KakaoColors.PrimaryDark else KakaoColors.SurfaceMuted,
        border = if (isSelected) null else BorderStroke(0.6.dp, KakaoColors.Divider),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else KakaoColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

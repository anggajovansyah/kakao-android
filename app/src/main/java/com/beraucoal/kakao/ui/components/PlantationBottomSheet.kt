package com.beraucoal.kakao.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beraucoal.kakao.data.BlokBagian
import com.beraucoal.kakao.data.KebunArea
import com.beraucoal.kakao.data.PohonKakao
import com.beraucoal.kakao.data.PohonStatus
import com.beraucoal.kakao.ui.theme.KakaoColors
import com.beraucoal.kakao.ui.theme.KakaoElevation
import com.beraucoal.kakao.ui.theme.PoppinsFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantationBottomSheet(
    kebun: KebunArea,
    selectedBlok: BlokBagian?,
    selectedPohon: PohonKakao?,
    onBlokSelected: (BlokBagian?) -> Unit,
    onPohonSelected: (PohonKakao?) -> Unit,
    onStatusChange: (PohonKakao, PohonStatus) -> Unit,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0: Overview, 1: Sub-Blok, 2: Daftar Pohon
    var selectedFilterStatus by remember { mutableStateOf<PohonStatus?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = KakaoColors.Surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // ── Header Kebun ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = kebun.namaKebun,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = KakaoColors.TextPrimary
                    )
                    Text(
                        text = "Pemilik: ${kebun.namaPemilik} • ${kebun.luasHektar} Hektar",
                        style = MaterialTheme.typography.bodySmall,
                        color = KakaoColors.TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = KakaoColors.TextMuted)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Stat Summary Cards ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatPill(
                    title = "Sehat",
                    count = kebun.totalPohonSehat,
                    color = KakaoColors.StepActive,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    title = "Butuh Pupuk",
                    count = kebun.totalPohonPupuk,
                    color = KakaoColors.Warning,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    title = "Hama",
                    count = kebun.totalPohonHama,
                    color = KakaoColors.Error,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Tab Switcher ──
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = KakaoColors.SurfaceMuted,
                contentColor = KakaoColors.PrimaryDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Ringkasan", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Sub-Blok (${kebun.blokList.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("Pohon (${kebun.totalPohon})", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Tab Contents ──
            Box(modifier = Modifier.heightIn(max = 360.dp)) {
                when (activeTab) {
                    0 -> OverviewTabContent(kebun = kebun)
                    1 -> BlokListTabContent(
                        blokList = kebun.blokList,
                        selectedBlok = selectedBlok,
                        onBlokClick = {
                            onBlokSelected(it)
                            activeTab = 2 // Pindah ke tab pohon setelah memilih blok
                        }
                    )
                    2 -> PohonListTabContent(
                        kebun = kebun,
                        selectedBlok = selectedBlok,
                        selectedPohon = selectedPohon,
                        selectedFilterStatus = selectedFilterStatus,
                        onFilterChange = { selectedFilterStatus = it },
                        onPohonClick = onPohonSelected,
                        onStatusChange = onStatusChange
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatPill(title: String, count: Int, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
        ) {
            Text(
                text = count.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                fontFamily = PoppinsFontFamily
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = KakaoColors.TextPrimary
            )
        }
    }
}

@Composable
private fun OverviewTabContent(kebun: KebunArea) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = KakaoColors.SurfaceMuted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Informasi Umum Perkebunan",
                    fontWeight = FontWeight.Bold,
                    color = KakaoColors.PrimaryDark,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                Text("ID Petani: ${kebun.idPetani}", fontSize = 12.sp, color = KakaoColors.TextSecondary)
                Text("Nomor WhatsApp: ${kebun.nomorWa}", fontSize = 12.sp, color = KakaoColors.TextSecondary)
                Text("Jumlah Sub-Blok: ${kebun.blokList.size} Area", fontSize = 12.sp, color = KakaoColors.TextSecondary)
                Text("Total Populasi Pohon: ${kebun.totalPohon} Batang", fontSize = 12.sp, color = KakaoColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun BlokListTabContent(
    blokList: List<BlokBagian>,
    selectedBlok: BlokBagian?,
    onBlokClick: (BlokBagian) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(blokList) { blok ->
            val isSelected = selectedBlok?.id == blok.id
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) KakaoColors.PrimaryContainer else KakaoColors.SurfaceMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBlokClick(blok) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = blok.namaBlok,
                            fontWeight = FontWeight.Bold,
                            color = KakaoColors.TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Populasi: ${blok.totalPohon} Pohon (${blok.pohonSehatCount} Sehat, ${blok.pohonHamaCount} Hama)",
                            fontSize = 12.sp,
                            color = KakaoColors.TextSecondary
                        )
                    }
                    Text(
                        text = "Lihat Pohon →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = KakaoColors.Primary
                    )
                }
            }
        }
    }
}

@Composable
private fun PohonListTabContent(
    kebun: KebunArea,
    selectedBlok: BlokBagian?,
    selectedPohon: PohonKakao?,
    selectedFilterStatus: PohonStatus?,
    onFilterChange: (PohonStatus?) -> Unit,
    onPohonClick: (PohonKakao) -> Unit,
    onStatusChange: (PohonKakao, PohonStatus) -> Unit
) {
    val allTrees = if (selectedBlok != null) selectedBlok.pohonList else kebun.blokList.flatMap { it.pohonList }
    val filteredTrees = if (selectedFilterStatus != null) allTrees.filter { it.status == selectedFilterStatus } else allTrees

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── Filter Chips ──
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            FilterChip(
                selected = selectedFilterStatus == null,
                onClick = { onFilterChange(null) },
                label = { Text("Semua (${allTrees.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilterStatus == PohonStatus.TERSERANG_HAMA,
                onClick = { onFilterChange(if (selectedFilterStatus == PohonStatus.TERSERANG_HAMA) null else PohonStatus.TERSERANG_HAMA) },
                label = { Text("Hama", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilterStatus == PohonStatus.PERLU_PUPUK,
                onClick = { onFilterChange(if (selectedFilterStatus == PohonStatus.PERLU_PUPUK) null else PohonStatus.PERLU_PUPUK) },
                label = { Text("Pupuk", fontSize = 11.sp) }
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredTrees) { pohon ->
                val isSelected = selectedPohon?.id == pohon.id
                val statusColor = when (pohon.status) {
                    PohonStatus.SEHAT -> KakaoColors.StepActive
                    PohonStatus.PERLU_PUPUK -> KakaoColors.Warning
                    PohonStatus.TERSERANG_HAMA -> KakaoColors.Error
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) KakaoColors.PrimaryContainer else KakaoColors.SurfaceMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPohonClick(pohon) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Pohon ${pohon.kodePohon}",
                                fontWeight = FontWeight.Bold,
                                color = KakaoColors.TextPrimary,
                                fontSize = 13.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = statusColor.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = pohon.status.label,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (isSelected) {
                            Spacer(Modifier.height(8.dp))
                            Text("Varietas: ${pohon.varietasKakao} • Umur: ${pohon.umurTahun} Tahun", fontSize = 11.sp, color = KakaoColors.TextSecondary)
                            Text("Pemupukan Terakhir: ${pohon.tanggalPemupukanTerakhir}", fontSize = 11.sp, color = KakaoColors.TextSecondary)
                            Text("Estimasi Panen: ${pohon.estimasiHasilKg} Kg", fontSize = 11.sp, color = KakaoColors.TextSecondary)

                            Spacer(Modifier.height(10.dp))
                            Text("Ubah Status Kesehatan Pohon:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KakaoColors.PrimaryDark)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                            ) {
                                Button(
                                    onClick = { onStatusChange(pohon, PohonStatus.SEHAT) },
                                    colors = ButtonDefaults.buttonColors(containerColor = KakaoColors.StepActive),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Sehat", fontSize = 10.sp, color = androidx.compose.ui.graphics.Color.White)
                                }
                                Button(
                                    onClick = { onStatusChange(pohon, PohonStatus.PERLU_PUPUK) },
                                    colors = ButtonDefaults.buttonColors(containerColor = KakaoColors.Warning),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Pupuk", fontSize = 10.sp, color = androidx.compose.ui.graphics.Color.White)
                                }
                                Button(
                                    onClick = { onStatusChange(pohon, PohonStatus.TERSERANG_HAMA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = KakaoColors.Error),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Hama", fontSize = 10.sp, color = androidx.compose.ui.graphics.Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

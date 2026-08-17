package com.beraucoal.kakao.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.beraucoal.kakao.data.*
import com.beraucoal.kakao.ui.theme.KakaoColors
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.beraucoal.kakao.utils.PolyUtil

@Composable
fun AddPohonDialog(
    kebun: KebunArea,
    blok: BlokBagian,
    defaultLocation: LatLng,
    onDismiss: () -> Unit,
    onSave: (PohonKakao) -> Unit
) {
    val context = LocalContext.current
    var selectedMethod by remember { mutableStateOf(PohonInputMethod.GPS_LAPANGAN) }
    var location by remember { mutableStateOf<LatLng?>(defaultLocation) }
    var varietas by remember { mutableStateOf("MCC 02 (Sulawesi 2)") }
    var status by remember { mutableStateOf(PohonStatus.SEHAT) }
    var isOutsidePolygon by remember { mutableStateOf(false) }
    var forceSaveConfirmed by remember { mutableStateOf(false) }

    val hasPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(location) {
        val locValue = location ?: return@LaunchedEffect
        if (blok.polygon.size >= 3) {
            isOutsidePolygon = !PolyUtil.containsLocation(locValue, blok.polygon, true)
        } else if (kebun.polygon.size >= 3) {
            isOutsidePolygon = !PolyUtil.containsLocation(locValue, kebun.polygon, true)
        } else {
            isOutsidePolygon = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = KakaoColors.Surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                // Header (Compact & Native Android Able)
                Text(
                    text = "Tambah Pohon - Blok ${blok.kodeBlok}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = KakaoColors.TextPrimary
                )
                Text(
                    text = "${blok.namaBlok} • ${kebun.namaKebun}",
                    fontSize = 12.sp,
                    color = KakaoColors.TextSecondary,
                    maxLines = 1
                )

                Spacer(Modifier.height(14.dp))

                // Metode Akuisisi Koordinat
                Text(
                    text = "Metode Titik Koordinat:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = KakaoColors.PrimaryDark
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    FilterChip(
                        selected = selectedMethod == PohonInputMethod.GPS_LAPANGAN,
                        onClick = {
                            selectedMethod = PohonInputMethod.GPS_LAPANGAN
                            if (hasPermission) {
                                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                                fusedClient.lastLocation.addOnSuccessListener { loc ->
                                    if (loc != null) location = LatLng(loc.latitude, loc.longitude)
                                }
                            }
                        },
                        label = { Text("GPS Lapangan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.height(32.dp)
                    )
                    FilterChip(
                        selected = selectedMethod == PohonInputMethod.MANUAL_TAP_PETA,
                        onClick = { selectedMethod = PohonInputMethod.MANUAL_TAP_PETA },
                        label = { Text("Titik Tengah Blok", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.height(32.dp)
                    )
                }

                Spacer(Modifier.height(6.dp))

                if (selectedMethod == PohonInputMethod.GPS_LAPANGAN && !hasPermission) {
                    Text("Izin GPS off. Sistem memakai titik tengah blok.", color = KakaoColors.Warning, fontSize = 11.sp)
                } else {
                    location?.let {
                        Text(
                            text = "Koordinat: ${String.format("%.5f", it.latitude)}, ${String.format("%.5f", it.longitude)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KakaoColors.TextPrimary
                        )
                    }
                }

                // Warning Point-in-Polygon (Compact)
                if (isOutsidePolygon) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = KakaoColors.Error.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = KakaoColors.Error, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Di luar batas blok. Tetap simpan?",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = KakaoColors.Error,
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = forceSaveConfirmed,
                                onCheckedChange = { forceSaveConfirmed = it },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Single Form Field: Label / Varietas (Tanpa Usia Tanaman!)
                OutlinedTextField(
                    value = varietas,
                    onValueChange = { varietas = it },
                    label = { Text("Label / Varietas Pohon", fontSize = 12.sp) },
                    placeholder = { Text("Contoh: MCC 02 / ICCRI 07", fontSize = 12.sp, color = KakaoColors.TextMuted) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = KakaoColors.TextPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Status Kesehatan Awal:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = KakaoColors.PrimaryDark
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                ) {
                    Button(
                        onClick = { status = PohonStatus.SEHAT },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (status == PohonStatus.SEHAT) KakaoColors.StepActive else KakaoColors.SurfaceMuted,
                            contentColor = if (status == PohonStatus.SEHAT) Color.White else KakaoColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(34.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("Sehat", fontSize = 11.sp, fontWeight = FontWeight.Bold) }

                    Button(
                        onClick = { status = PohonStatus.PERLU_PUPUK },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (status == PohonStatus.PERLU_PUPUK) KakaoColors.Warning else KakaoColors.SurfaceMuted,
                            contentColor = if (status == PohonStatus.PERLU_PUPUK) Color.White else KakaoColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(34.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("Pupuk", fontSize = 11.sp, fontWeight = FontWeight.Bold) }

                    Button(
                        onClick = { status = PohonStatus.TERSERANG_HAMA },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (status == PohonStatus.TERSERANG_HAMA) KakaoColors.Error else KakaoColors.SurfaceMuted,
                            contentColor = if (status == PohonStatus.TERSERANG_HAMA) Color.White else KakaoColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(34.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("Hama", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }

                Spacer(Modifier.height(18.dp))

                // Buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = KakaoColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val locValue = location ?: return@Button
                            val newTree = PohonKakao(
                                id = "TREE-${blok.kodeBlok}-${System.currentTimeMillis()}",
                                kodePohon = "P-${blok.kodeBlok}-${(blok.totalPohon + 1).toString().padStart(2, '0')}",
                                location = locValue,
                                umurTahun = 2.5, // Default usia tanaman automatic
                                status = status,
                                tanggalPemupukanTerakhir = "Baru Didata",
                                varietasKakao = varietas.ifBlank { "MCC 02 (Sulawesi 2)" },
                                estimasiHasilKg = 3.5,
                                catatanPetani = "Ditambahkan lewat Alur Mandiri v2",
                                inputMethod = selectedMethod,
                                syncStatus = SyncStatus.PENDING_SYNC
                            )
                            onSave(newTree)
                        },
                        enabled = location != null && (!isOutsidePolygon || forceSaveConfirmed),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KakaoColors.PrimaryDark),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text("Simpan", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

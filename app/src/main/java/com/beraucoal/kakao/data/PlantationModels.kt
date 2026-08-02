package com.beraucoal.kakao.data

import com.google.android.gms.maps.model.LatLng

enum class PohonStatus(val label: String) {
    SEHAT("Sehat"),
    PERLU_PUPUK("Butuh Pemupukan"),
    TERSERANG_HAMA("Terserang Hama PBK/VSD")
}

data class PohonKakao(
    val id: String,
    val kodePohon: String,
    val location: LatLng,
    val umurTahun: Double,
    val status: PohonStatus,
    val tanggalPemupukanTerakhir: String,
    val varietasKakao: String = "MCC 02 (Sulawesi 2)",
    val estimasiHasilKg: Double = 3.5,
    val catatanPetani: String = "Pohon produktif dalam pengawasan rutin"
)

data class BlokBagian(
    val id: String,
    val kodeBlok: String,
    val namaBlok: String,
    val polygon: List<LatLng>,
    val pohonList: List<PohonKakao>
) {
    val totalPohon: Int get() = pohonList.size
    val pohonSehatCount: Int get() = pohonList.count { it.status == PohonStatus.SEHAT }
    val pohonHamaCount: Int get() = pohonList.count { it.status == PohonStatus.TERSERANG_HAMA }
    val pohonPupukCount: Int get() = pohonList.count { it.status == PohonStatus.PERLU_PUPUK }
}

data class KebunArea(
    val id: String,
    val namaKebun: String,
    val namaPemilik: String,
    val idPetani: String,
    val nomorWa: String,
    val luasHektar: Double,
    val centerLocation: LatLng,
    val polygon: List<LatLng>,
    val blokList: List<BlokBagian>
) {
    val totalPohon: Int get() = blokList.sumOf { it.totalPohon }
    val totalPohonSehat: Int get() = blokList.sumOf { it.pohonSehatCount }
    val totalPohonHama: Int get() = blokList.sumOf { it.pohonHamaCount }
    val totalPohonPupuk: Int get() = blokList.sumOf { it.pohonPupukCount }
}

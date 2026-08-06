package com.beraucoal.kakao.data

import com.google.android.gms.maps.model.LatLng

enum class PohonStatus(val label: String) {
    SEHAT("Sehat"),
    PERLU_PUPUK("Butuh Pemupukan"),
    TERSERANG_HAMA("Terserang Hama PBK/VSD")
}

enum class KebunVerificationStatus(val label: String) {
    DRAFT("Draft - belum dikirim"),
    PENDING_REVIEW("Menunggu Verifikasi"),
    VERIFIED("Terverifikasi"),
    REJECTED("Perlu Revisi")
}

enum class SyncStatus(val label: String) {
    PENDING_SYNC("Pending Sync (Lokal)"),
    SYNCED("Synced"),
    SYNC_FAILED("Gagal Sync")
}

enum class PohonInputMethod(val label: String) {
    GPS_LAPANGAN("Sensor GPS Lapangan"),
    MANUAL_TAP_PETA("Tap Manual Peta")
}

data class RiwayatPelaporan(
    val id: String,
    val tanggal: String,
    val status: PohonStatus,
    val catatan: String,
    val pelapor: String = "Tim Agronomi & Petani"
)

data class PohonKakao(
    val id: String,
    val kodePohon: String,
    val location: LatLng,
    val umurTahun: Double,
    val status: PohonStatus,
    val tanggalPemupukanTerakhir: String,
    val varietasKakao: String = "MCC 02 (Sulawesi 2)",
    val estimasiHasilKg: Double = 3.5,
    val catatanPetani: String = "Pohon produktif dalam pengawasan rutin",
    val inputMethod: PohonInputMethod = PohonInputMethod.MANUAL_TAP_PETA,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val riwayatList: List<RiwayatPelaporan> = emptyList()
) {
    val riwayatAktif: List<RiwayatPelaporan>
        get() = if (riwayatList.isNotEmpty()) riwayatList else listOf(
            RiwayatPelaporan(
                id = "R-1",
                tanggal = tanggalPemupukanTerakhir,
                status = status,
                catatan = catatanPetani,
                pelapor = "Pemeriksaan Lapangan Terkini"
            ),
            RiwayatPelaporan(
                id = "R-2",
                tanggal = "20 Juni 2026",
                status = PohonStatus.SEHAT,
                catatan = "Pemupukan organik NPK dan pembersihan gulma sekeliling tajuk pohon.",
                pelapor = "Petani Mandiri"
            ),
            RiwayatPelaporan(
                id = "R-3",
                tanggal = "15 Mei 2026",
                status = PohonStatus.SEHAT,
                catatan = "Inspeksi rutin masa perkembangan bunga dan pentil buah kakao.",
                pelapor = "Tim Agronomi Berau Coal"
            )
        )
}

data class BlokBagian(
    val id: String,
    val kodeBlok: String,
    val namaBlok: String,
    val polygon: List<LatLng>,
    val pohonList: List<PohonKakao>,
    val isDefaultBlok: Boolean = false
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
    val blokList: List<BlokBagian>,
    val isSelfMapped: Boolean = false,
    val verificationStatus: KebunVerificationStatus = KebunVerificationStatus.VERIFIED,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val overlapFlag: Boolean = false
) {
    val totalPohon: Int get() = blokList.sumOf { it.totalPohon }
    val totalPohonSehat: Int get() = blokList.sumOf { it.pohonSehatCount }
    val totalPohonHama: Int get() = blokList.sumOf { it.pohonHamaCount }
    val totalPohonPupuk: Int get() = blokList.sumOf { it.pohonPupukCount }
}


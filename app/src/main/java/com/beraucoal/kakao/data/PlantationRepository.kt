package com.beraucoal.kakao.data

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlantationRepository {

    private val _kebunList = MutableStateFlow<List<KebunArea>>(emptyList())
    val kebunList: StateFlow<List<KebunArea>> = _kebunList.asStateFlow()

    init {
        _kebunList.value = generateMockPlantationData()
    }

    fun getKebunById(kebunId: String): KebunArea? {
        return _kebunList.value.find { it.id == kebunId }
    }

    fun updatePohonStatus(kebunId: String, blokId: String, pohonId: String, newStatus: PohonStatus) {
        _kebunList.value = _kebunList.value.map { kebun ->
            if (kebun.id == kebunId) {
                kebun.copy(
                    blokList = kebun.blokList.map { blok ->
                        if (blok.id == blokId) {
                            blok.copy(
                                pohonList = blok.pohonList.map { pohon ->
                                    if (pohon.id == pohonId) {
                                        pohon.copy(status = newStatus)
                                    } else pohon
                                }
                            )
                        } else blok
                    }
                )
            } else kebun
        }
    }

    fun addSelfMappedKebun(
        namaKebun: String,
        namaPemilik: String = "Petani Terverifikasi",
        idPetani: String = "PETANI-MANDIRI-01",
        nomorWa: String = "081234567890",
        luasHektar: Double,
        polygon: List<LatLng>,
        catatan: String = ""
    ): KebunArea {
        val kebunId = "KEBUN-SELF-${System.currentTimeMillis()}"
        val center = if (polygon.isNotEmpty()) {
            val avgLat = polygon.map { it.latitude }.average()
            val avgLng = polygon.map { it.longitude }.average()
            LatLng(avgLat, avgLng)
        } else LatLng(2.1500, 117.4667)

        val isOverlapping = checkOverlap(polygon)

        val defaultBlok = BlokBagian(
            id = "BLOK-$kebunId-01",
            kodeBlok = "DEFAULT",
            namaBlok = "Blok Utama - $namaKebun",
            polygon = polygon,
            pohonList = emptyList(),
            isDefaultBlok = true
        )

        val newKebun = KebunArea(
            id = kebunId,
            namaKebun = namaKebun,
            namaPemilik = namaPemilik,
            idPetani = idPetani,
            nomorWa = nomorWa,
            luasHektar = (luasHektar * 100.0).toInt() / 100.0,
            centerLocation = center,
            polygon = polygon,
            blokList = listOf(defaultBlok),
            isSelfMapped = true,
            verificationStatus = KebunVerificationStatus.PENDING_REVIEW,
            syncStatus = SyncStatus.PENDING_SYNC,
            overlapFlag = isOverlapping
        )

        _kebunList.value = _kebunList.value + newKebun
        simulateBackgroundSync(kebunId)
        return newKebun
    }

    fun addPohonToBlok(kebunId: String, blokId: String, newPohon: PohonKakao): Boolean {
        var added = false
        _kebunList.value = _kebunList.value.map { kebun ->
            if (kebun.id == kebunId) {
                kebun.copy(
                    blokList = kebun.blokList.map { blok ->
                        if (blok.id == blokId) {
                            added = true
                            blok.copy(pohonList = blok.pohonList + newPohon)
                        } else blok
                    }
                )
            } else kebun
        }
        return added
    }

    fun checkOverlap(newPolygon: List<LatLng>, excludeKebunId: String? = null): Boolean {
        if (newPolygon.isEmpty()) return false
        val existingKebuns = _kebunList.value.filter { it.id != excludeKebunId && it.polygon.isNotEmpty() }
        for (existing in existingKebuns) {
            for (pt in newPolygon) {
                if (PolyUtil.containsLocation(pt, existing.polygon, true)) {
                    return true
                }
            }
            for (pt in existing.polygon) {
                if (PolyUtil.containsLocation(pt, newPolygon, true)) {
                    return true
                }
            }
        }
        return false
    }

    private fun simulateBackgroundSync(kebunId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            delay(4000)
            _kebunList.value = _kebunList.value.map { kebun ->
                if (kebun.id == kebunId) kebun.copy(syncStatus = SyncStatus.SYNCED)
                else kebun
            }
        }
    }


    private fun generateMockPlantationData(): List<KebunArea> {
        val baseCenter = LatLng(2.15340, 117.48120) // Bedungun / Tanjung Redeb, Berau

        // Kebun 1: Kebun Kakao Utama Bedungun (Budi Kakao)
        val kebun1Center = LatLng(baseCenter.latitude, baseCenter.longitude)
        val kebun1Polygon = listOf(
            LatLng(kebun1Center.latitude + 0.003, kebun1Center.longitude - 0.003),
            LatLng(kebun1Center.latitude + 0.003, kebun1Center.longitude + 0.003),
            LatLng(kebun1Center.latitude - 0.003, kebun1Center.longitude + 0.003),
            LatLng(kebun1Center.latitude - 0.003, kebun1Center.longitude - 0.003)
        )

        // Blok A1 (Utara)
        val blokA1Poly = listOf(
            LatLng(kebun1Center.latitude + 0.0028, kebun1Center.longitude - 0.0028),
            LatLng(kebun1Center.latitude + 0.0028, kebun1Center.longitude + 0.0028),
            LatLng(kebun1Center.latitude + 0.0002, kebun1Center.longitude + 0.0028),
            LatLng(kebun1Center.latitude + 0.0002, kebun1Center.longitude - 0.0028)
        )
        val pohonBlokA1 = generateTreeGrid("A1", LatLng(kebun1Center.latitude + 0.0015, kebun1Center.longitude), 4, 5, 0.0004)

        // Blok A2 (Selatan)
        val blokA2Poly = listOf(
            LatLng(kebun1Center.latitude - 0.0002, kebun1Center.longitude - 0.0028),
            LatLng(kebun1Center.latitude - 0.0002, kebun1Center.longitude + 0.0028),
            LatLng(kebun1Center.latitude - 0.0028, kebun1Center.longitude + 0.0028),
            LatLng(kebun1Center.latitude - 0.0028, kebun1Center.longitude - 0.0028)
        )
        val pohonBlokA2 = generateTreeGrid("A2", LatLng(kebun1Center.latitude - 0.0015, kebun1Center.longitude), 4, 5, 0.0004)

        val kebun1 = KebunArea(
            id = "KEBUN-BERAU-01",
            namaKebun = "Lahan Kakao Bedungun Perdana",
            namaPemilik = "Budi Kakao Berau",
            idPetani = "PETANI-BERAU-01",
            nomorWa = "081234567890",
            luasHektar = 2.5,
            centerLocation = kebun1Center,
            polygon = kebun1Polygon,
            blokList = listOf(
                BlokBagian("BLOK-A1", "A1", "Blok A1 - Pembibitan & Produksi Utama", blokA1Poly, pohonBlokA1),
                BlokBagian("BLOK-A2", "A2", "Blok A2 - Kakao Sambung Pucuk", blokA2Poly, pohonBlokA2)
            )
        )

        // Kebun 2: Kebun Kakao Teluk Bayur (Siti Aminah)
        val kebun2Center = LatLng(baseCenter.latitude + 0.008, baseCenter.longitude + 0.007)
        val kebun2Polygon = listOf(
            LatLng(kebun2Center.latitude + 0.0025, kebun2Center.longitude - 0.0025),
            LatLng(kebun2Center.latitude + 0.0025, kebun2Center.longitude + 0.0025),
            LatLng(kebun2Center.latitude - 0.0025, kebun2Center.longitude + 0.0025),
            LatLng(kebun2Center.latitude - 0.0025, kebun2Center.longitude - 0.0025)
        )
        val pohonBlokB1 = generateTreeGrid("B1", kebun2Center, 4, 4, 0.0004)

        val kebun2 = KebunArea(
            id = "KEBUN-BERAU-02",
            namaKebun = "Perkebunan Kakao Teluk Bayur",
            namaPemilik = "Siti Aminah",
            idPetani = "PETANI-BERAU-02",
            nomorWa = "081398765432",
            luasHektar = 1.8,
            centerLocation = kebun2Center,
            polygon = kebun2Polygon,
            blokList = listOf(
                BlokBagian("BLOK-B1", "B1", "Blok B1 - Produksi Kakao Organik", kebun2Polygon, pohonBlokB1)
            )
        )

        return listOf(kebun1, kebun2)
    }

    private fun generateTreeGrid(
        blokCode: String,
        center: LatLng,
        rows: Int,
        cols: Int,
        step: Double
    ): List<PohonKakao> {
        val trees = mutableListOf<PohonKakao>()
        var counter = 1
        val startLat = center.latitude - ((rows - 1) * step / 2)
        val startLng = center.longitude - ((cols - 1) * step / 2)

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val lat = startLat + (r * step)
                val lng = startLng + (c * step)
                val status = when {
                    counter % 5 == 0 -> PohonStatus.TERSERANG_HAMA
                    counter % 3 == 0 -> PohonStatus.PERLU_PUPUK
                    else -> PohonStatus.SEHAT
                }
                trees.add(
                    PohonKakao(
                        id = "TREE-$blokCode-$counter",
                        kodePohon = "P-$blokCode-${String.format("%02d", counter)}",
                        location = LatLng(lat, lng),
                        umurTahun = 3.5 + (counter % 3),
                        status = status,
                        tanggalPemupukanTerakhir = "15 Juli 2026",
                        varietasKakao = if (counter % 2 == 0) "MCC 02 (Sulawesi 2)" else "ICCRI 07",
                        estimasiHasilKg = 3.0 + (counter % 4) * 0.5
                    )
                )
                counter++
            }
        }
        return trees
    }

    companion object {
        /**
         * Algoritma pengecekan perpotongan garis poli-titik (self-intersection).
         * Digunakan setiap penambahan titik baru saat pemetaan polygon mandiri.
         */
        fun hasSelfIntersection(points: List<LatLng>): Boolean {
            val n = points.size
            if (n < 4) return false
            for (i in 0 until n - 1) {
                val p1 = points[i]
                val p2 = points[i + 1]
                for (j in i + 2 until n - 1) {
                    if (i == 0 && j == n - 2 && points.first() == points.last()) continue
                    val q1 = points[j]
                    val q2 = points[j + 1]
                    if (segmentsIntersect(p1, p2, q1, q2)) {
                        return true
                    }
                }
            }
            if (n >= 4) {
                val last = points.last()
                val first = points.first()
                for (i in 1 until n - 2) {
                    val q1 = points[i]
                    val q2 = points[i + 1]
                    if (segmentsIntersect(last, first, q1, q2)) {
                        return true
                    }
                }
            }
            return false
        }

        private fun orientation(p: LatLng, q: LatLng, r: LatLng): Int {
            val valResult = (q.longitude - p.longitude) * (r.latitude - q.latitude) -
                    (q.latitude - p.latitude) * (r.longitude - q.longitude)
            if (Math.abs(valResult) < 1e-9) return 0
            return if (valResult > 0) 1 else 2
        }

        private fun onSegment(p: LatLng, q: LatLng, r: LatLng): Boolean {
            return q.latitude <= Math.max(p.latitude, r.latitude) && q.latitude >= Math.min(p.latitude, r.latitude) &&
                    q.longitude <= Math.max(p.longitude, r.longitude) && q.longitude >= Math.min(p.longitude, r.longitude)
        }

        private fun segmentsIntersect(p1: LatLng, q1: LatLng, p2: LatLng, q2: LatLng): Boolean {
            val o1 = orientation(p1, q1, p2)
            val o2 = orientation(p1, q1, q2)
            val o3 = orientation(p2, q2, p1)
            val o4 = orientation(p2, q2, q1)

            if (o1 != o2 && o3 != o4) return true
            if (o1 == 0 && onSegment(p1, p2, q1)) return true
            if (o2 == 0 && onSegment(p1, q2, q1)) return true
            if (o3 == 0 && onSegment(p2, p1, q2)) return true
            if (o4 == 0 && onSegment(p2, q1, q2)) return true

            return false
        }
    }
}

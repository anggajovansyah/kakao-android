package com.beraucoal.kakao.data

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
}

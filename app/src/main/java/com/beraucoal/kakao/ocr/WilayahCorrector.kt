package com.beraucoal.kakao.ocr

import android.content.Context
import java.util.zip.GZIPInputStream
import kotlin.math.max

/**
 * Mengoreksi hasil OCR field Kabupaten/Kota, Kecamatan, dan Kelurahan/Desa
 * dengan mencocokkannya ke data wilayah resmi Indonesia (BPS/Kemendagri),
 * supaya salah-baca huruf kecil dari ML Kit (mis. "GKAMPEK" -> "CIKAMPEK",
 * "TAMAN SARE" -> "TAMAN SARI") bisa otomatis dibetulkan.
 *
 * Pencocokan dilakukan BERTAHAP dan DIBATASI LINGKUP supaya tidak salah
 * cocok ke desa lain yang kebetulan namanya mirip di provinsi lain:
 *   1. Cocokkan teks Kabupaten/Kota (baris ke-2 KTP, di bawah Provinsi)
 *      terhadap SEMUA kabupaten/kota se-Indonesia.
 *   2. Cocokkan Kecamatan HANYA terhadap kecamatan-kecamatan di dalam
 *      kabupaten/kota yang sudah ketemu di langkah 1.
 *   3. Cocokkan Kelurahan/Desa HANYA terhadap desa-desa di dalam kecamatan
 *      yang sudah ketemu di langkah 2.
 *
 * Data disimpan sebagai asset .txt.gz (dipadatkan dari dataset BPS/Kemendagri)
 * supaya ukuran APK tidak bengkak -- didekompresi & di-parse sekali saat
 * pertama dipakai, lalu disimpan di memori untuk pemakaian berikutnya.
 */
class WilayahCorrector(private val context: Context) {

    data class CorrectionResult(
        val kecamatan: String,
        val kelurahanDesa: String,
        val kecamatanCorrected: Boolean,
        val kelurahanCorrected: Boolean
    )

    private var regencyNames: List<String> = emptyList() // semua nama kabupaten/kota
    private var regencyCodeByName: Map<String, String> = emptyMap()
    private var districtsByRegency: Map<String, List<Pair<String, String>>> = emptyMap() // regencyCode -> [(districtCode, name)]
    private var villagesByDistrict: Map<String, List<String>> = emptyMap() // districtCode -> [namaDesa]
    private var districtCodeByRegencyAndName: Map<String, String> = emptyMap() // "regencyCode|NAMA" -> districtCode

    private var loaded = false
    private var loadFailed = false

    /**
     * Wajib dipanggil (dari background thread/coroutine IO) sebelum correct().
     * SENGAJA tidak melempar exception ke pemanggil -- kalau load gagal (asset
     * korup, dsb.), `loadFailed` jadi true dan correct() akan diam-diam
     * mengembalikan data apa adanya (tanpa koreksi) alih-alih men-crash app.
     */
    fun ensureLoaded() {
        if (loaded || loadFailed) return
        synchronized(this) {
            if (loaded || loadFailed) return
            try {
                val regencies = readGzAsset("wilayah/regencies.txt.gz")
                    .mapNotNull { line ->
                        val parts = line.split("|")
                        if (parts.size < 2) null else parts[0] to parts[1]
                    }
                    .toMap()
                regencyNames = regencies.values.toList()
                regencyCodeByName = regencies.entries.associate { (code, name) -> name to code }

                val districtsMap = mutableMapOf<String, MutableList<Pair<String, String>>>()
                val districtCodeMap = mutableMapOf<String, String>()
                readGzAsset("wilayah/districts.txt.gz").forEach { line ->
                    val parts = line.split("|")
                    if (parts.size < 3) return@forEach
                    val (districtCode, regencyCode, name) = parts
                    districtsMap.getOrPut(regencyCode) { mutableListOf() }.add(districtCode to name)
                    districtCodeMap["$regencyCode|$name"] = districtCode
                }
                districtsByRegency = districtsMap
                districtCodeByRegencyAndName = districtCodeMap

                val villagesMap = mutableMapOf<String, MutableList<String>>()
                readGzAsset("wilayah/villages.txt.gz").forEach { line ->
                    val parts = line.split("|")
                    if (parts.size < 2) return@forEach
                    val (districtCode, name) = parts
                    villagesMap.getOrPut(districtCode) { mutableListOf() }.add(name)
                }
            villagesByDistrict = villagesMap

                loaded = true
            } catch (e: Exception) {
                // Gagal load (asset hilang/korup, dsb.) -- jangan crash app,
                // cukup tandai gagal supaya correct() skip koreksi dan
                // kembalikan hasil OCR apa adanya.
                loadFailed = true
            }
        }
    }

    private fun readGzAsset(path: String): List<String> {
        context.assets.open(path).use { input ->
            GZIPInputStream(input).bufferedReader(Charsets.UTF_8).use { reader ->
                return reader.readLines()
            }
        }
    }

    /**
     * @param rawOcrText teks OCR mentah (dipakai untuk cari baris Kabupaten/Kota
     *        di dekat awal teks -- posisinya lebih diandalkan daripada label
     *        teksnya sendiri karena OCR sering typo di kata "KABUPATEN"/"KOTA").
     */
    fun correct(rawOcrText: String, kecamatanRaw: String, kelurahanRaw: String): CorrectionResult {
        if (!loaded) {
            // Belum ter-load (mis. dipanggil di luar alur yang benar) -- kembalikan apa adanya.
            return CorrectionResult(kecamatanRaw, kelurahanRaw, false, false)
        }
        if (kecamatanRaw.isBlank()) return CorrectionResult(kecamatanRaw, kelurahanRaw, false, false)

        // Baris Kabupaten/Kota biasanya baris ke-2 di KTP (setelah "PROVINSI ...").
        val lines = rawOcrText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val kabupatenGuess = lines.getOrNull(1) ?: ""

        val regencyMatch = fuzzyBest(kabupatenGuess, regencyNames, cutoff = 0.5f)
            ?: return CorrectionResult(kecamatanRaw, kelurahanRaw, false, false)
        val regencyCode = regencyCodeByName[regencyMatch] ?: return CorrectionResult(kecamatanRaw, kelurahanRaw, false, false)

        val districtCandidates = districtsByRegency[regencyCode].orEmpty()
        val districtMatch = fuzzyBest(kecamatanRaw, districtCandidates.map { it.second }, cutoff = 0.6f)
        val correctedKecamatan = districtMatch ?: kecamatanRaw
        val kecamatanCorrected = districtMatch != null && districtMatch != kecamatanRaw

        if (districtMatch == null || kelurahanRaw.isBlank()) {
            return CorrectionResult(correctedKecamatan, kelurahanRaw, kecamatanCorrected, false)
        }

        val districtCode = districtCodeByRegencyAndName["$regencyCode|$districtMatch"]
        val villageCandidates = villagesByDistrict[districtCode].orEmpty()
        val villageMatch = fuzzyBest(kelurahanRaw, villageCandidates, cutoff = 0.6f)
        val correctedKelurahan = villageMatch ?: kelurahanRaw
        val kelurahanCorrected = villageMatch != null && villageMatch != kelurahanRaw

        return CorrectionResult(correctedKecamatan, correctedKelurahan, kecamatanCorrected, kelurahanCorrected)
    }

    private fun fuzzyBest(query: String, candidates: List<String>, cutoff: Float): String? {
        if (query.isBlank() || candidates.isEmpty()) return null
        val q = query.uppercase()
        var best: String? = null
        var bestScore = 0f
        for (candidate in candidates) {
            val score = levenshteinRatio(q, candidate.uppercase())
            if (score > bestScore) {
                bestScore = score
                best = candidate
            }
        }
        return if (bestScore >= cutoff) best else null
    }

    /** Rasio kemiripan 0..1 berbasis Levenshtein distance (1 = identik). */
    private fun levenshteinRatio(a: String, b: String): Float {
        if (a.isEmpty() && b.isEmpty()) return 1f
        val distance = levenshteinDistance(a, b)
        return 1f - distance.toFloat() / max(a.length, b.length)
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = IntArray(n + 1) { it }
        for (i in 1..m) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..n) {
                val temp = dp[j]
                dp[j] = minOf(
                    dp[j] + 1,
                    dp[j - 1] + 1,
                    prev + if (a[i - 1] == b[j - 1]) 0 else 1
                )
                prev = temp
            }
        }
        return dp[n]
    }
}

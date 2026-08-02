package com.beraucoal.kakao.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.beraucoal.kakao.data.KtpData
import com.beraucoal.kakao.data.OcrConfidence
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.max

/**
 * Membungkus ML Kit Text Recognition (on-device, gratis) khusus buat baca KTP Indonesia.
 *
 * CATATAN AKURASI: KTP Indonesia sering silau/blur difoto pakai kamera HP biasa.
 * Analyzer ini dilengkapi dengan algoritma OCR Character Healing (memulihkan typo angka NIK),
 * Pattern Anchoring untuk ekstraksi Tempat/Tgl Lahir, dan Levenshtein fuzzy label matching.
 * Hasil parsing tetap ditampilkan ke layar verifikasi supaya petani/admin bisa memeriksa sebelum lanjut.
 */
class KtpOcrAnalyzer {

    private val recognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap): KtpData {
        val image = InputImage.fromBitmap(bitmap, 0)
        val visionText = suspendCancellableCoroutine<Text> { cont ->
            recognizer.process(image)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
        val reconstructedText = reconstructRowOrder(visionText)
        return parseKtpText(reconstructedText)
    }

    /**
     * PENTING: ML Kit sering membaca KTP secara KOLOM (semua label dari atas
     * ke bawah dulu, baru semua isi) alih-alih BARIS (label lalu isinya di
     * baris yang sama) -- karena label dan isi tercetak di dua kolom terpisah
     * dan `visionText.text` bawaan ML Kit tidak menjamin urutan baca manusia
     * untuk layout dua kolom seperti ini.
     *
     * Fungsi ini menyusun ulang urutan teks berdasarkan POSISI ASLI di gambar:
     * kelompokkan baris (Text.Line) yang koordinat Y-nya berdekatan jadi satu
     * "baris fisik" (row), lalu di dalam tiap row urutkan dari kiri ke kanan.
     * Hasilnya: satu baris = "<label> <isi>" sesuai posisi asli di kartu.
     */
    private fun reconstructRowOrder(visionText: Text): String {
        data class LineBox(val text: String, val top: Int, val bottom: Int, val left: Int)

        val allLines = visionText.textBlocks
            .flatMap { it.lines }
            .mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                LineBox(line.text, box.top, box.bottom, box.left)
            }

        if (allLines.isEmpty()) return visionText.text

        val avgHeight = allLines.map { it.bottom - it.top }.average().takeIf { !it.isNaN() } ?: 30.0
        val rowThreshold = avgHeight * 0.6 // toleransi Y supaya baris sejajar dianggap 1 row

        val sortedByTop = allLines.sortedBy { it.top }
        val rows = mutableListOf<MutableList<LineBox>>()

        for (line in sortedByTop) {
            val lineCenter = (line.top + line.bottom) / 2.0
            val existingRow = rows.lastOrNull()?.takeIf { row ->
                val rowCenter = row.map { (it.top + it.bottom) / 2.0 }.average()
                abs(lineCenter - rowCenter) <= rowThreshold
            }
            if (existingRow != null) existingRow.add(line) else rows.add(mutableListOf(line))
        }

        return rows.joinToString("\n") { row -> row.sortedBy { it.left }.joinToString(" ") { it.text } }
    }

    /**
     * Baris KTP hasil rekonstruksi biasanya berbentuk "<label> <ISI>".
     * Di KTP asli, ISI field (nama, alamat, dst.) SELALU dicetak HURUF KAPITAL
     * SEMUA, sedangkan label pakai kapital awal biasa. Heuristik ini memotong
     * kata-kata awal yang BUKAN huruf kapital semua/angka -- sisanya isinya.
     */
    private fun stripLabelPrefix(row: String): String {
        val words = row.trim().split(Regex("\\s+"))
        val valueStartIndex = words.indexOfFirst { word ->
            val cleaned = word.trim(',', '.', ':', '-')
            cleaned.isNotEmpty() && (
                (cleaned == cleaned.uppercase() && cleaned.any { it.isLetter() }) ||
                    cleaned.any { it.isDigit() }
                )
        }
        return if (valueStartIndex == -1) "" else words.subList(valueStartIndex, words.size).joinToString(" ").trim(':', '-', ' ')
    }

    private fun findFieldByLabelFuzzy(lines: List<String>, vararg labelHints: String): String {
        for (hint in labelHints) {
            // Coba pencocokan substring biasa terlebih dahulu
            val idx = lines.indexOfFirst { it.contains(hint, ignoreCase = true) }
            if (idx != -1) {
                val stripped = stripLabelPrefix(lines[idx])
                if (stripped.isNotBlank()) return stripped
            }
        }
        // Jika gagal, gunakan pencocokan Levenshtein pada kata pertama di baris (mengatasi label typo dari OCR)
        for (line in lines) {
            val prefixWords = line.take(25).lowercase().trim()
            for (hint in labelHints) {
                if (levenshteinRatio(prefixWords, hint.lowercase()) >= 0.7f) {
                    val stripped = stripLabelPrefix(line)
                    if (stripped.isNotBlank()) return stripped
                }
            }
        }
        return ""
    }

    /**
     * Algoritma OCR Character Healing:
     * Memperbaiki kesalahan pembacaan kamera yang sering mengubah angka pada NIK
     * menjadi karakter abjad berwujud mirip (seperti O->0, l->1, S->5, Z->2, B->8).
     */
    private fun extractAndHealNik(rawText: String, lines: List<String>): Pair<String, Int> {
        // Coba cari NIK murni 16 digit terlebih dahulu
        val pureMatch = Regex("""\b\d{16}\b""").find(rawText)?.value
        if (pureMatch != null) {
            val rowIdx = lines.indexOfFirst { it.contains(pureMatch) }
            return Pair(pureMatch, rowIdx)
        }

        // Kalau tidak ada 16 digit murni, lacak baris per baris mencari token 15-17 karakter berdominasi angka
        for ((idx, line) in lines.withIndex()) {
            val tokens = line.split(Regex("""\s+"""))
            for (token in tokens) {
                val clean = token.trim(':', '.', '-', ',')
                if (clean.length in 15..17 && clean.count { it.isDigit() } >= 9) {
                    // Lakukan karakter healing pada huruf-huruf tipis pembeda angka
                    val healed = clean.map { char ->
                        when (char) {
                            'O', 'o', 'D' -> '0'
                            'l', 'i', 'I', '|' -> '1'
                            'Z', 'z' -> '2'
                            'A', 'a' -> '4'
                            'S', 's' -> '5'
                            'G', 'g' -> '6'
                            'B', 'b' -> '8'
                            else -> char
                        }
                    }.joinToString("").filter { it.isDigit() }

                    if (healed.length == 16) {
                        return Pair(healed, idx)
                    }
                }
            }
        }
        return Pair("", -1)
    }

    /**
     * Ekstraksi Tempat/Tgl Lahir berbasis Pattern Anchoring (pola tanggal atau bulan Indonesia).
     * Mampu menemukan TTL dengan akurat meskipun posisi offset baris tergeser kibasan cahaya/miring.
     */
    private fun extractTtlWithPattern(lines: List<String>, fallbackOffsetValue: String): String {
        val monthsIndo = "JANUARI|FEBRUARI|MARET|APRIL|MEI|JUNI|JULI|AGUSTUS|SEPTEMBER|OKTOBER|NOPEMBER|NOVEMBER|DESEMBER"
        val datePattern = Regex("""(\b\d{1,2}[-\s/]\d{1,2}[-\s/]\d{2,4}\b|\b\d{1,2}\s+($monthsIndo)\s+\d{2,4}\b)""", RegexOption.IGNORE_CASE)

        for (line in lines) {
            if (datePattern.containsMatchIn(line) && !line.contains("BERLAKU", ignoreCase = true) && !line.contains("EXPIRED", ignoreCase = true)) {
                val stripped = stripLabelPrefix(line)
                if (stripped.isNotBlank()) return stripped
            }
        }
        return fallbackOffsetValue
    }

    /**
     * Field identitas inti KTP Indonesia SELALU berurutan tepat seperti ini
     * tepat di bawah baris NIK: Nama, Tempat/Tgl Lahir, Jenis Kelamin, Alamat,
     * RT/RW, Kel/Desa, Kecamatan, Agama. Karena label hasil OCR sering typo
     * parah (mis. "TempatIgi Lahir", "Jenis klamin"), posisi baris relatif ke
     * NIK dipadukan dengan Pattern Anchoring dan Levenshtein Fuzzy matching.
     */
    private fun parseKtpText(rawText: String): KtpData {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val (nik, nikRowIndex) = extractAndHealNik(rawText, lines)

        fun fieldAtOffset(offset: Int): String {
            if (nikRowIndex == -1) return ""
            val idx = nikRowIndex + offset
            if (idx !in lines.indices) return ""
            return stripLabelPrefix(lines[idx])
        }

        val nama = fieldAtOffset(1).ifBlank { findFieldByLabelFuzzy(lines, "Nama", "Nama:", "NAMA") }
        val ttlRaw = fieldAtOffset(2).ifBlank { findFieldByLabelFuzzy(lines, "Tempat/Tgl Lahir", "Tempat Tgl Lahir", "Tempat/Tgi") }
        val ttl = extractTtlWithPattern(lines, ttlRaw)
        
        val alamat = fieldAtOffset(4).ifBlank { findFieldByLabelFuzzy(lines, "Alamat", "Alamat:", "ALAMAT") }
        val kelurahan = fieldAtOffset(6).ifBlank { findFieldByLabelFuzzy(lines, "Kel/Desa", "Kelurahan", "Desa", "Kel/Desa:") }
        val kecamatan = fieldAtOffset(7).ifBlank { findFieldByLabelFuzzy(lines, "Kecamatan", "Kec.", "Kecamatan:") }

        val confidence = when {
            nik.length == 16 && nama.isNotBlank() && ttl.isNotBlank() -> OcrConfidence.HIGH
            nik.length == 16 || nama.isNotBlank() -> OcrConfidence.MEDIUM
            else -> OcrConfidence.LOW
        }

        return KtpData(
            nik = nik,
            nama = nama,
            tempatTanggalLahir = ttl,
            alamat = alamat,
            kelurahanDesa = kelurahan,
            kecamatan = kecamatan,
            rawOcrText = rawText,
            ocrConfidence = confidence
        )
    }

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

    fun close() {
        recognizer.close()
    }
}

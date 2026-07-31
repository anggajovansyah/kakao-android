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

/**
 * Membungkus ML Kit Text Recognition (on-device, gratis) khusus buat baca KTP Indonesia.
 *
 * CATATAN AKURASI: KTP Indonesia sering silau/blur difoto pakai kamera HP biasa.
 * Analyzer ini TIDAK melakukan auto-submit -- hasil parsing selalu ditampilkan
 * ke layar verifikasi supaya petani/admin bisa koreksi manual sebelum lanjut.
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
        return if (valueStartIndex == -1) "" else words.subList(valueStartIndex, words.size).joinToString(" ")
    }

    private fun findFieldByLabelFuzzy(lines: List<String>, vararg labelHints: String): String {
        for (hint in labelHints) {
            val idx = lines.indexOfFirst { it.contains(hint, ignoreCase = true) }
            if (idx == -1) continue
            val stripped = stripLabelPrefix(lines[idx])
            if (stripped.isNotBlank()) return stripped
        }
        return ""
    }

    /**
     * Field identitas inti KTP Indonesia SELALU berurutan tepat seperti ini
     * tepat di bawah baris NIK: Nama, Tempat/Tgl Lahir, Jenis Kelamin, Alamat,
     * RT/RW, Kel/Desa, Kecamatan, Agama. Karena label hasil OCR sering typo
     * parah (mis. "TempatIgi Lahir", "Jenis klamin"), posisi baris relatif ke
     * NIK jauh lebih diandalkan daripada mencocokkan teks label yang rusak.
     * Kalau posisi ini kosong, baru fallback ke pencocokan label fuzzy.
     */
    private fun parseKtpText(rawText: String): KtpData {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val nik = Regex("""\b\d{16}\b""").find(rawText)?.value ?: ""
        val nikRowIndex = if (nik.isNotEmpty()) lines.indexOfFirst { it.contains(nik) } else -1

        fun fieldAtOffset(offset: Int): String {
            if (nikRowIndex == -1) return ""
            val idx = nikRowIndex + offset
            if (idx !in lines.indices) return ""
            return stripLabelPrefix(lines[idx])
        }

        // offset 3 (Jenis Kelamin) dan 8 (Agama) sengaja dilewati -- belum ada
        // field-nya di KtpData, tapi tetap dihitung supaya offset field lain pas
        val nama = fieldAtOffset(1).ifBlank { findFieldByLabelFuzzy(lines, "Nama") }
        val ttl = fieldAtOffset(2).ifBlank { findFieldByLabelFuzzy(lines, "Tempat/Tgl Lahir", "Tempat Tgl Lahir") }
        val alamat = fieldAtOffset(4).ifBlank { findFieldByLabelFuzzy(lines, "Alamat") }
        val kelurahan = fieldAtOffset(6).ifBlank { findFieldByLabelFuzzy(lines, "Kel/Desa", "Kelurahan") }
        val kecamatan = fieldAtOffset(7).ifBlank { findFieldByLabelFuzzy(lines, "Kecamatan") }

        val confidence = when {
            nik.length == 16 && nama.isNotBlank() -> OcrConfidence.HIGH
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

    fun close() {
        recognizer.close()
    }
}

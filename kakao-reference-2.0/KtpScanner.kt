package id.itsb.kakao.ktp

import android.annotation.SuppressLint
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/**
 * Pembacaan KTP di perangkat.
 *
 * KENAPA ML KIT
 * -------------
 * Pilihan yang dipertimbangkan:
 *
 *   ML Kit Text Recognition v2 (dipakai)
 *     Berjalan penuh di HP, tanpa jaringan, tanpa biaya per pindaian, tanpa
 *     kunci API. Ini yang menentukan: petani memindai KTP di kantor desa atau
 *     di rumah, sering tanpa sinyal, dan foto KTP tidak perlu meninggalkan HP
 *     sebelum petani menyetujuinya. Minimal API 21, model Latin ±4 MB bila
 *     dibundel.
 *
 *   Google Cloud Vision / AWS Textract
 *     Akurasi mentahnya lebih tinggi, tetapi butuh internet saat memindai,
 *     berbiaya per panggilan, dan mengirim gambar KTP ke luar negeri sebelum
 *     petani melihat hasilnya. Untuk 147 petani, biayanya kecil — masalahnya
 *     ketergantungan jaringan pada langkah yang justru sering tanpa sinyal.
 *
 *   Tesseract (tesseract4android)
 *     Gratis dan luring, tetapi dirancang untuk dokumen hasil pindai datar.
 *     Pada foto kartu dengan pantulan, latar tercetak, dan hologram, hasilnya
 *     jauh di bawah ML Kit.
 *
 *   SDK e-KTP komersial (Verihubs, Privy, Asli RI)
 *     Paling akurat untuk KTP secara khusus dan sudah mengurus liveness.
 *     Berlangganan, dan menambah pihak ketiga yang memegang data KTP petani.
 *     Layak dipertimbangkan bila proyek diperluas jauh di atas 147 petani.
 *
 * YANG SEBENARNYA MENENTUKAN AKURASI
 * ----------------------------------
 * Bukan pustaka OCR-nya, melainkan struktur NIK. NIK memuat tanggal lahir dan
 * jenis kelamin (tanggal + 40 untuk perempuan). Kalau keduanya cocok dengan
 * kolom "Tempat/Tgl Lahir" yang dibaca terpisah, dua pembacaan independen
 * saling menguatkan — jauh lebih kuat daripada skor kepercayaan OCR mana pun.
 *
 * Parser di berkas ini SENGAJA ringan: tugasnya hanya memberi umpan balik
 * seketika di layar ("NIK terbaca, nama terbaca") dan menentukan kapan tombol
 * rana boleh terbuka. Parsing yang menentukan dilakukan server di app/ktp.py,
 * memakai baris teks yang sama yang dikirim aplikasi. Satu sumber kebenaran,
 * bisa diuji, dan bisa diperbaiki tanpa merilis ulang aplikasi.
 */
object KtpScanner {

    private val pengenal = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Salah baca yang paling sering terjadi pada blok angka NIK. */
    private val perbaikanDigit = mapOf(
        'O' to '0', 'o' to '0', 'D' to '0', 'Q' to '0',
        'I' to '1', 'l' to '1', '|' to '1', '!' to '1',
        'Z' to '2', 'z' to '2', 'A' to '4', 'S' to '5', 's' to '5',
        'G' to '6', 'T' to '7', 'B' to '8', 'g' to '9', 'q' to '9',
    )

    data class Bacaan(
        val baris: List<String> = emptyList(),
        val nik: String? = null,
        val nama: String? = null,
        val tglLahir: LocalDate? = null,
        val kelDesa: String? = null,
        val nikSah: Boolean = false,
        val silangCocok: Boolean = false,
    ) {
        /** Tombol rana baru terbuka kalau ini benar. Memfoto KTP yang belum
         *  terbaca hanya menghasilkan penolakan dari server dan membuat petani
         *  mengulang tanpa tahu apa yang salah. */
        val siapDifoto: Boolean get() = nikSah && !nama.isNullOrBlank() && tglLahir != null

        val jumlahKolomTerbaca: Int
            get() = listOfNotNull(nik, nama, tglLahir, kelDesa).size
    }

    @SuppressLint("UnsafeOptInUsageError")
    suspend fun bacaBingkai(proxy: ImageProxy): Bacaan {
        val gambar = proxy.image ?: return Bacaan()
        val input = InputImage.fromMediaImage(gambar, proxy.imageInfo.rotationDegrees)
        val teks = pengenal.process(input).await()
        return urai(teks.textBlocks.flatMap { blok -> blok.lines.map { it.text } })
    }

    fun urai(baris: List<String>): Bacaan {
        val bersih = baris.map { it.trim() }.filter { it.isNotBlank() }

        val nik = cariNik(bersih)
        val sah = nik != null && nikSah(nik)
        val dariNik = nik?.let { tanggalDariNik(it) }
        val dariKolom = cariTanggal(bersih)

        return Bacaan(
            baris = bersih,
            nik = nik,
            nama = ambilNilai(bersih, listOf("nama")),
            tglLahir = dariKolom ?: dariNik,
            kelDesa = ambilNilai(bersih, listOf("kel/desa", "keldesa", "kelurahan"))?.uppercase(),
            nikSah = sah,
            silangCocok = sah && dariKolom != null && dariKolom == dariNik,
        )
    }

    // ── NIK ─────────────────────────────────────────────────────────────────
    private fun cariNik(baris: List<String>): String? {
        val pola = Regex("[0-9OoDQIl|!ZzASsGTBgq]{14,20}")
        return baris.asSequence()
            .flatMap { pola.findAll(it.replace(" ", "")).map { m -> m.value } }
            .map { tok -> tok.map { perbaikanDigit[it] ?: it }.joinToString("").filter(Char::isDigit) }
            .filter { it.length == 16 }
            .sortedByDescending { nikSah(it) }
            .firstOrNull()
    }

    private fun nikSah(n: String): Boolean {
        if (n.length != 16 || !n.all(Char::isDigit)) return false
        val provinsi = n.substring(0, 2).toInt()
        if (provinsi !in 11..96) return false
        val hh = n.substring(6, 8).toInt()
        val bb = n.substring(8, 10).toInt()
        val hari = if (hh > 40) hh - 40 else hh
        if (bb !in 1..12 || hari !in 1..31) return false
        if (n.substring(12) == "0000") return false
        return tanggalDariNik(n) != null
    }

    private fun tanggalDariNik(n: String): LocalDate? {
        if (n.length != 16) return null
        return runCatching {
            val hh = n.substring(6, 8).toInt()
            val bb = n.substring(8, 10).toInt()
            val tt = n.substring(10, 12).toInt()
            val hari = if (hh > 40) hh - 40 else hh
            val kini = LocalDate.now().year
            val abad = if (tt <= kini - 2000) 2000 else 1900
            LocalDate.of(abad + tt, bb, hari).takeIf {
                val umur = kini - it.year
                umur in 17..110
            }
        }.getOrNull()
    }

    // ── kolom berlabel ──────────────────────────────────────────────────────
    private fun kunci(s: String) = s.lowercase().replace(Regex("[^a-z/ ]"), "").trim()

    private fun ambilNilai(baris: List<String>, label: List<String>): String? {
        for ((i, b) in baris.withIndex()) {
            val k = kunci(b)
            val cocok = label.firstOrNull { l ->
                k.take(maxOf(l.length + 3, 6)).let { mirip(it, l) >= 0.72 }
            } ?: continue

            val nilai = if (b.contains(":")) b.substringAfter(":").trim()
            else b.trim().split(" ").let { kata ->
                // Prefiks TERPENDEK dulu. Kalau dibalik, "Nama DOMINIKUS AMBUS"
                // akan cocok sebagai label tiga kata dan menyisakan nilai kosong.
                (1..minOf(4, kata.size)).firstNotNullOfOrNull { n ->
                    val sisa = kata.drop(n).joinToString(" ").trim()
                    if (mirip(kunci(kata.take(n).joinToString(" ")), cocok) >= 0.85 && sisa.isNotEmpty())
                        sisa else null
                } ?: ""
            }
            // ML Kit kadang memisahkan label dan isinya ke dua baris terpisah.
            if (nilai.isBlank() && i + 1 < baris.size) return baris[i + 1].trim().ifBlank { null }
            return nilai.ifBlank { null }
        }
        return null
    }

    private fun cariTanggal(baris: List<String>): LocalDate? {
        val pola = Regex("""(\d{1,2})\s*[-/. ]\s*(\d{1,2})\s*[-/. ]\s*(\d{2,4})""")
        for (b in baris) {
            val teks = b.map { perbaikanDigit[it] ?: it }.joinToString("")
            val m = pola.find(teks) ?: continue
            val (d, mo, y0) = m.destructured
            val y = y0.toInt().let {
                if (it > 100) it else if (it <= LocalDate.now().year % 100) 2000 + it else 1900 + it
            }
            runCatching { return LocalDate.of(y, mo.toInt(), d.toInt()) }
        }
        return null
    }

    /** Kemiripan sederhana ala Sørensen–Dice pada bigram — cukup untuk
     *  mencocokkan label yang sebagian hurufnya salah dibaca. */
    private fun mirip(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.length < 2 || b.length < 2) return 0.0
        fun bigram(s: String) = (0 until s.length - 1).map { s.substring(it, it + 2) }
        val x = bigram(a); val y = bigram(b).toMutableList()
        var sama = 0
        for (g in x) if (y.remove(g)) sama++
        return 2.0 * sama / (x.size + bigram(b).size)
    }
}

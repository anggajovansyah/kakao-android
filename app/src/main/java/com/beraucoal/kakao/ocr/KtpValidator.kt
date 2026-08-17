package com.beraucoal.kakao.ocr

import java.time.LocalDate

/**
 * Validasi dan silang-periksa NIK KTP — ported dari reference 2.0 KtpScanner.kt.
 *
 * Object ini melengkapi KtpOcrAnalyzer yang sudah ada dengan:
 * 1. Validasi struktur NIK 16 digit (provinsi, tanggal lahir, urutan)
 * 2. Ekstraksi tanggal lahir dari NIK
 * 3. Silang-periksa tanggal lahir NIK ↔ kolom "Tempat/Tgl Lahir"
 * 4. Perbaikan digit salah baca yang umum (O→0, I→1, dll)
 *
 * KENAPA INI PENTING:
 * NIK memuat tanggal lahir dan jenis kelamin (perempuan DD+40).
 * Kalau keduanya cocok dengan kolom "Tempat/Tgl Lahir" yang dibaca
 * terpisah, dua pembacaan independen saling menguatkan — jauh lebih
 * kuat daripada skor kepercayaan OCR mana pun.
 *
 * Reference: kakao-reference-2.0/KtpScanner.kt
 */
object KtpValidator {

    /** Salah baca yang paling sering terjadi pada blok angka NIK. */
    private val perbaikanDigit = mapOf(
        'O' to '0', 'o' to '0', 'D' to '0', 'Q' to '0',
        'I' to '1', 'l' to '1', '|' to '1', '!' to '1',
        'Z' to '2', 'z' to '2', 'A' to '4', 'S' to '5', 's' to '5',
        'G' to '6', 'T' to '7', 'B' to '8', 'g' to '9', 'q' to '9',
    )

    /**
     * Hasil validasi NIK.
     */
    data class NikValidation(
        /** NIK yang sudah dibersihkan (16 digit) */
        val nikBersih: String?,
        /** Apakah NIK valid secara struktural */
        val valid: Boolean,
        /** Tanggal lahir yang diekstrak dari NIK */
        val tanggalLahirDariNik: LocalDate?,
        /** Apakah tanggal lahir dari NIK cocok dengan kolom terpisah */
        val silangCocok: Boolean,
        /** Pesan error jika tidak valid */
        val pesanError: String? = null
    )

    /**
     * Perbaiki karakter salah baca pada string angka.
     * Misal: "640305I50678OO02" → "6403051506780002"
     */
    fun perbaikiDigit(input: String): String {
        return input.map { perbaikanDigit[it] ?: it }
            .joinToString("")
            .filter(Char::isDigit)
    }

    /**
     * Cari kandidat NIK terbaik dari daftar baris OCR.
     * Mencari string 14-20 karakter yang bisa jadi NIK setelah perbaikan digit.
     */
    fun cariNik(barisList: List<String>): String? {
        val pola = Regex("[0-9OoDQIl|!ZzASsGTBgq]{14,20}")
        return barisList.asSequence()
            .flatMap { pola.findAll(it.replace(" ", "")).map { m -> m.value } }
            .map { tok -> perbaikiDigit(tok) }
            .filter { it.length == 16 }
            .sortedByDescending { nikSah(it) }
            .firstOrNull()
    }

    /**
     * Validasi struktur NIK 16 digit.
     *
     * Format NIK:
     *   6403 05 150678 0002
     *   ││││ ││ ││││││ └─── nomor urut (0001-9999)
     *   ││││ ││ └────────── DDMMYY lahir; perempuan DD + 40
     *   ││││ └───────────── kode kecamatan
     *   ││└──────────────── kode kabupaten/kota (6403 = Berau)
     *   └────────────────── kode provinsi (64 = Kaltim)
     */
    fun nikSah(nik: String): Boolean {
        if (nik.length != 16 || !nik.all(Char::isDigit)) return false

        val provinsi = nik.substring(0, 2).toInt()
        if (provinsi !in 11..96) return false

        val hh = nik.substring(6, 8).toInt()
        val bb = nik.substring(8, 10).toInt()
        val hari = if (hh > 40) hh - 40 else hh

        if (bb !in 1..12 || hari !in 1..31) return false
        if (nik.substring(12) == "0000") return false

        return tanggalDariNik(nik) != null
    }

    /**
     * Ekstrak tanggal lahir dari NIK.
     * Perempuan: DD + 40 (misal 55 = hari ke-15).
     */
    fun tanggalDariNik(nik: String): LocalDate? {
        if (nik.length != 16) return null
        return runCatching {
            val hh = nik.substring(6, 8).toInt()
            val bb = nik.substring(8, 10).toInt()
            val tt = nik.substring(10, 12).toInt()
            val hari = if (hh > 40) hh - 40 else hh
            val kini = LocalDate.now().year
            val abad = if (tt <= kini - 2000) 2000 else 1900
            LocalDate.of(abad + tt, bb, hari).takeIf {
                val umur = kini - it.year
                umur in 17..110
            }
        }.getOrNull()
    }

    /**
     * Validasi NIK lengkap dengan silang-periksa tanggal lahir.
     *
     * @param nik String NIK (sudah atau belum dibersihkan)
     * @param tanggalLahirKolom Tanggal lahir dari kolom terpisah di KTP (opsional)
     */
    fun validasi(nik: String?, tanggalLahirKolom: LocalDate? = null): NikValidation {
        if (nik.isNullOrBlank()) {
            return NikValidation(
                nikBersih = null,
                valid = false,
                tanggalLahirDariNik = null,
                silangCocok = false,
                pesanError = "NIK belum terbaca"
            )
        }

        val nikBersih = perbaikiDigit(nik)
        if (nikBersih.length != 16) {
            return NikValidation(
                nikBersih = nikBersih,
                valid = false,
                tanggalLahirDariNik = null,
                silangCocok = false,
                pesanError = "NIK harus 16 digit (terbaca ${nikBersih.length})"
            )
        }

        val sah = nikSah(nikBersih)
        val tglDariNik = tanggalDariNik(nikBersih)
        val silangCocok = sah && tanggalLahirKolom != null && tanggalLahirKolom == tglDariNik

        return NikValidation(
            nikBersih = nikBersih,
            valid = sah,
            tanggalLahirDariNik = tglDariNik,
            silangCocok = silangCocok,
            pesanError = when {
                !sah -> "NIK tidak valid secara struktural"
                tanggalLahirKolom != null && !silangCocok ->
                    "Tanggal lahir di NIK tidak cocok dengan kolom KTP"
                else -> null
            }
        )
    }

    /**
     * Samarkan NIK untuk tampilan (privacy).
     * "6403051506780002" → "6403 •••• •••• 0002"
     */
    fun samarkan(nik: String): String {
        if (nik.length != 16) return nik
        return "${nik.take(4)} •••• •••• ${nik.takeLast(4)}"
    }

    /**
     * Ekstrak kode wilayah dari NIK.
     */
    fun kodeProvinsi(nik: String): String? =
        if (nik.length >= 2) nik.substring(0, 2) else null

    fun kodeKabupaten(nik: String): String? =
        if (nik.length >= 4) nik.substring(0, 4) else null

    fun kodeKecamatan(nik: String): String? =
        if (nik.length >= 6) nik.substring(0, 6) else null

    fun adalahBerau(nik: String): Boolean = kodeKabupaten(nik) == "6403"

    /**
     * Kemiripan sederhana ala Sørensen–Dice pada bigram — cukup untuk
     * mencocokkan label yang sebagian hurufnya salah dibaca.
     * Dipakai oleh KtpOcrAnalyzer untuk fuzzy label matching.
     */
    fun mirip(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.length < 2 || b.length < 2) return 0.0
        fun bigram(s: String) = (0 until s.length - 1).map { s.substring(it, it + 2) }
        val x = bigram(a); val y = bigram(b).toMutableList()
        var sama = 0
        for (g in x) if (y.remove(g)) sama++
        return 2.0 * sama / (x.size + bigram(b).size)
    }
}

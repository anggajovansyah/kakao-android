package com.beraucoal.kakao.data

/**
 * Data KTP hasil ekstraksi OCR. Semua field bisa diedit manual oleh petani/admin
 * kalau hasil OCR kurang akurat (foto blur, silau, dsb).
 */
data class KtpData(
    val nik: String = "",
    val nama: String = "",
    val tempatTanggalLahir: String = "",
    val alamat: String = "",
    val kelurahanDesa: String = "",
    val kecamatan: String = "",
    val rawOcrText: String = "",
    val ocrConfidence: OcrConfidence = OcrConfidence.LOW
)

enum class OcrConfidence { HIGH, MEDIUM, LOW }

enum class VerificationStatus { PENDING, DISETUJUI, DITOLAK }

data class KebunLocation(
    val latitude: Double,
    val longitude: Double,
    val luasHektar: Double? = null,
    val catatan: String = ""
)

/**
 * Menyatukan seluruh state pendaftaran satu petani, dari scan KTP sampai mapping kebun.
 */
data class PetaniRegistration(
    val ktpData: KtpData = KtpData(),
    val nomorWhatsapp: String = "",
    val verificationStatus: VerificationStatus = VerificationStatus.PENDING,
    val otpVerified: Boolean = false,
    val kebunLocation: KebunLocation? = null,
    val petaniId: String? = null // diisi setelah tersimpan di NocoBase
)

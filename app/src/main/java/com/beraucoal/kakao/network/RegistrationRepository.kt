package com.beraucoal.kakao.network

import com.beraucoal.kakao.data.KtpData
import com.beraucoal.kakao.data.VerificationStatus
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Dipisah jadi 2 flag supaya masing-masing bisa dinyalakan independen begitu
 * kredensialnya siap:
 * - MOCK_NOCOBASE: true selama URL & API key NocoBase belum di-set (lihat RetrofitClient.kt)
 * - MOCK_FONNTE: true selama device token Fonnte belum di-set
 */
private const val MOCK_NOCOBASE = false // URL tunnel & token sudah terpasang, coba koneksi asli
private const val MOCK_FONNTE = false // token Fonnte sudah diisi, kirim OTP asli

class RegistrationRepository(
    private val nocoBaseApi: NocoBaseApi = RetrofitClient.nocoBaseApi,
    private val fonnteApi: FonnteApi = RetrofitClient.fonnteApi
) {

    // Kode OTP yang sedang aktif untuk request tertentu, disimpan di memori saja
    // (tidak persisten) -- Fonnte tidak punya konsep "verify OTP" bawaan, jadi
    // generate & cocokkan kodenya dilakukan di app ini sendiri.
    //
    // CATATAN KEAMANAN: karena app Android bisa di-decompile, siapa pun yang
    // niat bisa lihat logika ini dan pola pembuatan kodenya. Untuk kebutuhan
    // registrasi petani skala ini risikonya wajar, tapi kalau nanti butuh
    // jaminan keamanan lebih tinggi, pindahkan generate+verifikasi OTP ini ke
    // backend (mis. custom action NocoBase/workflow) alih-alih di client.
    private val activeOtpCodes = mutableMapOf<String, String>()

    suspend fun submitPetani(ktp: KtpData, nomorWhatsapp: String): Result<String> {
        if (MOCK_NOCOBASE) {
            delay(800)
            return Result.success("MOCK-${ktp.nik.takeLast(6)}")
        }
        return try {
            val res = nocoBaseApi.submitPetani(
                PetaniFields(
                    nik = ktp.nik,
                    nama = ktp.nama,
                    alamat = ktp.alamat,
                    kelurahanDesa = ktp.kelurahanDesa,
                    kecamatan = ktp.kecamatan,
                    nomorWhatsapp = nomorWhatsapp
                )
            )
            val body = res.body()
            if (res.isSuccessful && body != null) {
                Result.success(body.data.id.toString())
            } else {
                Result.failure(Exception("Gagal submit data petani: ${res.code()} ${res.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pollVerificationStatus(petaniId: String): Result<VerificationStatus> {
        if (MOCK_NOCOBASE) {
            delay(1500)
            return Result.success(VerificationStatus.DISETUJUI)
        }
        return try {
            val id = petaniId.toLongOrNull()
                ?: return Result.failure(Exception("ID petani tidak valid: $petaniId"))
            val res = nocoBaseApi.getPetani(id)
            val body = res.body()
            if (res.isSuccessful && body != null) {
                // Sesuaikan nilai string ini ("pending"/"disetujui"/"ditolak") dengan
                // nilai kolom status yang sebenarnya di collection "petani" NocoBase.
                val status = when (body.data.status?.lowercase()) {
                    "disetujui", "approved" -> VerificationStatus.DISETUJUI
                    "ditolak", "rejected" -> VerificationStatus.DITOLAK
                    else -> VerificationStatus.PENDING
                }
                Result.success(status)
            } else {
                Result.failure(Exception("Gagal cek status: ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendOtp(nomorWhatsapp: String): Result<String> {
        val requestId = "otp-${System.currentTimeMillis()}"
        val code = Random.nextInt(100000, 999999).toString()

        if (MOCK_FONNTE) {
            delay(800)
            activeOtpCodes[requestId] = "123456" // kode tetap di mock mode biar gampang dites
            return Result.success(requestId)
        }
        return try {
            val message = "Kode verifikasi AI Kakao Anda: $code. Jangan bagikan kode ini ke siapa pun."
            val res = fonnteApi.sendMessage(
                deviceToken = RetrofitClient.fonnteDeviceToken(),
                target = normalizeWhatsappNumber(nomorWhatsapp),
                message = message
            )
            if (res.isSuccessful && res.body()?.status != false) {
                activeOtpCodes[requestId] = code
                Result.success(requestId)
            } else {
                Result.failure(Exception("Gagal kirim OTP: ${res.body()?.reason ?: res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyOtp(requestId: String, kode: String): Result<Boolean> {
        delay(300) // simulasikan sedikit delay supaya UX loading terasa konsisten
        val expected = activeOtpCodes[requestId]
        val valid = expected != null && expected == kode
        if (valid) activeOtpCodes.remove(requestId) // OTP sekali pakai
        return Result.success(valid)
    }

    suspend fun submitKebun(petaniId: String, latitude: Double, longitude: Double, luasHektar: Double?): Result<Unit> {
        if (MOCK_NOCOBASE) {
            delay(800)
            return Result.success(Unit)
        }
        return try {
            val id = petaniId.toLongOrNull()
                ?: return Result.failure(Exception("ID petani tidak valid: $petaniId"))
            val res = nocoBaseApi.submitKebun(KebunFields(id, latitude, longitude, luasHektar))
            if (res.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Gagal simpan kebun: ${res.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fonnte butuh nomor dalam format internasional tanpa "+" (mis. 62812xxxxxxx).
     * Ubah awalan lokal "0" jadi "62" -- sesuaikan lagi kalau format nomor dari
     * form registrasi ternyata sudah dalam bentuk lain.
     */
    private fun normalizeWhatsappNumber(nomor: String): String {
        val digitsOnly = nomor.filter { it.isDigit() }
        return when {
            digitsOnly.startsWith("62") -> digitsOnly
            digitsOnly.startsWith("0") -> "62" + digitsOnly.drop(1)
            else -> digitsOnly
        }
    }
}

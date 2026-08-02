package com.beraucoal.kakao.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

// ---- NocoBase request/response bodies ----
// Konvensi resmi NocoBase (https://docs.nocobase.com/api/actions/):
//   POST /api/<collection>:create          body: JSON field-field kolomnya langsung (flat)
//   GET  /api/<collection>:get?filterByTk=<id>
//   POST /api/<collection>:update?filterByTk=<id>
// Auth: header "Authorization: Bearer <API key>"
// Response record tunggal biasanya dibungkus {"data": {...}} -- SESUAIKAN NocoBaseSingleResponse
// kalau instance NocoBase tim kamu ternyata beda format (versi/plugin custom bisa sedikit beda).

data class NocoBaseSingleResponse<T>(val data: T)
data class NocoBaseListResponse<T>(val data: List<T>)

data class PetaniFields(
    val nik: String,
    val nama: String,
    val alamat: String,
    val kelurahanDesa: String,
    val kecamatan: String,
    val nomorWhatsapp: String,
    // NocoBase otomatis isi "id" dan field status verifikasi kalau sudah ada di collection-nya --
    // sesuaikan nama kolom "status" ini dengan nama kolom asli di collection "petani" tim Backend.
    val status: String? = null
)

data class PetaniRecord(
    val id: Long,
    val nik: String? = null,
    val nama: String? = null,
    val alamat: String? = null,
    val kelurahanDesa: String? = null,
    val kecamatan: String? = null,
    val nomorWhatsapp: String? = null,
    val status: String? // "pending" | "disetujui" | "ditolak" -- sesuaikan dengan nilai asli di NocoBase
)

data class KebunFields(
    val petaniId: Long,
    val latitude: Double,
    val longitude: Double,
    val luasHektar: Double?
)

/**
 * Endpoint ke instance NocoBase yang jadi dashboard admin.
 * GANTI NOCOBASE_BASE_URL di RetrofitClient.kt sesuai URL dari tim Backend.
 * Nama collection "petani" dan "kebun" di sini HARUS SAMA PERSIS dengan nama
 * collection yang dibuat tim Backend di NocoBase -- cek dan sesuaikan sebelum
 * matikan MOCK_MODE.
 */
interface NocoBaseApi {
    @POST("api/petani:create")
    suspend fun submitPetani(@Body body: PetaniFields): Response<NocoBaseSingleResponse<PetaniRecord>>

    @GET("api/petani:get")
    suspend fun getPetani(@Query("filterByTk") petaniId: Long): Response<NocoBaseSingleResponse<PetaniRecord>>

    @GET("api/petani:list")
    suspend fun findPetaniByWa(@Query("filter[nomorWhatsapp]") nomorWhatsapp: String): Response<NocoBaseListResponse<PetaniRecord>>

    @GET("api/petani:list")
    suspend fun findPetaniByNik(@Query("filter[nik]") nik: String): Response<NocoBaseListResponse<PetaniRecord>>

    @POST("api/kebun:create")
    suspend fun submitKebun(@Body body: KebunFields): Response<NocoBaseSingleResponse<Map<String, Any?>>>
}

// ---- Fonnte (WhatsApp) ----
// Dokumentasi: https://fonnte.com/ -- endpoint tunggal untuk kirim pesan.
// Fonnte HANYA mengirim pesan, TIDAK punya konsep "OTP" bawaan -- kode OTP
// di-generate & diverifikasi di app ini sendiri (lihat RegistrationRepository),
// lalu dikirim sebagai teks biasa lewat endpoint ini.
// Auth: header "Authorization: <device token>" (BUKAN "Bearer <token>")

data class FonnteSendResponse(
    val status: Boolean? = null,
    val reason: String? = null,
    val id: List<String>? = null
)

interface FonnteApi {
    @FormUrlEncoded
    @POST("send")
    suspend fun sendMessage(
        @Header("Authorization") deviceToken: String,
        @Field("target") target: String,
        @Field("message") message: String
    ): Response<FonnteSendResponse>
}

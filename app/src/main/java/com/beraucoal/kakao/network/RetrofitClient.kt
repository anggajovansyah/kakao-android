package com.beraucoal.kakao.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // URL tunnel (localtunnel) ke instance NocoBase yang jalan lokal di PC tim
    // Backend. CATATAN: URL localtunnel bisa berubah tiap kali tunnel di-restart
    // -- kalau nanti koneksi tiba-tiba gagal, kemungkinan besar URL ini sudah usang.
    private const val NOCOBASE_BASE_URL = "https://great-roses-hear.loca.lt/"

    // Token dari tim Backend (2026-07-30). CATATAN: ini token LOGIN (bukan API
    // Key permanen dari menu "API Keys" NocoBase) -- expired otomatis
    // 2026-10-28. Minta API Key permanen ke tim Backend untuk pemakaian
    // jangka panjang, lalu ganti nilai ini.
    private const val NOCOBASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEsInJvbGVOYW1lIjoicm9vdCIsImlhdCI6MTc4NTM4MzI3OSwiZXhwIjoxNzkzMTU5Mjc5fQ.L2MDtMPTRNzSHPw2RKi5r6mTxmhPeNTO4Cu9GL9Qr7A"

    private const val FONNTE_BASE_URL = "https://api.fonnte.com/"

    // TODO: isi dengan device token dari dashboard Fonnte (Device -> pilih device
    // yang sudah di-scan QR -> copy token). Fonnte pakai token ini langsung di
    // header Authorization, TANPA prefix "Bearer".
    private const val FONNTE_DEVICE_TOKEN = "bzGH48RPvCAi9XWT43hv"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val nocoBaseAuthInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $NOCOBASE_API_KEY")
            // localtunnel (loca.lt) menampilkan halaman peringatan HTML ke
            // request pertama dari client baru untuk cegah abuse -- header ini
            // melewatinya supaya kita tetap dapat response JSON asli dari NocoBase.
            .addHeader("Bypass-Tunnel-Reminder", "true")
            .build()
        chain.proceed(request)
    }

    private val nocoBaseOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(nocoBaseAuthInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    private val fonnteOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val nocoBaseApi: NocoBaseApi by lazy {
        Retrofit.Builder()
            .baseUrl(NOCOBASE_BASE_URL)
            .client(nocoBaseOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NocoBaseApi::class.java)
    }

    val fonnteApi: FonnteApi by lazy {
        Retrofit.Builder()
            .baseUrl(FONNTE_BASE_URL)
            .client(fonnteOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FonnteApi::class.java)
    }

    // Dipakai RegistrationRepository untuk mengisi header Authorization Fonnte
    // (bukan lewat interceptor karena Fonnte butuh token per-request, bukan format Bearer).
    fun fonnteDeviceToken(): String = FONNTE_DEVICE_TOKEN
}

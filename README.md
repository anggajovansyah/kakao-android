# Kakao Digital - Registrasi Petani (Android/Kotlin)

App native Android (Kotlin + Jetpack Compose) untuk alur registrasi petani kakao,
sesuai diagram: Scan KTP → OCR → Verifikasi Admin (NocoBase) → OTP WhatsApp → Mapping Kebun → Selesai.

## Cara jalankan

Project ini sudah lengkap dengan Gradle wrapper (`gradlew`, `gradle-wrapper.jar`) dan launcher icon,
jadi tinggal buka dan langsung sync -- tidak ada file yang hilang.

1. Ekstrak zip, buka foldernya di Android Studio (Koala atau lebih baru) lewat "Open".
2. Tunggu Gradle sync selesai (dependency ML Kit, CameraX, Retrofit, Maps Compose sudah terdaftar
   di `app/build.gradle.kts`, akan otomatis terunduh).
3. **Satu-satunya langkah manual yang wajib** sebelum layar mapping kebun bisa jalan: ganti
   `YOUR_MAPS_API_KEY_HERE` di `AndroidManifest.xml` dengan API key Google Maps kamu sendiri
   (buat gratis di Google Cloud Console -> aktifkan "Maps SDK for Android"). Tanpa ini, 4 layar
   pertama (scan KTP, verifikasi, OTP) tetap jalan normal, hanya peta di layar terakhir yang blank.
4. Klik Run ke emulator/device Android 8.0 (API 26) ke atas yang punya kamera.

Kalau Android Studio minta "Gradle JDK", pakai JDK 17 (biasanya sudah termasuk di Android Studio terbaru).

## Status saat ini: MOCK_MODE aktif

`RegistrationRepository.kt` punya flag `MOCK_MODE = true` di bagian atas file.
Selama flag ini true, app bisa dicoba end-to-end (scan KTP asli via ML Kit, tapi
submit ke NocoBase & kirim OTP WhatsApp memakai data palsu/delay simulasi).

Kode OTP mock yang selalu valid: **123456**

## Yang perlu diisi sebelum production

- [ ] `RetrofitClient.kt`: ganti `NOCOBASE_BASE_URL` dengan URL instance NocoBase dari tim Backend
      (harus diakhiri `/`), dan `NOCOBASE_API_KEY` dengan API key-nya (NocoBase -> Settings -> API Keys).
- [ ] `RetrofitClient.kt`: ganti `FONNTE_DEVICE_TOKEN` dengan device token dari dashboard Fonnte
      (fonnte.com -> Device -> scan QR WhatsApp -> copy token perangkat).
- [ ] `ApiService.kt` (`PetaniFields`, `PetaniRecord`): field di sini (`nik`, `nama`, `alamat`,
      `kelurahanDesa`, `kecamatan`, `nomorWhatsapp`, `status`) HARUS SAMA PERSIS dengan nama kolom
      di collection "petani" NocoBase tim Backend -- cek dan sesuaikan nama field & nilai status
      ("pending"/"disetujui"/"ditolak") di `RegistrationRepository.pollVerificationStatus()`.
- [ ] `ApiService.kt` (`KebunFields`): sama, sesuaikan dengan collection "kebun".
- [ ] `RegistrationRepository.kt`: set `MOCK_MODE = false` setelah semua di atas selesai & sudah
      dicoba manual (mis. lewat Postman) ke instance NocoBase & Fonnte yang sebenarnya.
- [ ] **Keamanan OTP**: kode OTP saat ini di-generate dan diverifikasi LANGSUNG DI APP (lihat
      `RegistrationRepository.activeOtpCodes`) karena Fonnte cuma provider kirim pesan, bukan
      provider OTP. Ini cukup untuk skala awal, tapi karena app Android bisa di-decompile, kalau
      butuh jaminan keamanan lebih tinggi pindahkan logika generate+verifikasi OTP ini ke backend
      (mis. custom action/workflow di NocoBase) alih-alih di client.
- [ ] Fonnte pakai WhatsApp Web yang diotomatisasi (bukan WhatsApp Business API resmi Meta) --
      ada risiko nomor pengirim ke-banned kalau volume kirim tinggi ke banyak nomor baru sekaligus.
      Wajar untuk skala kecil-menengah, tapi pertimbangkan pindah ke jalur resmi kalau sudah produksi besar.
- [ ] `KtpOcrAnalyzer.kt` & `WilayahCorrector.kt`: sudah diuji di beberapa KTP asli dan hasilnya baik,
      tapi tetap uji lagi dengan KTP dari daerah/kondisi pencahayaan yang berbeda-beda.
- [ ] `KebunMappingScreen.kt`: default kamera peta di-set ke area Kalimantan Timur (perkiraan lokasi
      operasi PT. Berau Coal) -- sesuaikan koordinat kalau area kebun berbeda.
- [ ] Ikon app (`ic_launcher`) masih placeholder sederhana dari `splash_icon.png` -- ganti kalau
      sudah ada logo final resmi.

## Struktur

```
app/src/main/java/com/beraucoal/kakao/
├── MainActivity.kt              # entry point, minta izin kamera & lokasi
├── RegistrationNavGraph.kt      # navigasi antar 5 tahap
├── RegistrationViewModel.kt     # state alur registrasi end-to-end
├── data/RegistrationModels.kt   # KtpData, KebunLocation, PetaniRegistration
├── ocr/KtpOcrAnalyzer.kt        # ML Kit Text Recognition + parser field KTP
├── network/
│   ├── ApiService.kt            # interface Retrofit: NocoBase (petani/kebun) + Fonnte (WhatsApp)
│   ├── RetrofitClient.kt        # base URL, API key NocoBase, device token Fonnte (TODO: isi asli)
│   └── RegistrationRepository.kt# repository dengan MOCK_MODE + generate/verifikasi OTP lokal
└── ui/screens/
    ├── ScanKtpScreen.kt         # tahap 1: foto + OCR
    ├── VerifyDataScreen.kt      # tahap 2: form edit + kirim ke admin
    ├── OtpScreen.kt             # tahap 3: kirim & verifikasi OTP WhatsApp
    ├── KebunMappingScreen.kt    # tahap 4: pin lokasi kebun di peta
    └── DoneScreen.kt            # tahap 5: registrasi selesai
```

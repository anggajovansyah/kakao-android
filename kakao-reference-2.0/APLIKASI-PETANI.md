# Aplikasi petani (Android)

Prototipe yang bisa diklik: `prototipe-aplikasi/index.html` — buka di browser.
Kode Kotlin: `android/app/src/main/java/id/itsb/kakao/`.

---

## 1. Alur layar

```
Selamat datang
   ├─ Daftar Baru ──► Pindai KTP ──► Periksa data ──► Menunggu verifikasi
   │                   (ML Kit, luring)                      │
   │                                                admin menyetujui
   │                                                          ▼
   └─ Sudah Punya Kode ──────────────► Masuk (nama + kode) ──► Buat PIN ──┐
                                                                          ▼
                                                                      BERANDA
                          ┌──────────────┬──────────────┬──────────────┬─────────┐
                       Beranda        Foto          Kirim         Riwayat    Akun
                      cuaca+kebun   kamera+GPS     antrean       hasil AI
```

Buka aplikasi berikutnya di HP yang sama: **cukup PIN**, tanpa mengetik kode lagi.

## 2. Pendaftaran lewat KTP

### Kenapa ML Kit Text Recognition v2

| Pilihan | Kelebihan | Kenapa tidak dipilih |
|---|---|---|
| **ML Kit v2** *(dipakai)* | Luring penuh, gratis, tanpa kunci API, min API 21 | — |
| Google Cloud Vision / Textract | Akurasi mentah lebih tinggi | Butuh internet **saat memindai**, berbiaya per panggilan, gambar KTP keluar sebelum petani menyetujuinya |
| Tesseract | Gratis, luring | Dirancang untuk pindaian datar; pada foto kartu berpantulan hasilnya jauh di bawah ML Kit |
| SDK e-KTP komersial (Verihubs, Privy, Asli RI) | Paling akurat untuk KTP, sudah ada liveness | Berlangganan, dan menambah pihak ketiga yang memegang data KTP petani |

Yang menentukan: petani memindai KTP di kantor desa atau di rumah, **sering
tanpa sinyal**. Pustaka yang butuh jaringan akan gagal tepat pada langkah yang
paling menentukan.

Model dibundel dalam APK (+4 MB, `meta-data com.google.mlkit.vision.DEPENDENCIES`)
supaya OCR bekerja sejak pemasangan pertama — bukan diunduh belakangan saat
petani sudah di kebun.

### Yang sebenarnya membuat pembacaan andal

Bukan pustaka OCR-nya. **Struktur NIK-lah** yang menjadi pemeriksa silang:

```
6403 05 150678 0002
││││ ││ ││││││ └─── nomor urut
││││ ││ └────────── DDMMYY lahir; perempuan DD + 40
││││ └───────────── kecamatan
││└──────────────── kabupaten (6403 = Berau)
└────────────────── provinsi (64 = Kalimantan Timur)
```

NIK memuat tanggal lahir dan jenis kelamin. Kolom "Tempat/Tgl Lahir" dan
"Jenis Kelamin" dibaca terpisah. Kalau keduanya sepakat, dua pembacaan
independen saling menguatkan — jauh lebih kuat daripada skor kepercayaan OCR
mana pun. Kalau tidak sepakat, hampir pasti ada digit yang salah baca, dan
pendaftaran otomatis dilempar ke verifikasi manual.

Hasil uji (`server/tests/uji_ktp.py`, semua lulus):

```
NIK dengan salah baca O→0, I→1, S→5, B→8   dipulihkan utuh
titik dua hilang ("Nama  DOMINIKUS AMBUS")  nama tetap terbaca
satu digit NIK salah                        ketahuan lewat silang, tidak lolos
tanggal 32 / bulan 16 / urut 0000           ditolak
```

### Pencocokan ke master petani

Nama KTP dicocokkan ke 147 nama di `AI-KAKAO_master_petani`. Tiga hal yang
membuat pencocokan ini tidak naif:

1. **Angka hasil salah baca dikembalikan jadi huruf** sebelum dibandingkan
   (`D0MINIKUS AMBU5` → `dominikus ambus`). Nama orang tidak memuat angka.
2. **Urutan nama terbalik** ikut dibandingkan — `AMBUS DOMINIKUS` cocok dengan
   `Dominikus Ambus`. Urutan nama depan/belakang di KTP kerap berbeda dari
   catatan survei.
3. **Kolom Kel/Desa dipakai sebagai pemutus.** Ini yang membedakan `Jupri` di
   Birang dari `Pak Jupri` di Pegat Bukur — dua orang berbeda berjarak 16,8 km
   yang namanya menormalkan menjadi sama persis. Nama saja tidak akan pernah
   bisa memisahkan keduanya.

| | cocok otomatis |
|---|---|
| Tanpa kampung | 129 / 147 (88%) |
| **Dengan Kel/Desa dari KTP** | **143 / 147 (97%)** |
| Salah otomatis ke orang lain | **0** |

Empat sisanya memang harus dilihat manusia: nama KTP yang merupakan bagian dari
nama petani lain (`Wihelmus` terhadap `Wihelmus Soet`) selalu diserahkan ke
verifikator, berapa pun angka kemiripannya.

Uji ketahanan: 147 nama diberi 2 huruf salah baca acak → 85 tetap cocok
otomatis dengan benar, 62 ke manual, **0 salah orang**.

### Privasi

- NIK **tidak** disimpan dalam bentuk polos. Hanya hash berlada (`sha256` +
  rahasia server) dan bentuk tersamar `6403••••••••0002` untuk dilihat
  verifikator. Ruang 16 digit terlalu kecil untuk hash tanpa lada.
- Foto KTP **dihapus otomatis** setelah verifikator memutuskan. Sistem ini
  tidak butuh arsip 147 foto KTP; menyimpannya hanya menambah nilai bagi orang
  yang membobol server.
- Satu NIK hanya bisa dipakai untuk satu pendaftaran yang disetujui
  (indeks unik parsial di `pendaftaran`).

## 3. Masuk dengan nama + kode petani

Ini dijalankan sesuai permintaan, tetapi perlu dikatakan terus terang:

> **`farmer_code` bukan rahasia.** Bentuknya `PTN-00001-x` sampai `PTN-00147-x`,
> digit ceknya bisa dihitung siapa saja dalam sedetik, dan nama petani ada di
> dalam shapefile yang beredar di banyak tangan. Kalau hanya kedua hal itu yang
> menjaga akun, seluruh ruang kode habis dicoba sebelum kopi dingin.

Kemudahannya dipertahankan — petani tetap hanya mengetik nama dan kode —
ditambah tiga lapis yang tidak menambah gesekan bagi petani yang jujur:

| Lapis | Cara kerja | Beban bagi petani |
|---|---|---|
| **Perangkat pertama mengikat** | Kode yang sama di HP lain → `409 perangkat_baru`, perlu izin admin | Nol, kecuali benar-benar ganti HP |
| **PIN 6 angka** | Dipilih saat pertama masuk; hanya diminta saat **membuka** aplikasi | Enam ketukan, bukan saat mendaftar |
| **Pembatasan percobaan** | 5 gagal per kode per jam, 20 per perangkat | Nol |

Jawaban untuk "kode tidak ada" dan "nama tidak cocok" sengaja dibuat identik,
supaya tidak bisa dipakai memeriksa kode mana yang hidup.

Kalau tim Berau Coal menilai lapis 1 dan 2 terlalu berat untuk lapangan, **lapis
3 tetap harus ada.** Tanpa itu sistem ini terbuka lebar.

## 4. Beranda: kondisi kebun + cuaca

### Sumber cuaca

**BMKG** — `api.bmkg.go.id/publik/prakiraan-cuaca?adm4=<kode desa>`. Prakiraan
3 hari per kelurahan/desa dari lembaga resmi Indonesia, gratis, tanpa kunci API.
Resolusinya per desa, bukan per titik grid global. **Atribusi BMKG wajib
ditampilkan di layar** — itu syarat pemakaiannya, dan sudah ada di bawah kartu
cuaca.

Kode wilayah tingkat IV (Kemendagri) tidak ada di shapefile, jadi diisi admin
sekali lewat `POST /v1/admin/wilayah/{kampung}`. Sampai terisi, cuaca diambil
dari titik tengah kebun lewat **Open-Meteo**.

> **Peringatan lisensi.** Open-Meteo gratis hanya untuk pemakaian
> **non-komersial**. Proyek ini berjalan untuk PT Berau Coal, jadi kemungkinan
> besar tidak memenuhi syarat itu. Perlakukan Open-Meteo sebagai jembatan
> sementara sampai kode adm4 terisi. Kalau tetap dipakai jangka panjang: ambil
> langganan komersial mereka, atau pasang sendiri (kodenya AGPLv3).

Singgahan 1 jam per kampung. BMKG membatasi 60 permintaan/menit per IP dan
memperbarui datanya dua kali sehari — 147 petani yang membuka aplikasi
pagi-pagi tidak boleh menjadi 147 permintaan.

### Cuaca yang berguna

Angka "82% peluang hujan" tidak memberi tahu apa pun. Server menerjemahkannya
menjadi tindakan kebun:

- hujan ≥ 60% hari ini → *tunda penyemprotan fungisida, airnya akan tercuci*
- besok hujan, hari ini kering → *kalau ada buah masak, panen hari ini*
- dua hari basah berturut-turut → *risiko busuk buah naik, periksa dan pangkas*
- suhu ≥ 34 °C → *ambil foto sebelum pukul 10 agar bayangan tidak keras*

## 5. Kamera dan GPS

- **Tombol rana terkunci sampai GPS terkunci.** Foto tanpa koordinat tidak bisa
  dihubungkan ke poligon kebun mana pun — dan petani baru tahu setelah pulang.
- Ambang akurasi 50 m. Di bawah tajuk kakao yang rapat, 30 m adalah hal biasa;
  ambang ini menyaring perbaikan berbasis menara seluler saja.
- **Titik di luar poligon adalah peringatan, bukan penolakan.** Uji
  titik-dalam-poligon dijalankan **di HP** memakai poligon yang sudah diunduh,
  jadi peringatan muncul sebelum foto diambil — bukan setelah dikirim dan
  sinyal sudah hilang. Server tetap menandainya di kolom `dalam_poligon`.
- Pemilih kebun hanya muncul bila petani punya lebih dari satu. Dari 147
  petani, hanya 3 yang punya dua kebun; menanyakan "kebun mana?" kepada 144
  orang lain setiap kali memotret adalah gesekan tanpa manfaat.

## 6. Menyajikan hasil AI dengan jujur

Model buah (D-FINE v12) mencapai mAP50 **0,869 pada foto dekat** tetapi hanya
**0,357 pada foto lapangan apa adanya**. Karena itu:

- deteksi dengan kepercayaan < 50% ditandai **"perlu diperiksa"**, bukan
  disajikan sebagai diagnosis
- versi model ditampilkan di layar detail, agar hasil v12 dan v13 nanti tidak
  tercampur dalam ingatan pengguna
- organ yang modelnya belum ada menampilkan apa adanya: *"Foto tersimpan.
  Analisis batang belum tersedia."*
- kotak deteksi digambar di atas foto asli, sehingga petani melihat **buah mana**
  yang dimaksud — bukan sekadar membaca nama penyakit

Menampilkan tebakan lemah sebagai kepastian akan merusak kepercayaan petani
lebih cepat daripada tidak menampilkan apa pun.

## 7. Fitur yang belum Anda sebutkan tetapi sebaiknya ada

Sudah masuk prototipe:

| Fitur | Alasan |
|---|---|
| **PIN + pengikatan perangkat** | Menutup lubang login nama+kode (bagian 3) |
| **Antrean unggah luring** | Foto selalu masuk antrean lokal dulu; aplikasi yang menunggu jaringan terasa rusak di kebun tanpa sinyal |
| **"Kirim hanya lewat Wi-Fi"** | Kuota di lokasi mahal, dan hanya petani yang tahu kapan dia dekat Wi-Fi |
| **Riwayat luring** | Bisa dibuka di tengah kebun untuk membandingkan dengan kondisi hari ini |
| **Tombol "hasil ini keliru"** | Umpan balik petani masuk antrean tinjau peneliti — ini sumber data latih siklus berikutnya |
| **Peringatan cuaca** | Notifikasi sebelum hujan lebat; petani menyesuaikan jadwal panen |
| **Pengingat foto mingguan** | Yang menggerakkan penyuluh ke lapangan bukan skor, tapi umur data |
| **Nomor pendaftaran** | Disebutkan saat menghubungi petugas — tanpa ini, percakapan telepon buntu |

Perlu dipertimbangkan, belum dibuat:

- **Mode suara / ikon besar.** Sebagian petani tidak nyaman membaca teks
  panjang. Ringkasan hasil bisa dibacakan dalam bahasa Indonesia.
- **Berbagi ke WhatsApp.** Cara paling nyata petani menunjukkan hasil ke
  penyuluh atau kelompok tani.
- **Catatan tindakan.** "Sudah dipangkas 12 Agustus" — menutup lingkaran antara
  deteksi dan perlakuan, dan membuat dashboard bisa menilai apakah intervensi
  berhasil.
- **Mode kelompok tani.** Satu HP dipakai bergantian beberapa petani; sekarang
  satu HP = satu petani.
- **Harga kakao mingguan.** Bukan bagian dari deteksi penyakit, tetapi ini yang
  akan membuat petani membuka aplikasi setiap hari.

## 8. Berkas Kotlin

| Berkas | Peran |
|---|---|
| `ktp/KtpScanner.kt` | ML Kit + parser ringan untuk umpan balik seketika |
| `ui/layar/DaftarKtpLayar.kt` | CameraX + analisis bingkai, tombol rana terkunci |
| `data/Lokasi.kt` | GPS, uji titik-dalam-poligon, jarak ke batas |
| `ui/theme/Warna.kt` | Tangga status identik dengan dashboard |
| `net/KakaoApi.kt` | Kontrak Retrofit — satu-satunya tempat bentuk permintaan didefinisikan |
| `net/ApiClient.kt` | OkHttp, timeout disesuaikan sinyal 2G |
| `data/SesiStore.kt` | Token di EncryptedSharedPreferences |
| `data/TokenRefresher.kt` | Penyegaran token, sengaja tanpa Retrofit agar tidak memanggil diri sendiri |
| `data/Antrean.kt` | Antrean unggah Room — sumber kebenaran di sisi HP |
| `work/UploadWorker.kt` | WorkManager, backoff eksponensial, batasan jaringan |

Ini bagian-bagian yang menentukan benar-tidaknya perilaku di lapangan, siap
disalin ke modul aplikasi Anda. Layar selebihnya mengikuti prototipe.

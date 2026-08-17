"""Pembacaan KTP: parsing hasil OCR, validasi NIK, dan pencocokan ke master petani.

Modul ini adalah parser KANONIK. Aplikasi Android juga menjalankan ML Kit dan
mem-parsing hasilnya di HP, tetapi itu hanya untuk umpan balik seketika di layar
("NIK terbaca, nama terbaca"). Yang menentukan apa yang dilihat verifikator
adalah hasil parsing di sini — satu sumber kebenaran, bisa diuji, dan bisa
diperbaiki tanpa merilis ulang aplikasi.

Yang membuat pembacaan KTP jauh lebih andal daripada OCR biasa adalah struktur
NIK itu sendiri:

    6403 05 150678 0002
    ││││ ││ ││││││ └─── nomor urut (0001-9999)
    ││││ ││ └────────── DDMMYY lahir; perempuan DD + 40
    ││││ └───────────── kode kecamatan
    ││└──────────────── kode kabupaten/kota
    └────────────────── kode provinsi

Artinya NIK memuat tanggal lahir dan jenis kelamin. Kalau keduanya cocok dengan
kolom "Tempat/Tgl Lahir" dan "Jenis Kelamin" yang dibaca terpisah, dua
pembacaan independen saling menguatkan — dan itu jauh lebih kuat daripada skor
kepercayaan OCR mana pun. Kalau tidak cocok, hampir pasti ada digit yang salah
baca, dan pendaftaran dilempar ke verifikasi manual.
"""
from __future__ import annotations

import re
import unicodedata
from dataclasses import dataclass, field
from datetime import date
from difflib import SequenceMatcher

# ── kode provinsi (Permendagri) ─────────────────────────────────────────────
PROVINSI = {
    "11": "Aceh", "12": "Sumatera Utara", "13": "Sumatera Barat", "14": "Riau",
    "15": "Jambi", "16": "Sumatera Selatan", "17": "Bengkulu", "18": "Lampung",
    "19": "Kep. Bangka Belitung", "21": "Kep. Riau", "31": "DKI Jakarta",
    "32": "Jawa Barat", "33": "Jawa Tengah", "34": "DI Yogyakarta", "35": "Jawa Timur",
    "36": "Banten", "51": "Bali", "52": "Nusa Tenggara Barat", "53": "Nusa Tenggara Timur",
    "61": "Kalimantan Barat", "62": "Kalimantan Tengah", "63": "Kalimantan Selatan",
    "64": "Kalimantan Timur", "65": "Kalimantan Utara", "71": "Sulawesi Utara",
    "72": "Sulawesi Tengah", "73": "Sulawesi Selatan", "74": "Sulawesi Tenggara",
    "75": "Gorontalo", "76": "Sulawesi Barat", "81": "Maluku", "82": "Maluku Utara",
    "91": "Papua Barat", "92": "Papua Barat Daya", "93": "Papua Selatan",
    "94": "Papua", "95": "Papua Tengah", "96": "Papua Pegunungan",
}
KAB_BERAU = "6403"          # Kabupaten Berau, Kalimantan Timur

# Salah baca yang paling sering terjadi pada blok angka NIK.
DIGIT_FIX = str.maketrans({
    "O": "0", "o": "0", "D": "0", "Q": "0",
    "I": "1", "l": "1", "|": "1", "!": "1",
    "Z": "2", "z": "2",
    "A": "4",
    "S": "5", "s": "5",
    "G": "6",
    "T": "7",
    "B": "8",
    "g": "9", "q": "9",
})

LABEL = {
    "nik": ["nik"],
    "nama": ["nama"],
    "ttl": ["tempat/tgl lahir", "tempat tgl lahir", "tempattgl lahir", "tgl lahir"],
    "jenis_kelamin": ["jenis kelamin"],
    "gol_darah": ["gol darah", "gol. darah", "goldarah"],
    "alamat": ["alamat"],
    "rt_rw": ["rt/rw", "rtrw"],
    "kel_desa": ["kel/desa", "keldesa", "kelurahan/desa", "kel desa"],
    "kecamatan": ["kecamatan"],
    "agama": ["agama"],
    "status_perkawinan": ["status perkawinan"],
    "pekerjaan": ["pekerjaan"],
    "kewarganegaraan": ["kewarganegaraan"],
    "berlaku_hingga": ["berlaku hingga", "berlaku s/d"],
    "provinsi": ["provinsi"],
    "kabupaten": ["kabupaten", "kota"],
}
GELAR = {"pak", "bapak", "bpk", "bu", "ibu", "ibuk", "sdr", "sdri", "h", "hj", "tn", "ny"}


# ── util ────────────────────────────────────────────────────────────────────
def _bersih(s: str) -> str:
    s = unicodedata.normalize("NFKD", s)
    return re.sub(r"\s+", " ", s).strip()


def _kunci(s: str) -> str:
    return re.sub(r"[^a-z/ ]", "", _bersih(s).lower()).strip()


def _cocok_label(teks: str) -> tuple[str | None, float]:
    """Cocokkan awal baris ke salah satu label KTP, toleran terhadap salah baca."""
    k = _kunci(teks)
    if not k:
        return None, 0.0
    terbaik, skor = None, 0.0
    for field_, varian in LABEL.items():
        for v in varian:
            potong = k[: max(len(v) + 3, 6)]
            s = SequenceMatcher(None, potong, v).ratio()
            if s > skor:
                terbaik, skor = field_, s
    return (terbaik, skor) if skor >= 0.72 else (None, skor)


def _nilai(baris: str) -> str:
    """Ambil bagian setelah tanda titik dua; kalau OCR menghilangkannya, buang
    kata-kata label di depan."""
    if ":" in baris:
        return _bersih(baris.split(":", 1)[1])
    kata = _bersih(baris).split()
    # Prefiks TERPENDEK dulu. Kalau dibalik, "Nama DOMINIKUS AMBUS" akan cocok
    # sebagai label sepanjang tiga kata dan menyisakan nilai kosong.
    for i in range(1, min(4, len(kata)) + 1):
        f, skor = _cocok_label(" ".join(kata[:i]))
        sisa = " ".join(kata[i:]).strip()
        if f and skor >= 0.85 and sisa:
            return sisa
    return _bersih(baris)


# ── NIK ─────────────────────────────────────────────────────────────────────
@dataclass
class Nik:
    nomor: str
    valid: bool
    alasan: list[str] = field(default_factory=list)
    provinsi: str | None = None
    kode_kab: str | None = None
    tgl_lahir: date | None = None
    jenis_kelamin: str | None = None
    dari_berau: bool = False

    @property
    def tersamar(self) -> str:
        return f"{self.nomor[:4]}{'•' * 8}{self.nomor[-4:]}" if len(self.nomor) == 16 else "—"


def urai_nik(mentah: str) -> Nik:
    n = re.sub(r"\D", "", mentah.translate(DIGIT_FIX))
    alasan: list[str] = []

    if len(n) != 16:
        return Nik(n, False, [f"panjang {len(n)} digit, seharusnya 16"])

    prov, kab = n[:2], n[:4]
    if prov not in PROVINSI:
        alasan.append(f"kode provinsi '{prov}' tidak dikenal")

    hh, bb, tt = int(n[6:8]), int(n[8:10]), int(n[10:12])
    perempuan = hh > 40
    hari = hh - 40 if perempuan else hh

    if not 1 <= bb <= 12:
        alasan.append(f"bulan lahir '{bb:02d}' tidak masuk akal")
    if not 1 <= hari <= 31:
        alasan.append(f"tanggal lahir '{hh:02d}' tidak masuk akal")
    if n[12:16] == "0000":
        alasan.append("nomor urut 0000 tidak dipakai")

    # KTP dewasa: tahun 2 digit di atas tahun berjalan berarti abad sebelumnya
    kini = date.today()
    abad = 2000 if tt <= (kini.year - 2000) else 1900
    lahir = None
    if not alasan:
        try:
            lahir = date(abad + tt, bb, hari)
        except ValueError:
            alasan.append(f"tanggal {hari:02d}-{bb:02d}-{abad + tt} tidak ada di kalender")
        else:
            umur = (kini - lahir).days / 365.25
            if umur < 17:
                alasan.append(f"umur {umur:.0f} tahun — KTP terbit mulai usia 17")
            elif umur > 110:
                alasan.append(f"umur {umur:.0f} tahun tidak masuk akal")

    return Nik(
        nomor=n, valid=not alasan, alasan=alasan,
        provinsi=PROVINSI.get(prov), kode_kab=kab, tgl_lahir=lahir,
        jenis_kelamin=("PEREMPUAN" if perempuan else "LAKI-LAKI") if not alasan else None,
        dari_berau=(kab == KAB_BERAU),
    )


def _cari_nik(baris: list[str]) -> str | None:
    """Cari kandidat NIK meski labelnya tidak terbaca sama sekali."""
    kandidat: list[tuple[int, str]] = []
    for b in baris:
        for tok in re.findall(r"[0-9OoDQIl|!ZzASsGTBgq]{14,20}", b.replace(" ", "")):
            n = re.sub(r"\D", "", tok.translate(DIGIT_FIX))
            if len(n) == 16:
                nik = urai_nik(n)
                kandidat.append((0 if nik.valid else 1, n))
    if not kandidat:
        return None
    kandidat.sort()
    return kandidat[0][1]


# ── hasil parsing ───────────────────────────────────────────────────────────
@dataclass
class HasilKtp:
    nik: Nik | None = None
    nama: str | None = None
    tempat_lahir: str | None = None
    tgl_lahir: date | None = None
    jenis_kelamin: str | None = None
    alamat: str | None = None
    rt_rw: str | None = None
    kel_desa: str | None = None
    kecamatan: str | None = None
    pekerjaan: str | None = None
    provinsi: str | None = None
    kabupaten: str | None = None
    terbaca: dict[str, str] = field(default_factory=dict)
    silang: dict[str, bool] = field(default_factory=dict)
    peringatan: list[str] = field(default_factory=list)

    @property
    def skor(self) -> float:
        """0..1 — seberapa layak hasil ini dipercaya tanpa dilihat manusia."""
        n = 0.0
        if self.nik and self.nik.valid:
            n += 0.35
        if self.nama and len(self.nama) >= 3:
            n += 0.20
        if self.tgl_lahir:
            n += 0.10
        if self.alamat:
            n += 0.05
        if self.kel_desa:
            n += 0.05
        # bobot terbesar justru di pemeriksaan silang, bukan di jumlah kolom terbaca
        if self.silang.get("tgl_lahir"):
            n += 0.15
        if self.silang.get("jenis_kelamin"):
            n += 0.05
        if self.nik and self.nik.dari_berau:
            n += 0.05
        return round(min(n, 1.0), 3)

    @property
    def layak_otomatis(self) -> bool:
        return (self.skor >= 0.85 and bool(self.nik and self.nik.valid)
                and self.silang.get("tgl_lahir", False))


def urai_ktp(baris_ocr: list[str]) -> HasilKtp:
    baris = [_bersih(b) for b in baris_ocr if _bersih(b)]
    h = HasilKtp()
    terbaca: dict[str, str] = {}

    for i, b in enumerate(baris):
        f, _ = _cocok_label(b)
        if not f or f in terbaca:
            continue
        v = _nilai(b)
        # OCR kadang memisahkan label dan isinya ke dua baris
        if not v and i + 1 < len(baris) and not _cocok_label(baris[i + 1])[0]:
            v = baris[i + 1]
        if v:
            terbaca[f] = v

    h.terbaca = terbaca

    nomor = terbaca.get("nik") or ""
    kandidat = _cari_nik(baris)
    if re.sub(r"\D", "", nomor.translate(DIGIT_FIX)) != (kandidat or "") and kandidat:
        nomor = kandidat
    if nomor:
        h.nik = urai_nik(nomor)
        if not h.nik.valid:
            h.peringatan.extend(h.nik.alasan)
    else:
        h.peringatan.append("NIK tidak ditemukan pada gambar")

    if (nm := terbaca.get("nama")):
        # KTP menulis nama dalam huruf besar; gelar di depan bukan bagian nama
        kata = [k for k in re.sub(r"[^A-Za-z '.\-]", " ", nm).split()
                if k.lower().strip(".") not in GELAR]
        h.nama = " ".join(kata).strip().upper() or None

    if (t := terbaca.get("ttl")):
        bagian = t.rsplit(",", 1)
        if len(bagian) == 2:
            h.tempat_lahir = _bersih(bagian[0]).upper() or None
            t = bagian[1]
        if (m := re.search(r"(\d{1,2})\s*[-/. ]\s*(\d{1,2})\s*[-/. ]\s*(\d{2,4})",
                           t.translate(DIGIT_FIX))):
            d, mo, y = (int(m.group(i)) for i in (1, 2, 3))
            y = y if y > 100 else (2000 + y if y <= date.today().year % 100 else 1900 + y)
            try:
                h.tgl_lahir = date(y, mo, d)
            except ValueError:
                h.peringatan.append(f"tanggal lahir '{m.group(0)}' tidak ada di kalender")

    if (jk := terbaca.get("jenis_kelamin")):
        u = jk.upper()
        if "PEREM" in u or "WANITA" in u:
            h.jenis_kelamin = "PEREMPUAN"
        elif "LAKI" in u or "PRIA" in u:
            h.jenis_kelamin = "LAKI-LAKI"

    for k in ("alamat", "rt_rw", "kel_desa", "kecamatan", "pekerjaan", "provinsi", "kabupaten"):
        if (v := terbaca.get(k)):
            setattr(h, k, v.upper() if k != "alamat" else v)

    # ── pemeriksaan silang: dua pembacaan independen harus sepakat ──────────
    if h.nik and h.nik.valid:
        if h.tgl_lahir:
            sama = h.tgl_lahir == h.nik.tgl_lahir
            h.silang["tgl_lahir"] = sama
            if not sama:
                h.peringatan.append(
                    f"tanggal lahir di kolom ({h.tgl_lahir:%d-%m-%Y}) berbeda dari "
                    f"yang tersimpan di NIK ({h.nik.tgl_lahir:%d-%m-%Y})")
        if h.jenis_kelamin:
            sama = h.jenis_kelamin == h.nik.jenis_kelamin
            h.silang["jenis_kelamin"] = sama
            if not sama:
                h.peringatan.append("jenis kelamin di kolom berbeda dari yang tersimpan di NIK")
        if not h.nik.dari_berau:
            h.peringatan.append(
                f"NIK diterbitkan di luar Kabupaten Berau ({h.nik.provinsi or 'tidak dikenal'})")

    return h


# ── pencocokan ke master petani ─────────────────────────────────────────────
# Nama orang tidak memuat angka. Angka yang muncul di hasil OCR nama hampir
# selalu huruf yang salah dibaca, jadi dikembalikan sebelum dibandingkan.
_HURUF_LAGI = str.maketrans({"0": "o", "1": "i", "2": "z", "4": "a",
                             "5": "s", "6": "g", "7": "t", "8": "b", "9": "g"})


def _normal_nama(s: str) -> str:
    s = unicodedata.normalize("NFKD", str(s)).lower().translate(_HURUF_LAGI)
    s = re.sub(r"[^a-z ]", " ", s)
    kata = [k for k in s.split() if k not in GELAR]
    return " ".join(kata)


def skor_nama(a: str, b: str) -> float:
    """Gabungan kemiripan berurutan dan kemiripan himpunan kata.

    Dua-duanya perlu: 'Dominikus Ambus' vs 'Ambus Dominikus' punya kemiripan
    berurutan rendah tapi himpunan kata identik, dan urutan nama di KTP memang
    kadang terbalik dari catatan survei.
    """
    na, nb = _normal_nama(a), _normal_nama(b)
    if not na or not nb:
        return 0.0
    urut = SequenceMatcher(None, na, nb).ratio()
    # Urutan nama depan/belakang di KTP kerap terbalik dari catatan survei,
    # jadi versi kata-terurut ikut dibandingkan (dikenai potongan kecil).
    terurut = SequenceMatcher(None, " ".join(sorted(na.split())),
                              " ".join(sorted(nb.split()))).ratio() * 0.98
    ta, tb = set(na.split()), set(nb.split())
    himpunan = len(ta & tb) / len(ta | tb)
    return round(max(urut, terurut, 0.45 * urut + 0.55 * himpunan), 3)


@dataclass
class Calon:
    farmer_code: str
    nama_petani: str
    kampung: str | None
    skor: float
    cocok_kampung: bool = False


def cocokkan_petani(nama_ktp: str, master: list[dict], kel_desa: str | None = None,
                    batas_otomatis: float = 0.92,
                    jarak_aman: float = 0.08) -> tuple[Calon | None, list[Calon]]:
    """Kembalikan (calon otomatis atau None, tiga teratas untuk verifikator).

    Pencocokan otomatis hanya dilakukan bila calon teratas sangat mirip DAN
    jelas lebih baik daripada calon kedua. Dua nama yang sama-sama mirip 0,93
    berarti kartu bisa jatuh ke orang yang salah — itu kasus untuk manusia,
    bukan untuk ambang.

    `kel_desa` adalah kolom Kel/Desa dari KTP. Ini pemutus yang murah dan
    kuat: master memuat 'Jupri' di Birang dan 'Pak Jupri' di Pegat Bukur —
    dua orang berbeda berjarak 16,8 km yang namanya menormalkan jadi sama
    persis. Nama saja tidak akan pernah bisa memisahkan keduanya; kampung
    bisa.
    """
    desa = _normal_nama(kel_desa or "")
    calon = sorted(
        (Calon(m["farmer_code"], m["nama_petani"], m.get("kampung_utama"),
               skor_nama(nama_ktp, m["nama_petani"]),
               bool(desa) and desa == _normal_nama(m.get("kampung_utama") or ""))
         for m in master),
        key=lambda c: (-c.skor, not c.cocok_kampung))[:3]
    if not calon:
        return None, []
    teratas = calon[0]
    kedua = calon[1].skor if len(calon) > 1 else 0.0

    # Kalau nama di KTP hanyalah bagian dari nama petani lain — "WIHELMUS"
    # terhadap "Wihelmus Soet" — angka kemiripan boleh saja tinggi, tetapi
    # yang sebenarnya terjadi adalah dua orang berbeda dengan nama beririsan.
    # Kasus seperti ini selalu diserahkan ke verifikator.
    kata_ktp = set(_normal_nama(nama_ktp).split())
    bagian_dari_orang_lain = any(
        kata_ktp and kata_ktp < set(_normal_nama(c.nama_petani).split())
        for c in calon[1:])

    # Nama pendek ("Ali", "Ardi") terlalu mudah tertukar untuk diproses
    # otomatis, kecuali kampungnya ikut menguatkan.
    cukup_panjang = len(_normal_nama(teratas.nama_petani)) >= 5 or teratas.cocok_kampung

    # Skor seri atau nyaris seri boleh diputus oleh kampung — tetapi hanya
    # bila SATU calon saja yang kampungnya cocok.
    unggul = teratas.skor - kedua >= jarak_aman
    if not unggul and teratas.cocok_kampung:
        seri = [c for c in calon if teratas.skor - c.skor < jarak_aman]
        unggul = sum(1 for c in seri if c.cocok_kampung) == 1

    otomatis = teratas if (teratas.skor >= batas_otomatis and unggul
                           and cukup_panjang and not bagian_dari_orang_lain) else None
    return otomatis, calon

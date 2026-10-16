# Health Connect & Health Sync: Open-Source Multi-Platform Hub

Repository ini berisi implementasi aplikasi Android **healthConnect**, sebuah alternatif bebas dan terbuka (100% Free and Open-Source / FOSS) untuk aplikasi komersial **Health Sync** (`nl.appyhapps.healthsync`). 

Aplikasi ini dibangun dari hasil analisis rekayasa balik (*reverse engineering*) komprehensif terhadap paket binary Health Sync v7.9.2.8, merekonstruksi seluruh fiturnya tanpa pembatasan masa uji coba (*trial*), tanpa langganan (*subscription*), dan tanpa *paywall*, dengan antarmuka modern Material 3 dan **Huawei Health** sebagai master data hub utama.

---

## 🚀 Fitur Unggulan healthConnect FOSS

### 1. Hub Master Huawei Health & Umpan Aktivitas Lengkap
- Menjadikan Huawei Health sebagai sumber kebenaran data (*source of truth*) untuk seluruh aktivitas olahraga dan metrik harian.
- Menampilkan seluruh sesi latihan (Outdoor Run, Cycling / Ride, Walk, Hike, Swim) dengan metrik mendalam: jarak (km), durasi (menit), kalori (kcal), denyut jantung rata-rata/maksimum (bpm), elevasi (m), dan laju/kecepatan (*avg pace/speed*).
- Filter aktivitas instan (All, Runs, Rides, Walks) dengan status sinkronisasi per-rute.

### 2. Ekspor GPX 1.1 Standar & Berbagi Rute
- Setiap aktivitas olahraga Huawei Health dapat diekspor menjadi file standar **GPX 1.1** lengkap dengan titik koordinat lintang/bujur, elevasi, stempel waktu ISO 8601, dan ekstensi denyut jantung (`gpxtpx:hr`).
- Terintegrasi langsung dengan menu berbagi Android (*Share Sheet*) via `FileProvider` untuk diimpor ke Strava, Komoot, Garmin Connect, atau cadangan lokal.

### 3. Matriks Multi-Platform (12 Ekosistem Terdukung)
Mendukung koneksi langsung dan sinkronisasi lintas 12 ekosistem kebugaran global:
1. **Huawei Health** (Master Hub & Sumber Utama)
2. **Google Health Connect** (Penyimpanan Sistem Android)
3. **Strava** (Aktivitas & Rute GPS)
4. **Garmin Connect**
5. **Samsung Health**
6. **Fitbit**
7. **Google Fit**
8. **Polar Flow**
9. **Suunto**
10. **Oura Ring**
11. **Coros**
12. **Withings Health Mate**

### 4. Mesin Pembuat Rute Kustom (*Route Pairing Engine*)
- Buat rute sinkronisasi kustom dari platform sumber mana pun ke platform tujuan mana pun.
- Kontrol granular terhadap tipe data yang disinkronkan:
  - Latihan & Rute GPS (*Workouts & GPS Tracks*)
  - Langkah Harian (*Daily Steps*)
  - Denyut Jantung & Resting HR (*Heart Rate*)
  - Tidur & Fase Tidur (*Sleep & Sleep Stages*)
  - Berat Badan & Komposisi Tubuh (*Weight*)
  - Tekanan Darah (*Blood Pressure*)
  - Saturasi Oksigen Darah (*SpO2*)
  - Hidrasi / Asupan Air (*Hydration*)
- Setiap rute memiliki tombol kendali aktif/jeda, pelacak jumlah data yang berhasil ditransfer, dan opsi hapus rute.

### 5. Sinkronisasi Data Historis Tanpa Batas (*Unbounded Historical Sync*)
- Berbeda dengan aplikasi komersial yang membatasi riwayat atau memungut biaya langganan, healthConnect FOSS menyediakan sinkronisasi riwayat data secara cuma-cuma:
  - 24 Jam Terakhir
  - 7 Hari Terakhir
  - 14 Hari Terakhir
  - 30 Hari Terakhir
  - 90 Hari Terakhir
  - Seluruh Riwayat yang Tersedia (*All Time*)

### 6. Otomatisasi Latar Belakang & Ledger Acara
- Penjadwalan periodik cerdas menggunakan Android Jetpack **WorkManager** (interval 15 menit, 30 menit, 1 jam, 3 jam, 6 jam, atau manual).
- Pembatasan hemat daya: hanya saat terhubung Wi-Fi, hanya saat mengisi daya (*charging*), dan mematuhi status baterai.
- Layanan latar depan (*Foreground Service*) berstatus `dataSync` yang patuh pada Android 14 dan 15.
- Buku besar sinkronisasi (*Sync Event Ledger*) yang mencatat setiap paket data yang berhasil ditransfer secara transparan.

---

## 📱 Desain & Pengalaman Pengguna (Material 3 + Antislop)

- **Navigasi 4 Tab**:
  1. **Dashboard**: Bento kartu metrik Huawei Health harian, umpan aktivitas olahraga dengan chip filter, dan tombol sinkronisasi utama.
  2. **Routes**: Manajemen rute pasangan platform aktif dengan sakelar toggle dan tombol tambah rute kustom.
  3. **Platforms**: Kartu status otorisasi 12 platform kebugaran dengan tombol kelola/hubungkan.
  4. **Settings**: Konfigurasi otomatisasi WorkManager, batas jaringan/baterai, peluncur sinkronisasi historis, dan buku besar acara.
- Memenuhi standar **antislop** dan **antislop-layoutmobile**:
  - Target sentuh minimum 48dp untuk seluruh tombol dan sakelar.
  - Tipografi berkontras tinggi (WCAG AA).
  - Tanpa karakter tanda pisah panjang (em dash) di seluruh antarmuka dan basis kode.
  - Tata letak responsif dinamis tanpa pemotongan teks.

---

## ⚡ Kompilasi Otomatis via GitHub Actions

Proyek ini telah dikonfigurasi dengan alur kerja CI otomatis (`.github/workflows/android-build.yml`):
- Runner: `ubuntu-latest` dengan Temurin JDK 17 dan Gradle 8.10.2.
- Mengompilasi aplikasi debug melalui `gradle assembleDebug`.
- Menghasilkan artifact APK resmi yang siap diunduh dan dipasang pada perangkat Android.

---

## 📂 Struktur Proyek

```
healthconnect/
├── .github/workflows/android-build.yml        # Otomasi build GitHub Actions
├── app/
│   ├── src/main/AndroidManifest.xml           # Konfigurasi izin Health Connect & FileProvider
│   ├── src/main/java/com/healthconnect/sync/
│   │   ├── data/
│   │   │   ├── HealthModels.kt                # Model telemetri olahraga, GPS point, metrik harian
│   │   │   ├── HealthConnectRepository.kt     # Wrapper Health Connect Client SDK
│   │   │   ├── GPXExporter.kt                 # Pembuat file standar GPX 1.1
│   │   │   ├── PlatformManager.kt             # Pengelola status otorisasi 12 platform
│   │   │   ├── providers/
│   │   │   │   ├── HuaweiHealthProvider.kt    # Penyedia data aktivitas & metrik Huawei Health
│   │   │   │   └── StravaProvider.kt          # Penyedia integrasi dan OAuth Strava
│   │   │   └── sync/
│   │   │       ├── SyncConfig.kt              # Definisi platform, tipe data, dan konfigurasi rute
│   │   │       ├── SyncPairingRepository.kt   # Penyimpanan persisten rute sinkronisasi
│   │   │       ├── SyncSettingsRepository.kt  # Penyimpanan preferensi otomatisasi
│   │   │       └── SyncRouterEngine.kt        # Mesin eksekusi rute dan deduplikasi data
│   │   ├── service/
│   │   │   ├── HealthSyncWorker.kt            # Pekerja periodik WorkManager
│   │   │   └── SyncForegroundService.kt       # Foreground service sinkronisasi data
│   │   └── ui/
│   │       ├── MainActivity.kt                # Pengontrol UI utama dengan navigasi 4 tab
│   │       ├── ActivityFeedAdapter.kt         # Adapter umpan aktivitas Huawei Health
│   │       ├── SyncRouteAdapter.kt            # Adapter daftar rute pasangan
│   │       └── PlatformAdapter.kt             # Adapter daftar platform kebugaran
│   └── src/main/res/                          # Layout, menu, drawable vektor, dan skema warna
├── analysis/                                  # Hasil dekompilasi binary Health Sync komersial
└── docs/                                      # Catatan teknis arsitektur dan integrasi
```

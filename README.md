# Health Connect & Health Sync Reverse Engineering Analysis

Repository ini berisi hasil analisis rekayasa balik (*reverse engineering*) komprehensif terhadap paket aplikasi Android **Health Sync** (`nl.appyhapps.healthsync` versi 7.9.2.8), difokuskan pada implementasi integrasi **Android Health Connect**, perizinan latar belakang, arsitektur sinkronisasi, dan pemodelan data antar platform kebugaran.

Analisis ini dieksekusi menggunakan metodologi **REA (Reverse Engineer Anything)** dan parser binary Android.

---

## 📊 Ringkasan Target Aplikasi

```yaml
Target File: health-sync-7-9-2-8.apk
Package: nl.appyhapps.healthsync
Version Name: 7.9.2.8
Version Code: 1224
Min SDK: 24 (Android 7.0)
Target SDK: 37 (Android 15+)
SHA-256: 79eca4d3f6a8ba34af471c2f7c72b4cfe5929c7a9172a9247effa1d1f857e989
MultiDex: Ya (3 DEX files, 73.644 unique strings)
Native ABIs: arm64-v8a, armeabi-v7a, x86, x86_64
Total Permissions: 74 (60 izin spesifik Health Connect)
```

---

## 🗂 Struktur Repository

```
healthconnect/
├── README.md                                  # Dokumentasi utama proyek
├── docs/
│   ├── HEALTH_CONNECT_INTEGRATION.md          # Analisis detail integrasi Android Health Connect
│   └── ARCHITECTURE.md                        # Desain sinkronisasi, konkurensi, dan native libraries
└── analysis/
    ├── AndroidManifest.xml                    # Hasil dekompilasi penuh manifes aplikasi
    ├── permissions.json                       # 74 izin Android & Health Connect terstruktur
    ├── oauth_endpoints.json                   # Daftar deep links, custom URI schemes & OAuth redirect
    ├── data_models.json                       # 50 pemetaan model data internal vs Health Connect records
    └── components.json                        # Komponen activity, service, provider & library native
```

---

## 🔍 Temuan Utama

### 1. Kepatuhan Standar Android Health Connect
Health Sync secara ketat mengimplementasikan protokol resmi Google Health Connect:
- **Rationale Intent**: Mendeklarasikan filter `androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE` pada `MainActivity` untuk edukasi perizinan.
- **Privacy Policy / Usage**: Mengimplementasikan `activity-alias` `ViewPermissionUsageActivity` dengan `android.intent.action.VIEW_PERMISSION_USAGE` dan perizinan `android.permission.START_VIEW_PERMISSION_USAGE`.
- **Izin Lanjutan Android 14+**:
  - `READ_HEALTH_DATA_IN_BACKGROUND` untuk sinkronisasi otomatis tanpa membuka aplikasi.
  - `READ_HEALTH_DATA_HISTORY` untuk mengakses riwayat kesehatan lebih dari 30 hari.

### 2. Ekosistem Multi-Platform yang Didukung
Aplikasi mengintegrasikan lebih dari 25 platform kesehatan global:
- **Lokal SDK**: Health Connect, Google Fit, Samsung Health (`com.samsung.android.sdk.healthdata`), Huawei Health Kit (`com.huawei.hms.hihealth`).
- **Cloud OAuth & Relay**: Garmin, Strava, Fitbit, Polar, Suunto, Coros, Oura, Withings, FatSecret, Intervals.icu, Concept2, RideWithGPS, MedM, Smashrun, Guava, Schrittmeister.

### 3. Pipeline Latar Belakang & Kompresi Zstandard
- Sinkronisasi latar belakang berjalan aman pada Android modern menggunakan `SyncFGService` berstatus foreground service tipe `dataSync` (`0x00000001`).
- Menggunakan pustaka native berkinerja tinggi **`libzstd-jni-1.5.7-13.so`** (Zstandard) untuk kompresi data GPS dan titik telemetry besar, serta **`libdatastore_shared_counter.so`** untuk koordinasi multi-proses antarkomponen.

---

## 🛠️ Rebuilt Open-Source Android Application

Berdasarkan hasil analisis reverse engineering terhadap Health Sync, proyek ini merekonstruksi aplikasi sinkronisasi Health Connect dalam bentuk proyek Android Native modern berbasis **Kotlin**, **Android Jetpack**, dan **Health Connect Client SDK**:

- **Package**: `com.healthconnect.sync`
- **Health Connect Client**: `androidx.health.connect:connect-client:1.1.0-alpha11`
- **UI Architecture**: Material 3 + ViewBinding
- **Background Engine**: Android Jetpack WorkManager (`PeriodicWorkRequest`) + Foreground Data Sync Service (`SyncForegroundService`)
- **Permission Flow**: Rationale permission dialog handler (`androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE`) & Privacy policy alias (`ViewPermissionUsageActivity`)

---

## ⚡ Automated CI / GitHub Actions Compiler

Aplikasi dikompilasi secara otomatis di awan melalui **GitHub Actions Workflow** (`.github/workflows/android-build.yml`):
- Berjalan di runner `ubuntu-latest` dengan JDK 17 & Android SDK.
- Mengompilasi proyek dengan Gradle (`gradle assembleDebug`).
- Menghasilkan file **`app-debug.apk`** yang dapat langsung diunduh dari tab **Actions > Artifacts** di repository GitHub tanpa perlu setup Android Studio lokal!

---

## 📖 Dokumentasi Lengkap

- [Panduan Integrasi Health Connect](docs/HEALTH_CONNECT_INTEGRATION.md)
- [Arsitektur Sistem & Rekayasa Balik](docs/ARCHITECTURE.md)
- [Dekomposisi Manifes Android](analysis/AndroidManifest.xml)
- [Workflow Kompilasi GitHub Actions](.github/workflows/android-build.yml)

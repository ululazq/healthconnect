# Android Health Connect Integration Analysis

Analisis mendalam mengenai implementasi dan integrasi **Android Health Connect** pada aplikasi **Health Sync** (`nl.appyhapps.healthsync` versi 7.9.2.8).

---

## 1. Arsitektur Health Connect pada Android

Health Connect adalah platform dan API bersama dari Google & Android untuk menyelaraskan data kesehatan, kebugaran, dan tidur antar berbagai aplikasi secara aman dan privat.

Aplikasi Health Sync mendukung integrasi ganda:
- **Android 14+ (API 34+)**: Berjalan melalui subsistem bawaan OS Android (`android.health.connect.HealthConnectManager`).
- **Android < 14 (API 26–33)**: Berjalan melalui library Jetpack (`androidx.health.connect.client.HealthConnectClient`) yang berkomunikasi dengan aplikasi APK Health Connect mandiri via `HealthDataSdkService`.

---

## 2. Kontrak Manifes & Kebijakan Privasi

Google mewajibkan aplikasi yang berintegrasi dengan Health Connect untuk mematuhi kontrak manifes berikut, yang terbukti diimplementasikan secara presisi pada manifes Health Sync:

### A. Rationale Permissions Activity
Pengguna yang membuka Health Connect akan melihat dialog atau tautan edukasi mengapa aplikasi memerlukan akses data. Health Sync mendaftarkan intent filter ini pada `MainActivity`:

```xml
<activity android:name="nl.appyhapps.healthsync.MainActivity" android:exported="true" android:launchMode="1">
  <intent-filter>
    <action android:name="android.intent.action.MAIN"/>
    <category android:name="android.intent.category.LAUNCHER"/>
  </intent-filter>
  <!-- Health Connect Permission Rationale -->
  <intent-filter>
    <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"/>
  </intent-filter>
</activity>
```

### B. Health Permissions Usage Alias (`ViewPermissionUsageActivity`)
Untuk memenuhi regulasi transparansi Android Health Connect, tautan kebijakan privasi atau histori penggunaan izin disajikan melalui `activity-alias`:

```xml
<activity-alias 
    android:name="nl.appyhapps.healthsync.ViewPermissionUsageActivity" 
    android:permission="android.permission.START_VIEW_PERMISSION_USAGE" 
    android:exported="true" 
    android:targetActivity="nl.appyhapps.healthsync.MainActivity">
  <intent-filter>
    <action android:name="android.intent.action.VIEW_PERMISSION_USAGE"/>
    <category android:name="android.intent.category.HEALTH_PERMISSIONS"/>
  </intent-filter>
</activity-alias>
```

---

## 3. Matriks Perizinan Health Connect (Permissions)

Health Sync mendeklarasikan total **60 izin spesifik Health Connect** (30 Izin Baca + 30 Izin Tulis), mencakup seluruh domain data kesehatan:

| Kategori Data | Izin Baca (READ) | Izin Tulis (WRITE) |
|---|---|---|
| **Aktivitas & Langkah** | `READ_STEPS`, `READ_STEPS_CADENCE`, `READ_DISTANCE` | `WRITE_STEPS`, `WRITE_STEPS_CADENCE`, `WRITE_DISTANCE` |
| **Olahraga & Rute** | `READ_EXERCISE`, `READ_EXERCISE_ROUTES` | `WRITE_EXERCISE`, `WRITE_EXERCISE_ROUTE`, `WRITE_FLOORS_CLIMBED` |
| **Energi & Kalori** | `READ_ACTIVE_CALORIES_BURNED`, `READ_TOTAL_CALORIES_BURNED`, `READ_BASAL_METABOLIC_RATE` | `WRITE_ACTIVE_CALORIES_BURNED`, `WRITE_TOTAL_CALORIES_BURNED`, `WRITE_BASAL_METABOLIC_RATE` |
| **Kardiovaskular** | `READ_HEART_RATE`, `READ_RESTING_HEART_RATE`, `READ_HEART_RATE_VARIABILITY` | `WRITE_HEART_RATE`, `WRITE_RESTING_HEART_RATE`, `WRITE_HEART_RATE_VARIABILITY` |
| **Metabolisme & Darah** | `READ_BLOOD_GLUCOSE`, `READ_BLOOD_PRESSURE`, `READ_OXYGEN_SATURATION` | `WRITE_BLOOD_GLUCOSE`, `WRITE_BLOOD_PRESSURE`, `WRITE_OXYGEN_SATURATION` |
| **Komposisi Tubuh** | `READ_WEIGHT`, `READ_BODY_FAT`, `READ_BODY_WATER_MASS`, `READ_BONE_MASS` | `WRITE_WEIGHT`, `WRITE_BODY_FAT`, `WRITE_BODY_WATER_MASS`, `WRITE_BONE_MASS`, `WRITE_HEIGHT`, `WRITE_LEAN_BODY_MASS` |
| **Tidur & Pernapasan** | `READ_SLEEP`, `READ_RESPIRATORY_RATE` | `WRITE_SLEEP`, `WRITE_RESPIRATORY_RATE` |
| **Suhu Tubuh** | `READ_BODY_TEMPERATURE`, `READ_SKIN_TEMPERATURE` | `WRITE_BODY_TEMPERATURE`, `WRITE_SKIN_TEMPERATURE` |
| **Nutrisi & Hidrasi** | `READ_NUTRITION`, `READ_HYDRATION` | `WRITE_NUTRITION`, `WRITE_HYDRATION` |
| **Kebugaran Spesifik** | `READ_CYCLING_PEDALING_CADENCE`, `READ_POWER`, `READ_SPEED`, `READ_VO2_MAX` | `WRITE_CYCLING_PEDALING_CADENCE`, `WRITE_POWER`, `WRITE_SPEED`, `WRITE_VO2_MAX` |

### Izin Lanjutan Android 14 (Advanced Permissions)
1. **`android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND`**:
   Memungkinkan sinkronisasi latar belakang berjalan secara otomatis tanpa pengguna harus selalu membuka aplikasi.
2. **`android.permission.health.READ_HEALTH_DATA_HISTORY`**:
   Mengizinkan aplikasi membaca histori data kesehatan lebih dari batas default 30 hari terakhir.

---

## 4. Pemetaan Model Data Internal vs Health Connect Records

Dalam paket `nl.appyhapps.healthsync.data`, model data dirancang untuk melakukan serialisasi dan deserialisasi dua arah:

| Model Kotlin Internal | Record Health Connect | Deskripsi |
|---|---|---|
| `ActivityData` / `WorkoutData` | `ExerciseSessionRecord` | Sesi latihan, tipe olahraga, durasi |
| `LocationData` | `ExerciseRoute` | Titik koordinat GPS dan elevasi rute |
| `BinningHRData` / `HeartRateData` | `HeartRateRecord` | Detak jantung sampel & agregat per menit |
| `BinningStepData` / `StepData` | `StepsRecord` | Langkah kaki per interval waktu |
| `BloodPressureData` | `BloodPressureRecord` | Tekanan darah sistolik & diastolik (mmHg) |
| `BloodSugarData` | `BloodGlucoseRecord` | Glukosa darah (mmol/L atau mg/dL) |
| `Calories` | `ActiveCaloriesBurnedRecord` | Pembakaran kalori aktif |
| `HRVData` | `HeartRateVariabilityRmssdRecord` | Variabilitas denyut jantung (rMSSD) |
| `NutritionData` / `FoodDescription`| `NutritionRecord` | Karbohidrat, protein, lemak, mikronutrien |
| `OxygenSaturationData` | `OxygenSaturationRecord` | Saturasi oksigen darah (SpO2 %) |
| `SleepPeriod` / `SleepStage` | `SleepSessionRecord`, `SleepStageRecord`| Tahap tidur (Light, Deep, REM, Awake) |
| `TemperatureData` | `BodyTemperatureRecord`, `SkinTemperatureRecord` | Suhu inti tubuh atau suhu pergelangan |
| `WaterData` | `HydrationRecord` | Konsumsi air minum (Volume / Liter) |
| `WeightData` | `WeightRecord`, `BodyFatRecord` | Berat badan, % lemak tubuh, massa otot |

---

## 5. Mekanisme Ekspor & Impor (`HCExportActivity`)

Aplikasi menyediakan `HCExportActivity` khusus yang bertugas:
1. Membaca data dari Health Connect secara batch via `HealthConnectClient.readRecords()`.
2. Melakukan agregasi (menggunakan `AggregateRequest` dan `AggregateMetric`).
3. Mengonversi data ke format target partner (Garmin FIT/TCX, Strava GPX/API, Samsung Health SDK, atau Huawei Health Kit).
4. Menangani resolusi konflik jika record dengan timestamp sama sudah tercatat di platform target.

# Security Summary

Audit keamanan dan keandalan dilakukan menyeluruh pada **PARIJOTO LEARNING** terhadap alur autentikasi, kontrol akses berbasis peran (`ADMIN`, `GURU`, `SISWA`), aturan keamanan Cloud Firestore (`firestore.rules`), perlindungan kunci jawaban dan nilai, pembatasan unggahan lampiran, serta penegakan status sistem.

### Temuan & Perbaikan yang Diterapkan

1. **Pencegahan Mutasi Field Terlarang saat Siswa Bergabung ke Kelompok PBL**
   - **Risiko:** `PblAndAssessmentRepository.joinPblGroup` sebelumnya turut mengirim `leaderUid` saat siswa bergabung ke kelompok terbuka, padahal `firestore.rules` pada koleksi `groups/{groupId}` hanya mengizinkan pembaruan field `memberUids`, `memberNames`, `memberRoles`, dan `updatedAt` untuk anggota baru.
   - **Perbaikan:** Menghapus `leaderUid` dari payload pembaruan saat proses gabung kelompok sehingga integritas peran ketua kelompok tetap terjaga dan tidak memicu penolakan izin Firestore.

2. **Pembatasan Status & Field Immutable pada Percobaan Kuis Siswa**
   - **Risiko:** `QuizAndEvaluationRepository.autosaveQuizAttempt` dan `submitQuizAttempt` sebelumnya memperbarui dokumen `quiz_attempts/{attemptId}` dengan `set(...)` penuh dan memungkinkan status `"GRADED"` ditulis dari sisi siswa, melanggar batasan aturan `firestore.rules` yang mengunci field `quizId`, `studentUid`, `teacherUid`, `createdAt`, serta membatasi status sisi siswa hanya ke `IN_PROGRESS` atau `SUBMITTED`.
   - **Perbaikan:** Menggunakan `update(...)` dengan daftar field yang diizinkan aturan untuk dokumen yang sudah ada, serta memastikan pengiriman siswa selalu berstatus `"SUBMITTED"`. Status `"GRADED"` dan penerbitan dokumen pada koleksi `grades` tetap hanya dapat ditulis oleh akun `GURU` atau `ADMIN`.

3. **Penegakan Mode Pemeliharaan & Penutupan Pendaftaran di Session Shell**
   - **Risiko:** Pengaturan sistem `maintenanceMode` dan `allowRegistration` yang dikelola Admin di Firestore sebelumnya belum memblokir masuknya pengguna pada layer navigasi terautentikasi.
   - **Perbaikan:** Menambahkan layar `MaintenanceModeScreen` (memblokir `GURU` dan `SISWA` saat `maintenanceMode = true`, tetap mengizinkan `ADMIN`) dan `RegistrationClosedScreen` (menolak pendaftaran akun baru selain email bootstrap admin saat `allowRegistration = false`) pada `AuthenticatedSessionShell` di `ParijotoNavGraph.kt`.

4. **Pembatasan Kueri Kelompok Lintas Kelas & Pencegahan Kelompok Tanpa Kelas**
   - **Perbaikan:** Mempersempit `observeGroupsForClass(classId)` pada `PblAndAssessmentRepository` agar hanya membaca kelompok terbuka untuk `classId` terkait (tidak melakukan pembacaan lintas kelas), serta menonaktifkan pembuatan kelompok/tugas PBL di `TeacherPblMonitoringSection` apabila guru belum memiliki kelas aktif.

5. **Perlindungan Kunci Jawaban, Nilai, & Validasi Ukuran Berkas Lampiran**
   - `quizzes/{quizId}/answer_keys/{keyDocId}` pada `firestore.rules` mengunci akses `get` siswa hingga kuis berstatus `closed == true && showDiscussion == true && resultsPublished == true` (`allow list: if false`).
   - Koleksi `grades/{gradeId}` hanya dapat dibaca siswa apabila `published == true` dan dimiliki oleh UID siswa bersangkutan.
   - `StudentLkmEditorScreen` kini menghitung ukuran stream bytes hingga batas `5 MB + 1 byte` apabila `OpenableColumns.SIZE` tidak tersedia dari penyedia URI perangkat, menjaga konsistensi validasi tipe (`image/*`, `application/pdf`) dan batas ukuran 5 MB.

---

## PoC Verification

Verifikasi keamanan dan keandalan dilakukan pada kode sumber, konfigurasi aturan database, serta pengujian otomatis JVM/Robolectric (karena lingkungan build tidak memiliki emulator Android maupun `adb` aktif, pengujian jaringan langsung ke server Google Sign-In/App Check memerlukan perangkat/emulator fisik saat deployment):

1. **Verifikasi Invarian Autentikasi & Kredensial (Statis)**
   - Pemeriksaan `grep` pada seluruh `app/src/main/` mengonfirmasi nol penggunaan `signInAnonymously`, `signInWithEmailAndPassword`, maupun client ID hardcoded (`AUTH_INVARIANT_OK`).
   - Autentikasi menggunakan Google Sign-In via Jetpack `CredentialManager` (`GetSignInWithGoogleOption`) dan Firebase App Check tanpa kunci API statis di `.env.example` atau source code.

2. **Verifikasi Guard Navigasi, Autosave, Pembatasan Kelas, Locale CSV, & Responsivitas (Robolectric)**
   - Seluruh 38 kasus uji pada `ExampleRobolectricTest` dan `Phase8AuditRobolectricTest` berhasil lulus (`gradle :app:testDebugUnitTest`):
     - `MaintenanceModeScreen` dan `RegistrationClosedScreen` tampil sesuai status serta menjalankan callback keluar akun dengan benar.
     - `QuizRunnerScreen` melakukan flush jawaban tertunda ke `onAutosave` saat tombol kembali ditekan sebelum debounce selesai.
     - `AssignmentEditorScreen` menampilkan pemberitahuan informatif saat siswa belum tergabung ke kelas, dan `TeacherCreateGroupAndAssignmentCards` menonaktifkan tombol tambah kelompok saat daftar kelas kosong.
     - Format angka desimal ekspor CSV memakai `Locale.US` agar tetap bertitik (`.`) walaupun perangkat bersetelan lokal Indonesia (`in_ID`).
     - Tata letak responsif terverifikasi pada layar ponsel sempit (`360dp`) dengan `NavigationBar` bawah dan pada tablet (`840dp`) dengan `NavigationRail` samping.

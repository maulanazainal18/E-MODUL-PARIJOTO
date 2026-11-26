package com.example.data.dummy

data class DummyStatItem(
    val id: String,
    val label: String,
    val value: String,
    val note: String
)

data class DummyClassPreview(
    val id: String,
    val name: String,
    val code: String,
    val studentCount: Int,
    val activeModule: String
)

data class DummyModulePreview(
    val id: String,
    val title: String,
    val meeting: String,
    val topicsCount: Int,
    val progressPercent: Int
)

data class DummyTaskPreview(
    val id: String,
    val title: String,
    val pblPhase: String,
    val status: String
)

object Phase1DummyData {

    val onboardingSlides = listOf(
        Triple(
            "Eksplorasi Etnosains Parijoto",
            "Pelajari keanekaragaman hayati tingkat gen, spesies, dan ekosistem melalui tanaman endemik Gunung Muria (Medinilla speciosa).",
            "Berbasis E-Modul karya Ahzami, S.Pd., Gr. (2026)"
        ),
        Triple(
            "Problem Based Learning (4 JP)",
            "Ikuti 2 pertemuan pembelajaran aktif: validasi sains tradisi lokal hingga analisis Paradoks Konservasi & ekofisiologi sel.",
            "5 Fase PBL Terstruktur"
        ),
        Triple(
            "Kolaborasi E-LKM, Kuis & CER",
            "Bekerja dalam kelompok PBL, selesaikan kuis HOTS, susun kampanye digital konservasi, dan pantau progres belajar secara responsif.",
            "Untuk Siswa, Guru, & Admin"
        )
    )

    val adminStats = listOf(
        DummyStatItem("st_guru", "Akun Guru", "6", "Aktif membina kelas X"),
        DummyStatItem("st_siswa", "Akun Siswa", "184", "Tersebar di 6 rombel"),
        DummyStatItem("st_kelas", "Kelas Aktif", "6", "TA 2026/2027 Ganjil"),
        DummyStatItem("st_avg", "Rata-rata Nilai", "84", "Post-Test & Proyek CER")
    )

    val adminRecentActivities = listOf(
        "Guru Bu Sari membuat kelas X IPA 1 (Kode: PRJM26A)",
        "Kelompok Epifit 2 mengumpulkan E-LKM 1 Orientasi Masalah",
        "14 siswa menyelesaikan Latihan Pemahaman Modul 1"
    )

    val guruClasses = listOf(
        DummyClassPreview("cls_1", "X IPA 1 — Lereng Muria", "PRJ26A", 32, "Modul 1: Etnosains Parijoto"),
        DummyClassPreview("cls_2", "X IPA 2 — Colo", "PRJ26B", 30, "Modul 2: Konservasi Parijoto")
    )

    val guruPendingQueue = listOf(
        DummyTaskPreview("t_1", "E-LKM 1: Orientasi Masalah (30 pengumpulan)", "PBL Fase 1–2", "Siap Dinilai"),
        DummyTaskPreview("t_2", "Evaluasi Akhir Post-Test Uraian", "Pertemuan 2", "Menunggu Rubrik"),
        DummyTaskPreview("t_3", "Proyek Kampanye Digital CER", "PBL Fase 5", "Berlangsung")
    )

    val siswaModules = listOf(
        DummyModulePreview(
            id = "m1",
            title = "Modul 1: Etnosains Parijoto",
            meeting = "Pertemuan 1 (2 × 45 Menit)",
            topicsCount = 15,
            progressPercent = 65
        ),
        DummyModulePreview(
            id = "m2",
            title = "Modul 2: Pemanfaatan & Konservasi",
            meeting = "Pertemuan 2 (2 × 45 Menit)",
            topicsCount = 17,
            progressPercent = 20
        )
    )

    val siswaTasks = listOf(
        DummyTaskPreview("st_1", "E-LKM 1: Analisis Paradoks Konservasi", "Fase 1 & 2", "Draf Tersimpan"),
        DummyTaskPreview("st_2", "Latihan Pemahaman Modul 1 (5 Soal)", "Mandiri", "Belum Dikerjakan"),
        DummyTaskPreview("st_3", "Kampanye Digital Konservasi (CER)", "Fase 5", "Terjadwal")
    )
}

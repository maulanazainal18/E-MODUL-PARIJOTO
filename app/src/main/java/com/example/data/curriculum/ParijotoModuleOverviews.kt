package com.example.data.curriculum

data class ModuleConceptNode(
    val title: String,
    val points: List<String>
)

data class ModuleCaseStudy(
    val title: String,
    val scenario: String,
    val criticalNotes: List<String>,
    val illustrationType: String
)

data class ModuleOverviewData(
    val moduleId: String,
    val authorAttribution: String,
    val phaseAndGrade: String,
    val learningModel: String,
    val allocationJp: String,
    val objectives: List<String>,
    val conceptMap: List<ModuleConceptNode>,
    val sparkQuestions: List<String>,
    val caseStudy: ModuleCaseStudy,
    val summaryPoints: List<String>,
    val quizId: String
)

object ParijotoModuleOverviews {

    val modul1Overview = ModuleOverviewData(
        moduleId = "modul_1_etnosains",
        authorAttribution = "Ahzami, S.Pd., Gr. (2026) — Magister Pendidikan Biologi UNY",
        phaseAndGrade = "SMA/MA Kelas X • Fase E",
        learningModel = "Problem Based Learning (PBL) & Etnosains Kontekstual",
        allocationJp = "2 JP (2 × 45 Menit) • Pertemuan 1",
        objectives = listOf(
            "Menjelaskan konsep keanekaragaman hayati tingkat gen, spesies, dan ekosistem melalui flora lokal Lereng Gunung Muria.",
            "Mengidentifikasi kedudukan taksonomi, ciri morfologi licin (glabrous), serta karakter epifit Medinilla speciosa dan perbandingannya dengan Senggani.",
            "Menganalisis makna budaya tradisi mitoni dan mapati sebagai wujud kearifan etnosains masyarakat Desa Colo, Muria.",
            "Membedakan bukti praklinis (hewan uji), kajian komputasi (in-silico), dan bukti klinis manusia secara kritis pada metabolit sekunder Parijoto."
        ),
        conceptMap = listOf(
            ModuleConceptNode(
                title = "1. Tiga Tingkat Biodiversitas",
                points = listOf(
                    "Gen: variasi kepadatan dompolan buah Parijoto Lokal vs Thailand",
                    "Spesies: Parijoto (Medinilla speciosa) vs Senggani (Melastoma malabathricum)",
                    "Ekosistem: Hutan primer Muria lembap (penyangga 11 DAS) vs dataran rendah"
                )
            ),
            ModuleConceptNode(
                title = "2. Biologi & Habitat Epifit",
                points = listOf(
                    "Semak evergreen famili Melastomataceae, bioindikator hutan sehat",
                    "Hidup menumpang pada pohon inang (phorophyte) tanpa bersifat parasit",
                    "Buah dompolan merah muda berubah ungu gelap saat matang"
                )
            ),
            ModuleConceptNode(
                title = "3. Etnosains Tradisi Muria",
                points = listOf(
                    "Tradisi mitoni (7 bulan) & mapati (4 bulan) di Desa Colo, Kudus",
                    "Pemanfaatan tradisional rujak & rebusan daun/buah untuk daya tahan & sariawan",
                    "Pendekatan etnosains merekonstruksi pengetahuan lokal menjadi sains"
                )
            ),
            ModuleConceptNode(
                title = "4. Validasi Ilmiah Senyawa Bioaktif",
                points = listOf(
                    "β-Sitosterol & stigmasterol: stimulasi LH/FSH pada uji praklinis hewan",
                    "Flavonoid (fisetin, luteolin, kaempferol): antioksidan penekan ROS & simulasi in-silico PP1",
                    "Saponin & tanin: aktivitas antibakteri/antifungi awal, bukan pengganti obat klinis"
                )
            )
        ),
        sparkQuestions = listOf(
            "Mengapa tradisi mengonsumsi rujak Parijoto saat mitoni tetap harus dikaji berdasarkan tingkatan bukti ilmiah?",
            "Apa perbedaan mendasar antara hasil uji praklinis hewan uji β-sitosterol dengan uji klinis pada ibu hamil manusia?"
        ),
        caseStudy = ModuleCaseStudy(
            title = "Studi Kasus 1: Mengurai Klaim Tradisi Mitoni & Etika Literasi Sains",
            scenario = "Di kawasan wisata religi Sunan Muria (Desa Colo), rujak Parijoto kerap diburu karena tradisi mitoni dan informasi media sosial yang menyebutnya 'obat penyubur dan penjamin kesehatan kehamilan'. Padahal, riset Wijayanti (2023) baru menguji efek β-sitosterol terhadap hormon LH/FSH pada hewan uji (praklinis in-vivo), sedangkan Frediansyah et al. (2026) melakukan simulasi komputasi (in-silico) ikatan flavonoid terhadap enzim PP1.",
            criticalNotes = listOf(
                "Bukti praklinis (hewan uji) dan komputasi (in-silico) belum setara dengan uji klinis pada manusia.",
                "Buah Parijoto diposisikan sebagai pangan lokal bernutrisi antioksidan & fitosterol, bukan obat penyembuh penyakit atau penjamin kehamilan.",
                "Etnosains mengajak kita menghargai kearifan budaya sambil bersikap jujur secara ilmiah."
            ),
            illustrationType = "EVIDENCE_PYRAMID"
        ),
        summaryPoints = listOf(
            "Keanekaragaman hayati mencakup variasi gen, spesies, dan ekosistem yang teramati nyata pada populasi Medinilla speciosa di Gunung Muria.",
            "Parijoto merupakan tumbuhan epifit evergreen berdaun licin (glabrous) yang bergantung pada pohon inang dan kelembapan hutan primer.",
            "Etnosains menjembatani tradisi mitoni/mapati dan ramuan lokal dengan pengujian fitokimia modern.",
            "Klaim kesehatan wajib membedakan bukti praklinis, komputasi, dan klinis manusia agar tidak menyesatkan masyarakat."
        ),
        quizId = "quiz_modul_1"
    )

    val modul2Overview = ModuleOverviewData(
        moduleId = "modul_2_konservasi",
        authorAttribution = "Ahzami, S.Pd., Gr. (2026) — Magister Pendidikan Biologi UNY",
        phaseAndGrade = "SMA/MA Kelas X • Fase E",
        learningModel = "Problem Based Learning (PBL) & Etnosains Kontekstual",
        allocationJp = "2 JP (2 × 45 Menit) • Pertemuan 2",
        objectives = listOf(
            "Menganalisis fenomena Paradoks Konservasi akibat lonjakan bioprospeksi dan pemanenan serampangan (over-harvesting).",
            "Menjelaskan dampak deforestasi terhadap mikroklimat, kerusakan Fotosistem II (fotoinhibisi/klorosis), hormon ABA, dan plasmolisis sel epifit.",
            "Merancang sinergi strategi konservasi in-situ (aturan panen sisa 30% dan pemencaran ornitokori) serta ex-situ berbasis kultur jaringan (totipotensi).",
            "Menyusun argumen pelestarian berbasis data Indikasi Geografis (81,60 mg QE/g) menggunakan kerangka Claim–Evidence–Reasoning (CER)."
        ),
        conceptMap = listOf(
            ModuleConceptNode(
                title = "1. Bioprospeksi & Paradoks Konservasi",
                points = listOf(
                    "Bioekonomi UMKM Muria: sirup, teh daun, permen jeli, kombucha, sediaan fitofarmaka",
                    "Paradoks Konservasi: nilai ekonomi tinggi memicu eksploitasi populasi liar",
                    "Over-harvesting mematahkan meristem apikal serta pembuluh xilem-floem"
                )
            ),
            ModuleConceptNode(
                title = "2. Ekofisiologi Cekaman Deforestasi",
                points = listOf(
                    "Tren Muria 2019–2023: kanopi 479 ha -> 240 ha; epifit 42 -> 11 rumpun/ha",
                    "Hilangnya naungan memicu fotoinhibisi (ROS merusak PSII & membran tilakoid -> klorosis)",
                    "Defisit air memicu hormon ABA penutup stomata -> turgor turun -> plasmolisis & desikasi"
                )
            ),
            ModuleConceptNode(
                title = "3. Konservasi In-Situ & Ex-Situ",
                points = listOf(
                    "In-situ: KTH/Perdes panen lestari sisakan 30% buah matang untuk burung (ornitokori)",
                    "Ex-situ: budidaya di luar hutan & Kultur Jaringan berbasis sifat totipotensi sel",
                    "Tahapan kultur: eksplan unggul -> sterilisasi & inokulasi -> kalus/planlet -> aklimatisasi"
                )
            ),
            ModuleConceptNode(
                title = "4. Indikasi Geografis (IG) & Kampanye CER",
                points = listOf(
                    "Flavonoid Muria Kudus 81,60 mg QE/g vs Kab. Semarang 76,48 mg QE/g (Vifta et al., 2021)",
                    "Perlindungan HAKI daerah asal berbasis cekaman abiotik khas elevasi Muria",
                    "Komunikasi sains publik menggunakan struktur Claim–Evidence–Reasoning (CER)"
                )
            )
        ),
        sparkQuestions = listOf(
            "Mengapa budidaya Parijoto di lahan terbuka dataran rendah (100 mdpl) memicu daun menguning dan mengering?",
            "Bagaimana kombinasi aturan panen sisa 30%, kultur jaringan, dan Indikasi Geografis menyelesaikan Paradoks Konservasi?"
        ),
        caseStudy = ModuleCaseStudy(
            title = "Studi Kasus 2: Paradoks Konservasi & Kegagalan Budidaya Dataran Rendah",
            scenario = "Pak Budi, pelaku UMKM sirup Parijoto di Muria, memindahkan 50 bibit ke lahan terbuka dataran rendah (100 mdpl) demi pasokan cepat. Setelah 3 minggu di suhu siang +6,9°C lebih panas tanpa naungan, daun menguning kemerahan (klorosis), keriting layu, lalu mati. Sementara data ekologi hutan Muria (2019–2023) mencatat tutupan kanopi menyusut dari 479 ha menjadi 240 ha dan kepadatan epifit merosot dari 42 menjadi 11 rumpun/ha.",
            criticalNotes = listOf(
                "Gejala pada bibit Pak Budi disebabkan oleh fotoinhibisi (ROS merusak PSII & kloroplas) dan plasmolisis akibat hilangnya mikroklimat lembap.",
                "Solusi pasokan UMKM adalah kultur jaringan ex-situ (totipotensi) + paranet peniru mikroklimat lereng Muria, bukan memburu bibit hutan atau memaksa lahan terik.",
                "Kadar flavonoid Muria (81,60 mg QE/g) mendasari pentingnya pelestarian habitat asli sekaligus sertifikasi Indikasi Geografis (IG)."
            ),
            illustrationType = "DEFORESTATION_TREND"
        ),
        summaryPoints = listOf(
            "Popularitas bioekonomi Parijoto tanpa regulasi memicu Paradoks Konservasi dan kerusakan meristem apikal akibat over-harvesting.",
            "Deforestasi merusak mikroklimat tajuk pohon inang sehingga sel epifit mengalami fotoinhibisi, klorosis, penurunan turgor, plasmolisis, hingga desikasi.",
            "Konservasi terpadu memadukan perlindungan in-situ (sisa 30% buah untuk ornitokori) dan perbanyakan ex-situ melalui kultur jaringan berbasis totipotensi.",
            "Keunggulan metabolit flavonoid Lereng Muria (81,60 mg QE/g) menjadi dasar perlindungan Indikasi Geografis dan kampanye pelestarian CER."
        ),
        quizId = "quiz_modul_2"
    )

    fun forModule(moduleId: String): ModuleOverviewData =
        if (moduleId == ParijotoCurriculumPart1.modul2.id) modul2Overview else modul1Overview
}

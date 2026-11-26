package com.example.data.curriculum

import com.example.data.model.Assignment
import com.example.data.model.Question
import com.example.data.model.Quiz
import com.example.data.model.Rubric
import com.example.data.model.Subject

data class GlossaryEntry(
    val term: String,
    val definition: String
)

data class ConceptBranch(
    val title: String,
    val items: List<String>
)

object ParijotoAssessmentsAndGlossary {

    val defaultSubject = Subject(
        id = "subj_bio_x",
        title = "Biologi: Keanekaragaman Hayati (Etnosains Parijoto)",
        phase = "E",
        grade = "SMA/MA Kelas X",
        academicYear = "2026/2027",
        description = "Sumber utama: E-Modul Keanekaragaman Hayati: Parijoto Eksplorasi Etnosains, karya Ahzami, S.Pd., Gr. (2026), Magister Pendidikan Biologi UNY. Model Problem Based Learning (4 JP / 2 Pertemuan).",
        ownerUid = "SYSTEM_DEFAULT",
        published = true
    )

    val conceptMapBranches: List<ConceptBranch> = listOf(
        ConceptBranch(
            title = "01 Biologi & Ekologi",
            items = listOf(
                "Tingkat Gen (Variasi Kepadatan Dompolan Buah & Varietas)",
                "Tingkat Spesies (Parijoto vs Senggani dalam Melastomataceae)",
                "Tingkat Ekosistem (Mikroklimat Hutan Primer Gunung Muria)"
            )
        ),
        ConceptBranch(
            title = "02 Etnosains & Pemanfaatan",
            items = listOf(
                "Tradisi Mitoni & Mapati Masyarakat Desa Colo Muria",
                "Klaim Kesuburan & Ramuan Sariawan/Diare Tradisional",
                "Validasi Ilmiah: β-sitosterol (Praklinis), Flavonoid (Antioksidan/Komputasi), Saponin & Tanin"
            )
        ),
        ConceptBranch(
            title = "03 Ancaman & Konservasi",
            items = listOf(
                "Bioprospeksi, Paradoks Konservasi & Eksploitasi Liar",
                "Deforestasi, Fotoinhibisi Kloroplas & Plasmolisis/Desikasi",
                "Konservasi In-Situ (Panen 30% & Ornitokori), Ex-Situ (Kultur Jaringan/Totipotensi) & Indikasi Geografis"
            )
        )
    )

    val glossary: List<GlossaryEntry> = listOf(
        GlossaryEntry("Bioaktif (Senyawa Bioaktif)", "Senyawa kimia alami tumbuhan yang memiliki efek fisiologis atau biologis terhadap organisme lain (contoh: β-sitosterol, flavonoid, saponin, dan tanin pada buah Parijoto)."),
        GlossaryEntry("Bioprospeksi", "Eksplorasi, ekstraksi, dan komersialisasi sumber daya hayati menjadi produk turunan bernilai ekonomi tinggi."),
        GlossaryEntry("Cekaman Abiotik", "Kondisi lingkungan fisik atau kimiawi yang tidak optimal (seperti suhu ekstrem, radiasi UV, atau kekeringan) yang memengaruhi metabolisme tumbuhan."),
        GlossaryEntry("Deforestasi", "Penebangan hutan atau hilangnya tutupan lahan hutan secara masif yang mengubah fungsi awal ekosistem."),
        GlossaryEntry("Desikasi", "Kondisi kekeringan tingkat lanjut pada jaringan sel tumbuhan akibat hilangnya cairan secara ekstrem yang berujung pada kematian seluler."),
        GlossaryEntry("Eksploitasi (Over-harvesting)", "Pemanfaatan sumber daya hayati secara berlebihan demi keuntungan ekonomi tanpa mempertimbangkan kelestariannya di alam."),
        GlossaryEntry("Endemik", "Spesies organisme yang keberadaannya terbatas pada suatu wilayah geografis tertentu dan tidak ditemukan secara alami di tempat lain."),
        GlossaryEntry("Epifit", "Tumbuhan yang hidup menumpang pada tumbuhan lain (pohon inang) sebagai tempat hidup untuk mendapatkan cahaya, namun tidak bersifat parasit."),
        GlossaryEntry("Etnosains", "Sistem pengetahuan dan kognisi masyarakat lokal terkait alam sekitarnya (mitos/tradisi) yang dapat diuji dan direkonstruksi kebenarannya secara kaidah ilmiah modern."),
        GlossaryEntry("Fotoinhibisi", "Kerusakan fungsi kloroplas (khususnya PSII) akibat paparan intensitas cahaya dan suhu berlebihan sehingga menghambat fotosintesis pada daun."),
        GlossaryEntry("Indikasi Geografis (IG)", "Tanda perlindungan hukum (HAKI) yang menunjukkan daerah asal suatu produk yang karena faktor lingkungan geografisnya memberikan reputasi, kualitas, dan karakteristik tertentu."),
        GlossaryEntry("Konservasi Ex-Situ", "Upaya pelestarian keanekaragaman hayati yang dilakukan di luar habitat aslinya (contoh: kultur jaringan Parijoto di laboratorium dan greenhouse)."),
        GlossaryEntry("Konservasi In-Situ", "Upaya pelestarian keanekaragaman hayati langsung di dalam habitat aslinya (contoh: menjaga kelestarian Parijoto dan pohon inangnya di Hutan Gunung Muria)."),
        GlossaryEntry("Mikroklimat", "Kondisi iklim dalam skala ruang sangat kecil dan spesifik, seperti tingkat kelembapan dan intensitas cahaya di bawah naungan pohon hutan."),
        GlossaryEntry("Ornitokori", "Proses pemencaran atau penyebaran biji tumbuhan ke area yang lebih luas dengan bantuan agen perantara berupa burung."),
        GlossaryEntry("Paradoks Konservasi", "Fenomena ketika suatu spesies flora terbukti memiliki nilai ekonomi/biologis sangat tinggi, namun hal tersebut justru membuatnya sangat rentan terhadap kepunahan akibat perburuan massal."),
        GlossaryEntry("Plasmolisis", "Peristiwa terlepasnya membran plasma dari dinding sel tumbuhan akibat sel kehilangan banyak air secara ekstrem (dehidrasi)."),
        GlossaryEntry("Totipotensi", "Kemampuan setiap sel hidup tumbuhan untuk membelah diri dan tumbuh berdiferensiasi menjadi satu individu tumbuhan baru yang utuh.")
    )

    val modul1SelfChecklist = listOf(
        "Saya mampu menjelaskan definisi keanekaragaman hayati tingkat gen, spesies, dan ekosistem.",
        "Saya dapat mendeskripsikan karakteristik morfologi dan habitat epifit tanaman Parijoto (Medinilla speciosa).",
        "Saya memahami apa yang dimaksud dengan Etnosains dan tradisi mitoni/mapati pada masyarakat Muria.",
        "Saya mampu mengevaluasi klaim tradisional Parijoto dengan membedakan bukti praklinis, komputasi, dan klinis."
    )

    val modul2SelfChecklist = listOf(
        "Saya mampu menjelaskan bagaimana bioprospeksi masif dapat memicu Paradoks Konservasi pada populasi Parijoto liar.",
        "Saya dapat menganalisis dampak hilangnya pohon inang (deforestasi) terhadap peristiwa fotoinhibisi dan plasmolisis sel Parijoto.",
        "Saya mampu mengidentifikasi mekanisme pemencaran biji oleh burung (ornitokori) sebagai bentuk konservasi in-situ.",
        "Saya mampu merumuskan solusi budidaya ex-situ (kultur jaringan berbasis totipotensi) untuk menyuplai UMKM tanpa merusak hutan.",
        "Saya memahami pentingnya perlindungan Indikasi Geografis (IG) dan kampanye digital CER dalam menjaga keaslian produk serta ekologi Gunung Muria."
    )

    val quizModul1 = Quiz(
        id = "quiz_modul_1",
        classId = "DEFAULT",
        moduleId = ParijotoCurriculumPart1.modul1.id,
        ownerUid = "SYSTEM_DEFAULT",
        title = "Kuis Modul 1: Etnosains Parijoto",
        category = "LATIHAN_MODUL_1",
        durationMinutes = 20,
        questionCount = 5,
        maxAttempts = 3,
        shuffleQuestions = true,
        shuffleOptions = true,
        openForStudents = true,
        scheduleLabel = "Pertemuan 1 • Terbuka",
        resultsPublished = false,
        closed = false,
        showDiscussion = false,
        published = true
    )

    val quizModul2 = Quiz(
        id = "quiz_modul_2",
        classId = "DEFAULT",
        moduleId = ParijotoCurriculumPart1.modul2.id,
        ownerUid = "SYSTEM_DEFAULT",
        title = "Kuis Modul 2: Pemanfaatan & Konservasi Parijoto",
        category = "LATIHAN_MODUL_2",
        durationMinutes = 20,
        questionCount = 5,
        maxAttempts = 3,
        shuffleQuestions = true,
        shuffleOptions = true,
        openForStudents = true,
        scheduleLabel = "Pertemuan 2 • Terbuka",
        resultsPublished = false,
        closed = false,
        showDiscussion = false,
        published = true
    )

    val quizPostTest = Quiz(
        id = "quiz_post_test",
        classId = "DEFAULT",
        moduleId = ParijotoCurriculumPart1.modul2.id,
        ownerUid = "SYSTEM_DEFAULT",
        title = "Evaluasi Akhir (Post-Test HOTS & Uraian)",
        category = "POST_TEST",
        durationMinutes = 45,
        questionCount = 8,
        maxAttempts = 2,
        shuffleQuestions = true,
        shuffleOptions = true,
        openForStudents = true,
        scheduleLabel = "Akhir Pertemuan 2 • Terbuka",
        resultsPublished = false,
        closed = false,
        showDiscussion = false,
        published = true
    )

    val modul1Questions: List<Question> = listOf(
        Question(
            id = "q_m1_1",
            quizId = quizModul1.id,
            orderIndex = 1,
            type = "MULTIPLE_CHOICE",
            prompt = "Masyarakat lereng Gunung Muria sering menemukan tanaman Parijoto tumbuh menempel pada batang pohon-pohon besar di area hutan yang rimbun dan lembap. Meskipun menempel, daun Parijoto tetap hijau subur dan pohon inang yang ditumpanginya tidak mengalami kerusakan atau kekurangan nutrisi. Berdasarkan karakteristik ekologisnya, Parijoto merepresentasikan keanekaragaman tingkat spesies dari kelompok tumbuhan...",
            optionA = "Epifit",
            optionB = "Saprofit",
            optionC = "Hemiparasit",
            optionD = "Liana darat",
            optionE = "Parasit sejati",
            hotsCategory = "Konsep Ekologi",
            maxPoints = 20,
            ownerUid = "SYSTEM_DEFAULT",
            published = true
        ),
        Question(
            id = "q_m1_2",
            quizId = quizModul1.id,
            orderIndex = 2,
            type = "MULTIPLE_CHOICE",
            prompt = "Keanekaragaman hayati dapat diamati pada berbagai tingkatan. Keberadaan Medinilla speciosa (Parijoto) yang hidup berdampingan dengan spesies tumbuhan paku dan lumut endemik di habitat mikroklimat hutan primer Gunung Muria, merupakan contoh dari keanekaragaman hayati tingkat...",
            optionA = "Biosfer",
            optionB = "Ekosistem",
            optionC = "Ekoregion",
            optionD = "Genetik lokal",
            optionE = "Spesies (Jenis)",
            hotsCategory = "Konsep Biodiversitas",
            maxPoints = 20,
            ownerUid = "SYSTEM_DEFAULT",
            published = true
        ),
        Question(
            id = "q_m1_3",
            quizId = quizModul1.id,
            orderIndex = 3,
            type = "MULTIPLE_CHOICE",
            prompt = "Masyarakat lokal secara turun-temurun menggunakan buah Parijoto dalam tradisi mitoni karena dipercaya mendukung kesuburan (Etnosains). Melalui uji praklinis in-vivo pada hewan uji, terbukti bahwa buah Parijoto mengandung fitosterol spesifik yang strukturnya mirip prekursor kolesterol dan mampu menstimulasi hormon reproduksi (LH dan FSH), yaitu...",
            optionA = "Tanin",
            optionB = "Saponin",
            optionC = "Flavonoid",
            optionD = "Antosianin",
            optionE = "β-sitosterol",
            hotsCategory = "Validasi Biokimia",
            maxPoints = 20,
            ownerUid = "SYSTEM_DEFAULT",
            published = true
        ),
        Question(
            id = "q_m1_4",
            quizId = quizModul1.id,
            orderIndex = 4,
            type = "MULTIPLE_CHOICE",
            prompt = "Nenek moyang masyarakat Muria sering memanfaatkan air rebusan daun dan buah Parijoto sebagai ramuan tradisional untuk meredakan luka sariawan dan peradangan kulit. Validasi biokimia menunjukkan adanya kombinasi zat aktif yang mampu merusak membran sel mikroba patogen sekaligus bersifat astringent (menciutkan selaput lendir), yaitu...",
            optionA = "Saponin dan Tanin",
            optionB = "Alkaloid dan Flavonoid",
            optionC = "Tanin dan β-sitosterol",
            optionD = "Flavonoid dan β-sitosterol",
            optionE = "Saponin dan Kalsium Oksalat",
            hotsCategory = "Etnosains",
            maxPoints = 20,
            ownerUid = "SYSTEM_DEFAULT",
            published = true
        ),
        Question(
            id = "q_m1_5",
            quizId = quizModul1.id,
            orderIndex = 5,
            type = "MULTIPLE_CHOICE",
            prompt = "Parijoto merupakan tumbuhan semak yang sangat bergantung pada mikroklimat teduh hutan primer. Jika terjadi penebangan pohon besar penyedia naungan secara masif di lereng Gunung Muria demi perluasan lahan, maka dampak paling logis yang mengancam baik dari aspek ekologi biologi maupun pelestarian etnosains masyarakat setempat adalah...",
            optionA = "Pengetahuan Etnosains masyarakat akan otomatis bergeser untuk meneliti khasiat tumbuhan pinus.",
            optionB = "Meningkatnya populasi Parijoto akibat paparan sinar matahari langsung yang mempercepat fotosintesis.",
            optionC = "Kandungan antioksidan pada Parijoto akan meningkat drastis sebagai bentuk pertahanan ekstrem terhadap cuaca panas.",
            optionD = "Parijoto akan cepat beradaptasi menjadi tumbuhan parasit murni yang mengambil nutrisi sisa dari tanah bekas tebangan.",
            optionE = "Hilangnya habitat mikroklimat memicu kematian populasi liar Parijoto, sehingga masyarakat akan kehilangan bahan baku asli untuk tradisi mitoni.",
            hotsCategory = "HOTS Analisis",
            maxPoints = 20,
            ownerUid = "SYSTEM_DEFAULT",
            published = true
        )
    )

    val modul2Questions: List<Question> = listOf(
        Question(
            id = "q_m2_1",
            quizId = quizModul2.id,
            orderIndex = 1,
            type = "MULTIPLE_CHOICE",
            prompt = "Keberhasilan riset ilmiah mengenai kandungan β-sitosterol pada Parijoto memicu menjamurnya UMKM sirup herbal di lereng Muria. Sayangnya, fenomena ini justru menyebabkan populasi liar Parijoto semakin terancam punah di habitat aslinya. Konsep ekologi yang tepat untuk menggambarkan benturan antara nilai ekonomi dan ancaman kepunahan ini adalah...",
            optionA = "Paradoks Konservasi",
            optionB = "Kompetisi Interspesifik",
            optionC = "Suksesi Primer Ekosistem",
            optionD = "Degradasi Habitat Endemik",
            optionE = "Bioprospeksi Berkelanjutan",
            hotsCategory = "Konservasi",
            maxPoints = 20,
            ownerUid = "SYSTEM_DEFAULT",
            published = true
        ),
        Question(
            id = "q_m2_2",
            quizId = quizModul2.id,
            orderIndex = 2,
            type = "MULTIPLE_CHOICE",
            prompt = "Penebangan pohon inang berkanopi besar di hutan Muria menyebabkan hilangnya naungan bagi Parijoto liar. Akibat pemaparan suhu panas ekstrem, sel-sel Parijoto kehilangan cairan vakuola secara masif hingga membran plasma terlepas dari dinding selnya. Peristiwa fisiologis yang mematikan populasi epifit ini disebut...",
            optionA = "Osmosis Hipotonik",
            optionB = "Transpirasi dan Gutasi",
            optionC = "Plasmolisis dan Desikasi",
            optionD = "Fotoinhibisi dan Klorosis",
            optionE = "Plasmolisis dan Plasmoptisis",
            hotsCategory = "Fisiologi Sel",
            maxPoints = 20,
            ownerUid = "SYSTEM_DEFAULT",
            published = true
        ),
        Question(
            id = "q_m2_3",
            quizId = quizModul2.id,
            orderIndex = 3,
            type = "MULTIPLE_CHOICE",
            prompt = "Kelompok tani Desa Colo mengusulkan strategi konservasi berbasis kearifan lokal (Community-Based Conservation). Mereka melarang pemanenan tebang habis dan mewajibkan warga menyisakan 30% buah matang di pohonnya. Secara ekologis, tujuan menyisakan buah di pohon tersebut adalah untuk...",
            optionA = "Menjaga kelembapan udara (mikroklimat) di sekitar pohon inang",
            optionB = "Mencegah terjadinya peristiwa fotoinhibisi pada daun yang tersisa",
            optionC = "Memberikan waktu bagi sel meristem apikal untuk melakukan regenerasi",
            optionD = "Meningkatkan kadar antioksidan (flavonoid) pada sisa panen berikutnya",
            optionE = "Mengundang fauna pemakan buah (ornitokori) untuk memencarkan biji alami",
            hotsCategory = "Konservasi In-Situ",
            maxPoints = 20,
            ownerUid = "SYSTEM_DEFAULT",
            published = true
        ),
        Question(
            id = "q_m2_4",
            quizId = quizModul2.id,
            orderIndex = 4,
            type = "MULTIPLE_CHOICE",
            prompt = "Tingginya permintaan bahan baku industri Parijoto tidak dapat dipenuhi hanya dengan mengandalkan populasi liar di hutan. Solusi pelestarian ex-situ paling mutakhir yang dapat menghasilkan ribuan bibit unggul secara cepat tanpa menebang tanaman di hutan adalah dengan menerapkan bioteknologi...",
            optionA = "Kloning transfer inti sel somatik",
            optionB = "Rekayasa genetika menggunakan plasmid bakteri",
            optionC = "Hidroponik sistem Nutrient Film Technique (NFT)",
            optionD = "Pemuliaan mutasi menggunakan radiasi sinar gamma",
            optionE = "Kultur jaringan (Tissue Culture) memanfaatkan sifat totipotensi sel",
            hotsCategory = "Bioteknologi",
            maxPoints = 20,
            ownerUid = "SYSTEM_DEFAULT",
            published = true
        ),
        Question(
            id = "q_m2_5",
            quizId = quizModul2.id,
            orderIndex = 5,
            type = "MULTIPLE_CHOICE",
            prompt = "Perhatikan data perbandingan kadar flavonoid total ekstrak etanol buah Parijoto (Vifta dkk., 2021):\n• Kabupaten Kudus (Lereng Gunung Muria): 81,60 mg QE/g\n• Kabupaten Semarang (Kawasan Pegunungan Lain): 76,48 mg QE/g\nBerdasarkan data tersebut, simpulan ilmiah yang paling tepat untuk mendasari pengajuan Indikasi Geografis (IG) Parijoto Muria adalah...",
            optionA = "Parijoto sama sekali tidak dapat tumbuh dan dibudidayakan di luar kawasan hutan lindung Muria.",
            optionB = "Indikasi Geografis akan secara otomatis mematikan UMKM dan menghentikan laju deforestasi hutan.",
            optionC = "Semua jenis tanaman epifit yang ditanam di pegunungan akan selalu menghasilkan kadar flavonoid yang identik.",
            optionD = "Cekaman abiotik (iklim mikro/elevasi) spesifik lereng Muria menghasilkan kadar metabolit sekunder yang paling optimal.",
            optionE = "Masyarakat lokal Muria adalah satu-satunya kelompok manusia yang menguasai teknik panen tanpa merusak sel meristem.",
            hotsCategory = "HOTS Data & IG",
            maxPoints = 20,
            ownerUid = "SYSTEM_DEFAULT",
            published = true
        )
    )

    val postTestQuestions: List<Question> = listOf(
        Question(
            id = "q_pt_1",
            quizId = quizPostTest.id,
            orderIndex = 1,
            type = "MULTIPLE_CHOICE",
            stimulusText = "Wacana 1 (untuk soal nomor 1 & 2):\nKeluarga Pak Budi yang tinggal di lereng Gunung Muria memiliki usaha turun-temurun membuat jamu herbal berbahan dasar buah Parijoto liar. Belakangan ini, Pak Budi menyadari dompolan buah Parijoto di hutan semakin sulit dicari dan daun-daunnya menguning kemerahan (klorosis) akibat pepohonan besar di sekitarnya ditebang untuk perluasan kebun kopi. Di sisi lain, sebuah jurnal penelitian biokimia memvalidasi secara praklinis bahwa senyawa β-sitosterol dalam Parijoto mampu menstimulasi hormon LH dan FSH pada hewan uji.",
            prompt = "Berdasarkan wacana di atas, kesimpulan manakah yang paling akurat dalam memisahkan antara akar permasalahan ekologis dengan fakta khasiat biokimia pada fenomena tersebut?",
            optionA = "Klorosis daun secara langsung memicu penurunan kualitas jamu.",
            optionB = "Tradisi jamu Pak Budi terbukti menjadi penyebab utama deforestasi hutan.",
            optionC = "Validasi khasiat praklinis β-sitosterol memicu eksploitasi berlebihan (paradoks konservasi) di tengah ancaman deforestasi.",
            optionD = "Pohon kopi dapat menggantikan fungsi ekologis pohon inang karena kemampuannya menghasilkan naungan untuk menekan transpirasi.",
            optionE = "Masyarakat Muria tidak perlu lagi memproduksi jamu herbal karena senyawa penstimulasi LH dan FSH sudah bisa disintesis massal.",
            hotsCategory = "HOTS Kasus",
            maxPoints = 15,
            ownerUid = "SYSTEM_DEFAULT",
            published = true
        ),
        Question(
            id = "q_pt_2",
            quizId = quizPostTest.id,
            orderIndex = 2,
            type = "MULTIPLE_CHOICE",
            stimulusText = "Merujuk pada Wacana 1 (usaha keluarga Pak Budi di lereng Gunung Muria).",
            prompt = "Sebagai murid yang memiliki literasi konservasi, solusi pelestarian in-situ paling rasional dan berbasis ekologis yang bisa Anda sarankan kepada Pak Budi adalah...",
            optionA = "Meminta pemerintah menetapkan status Indikasi Geografis (IG).",
            optionB = "Menerapkan panen parsial dengan menyisakan 30% buah di pohon agar dipencarkan oleh burung endemik (ornitokori).",
            optionC = "Mencabut anakan Parijoto liar beserta akarnya dari hutan secara hati-hati untuk ditanam di dalam pot rumah.",
            optionD = "Membangun fasilitas laboratorium kultur jaringan tingkat lanjut di rumahnya untuk memproduksi ribuan bibit hibrida.",
            optionE = "Menyemprotkan cairan hormon pertumbuhan sintetik ke daun Parijoto liar agar terhindar dari proses plasmolisis.",
            hotsCategory = "HOTS Solusi In-Situ",
            maxPoints = 15,
            ownerUid = "SYSTEM_DEFAULT",
            published = true
        ),
        Question(
            id = "q_pt_3",
            quizId = quizPostTest.id,
            orderIndex = 3,
            type = "MULTIPLE_CHOICE",
            stimulusText = "Wacana 2 (untuk soal nomor 3 & 4):\nMasyarakat di sebuah desa di lereng Gunung Muria rutin mengadakan tradisi mitoni (syukuran tujuh bulanan kehamilan) dengan menyajikan rujak buah Parijoto. Secara turun-temurun, tetua desa meyakini buah tersebut baik bagi ibu dan janin. Mahasiswa biologi kemudian melakukan uji fitokimia laboratorium (diadaptasi dari Sa'adah dkk., 2017):\n• Ekstrak Buah Parijoto Liar Muria: β-sitosterol, Flavonoid\n• Rujak Buah Umum (Non-Parijoto): Vitamin C, Fruktosa",
            prompt = "Berdasarkan prinsip Etnosains dan interpretasi data laboratorium tersebut, kesimpulan saintifik mana yang paling tepat untuk menjelaskan tradisi mitoni secara ilmiah (tanpa mengklaim sebagai obat mutlak)?",
            optionA = "Tradisi mitoni tidak relevan dengan keilmuan biokimia.",
            optionB = "Keyakinan warga sepenuhnya keliru karena kesehatan janin hanya dipengaruhi protein.",
            optionC = "Uji fitokimia membuktikan tradisi mitoni salah sasaran karena Parijoto hanya untuk obat luar.",
            optionD = "Terdapat benang merah saintifik: β-sitosterol teruji praklinis menstimulasi hormon reproduksi dan flavonoid berperan sebagai antioksidan pelindung sel.",
            optionE = "Semua buah liar pegunungan pada dasarnya mengandung senyawa toksik yang berbahaya bagi janin.",
            hotsCategory = "HOTS Etnosains",
            maxPoints = 15,
            ownerUid = "SYSTEM_DEFAULT",
            published = true
        ),
        Question(
            id = "q_pt_4",
            quizId = quizPostTest.id,
            orderIndex = 4,
            type = "MULTIPLE_CHOICE",
            stimulusText = "Merujuk pada Wacana 2 tentang upaya melestarikan tradisi mitoni tanpa mengambil Parijoto terus-menerus dari hutan primer.",
            prompt = "Untuk melestarikan ketersediaan buah tradisi mitoni tanpa mengambil Parijoto liar di hutan primer, warga disarankan melakukan budidaya ex-situ menggunakan teknik kultur jaringan. Syarat mutlak secara fisiologis yang memungkinkan teknik pelestarian ini berhasil adalah...",
            optionA = "Sel tumbuhan memiliki sifat totipotensi untuk membelah dan berdiferensiasi menjadi individu utuh.",
            optionB = "Biji Parijoto memiliki masa dormansi sangat singkat sehingga mudah berkecambah.",
            optionC = "Akar Parijoto mampu menyerap nutrisi zat pengatur tumbuh melalui sistem osmosis.",
            optionD = "Kemampuan jaringan meristem Parijoto untuk melakukan plasmolisis di lingkungan hipotonik.",
            optionE = "Sel tumbuhan secara alami memiliki kloroplas yang mampu berfotosintesis mandiri tanpa nutrisi media.",
            hotsCategory = "Bioteknologi Sel",
            maxPoints = 15,
            ownerUid = "SYSTEM_DEFAULT",
            published = true
        ),
        Question(
            id = "q_pt_5",
            quizId = quizPostTest.id,
            orderIndex = 5,
            type = "MULTIPLE_CHOICE",
            stimulusText = "Wacana 3:\nSeorang pengusaha dari daerah dataran rendah pesisir membeli ribuan bibit Parijoto hasil kultur jaringan dari Gunung Muria untuk ditanam di perkebunannya yang bersuhu panas (100 mdpl). Setelah dipanen, ia mengklaim produk sirupnya berkualitas identik dengan buatan asli Muria (1.100 mdpl). Padahal data uji laboratorium (Vifta dkk., 2021) mencatat: Kadar flavonoid Parijoto Muria = 81,60 mg QE/g, sedangkan kawasan lain lebih rendah (76,48 mg QE/g).",
            prompt = "Mengapa perbedaan kadar metabolit sekunder tersebut dapat terjadi, dan langkah ekologis serta legal apa yang tepat untuk melindungi keaslian komoditas Parijoto Muria?",
            optionA = "Terjadi kerusakan kloroplas akibat fotoinhibisi; solusinya daftar hak merek.",
            optionB = "Buah Parijoto di dataran rendah dimakan burung; solusinya memberi label halal.",
            optionC = "Pengusaha menyiram tanaman dengan air garam; solusinya mematenkan teknik tersebut.",
            optionD = "Parijoto pesisir mengalami mutasi genetik; solusinya melarang penyebaran bibit ke luar.",
            optionE = "Parijoto Muria memproduksi flavonoid tinggi sebagai respons cekaman abiotik spesifik lereng gunung; solusinya adalah pendaftaran Indikasi Geografis (IG).",
            hotsCategory = "HOTS Bioekonomi & IG",
            maxPoints = 20,
            ownerUid = "SYSTEM_DEFAULT",
            published = true
        ),
        Question(
            id = "q_pt_6",
            quizId = quizPostTest.id,
            orderIndex = 6,
            type = "MULTIPLE_CHOICE",
            stimulusText = "Wacana 4 (Data Pemantauan Ekologi 2019–2023):\n• 2019: Tutupan Kanopi Pohon Inang 479 ha | Kelembapan Udara Mikro 84% | Suhu Siang 21,5°C | Kepadatan Epifit 42 rumpun/ha\n• 2021: Tutupan Kanopi Pohon Inang 355 ha | Kelembapan Udara Mikro 69% | Suhu Siang 24,8°C | Kepadatan Epifit 27 rumpun/ha\n• 2023: Tutupan Kanopi Pohon Inang 240 ha | Kelembapan Udara Mikro 52% | Suhu Siang 28,4°C | Kepadatan Epifit 11 rumpun/ha",
            prompt = "Berdasarkan interpretasi tren data pemantauan tersebut, kesimpulan kausal manakah yang paling tepat menjelaskan penurunan drastis kepadatan tumbuhan epifit pada periode 2019–2023?",
            optionA = "Kenaikan suhu udara sebesar 6,9 °C mempercepat respirasi pohon inang sehingga pohon inang berhenti menyalurkan air ke akar epifit.",
            optionB = "Penyusutan kanopi pohon inang memicu paparan cahaya berlebih dan turunnya kelembapan mikro sebesar 32%, sehingga epifit mengalami fotoinhibisi dan plasmolisis/desikasi.",
            optionC = "Penurunan tutupan kanopi meningkatkan kecepatan angin sehingga burung pemakan buah berkembang biak terlalu pesat.",
            optionD = "Kepadatan epifit menurun karena kelembapan 52% terlalu tinggi bagi pertumbuhan daun Parijoto yang toleran kekeringan.",
            optionE = "Kenaikan suhu siang menyebabkan Parijoto memproduksi β-sitosterol berlebih sehingga buah gagal berkecambah.",
            hotsCategory = "HOTS Interpretasi Data",
            maxPoints = 20,
            ownerUid = "SYSTEM_DEFAULT",
            published = true
        ),
        Question(
            id = "q_pt_7",
            quizId = quizPostTest.id,
            orderIndex = 7,
            type = "ESSAY",
            stimulusText = "Studi Kasus Bioekonomi vs Ekologi:\nSebuah perusahaan farmasi besar berencana membeli berton-ton buah Parijoto segar setiap bulannya dari warga Desa Colo untuk diekstrak senyawa flavonoid dan saponinnya. Kepala Desa menolak mengambil bahan baku dari hutan karena khawatir terjadi over-harvesting.",
            prompt = "Berdasarkan pemahaman Anda tentang totipotensi sel dan kultur jaringan, rancanglah sebuah solusi jalan tengah (kompromi) berbasis bioteknologi agar perusahaan farmasi tetap mendapatkan suplai bahan baku tanpa merusak populasi Parijoto liar di Gunung Muria! Jelaskan tahapan logis dari solusi Anda!",
            hotsCategory = "Uraian HOTS Analisis & Sintesis",
            maxPoints = 50,
            ownerUid = "SYSTEM_DEFAULT",
            published = true
        ),
        Question(
            id = "q_pt_8",
            quizId = quizPostTest.id,
            orderIndex = 8,
            type = "ESSAY",
            stimulusText = "Studi Kasus Ekofisiologi & Kampanye Digital:\nPemerintah daerah berencana membuka kawasan wisata alam baru dengan menebang sebagian pepohonan besar di zona penyangga Gunung Muria. Peneliti botani memperingatkan bahwa hilangnya kanopi pohon pelindung akan memicu kematian massal Parijoto liar dan menghancurkan bahan baku UMKM sirup herbal desa setempat.",
            prompt = "Analisislah dua peristiwa fisiologis seluler (berkaitan dengan cahaya berlebih dan suhu/defisit air) yang menyebabkan kematian massal Parijoto liar jika pepohonan tersebut ditebang! Kemudian, susunlah rancangan narasi singkat (caption kampanye digital persuasif berbasis CER) untuk meyakinkan pemerintah agar membatalkan penebangan tersebut!",
            hotsCategory = "Uraian HOTS Ekofisiologi & CER",
            maxPoints = 50,
            ownerUid = "SYSTEM_DEFAULT",
            published = true
        )
    )

    val defaultAnswerKeys: Map<String, Map<String, String>> = mapOf(
        quizModul1.id to mapOf(
            "q_m1_1" to "A",
            "q_m1_2" to "E",
            "q_m1_3" to "E",
            "q_m1_4" to "A",
            "q_m1_5" to "E"
        ),
        quizModul2.id to mapOf(
            "q_m2_1" to "A",
            "q_m2_2" to "C",
            "q_m2_3" to "E",
            "q_m2_4" to "E",
            "q_m2_5" to "D"
        ),
        quizPostTest.id to mapOf(
            "q_pt_1" to "C",
            "q_pt_2" to "B",
            "q_pt_3" to "D",
            "q_pt_4" to "A",
            "q_pt_5" to "E",
            "q_pt_6" to "B"
        )
    )

    val defaultExplanations: Map<String, Map<String, String>> = mapOf(
        quizModul1.id to mapOf(
            "q_m1_1" to "Parijoto menumpang pada pohon inang hanya sebagai tempat tumbuh tanpa mengambil nutrisi inang, sehingga termasuk tumbuhan epifit.",
            "q_m1_2" to "Keberadaan berbagai spesies berbeda (Parijoto, paku, lumut) dalam satu komunitas merupakan keanekaragaman hayati tingkat spesies (jenis).",
            "q_m1_3" to "Pada uji praklinis hewan uji (Wijayanti, 2023), β-sitosterol menstimulasi hormon LH dan FSH; tetap perlu diingat ini bukti praklinis, bukan obat klinis mutlak pada manusia.",
            "q_m1_4" to "Kombinasi saponin (merusak membran sel mikroba) dan tanin (bersifat astringent/menciutkan selaput lendir) memvalidasi pemanfaatan tradisional rebusan untuk sariawan/infeksi ringan.",
            "q_m1_5" to "Penebangan pohon penaung melenyapkan mikroklimat teduh lembap sehingga populasi epifit liar mati dan mengancam ketersediaan bahan tradisi mitoni."
        ),
        quizModul2.id to mapOf(
            "q_m2_1" to "Paradoks Konservasi menggambarkan kondisi saat naiknya nilai ekonomi suatu flora justru meningkatkan eksploitasi liar yang mengancam kepunahannya.",
            "q_m2_2" to "Dehidrasi ekstrem memicu osmosis keluar hingga membran plasma lepas dari dinding sel (plasmolisis) lalu mati kering permanen (desikasi).",
            "q_m2_3" to "Menyisakan 30% buah matang menyediakan pakan bagi burung frugivor agar memencarkan biji Parijoto secara alami (ornitokori).",
            "q_m2_4" to "Kultur jaringan memanfaatkan sifat totipotensi sel untuk memperbanyak ribuan bibit unggul seragam secara ex-situ tanpa merusak hutan.",
            "q_m2_5" to "Data Vifta dkk. (2021) menunjukkan cekaman abiotik khas lereng Muria memicu kadar flavonoid lebih tinggi (81,60 mg QE/g), mendasari perlindungan hukum Indikasi Geografis (IG)."
        ),
        quizPostTest.id to mapOf(
            "q_pt_1" to "Berpikir kritis memisahkan antara pemicu ekonomi-sosial (validasi praklinis β-sitosterol memicu eksploitasi/paradoks konservasi) dan kerusakan ekologi akibat deforestasi.",
            "q_pt_2" to "Panen parsial menyisakan 30% buah merupakan konservasi in-situ logis yang menjaga regenerasi lewat ornitokori sekaligus memberi ruang ekonomi berkelanjutan.",
            "q_pt_3" to "Etnosains direkonstruksi secara saintifik: β-sitosterol (bukti praklinis) & flavonoid (antioksidan pelindung sel), sambil tetap membedakannya dari bukti uji klinis manusia.",
            "q_pt_4" to "Sifat totipotensi memungkinkan satu irisan jaringan tumbuhan membelah dan berdiferensiasi menjadi individu baru yang utuh.",
            "q_pt_5" to "Kadar flavonoid lebih optimal di Muria akibat cekaman abiotik geografis spesifik; Indikasi Geografis (IG) melindungi keaslian dan hak ekonomi warga Muria.",
            "q_pt_6" to "Penyusutan kanopi 479->240 ha menyebabkan suhu naik +6,9°C dan kelembapan turun dari 84% ke 52%, memicu fotoinhibisi kloroplas serta plasmolisis/desikasi sel epifit."
        )
    )

    val defaultAssignments: List<Assignment> = listOf(
        Assignment(
            id = "asg_elkm_1",
            classId = "DEFAULT",
            teacherUid = "SYSTEM_DEFAULT",
            moduleId = ParijotoCurriculumPart1.modul1.id,
            pblPhase = 1,
            title = "E-LKM 1: Orientasi Masalah & Pengorganisasian PBL",
            instructions = "Pertemuan 1 (PBL Fase 1 & 2):\n1. Analisis wacana & kasus bagaimana validasi biokimia tradisi mitoni memicu eksploitasi liar Parijoto di Hutan Lindung Gunung Muria.\n2. Diskusikan bersama kelompok PBL untuk merumuskan akar permasalahan utama dan menyusun hipotesis awal dampak eksploitasi terhadap keanekaragaman hayati tingkat spesies dan ekosistem.",
            type = "E_LKM_1",
            meetingNumber = 1,
            dueDateLabel = "Pertemuan 1",
            rubricId = "rubric_keterampilan_pbl",
            published = true
        ),
        Assignment(
            id = "asg_elkm_2",
            classId = "DEFAULT",
            teacherUid = "SYSTEM_DEFAULT",
            moduleId = ParijotoCurriculumPart1.modul2.id,
            pblPhase = 3,
            title = "E-LKM 2: Penyelidikan & Strategi Konservasi Terpadu",
            instructions = "Pertemuan 2 (PBL Fase 3 & 4):\n1. Kumpulkan data saintifik spesifik (senyawa fitokimia, data flavonoid Vifta 2021, serta parameter abiotik epifit 2019–2023) untuk memvalidasi etnosains secara proporsional (bedakan praklinis, komputasi, klinis) dan menganalisis dampak deforestasi (fotoinhibisi & plasmolisis).\n2. Rumuskan gagasan solusi konservasi terpadu (in-situ 30% + ornitokori, ex-situ kultur jaringan totipotensi, & Indikasi Geografis).",
            type = "E_LKM_2",
            meetingNumber = 2,
            dueDateLabel = "Pertemuan 2",
            rubricId = "rubric_keterampilan_pbl",
            published = true
        ),
        Assignment(
            id = "asg_proyek_cer",
            classId = "DEFAULT",
            teacherUid = "SYSTEM_DEFAULT",
            moduleId = ParijotoCurriculumPart1.modul2.id,
            pblPhase = 5,
            title = "Proyek Kampanye Digital Konservasi (CER)",
            instructions = "PBL Fase 5 (Evaluasi & Presentasi):\nSusun produk kampanye digital pelestarian Parijoto Muria menggunakan struktur argumentasi ilmiah Claim–Evidence–Reasoning (CER), lengkapi caption persuasif serta lampiran visual/poster (jika ada), dan tulis kesimpulan sintesis kelompok.",
            type = "PROYEK_CER",
            meetingNumber = 2,
            dueDateLabel = "Akhir Pertemuan 2",
            rubricId = "rubric_keterampilan_pbl",
            published = true
        )
    )

    val defaultRubrics: List<Rubric> = listOf(
        Rubric(
            id = "rubric_uraian_hots",
            ownerUid = "SYSTEM_DEFAULT",
            title = "Rubrik Penskoran Soal Uraian Evaluasi Akhir (HOTS)",
            category = "URAIAN",
            maxTotalScore = 8,
            criteria = listOf(
                "Soal 1 (Bioekonomi vs Ekologi - Skor 0..4): 4 = Mengusulkan Kultur Jaringan ex-situ, menjelaskan totipotensi sel tepat, tahapan logis (eksplan -> inokulasi steril -> aklimatisasi -> budidaya massal).",
                "Soal 2 (Ekofisiologi & Kampanye Digital - Skor 0..4): 4 = Menjelaskan 2 peristiwa seluler (Fotoinhibisi kloroplas & Plasmolisis/Desikasi) secara tepat + narasi kampanye CER persuasif.",
                "Rumus: Nilai Akhir Uraian = (Total Skor Perolehan / 8) × 100"
            ),
            published = true
        ),
        Rubric(
            id = "rubric_keterampilan_pbl",
            ownerUid = "SYSTEM_DEFAULT",
            title = "Rubrik Penilaian Keterampilan (Proyek E-LKM & Kampanye CER)",
            category = "KETERAMPILAN_LKM",
            maxTotalScore = 12,
            criteria = listOf(
                "1. Keterampilan Penyelidikan (PBL Fase 3, Skor 1–4): Menggunakan >3 sumber literatur kredibel & analisis data mendalam (4).",
                "2. Rumusan Solusi Konservasi (PBL Fase 4, Skor 1–4): Solusi inovatif, rasional secara ekofisiologi, dan aplikatif bagi kelestarian habitat (4).",
                "3. Kemampuan Sintesis & Kesimpulan (PBL Fase 5, Skor 1–4): Sintesis data etnosains & ekologi logis menjawab rumusan masalah secara komprehensif (4).",
                "Rumus: Nilai Keterampilan = (Skor Perolehan / 12) × 100"
            ),
            published = true
        ),
        Rubric(
            id = "rubric_sikap",
            ownerUid = "SYSTEM_DEFAULT",
            title = "Lembar Observasi & Rubrik Penilaian Sikap",
            category = "SIKAP",
            maxTotalScore = 16,
            criteria = listOf(
                "1. Kolaborasi (1–4): Terlibat aktif membagi peran & menghargai gagasan tim di E-LKM.",
                "2. Tanggung Jawab (1–4): Menyelesaikan bagian tugas kelompok dan post-test tepat waktu & jujur.",
                "3. Keimanan & Ketakwaan terhadap Tuhan YME (1–4): Empati dan kesadaran konservasi atas kelestarian flora ciptaan Tuhan.",
                "4. Kepedulian Lingkungan (1–4): Menerapkan pola pikir keberlanjutan (sustainability) dalam solusi konservasi."
            ),
            published = true
        )
    )
}

package com.example.ui.reports

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.curriculum.LearningReportEngine
import com.example.data.curriculum.QuizEngineUtils
import com.example.data.curriculum.ReportComputationResult
import com.example.data.curriculum.ReportFilter
import com.example.data.curriculum.ReportRoleMode
import com.example.data.export.ReportExportManager
import com.example.data.model.Assignment
import com.example.data.model.ClassRoom
import com.example.data.model.Enrollment
import com.example.data.model.GradeRecord
import com.example.data.model.LearningModule
import com.example.data.model.Lesson
import com.example.data.model.Quiz
import com.example.data.model.StudentLearningProgress
import com.example.data.model.Submission
import com.example.ui.components.ComponentComparisonBarChartCard
import com.example.ui.components.MetricStatCard
import com.example.ui.components.ProgressionLineChartCard

@Composable
fun AdminReportsTab(
    classes: List<ClassRoom>,
    enrollments: List<Enrollment>,
    modules: List<LearningModule>,
    lessons: List<Lesson>,
    assignments: List<Assignment>,
    submissions: List<Submission>,
    quizzes: List<Quiz>,
    grades: List<GradeRecord>
) {
    val state = rememberLearningReportState(
        roleMode = ReportRoleMode.ADMIN,
        lockedStudentUid = null,
        classes = classes,
        enrollments = enrollments,
        modules = modules,
        lessons = lessons,
        assignments = assignments,
        submissions = submissions,
        quizzes = quizzes,
        grades = grades,
        studentPersonalProgress = null
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_reports_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        learningReportItems(
            state = state,
            classes = classes,
            modules = modules,
            enrollments = enrollments
        )
    }
}

@Composable
fun rememberLearningReportState(
    roleMode: ReportRoleMode,
    lockedStudentUid: String?,
    classes: List<ClassRoom>,
    enrollments: List<Enrollment>,
    modules: List<LearningModule>,
    lessons: List<Lesson>,
    assignments: List<Assignment>,
    submissions: List<Submission>,
    quizzes: List<Quiz>,
    grades: List<GradeRecord>,
    studentPersonalProgress: StudentLearningProgress? = null
): LearningReportUiState {
    val context = LocalContext.current
    val initialStudentUid = if (roleMode == ReportRoleMode.SISWA) lockedStudentUid else null
    var filter by remember(roleMode, lockedStudentUid) {
        mutableStateOf(ReportFilter(studentUid = initialStudentUid))
    }
    var studentSearch by remember { mutableStateOf("") }
    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }

    val effectiveFilter = if (roleMode == ReportRoleMode.SISWA) {
        filter.copy(studentUid = lockedStudentUid)
    } else {
        filter
    }

    val result = remember(
        effectiveFilter,
        roleMode,
        classes,
        enrollments,
        modules,
        lessons,
        assignments,
        submissions,
        quizzes,
        grades,
        studentPersonalProgress
    ) {
        LearningReportEngine.compute(
            filter = effectiveFilter,
            publishedOnly = (roleMode == ReportRoleMode.SISWA),
            classes = classes,
            enrollments = enrollments,
            modules = modules,
            lessons = lessons,
            assignments = assignments,
            submissions = submissions,
            quizzes = quizzes,
            grades = grades,
            studentPersonalProgress = studentPersonalProgress
        )
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ReportExportManager.MIME_PDF)
    ) { uri ->
        val bytes = pendingExportBytes
        if (uri != null && bytes != null) {
            ReportExportManager.writeBytesToUri(context, uri, bytes)
                .onSuccess { Toast.makeText(context, "Laporan PDF berhasil disimpan.", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, it.localizedMessage ?: "Gagal menyimpan PDF.", Toast.LENGTH_SHORT).show() }
        }
        pendingExportBytes = null
    }

    val excelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ReportExportManager.MIME_XLSX)
    ) { uri ->
        val bytes = pendingExportBytes
        if (uri != null && bytes != null) {
            ReportExportManager.writeBytesToUri(context, uri, bytes)
                .onSuccess { Toast.makeText(context, "Laporan Excel (.xlsx) berhasil disimpan.", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, it.localizedMessage ?: "Gagal menyimpan Excel.", Toast.LENGTH_SHORT).show() }
        }
        pendingExportBytes = null
    }

    val reportTitle = when (roleMode) {
        ReportRoleMode.ADMIN -> "Laporan Rekapitulasi Seluruh Kelas - PARIJOTO LEARNING"
        ReportRoleMode.GURU -> "Laporan Hasil Belajar Biologi - PARIJOTO LEARNING"
        ReportRoleMode.SISWA -> "Laporan Hasil Belajar Resmi Siswa - PARIJOTO LEARNING"
    }
    val reportSubtitle = "Keanekaragaman Hayati: Eksplorasi Etnosains Parijoto (SMA/MA Kelas X Fase E)"

    return LearningReportUiState(
        roleMode = roleMode,
        filter = effectiveFilter,
        studentSearch = studentSearch,
        result = result,
        onFilterChange = { filter = it },
        onStudentSearchChange = { studentSearch = it },
        onDownloadPdf = {
            val bytes = ReportExportManager.buildPdfReportBytes(
                title = reportTitle,
                subtitle = reportSubtitle,
                result = result,
                isStudentMode = (roleMode == ReportRoleMode.SISWA)
            )
            pendingExportBytes = bytes
            pdfLauncher.launch(ReportExportManager.buildReportFileName("laporan_parijoto", "pdf"))
        },
        onDownloadExcel = {
            val bytes = ReportExportManager.buildExcelWorkbookBytes(
                title = reportTitle,
                subtitle = reportSubtitle,
                result = result,
                isStudentMode = (roleMode == ReportRoleMode.SISWA)
            )
            pendingExportBytes = bytes
            excelLauncher.launch(ReportExportManager.buildReportFileName("rekap_parijoto", "xlsx"))
        },
        onSharePdf = {
            val bytes = ReportExportManager.buildPdfReportBytes(
                title = reportTitle,
                subtitle = reportSubtitle,
                result = result,
                isStudentMode = (roleMode == ReportRoleMode.SISWA)
            )
            ReportExportManager.shareBytesViaFileProvider(
                context = context,
                fileName = ReportExportManager.buildReportFileName("laporan_parijoto", "pdf"),
                mimeType = ReportExportManager.MIME_PDF,
                bytes = bytes,
                chooserTitle = "Bagikan Laporan PDF"
            ).onFailure {
                Toast.makeText(context, it.localizedMessage ?: "Gagal membagikan PDF.", Toast.LENGTH_SHORT).show()
            }
        },
        onShareExcel = {
            val bytes = ReportExportManager.buildExcelWorkbookBytes(
                title = reportTitle,
                subtitle = reportSubtitle,
                result = result,
                isStudentMode = (roleMode == ReportRoleMode.SISWA)
            )
            ReportExportManager.shareBytesViaFileProvider(
                context = context,
                fileName = ReportExportManager.buildReportFileName("rekap_parijoto", "xlsx"),
                mimeType = ReportExportManager.MIME_XLSX,
                bytes = bytes,
                chooserTitle = "Bagikan Rekap Excel (.xlsx)"
            ).onFailure {
                Toast.makeText(context, it.localizedMessage ?: "Gagal membagikan Excel.", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

data class LearningReportUiState(
    val roleMode: ReportRoleMode,
    val filter: ReportFilter,
    val studentSearch: String,
    val result: ReportComputationResult,
    val onFilterChange: (ReportFilter) -> Unit,
    val onStudentSearchChange: (String) -> Unit,
    val onDownloadPdf: () -> Unit,
    val onDownloadExcel: () -> Unit,
    val onSharePdf: () -> Unit,
    val onShareExcel: () -> Unit
)

fun LazyListScope.learningReportItems(
    state: LearningReportUiState,
    classes: List<ClassRoom>,
    modules: List<LearningModule>,
    enrollments: List<Enrollment>
) {
    val isStudent = state.roleMode == ReportRoleMode.SISWA
    val res = state.result

    item(key = "report_header_banner") {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    when (state.roleMode) {
                        ReportRoleMode.ADMIN -> "Rekapitulasi & Laporan Seluruh Kelas (Admin)"
                        ReportRoleMode.GURU -> "Rekapitulasi Penilaian & Laporan Hasil Belajar"
                        ReportRoleMode.SISWA -> "Laporan Hasil Belajar Resmi Saya"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (isStudent) {
                        "Menampilkan nilai resmi yang telah dipublikasikan guru beserta penyelesaian materi dan status tugasmu."
                    } else {
                        "Semua nilai terhubung ke komponen penilaian (Kuis, Post-Test, Keterampilan E-LKM, Proyek CER, Sikap) dan dapat diunduh sebagai PDF & Excel."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (isStudent) {
        item(key = "student_published_gate_notice") {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.testTag("student_report_published_notice")
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = "Terlindungi", tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Siswa hanya dapat melihat laporan hasil belajar milik sendiri yang sudah dipublikasikan oleh guru.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    item(key = "report_filters") {
        ReportFilterCard(
            roleMode = state.roleMode,
            filter = state.filter,
            classes = classes,
            modules = modules,
            enrollments = enrollments,
            studentSearch = state.studentSearch,
            onStudentSearchChange = state.onStudentSearchChange,
            onFilterChange = state.onFilterChange
        )
    }

    if (!isStudent) {
        item(key = "report_export_actions") {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ekspor & Unduh Laporan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = state.onDownloadPdf,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("export_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Unduh PDF")
                            Spacer(Modifier.width(6.dp))
                            Text("Unduh PDF")
                        }
                        IconButton(
                            onClick = state.onSharePdf,
                            modifier = Modifier.testTag("share_pdf_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Bagikan PDF")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = state.onDownloadExcel,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("export_excel_button")
                        ) {
                            Icon(Icons.Default.TableView, contentDescription = "Unduh Excel")
                            Spacer(Modifier.width(6.dp))
                            Text("Unduh Excel (.xlsx)")
                        }
                        IconButton(
                            onClick = state.onShareExcel,
                            modifier = Modifier.testTag("share_excel_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Bagikan Excel")
                        }
                    }
                }
            }
        }
    }

    item(key = "report_summary_metrics") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricStatCard(
                    label = if (isStudent) "Nilai Rapor" else "Rata-rata Akhir",
                    value = "${QuizEngineUtils.roundScore(res.overallAvgFinalScore)}",
                    icon = Icons.Default.Grade,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    label = "Selesai Materi",
                    value = "${QuizEngineUtils.roundScore(res.overallMaterialCompletionPercent)}%",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricStatCard(
                    label = "Tugas Terkumpul",
                    value = "${QuizEngineUtils.roundScore(res.overallAssignmentCompletionPercent)}% (${res.totalSubmittedTasks}/${res.totalExpectedTasks})",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    label = if (isStudent) "Post-Test" else "Cakupan Siswa",
                    value = if (isStudent) {
                        res.overallAvgPostTest?.let { "${QuizEngineUtils.roundScore(it)}" } ?: "-"
                    } else {
                        "${res.studentReports.size} Siswa"
                    },
                    icon = Icons.Default.TableView,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    item(key = "report_progression_chart") {
        ProgressionLineChartCard(
            title = "Grafik Perkembangan Capaian Belajar",
            subtitle = "Alur progres dari Pertemuan 1 (Kuis 1, E-LKM 1) hingga Pertemuan 2 (Kuis 2, E-LKM 2, Proyek CER, Post-Test, Sikap)",
            points = res.overallProgressionPoints
        )
    }

    item(key = "report_component_chart") {
        ComponentComparisonBarChartCard(
            quizScore = res.overallAvgQuiz,
            postTestScore = res.overallAvgPostTest,
            skillScore = res.overallAvgSkill,
            projectScore = res.overallAvgProject,
            attitudeScore = res.overallAvgAttitude
        )
    }

    if (!isStudent) {
        item(key = "report_class_header") {
            Text(
                "Rekapitulasi Nilai per Kelas (${res.classReports.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        if (res.classReports.isEmpty()) {
            item(key = "report_class_empty") {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text("Belum ada kelas pada filter ini.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(14.dp))
                }
            }
        } else {
            items(res.classReports, key = { "cls_rep_${it.classId}" }) { clsSummary ->
                ClassReportCard(clsSummary)
            }
        }
    }

    item(key = "report_student_header") {
        Text(
            if (isStudent) "Rapor & Penelusuran Nilai Saya" else "Rekapitulasi Nilai Setiap Siswa (${res.studentReports.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }

    if (res.studentReports.isEmpty()) {
        item(key = "report_student_empty") {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    if (isStudent) "Belum ada data laporan belajar pada filter yang dipilih."
                    else "Belum ada siswa aktif pada filter kelas/siswa yang dipilih.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    } else {
        items(res.studentReports, key = { "stu_rep_${it.studentUid}_${it.classId}" }) { stuSummary ->
            StudentReportCard(summary = stuSummary, isStudentMode = isStudent)
        }
    }
}

package com.example.ui.teacher

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.curriculum.ParijotoAssessmentsAndGlossary
import com.example.data.curriculum.ParijotoCurriculumPart1
import com.example.data.curriculum.ParijotoCurriculumPart2
import com.example.data.curriculum.QuizEngineUtils
import com.example.data.curriculum.ReportRoleMode
import com.example.data.model.Assignment
import com.example.data.model.ClassRoom
import com.example.data.model.Enrollment
import com.example.data.model.GradeRecord
import com.example.data.model.LearningModule
import com.example.data.model.Lesson
import com.example.data.model.Question
import com.example.data.model.Quiz
import com.example.data.model.QuizAnswerKey
import com.example.data.model.QuizAttempt
import com.example.data.model.Reflection
import com.example.data.model.Rubric
import com.example.data.model.Submission
import com.example.data.model.UserAccount
import com.example.ui.components.StatusPill
import com.example.ui.components.shareTextIntent
import com.example.ui.reports.learningReportItems
import com.example.ui.reports.rememberLearningReportState
import com.example.ui.student.submissionStatusLabel
import kotlin.math.roundToInt

@Composable
fun TeacherGradingTab(
    enrollments: List<Enrollment>,
    assignments: List<Assignment>,
    submissions: List<Submission>,
    quizzes: List<Quiz>,
    questions: List<Question> = emptyList(),
    answerKeys: Map<String, QuizAnswerKey> = emptyMap(),
    attempts: List<QuizAttempt>,
    grades: List<GradeRecord>,
    rubrics: List<Rubric> = ParijotoAssessmentsAndGlossary.defaultRubrics,
    reflections: List<Reflection>,
    busy: Boolean,
    classes: List<ClassRoom> = emptyList(),
    modules: List<LearningModule> = listOf(ParijotoCurriculumPart1.modul1, ParijotoCurriculumPart1.modul2),
    lessons: List<Lesson> = ParijotoCurriculumPart1.modul1Lessons + ParijotoCurriculumPart2.modul2Lessons,
    onEnsureAnswerKeys: () -> Unit = {},
    onGradeAttempt: (QuizAttempt, String, Int, Int, String, Boolean) -> Unit = { _, _, _, _, _, _ -> },
    onGradeQuizAttempt: (QuizAttempt, Quiz?, Map<String, Int>, String, Boolean) -> Unit = { _, _, _, _, _ -> },
    onGradeSubmission: (Submission, String, Int, Int, Int, String, Boolean) -> Unit,
    onReturnForRevision: (Submission, String) -> Unit = { _, _ -> },
    onGradeAttitude: (Enrollment, Int, Int, Int, Int, String) -> Unit,
    onExportCsv: () -> String
) {
    val context = LocalContext.current
    var queueTab by remember { mutableIntStateOf(0) }
    var quizCategoryFilter by remember { mutableStateOf("ALL") }
    var submissionStatusFilter by remember { mutableStateOf("ALL") }

    val reportState = rememberLearningReportState(
        roleMode = ReportRoleMode.GURU,
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

    LaunchedEffect(queueTab) {
        if (queueTab == 0) onEnsureAnswerKeys()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("teacher_grading_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = queueTab == 0,
                        onClick = { queueTab = 0 },
                        label = { Text("Kuis & Uraian") },
                        modifier = Modifier.height(48.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = queueTab == 1,
                        onClick = { queueTab = 1 },
                        label = { Text("LKM & CER") },
                        modifier = Modifier.height(48.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = queueTab == 2,
                        onClick = { queueTab = 2 },
                        label = { Text("Sikap & Rekap") },
                        modifier = Modifier.height(48.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = queueTab == 3,
                        onClick = { queueTab = 3 },
                        label = { Text("Laporan & Rekap") },
                        modifier = Modifier.height(48.dp).testTag("teacher_grading_tab_reports")
                    )
                }
            }
        }

        when (queueTab) {
            0 -> {
                item {
                    val cats = listOf(
                        "ALL" to "Semua",
                        "LATIHAN_MODUL_1" to "Kuis Modul 1",
                        "LATIHAN_MODUL_2" to "Kuis Modul 2",
                        "POST_TEST" to "Evaluasi Akhir"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(cats, key = { it.first }) { (key, label) ->
                            FilterChip(
                                selected = quizCategoryFilter == key,
                                onClick = { quizCategoryFilter = key },
                                label = { Text(label) },
                                modifier = Modifier.height(48.dp)
                            )
                        }
                    }
                }

                val submittedAttempts = attempts.filter { att ->
                    if (att.status == "IN_PROGRESS") return@filter false
                    val q = quizzes.firstOrNull { it.id == att.quizId }
                    quizCategoryFilter == "ALL" || q?.category == quizCategoryFilter
                }.sortedByDescending { it.attemptNumber }

                if (submittedAttempts.isEmpty()) {
                    item {
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(
                                "Belum ada siswa yang mengumpulkan kuis atau Evaluasi Akhir pada kategori ini.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    items(submittedAttempts, key = { it.id }) { attempt ->
                        val quiz = quizzes.firstOrNull { it.id == attempt.quizId }
                        val quizTitle = quiz?.title ?: attempt.quizId
                        val allQuizQs = questions.filter { it.quizId == attempt.quizId }.sortedBy { it.orderIndex }
                        val attemptQs = if (attempt.questionOrder.isNotEmpty()) {
                            val byId = allQuizQs.associateBy { it.id }
                            attempt.questionOrder.mapNotNull { byId[it] }.ifEmpty { allQuizQs }
                        } else {
                            allQuizQs
                        }
                        val mcqQs = attemptQs.filter { it.type == "MULTIPLE_CHOICE" }
                        val essayQs = attemptQs.filter { it.type == "ESSAY" }
                        val keyMap = answerKeys[attempt.quizId]?.keys
                            ?: ParijotoAssessmentsAndGlossary.defaultAnswerKeys[attempt.quizId]
                            ?: emptyMap()
                        val mcqSummary = QuizEngineUtils.computeMcqAutoScore(mcqQs, attempt.answers, keyMap)

                        val essayScoreMap = remember(attempt.id, essayQs.size) {
                            mutableStateMapOf<String, Int>().apply {
                                essayQs.forEach { eq ->
                                    this[eq.id] = attempt.essayItemScores[eq.id] ?: 3
                                }
                            }
                        }
                        val essaySummary = QuizEngineUtils.computeEssayManualScore(essayQs, essayScoreMap)
                        val combinedScore = QuizEngineUtils.combineQuizFinalScore(
                            hasMcq = mcqQs.isNotEmpty(),
                            hasEssay = essayQs.isNotEmpty(),
                            mcqPct = mcqSummary.percentageScore,
                            essayPct = essaySummary.percentageScore
                        )
                        var feedback by remember(attempt.id, attempt.teacherFeedback) {
                            mutableStateOf(attempt.teacherFeedback.orEmpty())
                        }

                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${attempt.studentName} • $quizTitle", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text("Percobaan #${attempt.attemptNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusPill(
                                        text = if (attempt.resultsPublished) "Dipublikasikan" else "Belum Publikasi",
                                        containerColor = if (attempt.resultsPublished) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = if (attempt.resultsPublished) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                // Mekanisme 1: Penilaian Otomatis Pilihan Ganda
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = "Otomatis PG", tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Penilaian Otomatis Pilihan Ganda", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            Text(
                                                "Skor PG: ${QuizEngineUtils.roundScore(mcqSummary.percentageScore)} (${mcqSummary.correctCount}/${mcqSummary.totalMcqCount} benar • ${mcqSummary.earnedPoints}/${mcqSummary.maxPoints} poin)",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }

                                // Mekanisme 2: Penilaian Manual Uraian (Rubrik per Butir)
                                if (essayQs.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.EditNote, contentDescription = "Manual Uraian", tint = MaterialTheme.colorScheme.secondary)
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    "Penilaian Manual Soal Uraian (Rubrik 0–4) • Skor Uraian: ${QuizEngineUtils.roundScore(essaySummary.percentageScore)}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            essayQs.forEachIndexed { idx, eq ->
                                                val studentAns = attempt.essayAnswers[eq.id].orEmpty()
                                                val score = essayScoreMap[eq.id] ?: 0
                                                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface) {
                                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text("Uraian ${idx + 1}: ${eq.prompt.take(120)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                        Text(studentAns.ifBlank { "(Siswa tidak mengisi uraian)" }, style = MaterialTheme.typography.bodySmall)
                                                        Text("Skor Rubrik Soal ${idx + 1}: $score / 4", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                        Slider(
                                                            value = score.toFloat(),
                                                            onValueChange = { essayScoreMap[eq.id] = it.roundToInt() },
                                                            valueRange = 0f..4f,
                                                            steps = 3,
                                                            modifier = Modifier.testTag("essay_score_slider_${attempt.id}_${eq.id}")
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Text(
                                    "Pratinjau Nilai Akhir Gabungan: ${QuizEngineUtils.roundScore(combinedScore)} / 100",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                OutlinedTextField(
                                    value = feedback,
                                    onValueChange = { feedback = it },
                                    label = { Text("Umpan Balik Guru") },
                                    modifier = Modifier.fillMaxWidth().testTag("attempt_feedback_input_${attempt.id}")
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = {
                                            onGradeQuizAttempt(attempt, quiz, essayScoreMap.toMap(), feedback, false)
                                            onGradeAttempt(attempt, quizTitle, essayScoreMap.values.firstOrNull() ?: 3, essayScoreMap.values.drop(1).firstOrNull() ?: 3, feedback, false)
                                        },
                                        enabled = !busy,
                                        modifier = Modifier.weight(1f).height(48.dp).testTag("save_attempt_draft_${attempt.id}")
                                    ) {
                                        Text("Simpan Draf")
                                    }
                                    Button(
                                        onClick = {
                                            onGradeQuizAttempt(attempt, quiz, essayScoreMap.toMap(), feedback, true)
                                            onGradeAttempt(attempt, quizTitle, essayScoreMap.values.firstOrNull() ?: 3, essayScoreMap.values.drop(1).firstOrNull() ?: 3, feedback, true)
                                        },
                                        enabled = !busy,
                                        modifier = Modifier.weight(1.3f).height(48.dp).testTag("publish_attempt_grade_${attempt.id}")
                                    ) {
                                        Text("Publikasikan Nilai")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                item {
                    val skillRubric = rubrics.firstOrNull { it.category == "KETERAMPILAN_LKM" }
                    if (skillRubric != null) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(skillRubric.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                skillRubric.criteria.forEach { criterion ->
                                    Text("• $criterion", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                item {
                    val statuses = listOf("ALL" to "Semua", "SUBMITTED" to "Menunggu", "REVISION_REQUIRED" to "Revisi", "GRADED" to "Dinilai")
                    LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(statuses, key = { it.first }) { (key, label) ->
                            FilterChip(
                                selected = submissionStatusFilter == key,
                                onClick = { submissionStatusFilter = key },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.height(48.dp)
                            )
                        }
                    }
                }
                val submitted = submissions.filter { sub ->
                    sub.status != "DRAFT" && (submissionStatusFilter == "ALL" || sub.status == submissionStatusFilter)
                }
                if (submitted.isEmpty()) {
                    item {
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text("Belum ada pengumpulan E-LKM atau proyek CER pada filter ini.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(14.dp))
                        }
                    }
                } else {
                    items(submitted, key = { it.id }) { sub ->
                        val asgTitle = assignments.firstOrNull { it.id == sub.assignmentId }?.title ?: sub.assignmentId
                        var s1 by remember(sub.id) { mutableIntStateOf(3) }
                        var s2 by remember(sub.id) { mutableIntStateOf(3) }
                        var s3 by remember(sub.id) { mutableIntStateOf(3) }
                        var fb by remember(sub.id, sub.teacherFeedback) { mutableStateOf(sub.teacherFeedback.orEmpty()) }

                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${sub.studentName} • $asgTitle", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    StatusPill(
                                        text = submissionStatusLabel(sub.status),
                                        containerColor = if (sub.status == "REVISION_REQUIRED") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = if (sub.status == "REVISION_REQUIRED") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                sub.problemAnalysis?.takeIf { it.isNotBlank() }?.let { Text("Analisis Masalah: $it", style = MaterialTheme.typography.bodySmall) }
                                sub.hypothesis?.takeIf { it.isNotBlank() }?.let { Text("Hipotesis: $it", style = MaterialTheme.typography.bodySmall) }
                                sub.scientificEvidence?.takeIf { it.isNotBlank() }?.let { Text("Bukti Ilmiah: $it", style = MaterialTheme.typography.bodySmall) }
                                sub.dataAnalysisTable?.takeIf { it.isNotBlank() }?.let { Text("Analisis Data: $it", style = MaterialTheme.typography.bodySmall) }
                                sub.conservationSolution?.takeIf { it.isNotBlank() }?.let { Text("Solusi Konservasi: $it", style = MaterialTheme.typography.bodySmall) }
                                sub.discussionSummary?.takeIf { it.isNotBlank() }?.let { Text("Hasil Diskusi Kelompok: $it", style = MaterialTheme.typography.bodySmall) }
                                sub.cerClaim?.takeIf { it.isNotBlank() }?.let { Text("CER Claim: $it", style = MaterialTheme.typography.bodySmall) }
                                sub.cerEvidence?.takeIf { it.isNotBlank() }?.let { Text("CER Evidence: $it", style = MaterialTheme.typography.bodySmall) }
                                sub.cerReasoning?.takeIf { it.isNotBlank() }?.let { Text("CER Reasoning: $it", style = MaterialTheme.typography.bodySmall) }
                                sub.campaignCaption?.takeIf { it.isNotBlank() }?.let { Text("Caption: $it", style = MaterialTheme.typography.bodySmall) }
                                sub.attachmentName?.let { attName ->
                                    val mimeInfo = sub.attachmentMimeType?.let { " • $it" }.orEmpty()
                                    Text(
                                        "Lampiran: $attName (${(sub.attachmentSizeBytes ?: 0L) / 1024} KB$mimeInfo)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    val attUrl = sub.attachmentUrl
                                    if (!attUrl.isNullOrBlank() && sub.attachmentMimeType?.startsWith("image/") == true) {
                                        if (attUrl.startsWith("content://")) {
                                            Text(
                                                "Lampiran gambar tersimpan sebagai URI lokal perangkat pengirim (pratinjau penuh tersedia di perangkat siswa jika belum diunggah ke cloud storage).",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        AsyncImage(
                                            model = attUrl,
                                            contentDescription = "Pratinjau Lampiran Karya Siswa",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(140.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                    }
                                }

                                Text("Penyelidikan (1-4): $s1", style = MaterialTheme.typography.labelMedium)
                                Slider(value = s1.toFloat(), onValueChange = { s1 = it.roundToInt() }, valueRange = 1f..4f, steps = 2)
                                Text("Solusi Konservasi (1-4): $s2", style = MaterialTheme.typography.labelMedium)
                                Slider(value = s2.toFloat(), onValueChange = { s2 = it.roundToInt() }, valueRange = 1f..4f, steps = 2)
                                Text("Sintesis & CER (1-4): $s3", style = MaterialTheme.typography.labelMedium)
                                Slider(value = s3.toFloat(), onValueChange = { s3 = it.roundToInt() }, valueRange = 1f..4f, steps = 2)
                                OutlinedTextField(
                                    value = fb,
                                    onValueChange = { fb = it },
                                    label = { Text("Komentar, Koreksi & Umpan Balik Guru") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth().testTag("submission_feedback_input_${sub.id}")
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { onReturnForRevision(sub, fb) },
                                        enabled = !busy && fb.isNotBlank(),
                                        modifier = Modifier.weight(1f).height(48.dp).testTag("return_revision_button_${sub.id}")
                                    ) {
                                        Icon(Icons.Default.Replay, contentDescription = "Kembalikan Revisi")
                                        Spacer(Modifier.width(4.dp))
                                        Text("Revisi")
                                    }
                                    Button(
                                        onClick = { onGradeSubmission(sub, asgTitle, s1, s2, s3, fb, true) },
                                        enabled = !busy,
                                        modifier = Modifier.weight(1.4f).height(48.dp).testTag("publish_skill_grade_button_${sub.id}")
                                    ) {
                                        Text("Nilai (${((s1 + s2 + s3) * 100) / 12})")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                item {
                    OutlinedButton(
                        onClick = {
                            shareTextIntent(context, "Rekap Nilai Kelas (CSV)", onExportCsv())
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("export_grades_csv_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Ekspor")
                        Spacer(Modifier.width(8.dp))
                        Text("Bagikan Rekap Nilai CSV (${grades.size} entri)")
                    }
                }

                // Rekap Nilai Evaluasi & Penilaian Kelas di Dashboard Guru
                item {
                    val groupedGrades = grades.groupBy { it.studentUid to it.studentName }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.testTag("teacher_grade_recap_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Rekap Nilai Evaluasi Siswa (${groupedGrades.size} siswa)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (groupedGrades.isEmpty()) {
                                Text("Belum ada entri nilai tersimpan.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                groupedGrades.entries.forEach { (studentPair, list) ->
                                    val knowAvg = list.filter { it.assessmentType == "PENGETAHUAN" }.map { it.finalScore }.takeIf { it.isNotEmpty() }?.average()
                                    val skillAvg = list.filter { it.assessmentType == "KETERAMPILAN" }.map { it.finalScore }.takeIf { it.isNotEmpty() }?.average()
                                    val attAvg = list.filter { it.assessmentType == "SIKAP" }.map { it.finalScore }.takeIf { it.isNotEmpty() }?.average()
                                    val publishedCount = list.count { it.published }
                                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(studentPair.second, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                                Text("$publishedCount/${list.size} Publikasi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                            Text(
                                                "Pengetahuan: ${knowAvg?.toInt() ?: "-"} • Keterampilan: ${skillAvg?.toInt() ?: "-"} • Sikap: ${attAvg?.toInt() ?: "-"}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                items(enrollments, key = { "att_${it.id}" }) { enr ->
                    var c1 by remember(enr.id) { mutableIntStateOf(3) }
                    var c2 by remember(enr.id) { mutableIntStateOf(4) }
                    var c3 by remember(enr.id) { mutableIntStateOf(3) }
                    var c4 by remember(enr.id) { mutableIntStateOf(4) }
                    var fb by remember(enr.id) { mutableStateOf("") }

                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Observasi Sikap: ${enr.studentName} (${enr.className})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Kolaborasi ($c1) • Ketakwaan & Ekologi ($c2) • Tanggung Jawab ($c3) • Kepedulian ($c4)", style = MaterialTheme.typography.bodySmall)
                            Slider(value = c1.toFloat(), onValueChange = { c1 = it.roundToInt() }, valueRange = 1f..4f, steps = 2)
                            Slider(value = c2.toFloat(), onValueChange = { c2 = it.roundToInt() }, valueRange = 1f..4f, steps = 2)
                            Slider(value = c3.toFloat(), onValueChange = { c3 = it.roundToInt() }, valueRange = 1f..4f, steps = 2)
                            Slider(value = c4.toFloat(), onValueChange = { c4 = it.roundToInt() }, valueRange = 1f..4f, steps = 2)
                            OutlinedTextField(value = fb, onValueChange = { fb = it }, label = { Text("Catatan Sikap") }, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = { onGradeAttitude(enr, c1, c2, c3, c4, fb) },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Simpan Nilai Sikap (${((c1 + c2 + c3 + c4) * 100) / 16})")
                            }
                        }
                    }
                }

                if (reflections.isNotEmpty()) {
                    item {
                        Text("Refleksi Mandiri Siswa (${reflections.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    items(reflections, key = { it.id }) { ref ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${ref.studentName} (${ref.checklistYesCount}/${ref.totalChecklist} tercapai)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                ref.notes?.takeIf { it.isNotBlank() }?.let { Text("Perlu diulang: $it", style = MaterialTheme.typography.bodySmall) }
                                ref.pblReflection?.takeIf { it.isNotBlank() }?.let { Text("Refleksi PBL: $it", style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
            }

            else -> {
                learningReportItems(
                    state = reportState,
                    classes = classes,
                    modules = modules,
                    enrollments = enrollments
                )
            }
        }
    }
}

@Composable
fun TeacherProfileTab(
    user: UserAccount,
    classCount: Int,
    studentCount: Int,
    busy: Boolean,
    onUpdateName: (String) -> Unit,
    onOpenGlossary: () -> Unit,
    onSignOut: () -> Unit
) {
    var nama by remember(user.nama) { mutableStateOf(user.nama) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Profil Guru Biologi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(user.email, style = MaterialTheme.typography.bodySmall)
                    Text("Kelas dibina: $classCount • Siswa terdaftar: $studentCount", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = nama,
                        onValueChange = { nama = it },
                        label = { Text("Nama Lengkap & Gelar") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onUpdateName(nama) },
                        enabled = !busy && nama.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Simpan")
                        Spacer(Modifier.width(8.dp))
                        Text("Simpan Profil Guru")
                    }
                }
            }
        }

        item {
            OutlinedButton(onClick = onOpenGlossary, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Glosarium")
                Spacer(Modifier.width(8.dp))
                Text("Buka Glosarium & Peta Konsep")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Keluar")
                Spacer(Modifier.width(8.dp))
                Text("Keluar dari Akun")
            }
        }
    }
}

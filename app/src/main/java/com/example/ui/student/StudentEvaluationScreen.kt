package com.example.ui.student

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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.example.data.model.StudentLearningProgress
import com.example.data.model.Submission
import com.example.ui.components.StatusPill
import com.example.ui.reports.learningReportItems
import com.example.ui.reports.rememberLearningReportState

private data class ActiveQuizSession(
    val quiz: Quiz,
    val attemptNumber: Int,
    val existingAttempt: QuizAttempt?
)

@Composable
fun StudentEvaluationTab(
    studentUid: String = "local_student",
    quizzes: List<Quiz>,
    questions: List<Question>,
    attempts: List<QuizAttempt>,
    answerKeysCache: Map<String, QuizAnswerKey>,
    publishedGrades: List<GradeRecord>,
    reflections: List<Reflection>,
    busy: Boolean,
    enrollments: List<Enrollment> = emptyList(),
    enrolledClassesMap: Map<String, ClassRoom> = emptyMap(),
    modules: List<LearningModule> = listOf(ParijotoCurriculumPart1.modul1, ParijotoCurriculumPart1.modul2),
    lessons: List<Lesson> = ParijotoCurriculumPart1.modul1Lessons + ParijotoCurriculumPart2.modul2Lessons,
    learningProgress: StudentLearningProgress = StudentLearningProgress(studentUid = studentUid),
    assignments: List<Assignment> = ParijotoAssessmentsAndGlossary.defaultAssignments,
    submissions: List<Submission> = emptyList(),
    onLoadAnswerKey: (Quiz) -> Unit,
    onAutosaveQuiz: (Quiz, Int, List<String>, Int, Int, Map<String, String>, Map<String, String>, Boolean) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onSubmitQuiz: (Quiz, List<Question>, Int, List<String>, Int, Map<String, String>, Map<String, String>) -> Unit,
    onSubmitReflection: (String, List<Boolean>, String?, String?) -> Unit
) {
    var activeSession by remember { mutableStateOf<ActiveQuizSession?>(null) }
    var evalMode by remember { mutableIntStateOf(0) }
    var categoryFilter by remember { mutableStateOf("ALL") }
    var reflectionModuleId by remember { mutableStateOf(ParijotoCurriculumPart1.modul1.id) }

    activeSession?.let { session ->
        val quiz = quizzes.firstOrNull { it.id == session.quiz.id } ?: session.quiz
        val latestAttempt = session.existingAttempt?.let { existing ->
            attempts.firstOrNull { it.id == existing.id } ?: existing
        } ?: attempts.firstOrNull { it.quizId == quiz.id && it.attemptNumber == session.attemptNumber }

        LaunchedEffect(quiz.id, quiz.closed, quiz.showDiscussion, quiz.resultsPublished) {
            if (quiz.closed && quiz.showDiscussion && quiz.resultsPublished) {
                onLoadAnswerKey(quiz)
            }
        }
        QuizRunnerScreen(
            quiz = quiz,
            questions = questions,
            studentUid = studentUid,
            attemptNumber = session.attemptNumber,
            existingAttempt = latestAttempt,
            answerKey = answerKeysCache[quiz.id],
            busy = busy,
            onBack = { activeSession = null },
            onAutosave = { order, mcqCount, remSec, mcqs, essays, showToast ->
                onAutosaveQuiz(quiz, session.attemptNumber, order, mcqCount, remSec, mcqs, essays, showToast)
            },
            onSubmit = { selectedQs, order, remSec, mcqs, essays ->
                onSubmitQuiz(quiz, selectedQs, session.attemptNumber, order, remSec, mcqs, essays)
            }
        )
        return
    }

    val filteredQuizzes = remember(quizzes, categoryFilter) {
        if (categoryFilter == "ALL") quizzes else quizzes.filter { it.category == categoryFilter }
    }
    val completedHistory = remember(attempts) {
        attempts.filter { it.status != "IN_PROGRESS" }.sortedByDescending { it.attemptNumber }
    }

    val checklist = if (reflectionModuleId == ParijotoCurriculumPart1.modul1.id) {
        ParijotoAssessmentsAndGlossary.modul1SelfChecklist
    } else {
        ParijotoAssessmentsAndGlossary.modul2SelfChecklist
    }
    val existingRef = reflections.firstOrNull { it.moduleId == reflectionModuleId }
    val checklistState = remember(reflectionModuleId, existingRef?.id) {
        mutableStateMapOf<Int, Boolean>().apply {
            checklist.indices.forEach { i ->
                this[i] = existingRef?.answers?.getOrNull(i) ?: false
            }
        }
    }
    var reflectionNotes by remember(reflectionModuleId, existingRef?.id) { mutableStateOf(existingRef?.notes.orEmpty()) }
    var pblReflection by remember(reflectionModuleId, existingRef?.id) { mutableStateOf(existingRef?.pblReflection.orEmpty()) }

    val reportClasses = remember(enrolledClassesMap) { enrolledClassesMap.values.toList() }
    val reportState = rememberLearningReportState(
        roleMode = ReportRoleMode.SISWA,
        lockedStudentUid = studentUid,
        classes = reportClasses,
        enrollments = enrollments,
        modules = modules,
        lessons = lessons,
        assignments = assignments,
        submissions = submissions,
        quizzes = quizzes,
        grades = publishedGrades,
        studentPersonalProgress = learningProgress
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("student_evaluation_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = evalMode == 0,
                    onClick = { evalMode = 0 },
                    label = { Text("Kuis & Refleksi") },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("student_eval_mode_quiz")
                )
                FilterChip(
                    selected = evalMode == 1,
                    onClick = { evalMode = 1 },
                    label = { Text("Laporan Hasil Belajar") },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("student_eval_mode_report")
                )
            }
        }

        if (evalMode == 1) {
            learningReportItems(
                state = reportState,
                classes = reportClasses,
                modules = modules,
                enrollments = enrollments
            )
            return@LazyColumn
        }

        item {
            Text("Kuis Modul & Evaluasi Akhir", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }

        item {
            val categories = listOf(
                "ALL" to "Semua",
                "LATIHAN_MODUL_1" to "Kuis Modul 1",
                "LATIHAN_MODUL_2" to "Kuis Modul 2",
                "POST_TEST" to "Evaluasi Akhir"
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories, key = { it.first }) { (key, label) ->
                    FilterChip(
                        selected = categoryFilter == key,
                        onClick = { categoryFilter = key },
                        label = { Text(label) },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("eval_filter_$key")
                    )
                }
            }
        }

        items(filteredQuizzes, key = { it.id }) { quiz ->
            val quizAttempts = attempts.filter { it.quizId == quiz.id }.sortedByDescending { it.attemptNumber }
            val inProgress = quizAttempts.firstOrNull { it.status == "IN_PROGRESS" }
            val submittedAttempts = quizAttempts.filter { it.status != "IN_PROGRESS" }
            val latestSubmitted = submittedAttempts.firstOrNull()
            val availableCount = questions.count { it.quizId == quiz.id && it.published }
            val effectiveCount = if (availableCount == 0) 0 else minOf(quiz.questionCount, availableCount)
            val access = QuizEngineUtils.isQuizAccessibleNow(quiz)
            val canStartOrResume = access.canStartNew && (inProgress != null || submittedAttempts.size < quiz.maxAttempts) && effectiveCount > 0

            val statusLabel = when {
                inProgress != null -> "Draf #${inProgress.attemptNumber}"
                latestSubmitted != null && (quiz.resultsPublished || latestSubmitted.resultsPublished) ->
                    "Nilai: ${latestSubmitted.totalScore.toInt()}"
                latestSubmitted != null -> "Terkirim (Terlindungi)"
                !access.canStartNew -> "Terkunci"
                else -> "Terbuka"
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.testTag("quiz_card_${quiz.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Quiz, contentDescription = "Evaluasi", tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(quiz.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    QuizEngineUtils.quizCategoryLabel(quiz.category),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        StatusPill(
                            text = statusLabel,
                            containerColor = if (access.canStartNew) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (access.canStartNew) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Text(
                        "$effectiveCount soal • ${quiz.durationMinutes} menit • Percobaan ${submittedAttempts.size}/${quiz.maxAttempts} • ${quiz.scheduleLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!access.canStartNew) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = "Terkunci", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(6.dp))
                            Text(access.reason, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (canStartOrResume) {
                            Button(
                                onClick = {
                                    val nextAttemptNum = inProgress?.attemptNumber ?: (submittedAttempts.size + 1)
                                    activeSession = ActiveQuizSession(
                                        quiz = quiz,
                                        attemptNumber = nextAttemptNum,
                                        existingAttempt = inProgress
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("start_quiz_button_${quiz.id}")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Kerjakan")
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    if (inProgress != null) "Lanjutkan Draf" else if (submittedAttempts.isEmpty()) "Kerjakan" else "Coba Lagi (#${submittedAttempts.size + 1})"
                                )
                            }
                        }
                        if (latestSubmitted != null) {
                            OutlinedButton(
                                onClick = {
                                    activeSession = ActiveQuizSession(
                                        quiz = quiz,
                                        attemptNumber = latestSubmitted.attemptNumber,
                                        existingAttempt = latestSubmitted
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("view_latest_attempt_${quiz.id}")
                            ) {
                                Text(
                                    if (quiz.closed && quiz.showDiscussion && quiz.resultsPublished) "Pembahasan" else "Lihat Jawaban"
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = "Riwayat Evaluasi", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text(
                    "Riwayat Hasil Evaluasi (${completedHistory.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (completedHistory.isEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        "Belum ada riwayat evaluasi yang dikumpulkan.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(completedHistory, key = { it.id }) { att ->
                val quiz = quizzes.firstOrNull { it.id == att.quizId }
                val isPublished = (quiz?.resultsPublished == true) || att.resultsPublished
                val isDiscussionOpen = isPublished && quiz?.closed == true && quiz.showDiscussion
                Card(
                    onClick = {
                        if (quiz != null) {
                            activeSession = ActiveQuizSession(quiz, att.attemptNumber, att)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.testTag("history_attempt_card_${att.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${quiz?.title ?: att.quizId} • Percobaan #${att.attemptNumber}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            StatusPill(
                                text = if (isPublished) "Skor: ${att.totalScore.toInt()}" else "Nilai Dilindungi",
                                containerColor = if (isPublished) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (isPublished) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        if (isPublished) {
                            Text(
                                buildString {
                                    append("PG: ${att.autoScore.toInt()} (${att.correctCount}/${att.totalMcqCount})")
                                    if (att.essayAnswers.isNotEmpty() || att.essayScore > 0.0) {
                                        append(" • Uraian: ${att.essayScore.toInt()}")
                                    }
                                    append(if (isDiscussionOpen) " • Pembahasan terbuka" else " • Pembahasan belum dibuka")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                "Jawaban sudah dikumpulkan. Nilai & kunci jawaban tetap dirahasiakan hingga dipublikasikan guru.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("Rekap Nilai Resmi dari Guru (${publishedGrades.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }

        if (publishedGrades.isEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        "Belum ada nilai resmi yang dipublikasikan oleh guru.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(publishedGrades, key = { it.id }) { g ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(g.sourceTitle, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text("Skor Akhir: ${g.finalScore.toInt()}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("Kategori: ${g.assessmentType}", style = MaterialTheme.typography.labelSmall)
                        g.feedback?.takeIf { it.isNotBlank() }?.let {
                            Text("Umpan balik guru: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("Penilaian Diri & Refleksi Pembelajaran", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = reflectionModuleId == ParijotoCurriculumPart1.modul1.id,
                    onClick = { reflectionModuleId = ParijotoCurriculumPart1.modul1.id },
                    label = { Text("Refleksi Modul 1") },
                    modifier = Modifier.weight(1f).height(48.dp)
                )
                FilterChip(
                    selected = reflectionModuleId == ParijotoCurriculumPart1.modul2.id,
                    onClick = { reflectionModuleId = ParijotoCurriculumPart1.modul2.id },
                    label = { Text("Refleksi Modul 2") },
                    modifier = Modifier.weight(1f).height(48.dp)
                )
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    checklist.forEachIndexed { idx, statement ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = checklistState[idx] == true,
                                onCheckedChange = { checklistState[idx] = it }
                            )
                            Text(statement, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        }
                    }
                    OutlinedTextField(
                        value = reflectionNotes,
                        onValueChange = { reflectionNotes = it },
                        label = { Text("Hal yang masih perlu dipelajari ulang") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pblReflection,
                        onValueChange = { pblReflection = it },
                        label = { Text("Refleksi Pengalaman Kolaborasi PBL") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val listAnswers = checklist.indices.map { checklistState[it] == true }
                            onSubmitReflection(reflectionModuleId, listAnswers, reflectionNotes, pblReflection)
                        },
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_reflection_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Simpan Refleksi")
                        Spacer(Modifier.width(6.dp))
                        Text("Simpan Refleksi Diri")
                    }
                }
            }
        }
    }
}

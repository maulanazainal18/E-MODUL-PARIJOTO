package com.example.ui.student

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.curriculum.ParijotoModuleOverviews
import com.example.data.model.Enrollment
import com.example.data.model.LearningModule
import com.example.data.model.Lesson
import com.example.data.model.Question
import com.example.data.model.Quiz
import com.example.data.model.QuizAnswerKey
import com.example.data.model.QuizAttempt
import com.example.data.model.Reflection
import com.example.data.model.StudentLearningProgress
import com.example.ui.components.ModuleHeroArtwork
import com.example.ui.components.StatusPill

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentModulesTab(
    studentUid: String = "local_student",
    modules: List<LearningModule>,
    lessons: List<Lesson>,
    enrollment: Enrollment?,
    learningProgress: StudentLearningProgress,
    downloadedModules: Set<String>,
    quizzes: List<Quiz>,
    questions: List<Question>,
    attempts: List<QuizAttempt>,
    answerKeysCache: Map<String, QuizAnswerKey>,
    reflections: List<Reflection>,
    busy: Boolean,
    aiStreamText: String,
    aiLoading: Boolean,
    onToggleOffline: (String, Boolean) -> Unit,
    onMarkLessonComplete: (String, String) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onFontScaleChange: (Double) -> Unit,
    onRecordReadProgress: (String, String, Int) -> Unit,
    onLoadAnswerKey: (Quiz) -> Unit,
    onAutosaveQuiz: (Quiz, Int, List<String>, Int, Int, Map<String, String>, Map<String, String>, Boolean) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onSubmitQuiz: (Quiz, List<Question>, Int, List<String>, Int, Map<String, String>, Map<String, String>) -> Unit,
    onSubmitReflection: (String, List<Boolean>, String?, String?) -> Unit,
    onAskAiTutor: (String, String) -> Unit,
    onOpenGlossary: () -> Unit
) {
    var selectedModuleId by remember { mutableStateOf(modules.firstOrNull()?.id ?: "modul_1_etnosains") }
    var activeLesson by remember { mutableStateOf<Lesson?>(null) }
    var activeModuleQuiz by remember { mutableStateOf<Quiz?>(null) }
    var sectionIndex by remember { mutableIntStateOf(0) } // 0: Daftar Isi, 1: Identitas & Peta, 2: Kasus & Ringkas, 3: Kuis & Refleksi
    var filterMode by remember { mutableStateOf("ALL") } // ALL, BOOKMARK, UNREAD
    var searchAllModules by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val completedSet = remember(enrollment?.completedLessons, learningProgress.completedLessons) {
        (enrollment?.completedLessons.orEmpty() + learningProgress.completedLessons).toSet()
    }
    val bookmarkedSet = remember(learningProgress.bookmarkedLessons) {
        learningProgress.bookmarkedLessons.toSet()
    }

    if (activeModuleQuiz != null) {
        val quiz = quizzes.firstOrNull { it.id == activeModuleQuiz!!.id } ?: activeModuleQuiz!!
        val quizAttempts = attempts.filter { it.quizId == quiz.id }.sortedByDescending { it.attemptNumber }
        val inProgress = quizAttempts.firstOrNull { it.status == "IN_PROGRESS" }
        val existingAttempt = inProgress ?: quizAttempts.firstOrNull()
        val attemptNum = existingAttempt?.attemptNumber ?: 1
        LaunchedEffect(quiz.id, quiz.closed, quiz.showDiscussion, quiz.resultsPublished) {
            if (quiz.closed && quiz.showDiscussion && quiz.resultsPublished) {
                onLoadAnswerKey(quiz)
            }
        }
        QuizRunnerScreen(
            quiz = quiz,
            questions = questions,
            studentUid = studentUid,
            attemptNumber = attemptNum,
            existingAttempt = existingAttempt,
            answerKey = answerKeysCache[quiz.id],
            busy = busy,
            onBack = { activeModuleQuiz = null },
            onAutosave = { order, mcqCount, remSec, mcqs, essays, showToast ->
                onAutosaveQuiz(quiz, attemptNum, order, mcqCount, remSec, mcqs, essays, showToast)
            },
            onSubmit = { selectedQs, order, remSec, mcqs, essays ->
                onSubmitQuiz(quiz, selectedQs, attemptNum, order, remSec, mcqs, essays)
            }
        )
        return
    }

    if (activeLesson != null) {
        val currentLesson = activeLesson!!
        BackHandler { activeLesson = null }
        val sameModuleLessons = lessons.filter { it.moduleId == currentLesson.moduleId }.sortedBy { it.orderIndex }
        LessonReaderScreen(
            lesson = currentLesson,
            moduleLessons = sameModuleLessons,
            completedLessons = completedSet,
            bookmarkedLessons = bookmarkedSet,
            fontScale = learningProgress.readerFontScale.toFloat().coerceIn(0.85f, 1.4f),
            savedScrollPercent = learningProgress.lessonReadProgress[currentLesson.id] ?: 10,
            aiStreamText = aiStreamText,
            aiLoading = aiLoading,
            onBack = { activeLesson = null },
            onSelectLesson = { next ->
                selectedModuleId = next.moduleId
                activeLesson = next
            },
            onToggleBookmark = onToggleBookmark,
            onFontScaleChange = onFontScaleChange,
            onRecordReadProgress = onRecordReadProgress,
            onMarkComplete = { onMarkLessonComplete(currentLesson.id, currentLesson.moduleId) },
            onAskAiTutor = onAskAiTutor
        )
        return
    }

    val selectedModule = modules.firstOrNull { it.id == selectedModuleId } ?: modules.firstOrNull()
    val moduleLessons = lessons.filter { it.moduleId == selectedModuleId }.sortedBy { it.orderIndex }
    val moduleCompletedCount = moduleLessons.count { completedSet.contains(it.id) }
    val moduleProgressFraction = if (moduleLessons.isEmpty()) 0f else moduleCompletedCount.toFloat() / moduleLessons.size
    val overview = ParijotoModuleOverviews.forModule(selectedModuleId)
    val isOfflineSaved = downloadedModules.contains(selectedModuleId)
    val resumeLesson = learningProgress.lastLessonByModule[selectedModuleId]?.let { lastId ->
        moduleLessons.firstOrNull { it.id == lastId }
    }

    val displayedLessons = remember(lessons, selectedModuleId, searchQuery, filterMode, searchAllModules, bookmarkedSet, completedSet) {
        lessons.filter { les ->
            val matchModule = searchAllModules || les.moduleId == selectedModuleId
            val matchSearch = searchQuery.isBlank() ||
                les.title.contains(searchQuery, ignoreCase = true) ||
                les.learningObjective.contains(searchQuery, ignoreCase = true) ||
                les.explanation.contains(searchQuery, ignoreCase = true) ||
                les.summary.contains(searchQuery, ignoreCase = true)
            val matchFilter = when (filterMode) {
                "BOOKMARK" -> bookmarkedSet.contains(les.id)
                "UNREAD" -> !completedSet.contains(les.id)
                else -> true
            }
            matchModule && matchSearch && matchFilter
        }.sortedWith(compareBy({ it.moduleId }, { it.orderIndex }))
    }

    val moduleQuiz = quizzes.firstOrNull { it.id == overview.quizId } ?: quizzes.firstOrNull { it.moduleId == selectedModuleId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                modules.forEach { mod ->
                    FilterChip(
                        selected = selectedModuleId == mod.id,
                        onClick = { selectedModuleId = mod.id },
                        label = { Text("Modul ${mod.moduleNumber}") },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("select_module_${mod.moduleNumber}")
                    )
                }
            }
        }

        if (selectedModule != null) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModuleHeroArtwork(moduleNumber = selectedModule.moduleNumber)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(
                                text = selectedModule.meetingLabel,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "$moduleCompletedCount/${moduleLessons.size} Subbab Selesai",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(selectedModule.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(selectedModule.subtitle, style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(
                            progress = { moduleProgressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp)
                        )
                        if (resumeLesson != null) {
                            Button(
                                onClick = { activeLesson = resumeLesson },
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("resume_last_lesson_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Lanjutkan Membaca")
                                Spacer(Modifier.width(6.dp))
                                Text("Lanjutkan: ${resumeLesson.title}", maxLines = 1)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onToggleOffline(selectedModule.id, isOfflineSaved) },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("toggle_offline_module")
                            ) {
                                Icon(
                                    if (isOfflineSaved) Icons.Default.CloudDone else Icons.Default.CloudDownload,
                                    contentDescription = "Mode Offline"
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (isOfflineSaved) "Offline Aktif" else "Simpan Offline", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = onOpenGlossary,
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Glosarium", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Section tabs inside each Module
        item {
            val sectionLabels = listOf("Daftar Isi", "Identitas", "Studi Kasus", "Kuis")
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(sectionLabels) { idx, label ->
                    FilterChip(
                        selected = sectionIndex == idx,
                        onClick = { sectionIndex = idx },
                        label = { Text(label, maxLines = 1) },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("module_section_tab_$idx")
                    )
                }
            }
        }

        when (sectionIndex) {
            0 -> {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari materi (β-sitosterol, epifit, totipotensi)...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari Materi") },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Bersihkan pencarian")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("module_search_input")
                    )
                }

                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = filterMode == "ALL",
                            onClick = { filterMode = "ALL" },
                            label = { Text("Semua (${moduleLessons.size})") },
                            modifier = Modifier.height(48.dp).testTag("filter_lessons_all")
                        )
                        FilterChip(
                            selected = filterMode == "BOOKMARK",
                            onClick = { filterMode = "BOOKMARK" },
                            label = { Text("Bookmark (${bookmarkedSet.size})") },
                            modifier = Modifier.height(48.dp).testTag("filter_lessons_bookmark")
                        )
                        FilterChip(
                            selected = searchAllModules,
                            onClick = { searchAllModules = !searchAllModules },
                            label = { Text(if (searchAllModules) "2 Modul" else "Modul Ini") },
                            modifier = Modifier.height(48.dp).testTag("toggle_search_all_modules")
                        )
                    }
                }

                if (displayedLessons.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = "Belum ada subbab yang cocok dengan filter atau kata kunci pencarian.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(displayedLessons, key = { it.id }) { lesson ->
                        val done = completedSet.contains(lesson.id)
                        val bookmarked = bookmarkedSet.contains(lesson.id)
                        val readPct = learningProgress.lessonReadProgress[lesson.id] ?: 0
                        Card(
                            onClick = { activeLesson = lesson },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.testTag("lesson_card_${lesson.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = if (done) "Selesai" else "Belum selesai",
                                    tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(lesson.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        lesson.learningObjective,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                    if (readPct > 0 || done) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = if (done) "Selesai dipelajari" else "Progres baca $readPct%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onToggleBookmark(lesson.id) },
                                    modifier = Modifier.size(48.dp).testTag("bookmark_toggle_${lesson.id}")
                                ) {
                                    Icon(
                                        if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = if (bookmarked) "Hapus Bookmark" else "Bookmark Subbab",
                                        tint = if (bookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                if (selectedModule != null) {
                    item {
                        ModuleIdentityAndConceptSection(module = selectedModule, overview = overview)
                    }
                }
            }

            2 -> {
                item {
                    ModuleCaseStudyAndSummarySection(overview = overview)
                }
            }

            else -> {
                item {
                    val qCount = if (moduleQuiz != null) questions.count { it.quizId == moduleQuiz.id } else 0
                    val attempt = if (moduleQuiz != null) attempts.firstOrNull { it.quizId == moduleQuiz.id } else null
                    val existingRef = reflections.firstOrNull { it.moduleId == selectedModuleId }
                    ModuleQuizAndReflectionSection(
                        moduleId = selectedModuleId,
                        moduleQuiz = moduleQuiz,
                        questionCount = qCount,
                        attempt = attempt,
                        existingReflection = existingRef,
                        busy = busy,
                        onStartQuiz = { q -> activeModuleQuiz = q },
                        onSubmitReflection = onSubmitReflection
                    )
                }
            }
        }
    }
}

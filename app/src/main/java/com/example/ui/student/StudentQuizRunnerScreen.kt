package com.example.ui.student

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import com.example.data.curriculum.QuizEngineUtils
import com.example.data.model.Question
import com.example.data.model.Quiz
import com.example.data.model.QuizAnswerKey
import com.example.data.model.QuizAttempt
import com.example.ui.components.StatusPill
import kotlinx.coroutines.delay

@Composable
internal fun QuizRunnerScreen(
    quiz: Quiz,
    questions: List<Question>,
    studentUid: String = "local_student",
    attemptNumber: Int = 1,
    existingAttempt: QuizAttempt?,
    answerKey: QuizAnswerKey?,
    busy: Boolean,
    onBack: () -> Unit,
    onAutosave: (List<String>, Int, Int, Map<String, String>, Map<String, String>, Boolean) -> Unit = { _, _, _, _, _, _ -> },
    onSubmit: (List<Question>, List<String>, Int, Map<String, String>, Map<String, String>) -> Unit
) {
    val isEditable = existingAttempt == null || existingAttempt.status == "IN_PROGRESS"

    val selectedQuestions = remember(
        quiz.id,
        studentUid,
        attemptNumber,
        existingAttempt?.questionOrder,
        questions.map { it.id }
    ) {
        QuizEngineUtils.selectQuizQuestions(
            quiz = quiz,
            allPublishedQuestions = questions,
            studentUid = studentUid,
            attemptNumber = attemptNumber,
            existingOrder = existingAttempt?.questionOrder.orEmpty()
        )
    }
    val questionOrder = remember(selectedQuestions) { selectedQuestions.map { it.id } }
    val totalMcqCount = remember(selectedQuestions) { selectedQuestions.count { it.type == "MULTIPLE_CHOICE" } }

    val mcqAnswers = remember(quiz.id, existingAttempt?.id, attemptNumber) {
        mutableStateMapOf<String, String>().apply {
            existingAttempt?.answers?.let { putAll(it) }
        }
    }
    val essayAnswers = remember(quiz.id, existingAttempt?.id, attemptNumber) {
        mutableStateMapOf<String, String>().apply {
            existingAttempt?.essayAnswers?.let { putAll(it) }
        }
    }

    var remainingSeconds by remember(quiz.id, existingAttempt?.id, attemptNumber) {
        mutableIntStateOf(
            existingAttempt?.remainingSeconds?.takeIf { it > 0 }
                ?: (quiz.durationMinutes.coerceAtLeast(1) * 60)
        )
    }
    var answersDirtyVersion by remember(quiz.id, attemptNumber) { mutableIntStateOf(0) }
    var autoSubmitted by remember(quiz.id, attemptNumber) { mutableStateOf(false) }

    // Countdown timer + periodic autosave every 30s + auto-submit on timeout
    LaunchedEffect(quiz.id, attemptNumber, isEditable) {
        if (!isEditable) return@LaunchedEffect
        while (remainingSeconds > 0 && !autoSubmitted) {
            delay(1000L)
            remainingSeconds = (remainingSeconds - 1).coerceAtLeast(0)
            if (remainingSeconds > 0 && remainingSeconds % 30 == 0) {
                onAutosave(
                    questionOrder,
                    totalMcqCount,
                    remainingSeconds,
                    mcqAnswers.toMap(),
                    essayAnswers.toMap(),
                    false
                )
            }
        }
        if (remainingSeconds == 0 && !autoSubmitted && selectedQuestions.isNotEmpty()) {
            autoSubmitted = true
            onSubmit(
                selectedQuestions,
                questionOrder,
                0,
                mcqAnswers.toMap(),
                essayAnswers.toMap()
            )
        }
    }

    // Debounced autosave whenever student selects an answer or types essay text
    LaunchedEffect(answersDirtyVersion, isEditable) {
        if (!isEditable || answersDirtyVersion == 0) return@LaunchedEffect
        delay(1200L)
        onAutosave(
            questionOrder,
            totalMcqCount,
            remainingSeconds,
            mcqAnswers.toMap(),
            essayAnswers.toMap(),
            false
        )
    }

    val isSubmitted = !isEditable
    val isResultPublished = quiz.resultsPublished || (existingAttempt?.resultsPublished == true)
    val canShowDiscussion = isSubmitted && isResultPublished && quiz.closed && quiz.showDiscussion
    val exitWithAutosave: () -> Unit = {
        if (isEditable && answersDirtyVersion > 0) {
            onAutosave(
                questionOrder,
                totalMcqCount,
                remainingSeconds,
                mcqAnswers.toMap(),
                essayAnswers.toMap(),
                false
            )
        }
        onBack()
    }

    BackHandler(onBack = exitWithAutosave)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("quiz_runner_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = exitWithAutosave, modifier = Modifier.testTag("quiz_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(quiz.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            text = QuizEngineUtils.quizCategoryLabel(quiz.category),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        StatusPill(
                            text = "Percobaan #$attemptNumber",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        if (isEditable) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (remainingSeconds <= 120) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = "Sisa Waktu",
                                tint = if (remainingSeconds <= 120) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Sisa Waktu: ${QuizEngineUtils.formatCountdownSeconds(remainingSeconds)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("quiz_countdown_timer")
                                )
                                Text(
                                    "${selectedQuestions.size} soal • Autosave aktif",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                onAutosave(
                                    questionOrder,
                                    totalMcqCount,
                                    remainingSeconds,
                                    mcqAnswers.toMap(),
                                    essayAnswers.toMap(),
                                    true
                                )
                            },
                            enabled = !busy,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("quiz_manual_autosave_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Simpan Draf")
                            Spacer(Modifier.width(4.dp))
                            Text("Simpan")
                        }
                    }
                }
            }

            if (quiz.shuffleQuestions || quiz.shuffleOptions) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "Acak",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            buildString {
                                append("Mode acak aktif: ")
                                val parts = mutableListOf<String>()
                                if (quiz.shuffleQuestions) parts.add("urutan soal")
                                if (quiz.shuffleOptions) parts.add("pilihan jawaban A–E")
                                append(parts.joinToString(" & "))
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (isSubmitted && !isResultPublished) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.testTag("quiz_unpublished_lock_banner")
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = "Nilai Terkunci")
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Jawaban Anda telah dikumpulkan. Nilai, kunci jawaban, dan pembahasan dilindungi hingga guru selesai menilai dan mempublikasikan hasil.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        } else if (isResultPublished && existingAttempt != null) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.testTag(
                        if (canShowDiscussion) "quiz_discussion_unlocked_banner" else "quiz_published_score_banner"
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Nilai Dipublikasikan", tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Nilai Akhir: ${existingAttempt.totalScore.toInt()} / 100",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (existingAttempt.totalMcqCount > 0) {
                            Text(
                                "Skor PG Otomatis: ${existingAttempt.autoScore.toInt()} (${existingAttempt.correctCount}/${existingAttempt.totalMcqCount} benar)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (existingAttempt.essayAnswers.isNotEmpty() || existingAttempt.essayScore > 0.0) {
                            Text(
                                "Skor Uraian Manual (Rubrik): ${existingAttempt.essayScore.toInt()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        existingAttempt.teacherFeedback?.takeIf { it.isNotBlank() }?.let { fb ->
                            Text("Umpan Balik Guru: $fb", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                        if (!canShowDiscussion) {
                            Text(
                                "Kunci jawaban & pembahasan belum dibuka oleh guru.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (selectedQuestions.isEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        "Belum ada soal yang dibuka oleh guru untuk evaluasi ini.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            itemsIndexed(selectedQuestions, key = { _, q -> q.id }) { idx, q ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.testTag("quiz_question_card_${q.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Soal ${idx + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (!q.stimulusText.isNullOrBlank()) {
                                    StatusPill(
                                        "Berbasis Kasus",
                                        MaterialTheme.colorScheme.tertiaryContainer,
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                                q.hotsCategory?.takeIf { it.isNotBlank() }?.let { cat ->
                                    StatusPill(
                                        cat,
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        q.stimulusText?.takeIf { it.isNotBlank() }?.let { stim ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(stim, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                            }
                        }

                        Text(q.prompt, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)

                        if (q.type == "MULTIPLE_CHOICE") {
                            val options = remember(quiz.id, q.id, studentUid, attemptNumber, quiz.shuffleOptions) {
                                QuizEngineUtils.getShuffledOptionsForQuestion(quiz, q, studentUid, attemptNumber)
                            }
                            options.forEach { opt ->
                                val selected = mcqAnswers[q.id] == opt.originalKey
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = isEditable) {
                                            mcqAnswers[q.id] = opt.originalKey
                                            answersDirtyVersion++
                                        }
                                        .padding(vertical = 2.dp)
                                        .testTag("mcq_${q.id}_opt_${opt.originalKey}")
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = if (isEditable) {
                                            {
                                                mcqAnswers[q.id] = opt.originalKey
                                                answersDirtyVersion++
                                            }
                                        } else {
                                            null
                                        },
                                        enabled = isEditable
                                    )
                                    Text(
                                        "${opt.displayLetter}. ${opt.text}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            if (canShowDiscussion && answerKey != null) {
                                val correctOriginal = answerKey.keys[q.id]
                                val matchedOpt = options.firstOrNull { it.originalKey == correctOriginal }
                                val expl = answerKey.explanations[q.id]
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.testTag("question_discussion_${q.id}")
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        if (!correctOriginal.isNullOrBlank()) {
                                            val keyLabel = matchedOpt?.let { "${it.displayLetter}. ${it.text}" } ?: correctOriginal
                                            Text(
                                                "Kunci Jawaban: $keyLabel",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (!expl.isNullOrBlank()) {
                                            Text("Pembahasan: $expl", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = essayAnswers[q.id].orEmpty(),
                                onValueChange = {
                                    if (isEditable) {
                                        essayAnswers[q.id] = it
                                        answersDirtyVersion++
                                    }
                                },
                                enabled = isEditable,
                                label = { Text("Jawaban Uraian Analitis Anda") },
                                minLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("essay_input_${q.id}")
                            )
                            if (isResultPublished && existingAttempt?.essayItemScores?.containsKey(q.id) == true) {
                                Text(
                                    "Skor Rubrik Guru: ${existingAttempt.essayItemScores[q.id]} / 4",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (canShowDiscussion && answerKey != null) {
                                answerKey.explanations[q.id]?.takeIf { it.isNotBlank() }?.let { guide ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        modifier = Modifier.testTag("question_discussion_${q.id}")
                                    ) {
                                        Text(
                                            "Pedoman Pembahasan Uraian: $guide",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isEditable && selectedQuestions.isNotEmpty()) {
            item {
                Button(
                    onClick = {
                        autoSubmitted = true
                        onSubmit(
                            selectedQuestions,
                            questionOrder,
                            remainingSeconds,
                            mcqAnswers.toMap(),
                            essayAnswers.toMap()
                        )
                    },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_quiz_attempt_button")
                ) {
                    Text("Kumpulkan Jawaban Evaluasi")
                }
            }
        }
    }
}

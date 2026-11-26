package com.example.ui.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.components.StatusPill

fun LazyListScope.teacherQuizAndBankItems(
    quizzes: List<Quiz>,
    questions: List<Question>,
    answerKeys: Map<String, QuizAnswerKey>,
    selectedQuizId: String,
    editingQuestion: Question?,
    busy: Boolean,
    onSelectQuiz: (String) -> Unit,
    onEditQuestion: (Question?) -> Unit,
    onRequestDeleteQuestion: (Question) -> Unit,
    onSaveQuizConfig: (Quiz) -> Unit,
    onSaveBankQuestion: (Quiz, Question, String?, String?) -> Unit
) {
    val activeQuiz = quizzes.firstOrNull { it.id == selectedQuizId } ?: quizzes.firstOrNull()

    if (quizzes.isNotEmpty()) {
        item(key = "quiz_category_selector") {
            LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quizzes, key = { it.id }) { quiz ->
                    FilterChip(
                        selected = activeQuiz?.id == quiz.id,
                        onClick = {
                            onSelectQuiz(quiz.id)
                            onEditQuestion(null)
                        },
                        label = { Text(QuizEngineUtils.quizCategoryLabel(quiz.category), maxLines = 1) },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("teacher_quiz_tab_${quiz.category}")
                    )
                }
            }
        }
    }

    if (activeQuiz != null) {
        val quizQuestions = questions.filter { it.quizId == activeQuiz.id }.sortedBy { it.orderIndex }

        item(key = "quiz_config_${activeQuiz.id}") {
            QuizConfigEditorCard(
                quiz = activeQuiz,
                availableQuestionsCount = quizQuestions.count { it.published },
                busy = busy,
                onSaveConfig = onSaveQuizConfig
            )
        }

        item(key = "bank_editor_${activeQuiz.id}_${editingQuestion?.id ?: "new"}") {
            BankQuestionEditorCard(
                quiz = activeQuiz,
                editingQuestion = editingQuestion,
                existingCount = quizQuestions.size,
                existingAnswerKey = answerKeys[activeQuiz.id],
                busy = busy,
                onCancelEdit = { onEditQuestion(null) },
                onSave = { savedQ, corrOpt, expl ->
                    onSaveBankQuestion(activeQuiz, savedQ, corrOpt, expl)
                    onEditQuestion(null)
                }
            )
        }

        item(key = "bank_header_${activeQuiz.id}") {
            Text(
                "Bank Soal ${QuizEngineUtils.quizCategoryLabel(activeQuiz.category)} (${quizQuestions.size} butir)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(quizQuestions, key = { it.id }) { q ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.testTag("bank_question_card_${q.id}")
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            StatusPill(
                                if (q.type == "ESSAY") "Uraian" else "PG A–E",
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (!q.stimulusText.isNullOrBlank()) {
                                StatusPill(
                                    "Kasus",
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            StatusPill(
                                if (q.published) "Dibuka" else "Draf",
                                if (q.published) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                                if (q.published) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onEditQuestion(q) },
                                modifier = Modifier.size(48.dp).testTag("teacher_edit_question_${q.id}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Soal")
                            }
                            if (q.ownerUid != "SYSTEM_DEFAULT") {
                                IconButton(
                                    onClick = { onRequestDeleteQuestion(q) },
                                    modifier = Modifier.size(48.dp).testTag("teacher_delete_question_${q.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus Soal", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    Text(
                        "${q.orderIndex}. ${q.prompt}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    val keyLetter = answerKeys[activeQuiz.id]?.keys?.get(q.id)
                    Text(
                        buildString {
                            append("Kategori: ${q.hotsCategory ?: "Umum"} • Bobot: ${q.maxPoints}")
                            if (q.type == "MULTIPLE_CHOICE" && !keyLetter.isNullOrBlank()) {
                                append(" • Kunci: $keyLetter")
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteBankQuestionDialog(
    target: Question?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    target ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hapus Butir Soal?") },
        text = { Text("Soal '${target.prompt.take(60)}...' akan dihapus permanen dari Bank Soal.") },
        confirmButton = {
            Button(
                onClick = { onConfirm(target.id) },
                modifier = Modifier.testTag("dialog_confirm_button")
            ) {
                Text("Hapus")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun QuizConfigEditorCard(
    quiz: Quiz,
    availableQuestionsCount: Int,
    busy: Boolean,
    onSaveConfig: (Quiz) -> Unit
) {
    var openForStudents by remember(quiz.id, quiz.openForStudents) { mutableStateOf(quiz.openForStudents) }
    var scheduleLabel by remember(quiz.id, quiz.scheduleLabel) { mutableStateOf(quiz.scheduleLabel) }
    var durationText by remember(quiz.id, quiz.durationMinutes) { mutableStateOf(quiz.durationMinutes.toString()) }
    var countText by remember(quiz.id, quiz.questionCount) { mutableStateOf(quiz.questionCount.toString()) }
    var maxAttemptsText by remember(quiz.id, quiz.maxAttempts) { mutableStateOf(quiz.maxAttempts.toString()) }
    var shuffleQuestions by remember(quiz.id, quiz.shuffleQuestions) { mutableStateOf(quiz.shuffleQuestions) }
    var shuffleOptions by remember(quiz.id, quiz.shuffleOptions) { mutableStateOf(quiz.shuffleOptions) }
    var closed by remember(quiz.id, quiz.closed) { mutableStateOf(quiz.closed) }
    var resultsPublished by remember(quiz.id, quiz.resultsPublished) { mutableStateOf(quiz.resultsPublished) }
    var showDiscussion by remember(quiz.id, quiz.showDiscussion) { mutableStateOf(quiz.showDiscussion) }

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Quiz, contentDescription = "Pengaturan Evaluasi", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(quiz.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                StatusPill("$availableQuestionsCount Soal Aktif", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
            }

            OutlinedTextField(
                value = scheduleLabel,
                onValueChange = { scheduleLabel = it },
                label = { Text("Jadwal / Keterangan Sesi") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("quiz_schedule_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Durasi (mnt)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("quiz_duration_input")
                )
                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Jml Soal") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("quiz_count_input")
                )
                OutlinedTextField(
                    value = maxAttemptsText,
                    onValueChange = { maxAttemptsText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Maks Coba") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("quiz_max_attempts_input")
                )
            }

            ConfigSwitchRow("Buka Akses Pengerjaan Siswa", openForStudents, "quiz_open_switch") {
                openForStudents = it
            }
            ConfigSwitchRow("Acak Urutan Soal per Siswa", shuffleQuestions, "quiz_shuffle_q_switch") {
                shuffleQuestions = it
            }
            ConfigSwitchRow("Acak Pilihan Jawaban A–E", shuffleOptions, "quiz_shuffle_opt_switch") {
                shuffleOptions = it
            }
            ConfigSwitchRow("Tutup Sesi Pengerjaan Evaluasi", closed, "quiz_closed_switch") {
                closed = it
                if (!it) showDiscussion = false
            }
            ConfigSwitchRow("Publikasikan Nilai & Hasil ke Siswa", resultsPublished, "quiz_publish_results_switch") {
                resultsPublished = it
                if (!it) showDiscussion = false
            }
            ConfigSwitchRow("Buka Kunci & Pembahasan Soal", showDiscussion, "quiz_discussion_switch") {
                showDiscussion = it
                if (it) {
                    closed = true
                    resultsPublished = true
                }
            }

            Button(
                onClick = {
                    onSaveConfig(
                        quiz.copy(
                            openForStudents = openForStudents,
                            scheduleLabel = scheduleLabel.ifBlank { "Fleksibel" },
                            durationMinutes = durationText.toIntOrNull()?.coerceIn(5, 180) ?: quiz.durationMinutes,
                            questionCount = countText.toIntOrNull()?.coerceIn(1, 100) ?: quiz.questionCount,
                            maxAttempts = maxAttemptsText.toIntOrNull()?.coerceIn(1, 10) ?: quiz.maxAttempts,
                            shuffleQuestions = shuffleQuestions,
                            shuffleOptions = shuffleOptions,
                            closed = closed,
                            resultsPublished = resultsPublished,
                            showDiscussion = showDiscussion && closed && resultsPublished
                        )
                    )
                },
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("teacher_save_quiz_config_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = "Simpan Pengaturan")
                Spacer(Modifier.width(6.dp))
                Text("Simpan Pengaturan Evaluasi")
            }
        }
    }
}

@Composable
private fun ConfigSwitchRow(
    label: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.testTag(tag))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BankQuestionEditorCard(
    quiz: Quiz,
    editingQuestion: Question?,
    existingCount: Int,
    existingAnswerKey: QuizAnswerKey?,
    busy: Boolean,
    onCancelEdit: () -> Unit,
    onSave: (Question, String?, String?) -> Unit
) {
    var qType by remember(editingQuestion?.id, quiz.id) { mutableStateOf(editingQuestion?.type ?: "MULTIPLE_CHOICE") }
    var hotsTag by remember(editingQuestion?.id, quiz.id) { mutableStateOf(editingQuestion?.hotsCategory ?: "HOTS Analisis") }
    var stimulus by remember(editingQuestion?.id, quiz.id) { mutableStateOf(editingQuestion?.stimulusText.orEmpty()) }
    var prompt by remember(editingQuestion?.id, quiz.id) { mutableStateOf(editingQuestion?.prompt.orEmpty()) }
    var optA by remember(editingQuestion?.id, quiz.id) { mutableStateOf(editingQuestion?.optionA.orEmpty()) }
    var optB by remember(editingQuestion?.id, quiz.id) { mutableStateOf(editingQuestion?.optionB.orEmpty()) }
    var optC by remember(editingQuestion?.id, quiz.id) { mutableStateOf(editingQuestion?.optionC.orEmpty()) }
    var optD by remember(editingQuestion?.id, quiz.id) { mutableStateOf(editingQuestion?.optionD.orEmpty()) }
    var optE by remember(editingQuestion?.id, quiz.id) { mutableStateOf(editingQuestion?.optionE.orEmpty()) }
    var correct by remember(editingQuestion?.id, quiz.id) {
        mutableStateOf(editingQuestion?.id?.let { existingAnswerKey?.keys?.get(it) } ?: "A")
    }
    var expl by remember(editingQuestion?.id, quiz.id) {
        mutableStateOf(editingQuestion?.id?.let { existingAnswerKey?.explanations?.get(it) }.orEmpty())
    }
    var pointsText by remember(editingQuestion?.id, quiz.id) {
        mutableStateOf((editingQuestion?.maxPoints ?: if (qType == "ESSAY") 50 else 20).toString())
    }
    var qPublished by remember(editingQuestion?.id, quiz.id) { mutableStateOf(editingQuestion?.published ?: true) }

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (editingQuestion == null) "Tambah Butir ke Bank Soal" else "Edit Soal #${editingQuestion.orderIndex}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (editingQuestion != null) {
                    TextButton(onClick = onCancelEdit) { Text("Batal Edit") }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = qType == "MULTIPLE_CHOICE",
                    onClick = { qType = "MULTIPLE_CHOICE" },
                    label = { Text("Pilihan Ganda (A–E)") },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("bank_type_mcq")
                )
                FilterChip(
                    selected = qType == "ESSAY",
                    onClick = { qType = "ESSAY" },
                    label = { Text("Soal Uraian") },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("bank_type_essay")
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = hotsTag,
                    onValueChange = { hotsTag = it },
                    label = { Text("Kategori (HOTS / Kasus / Etnosains)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("bank_hots_tag_input")
                )
                OutlinedTextField(
                    value = pointsText,
                    onValueChange = { pointsText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Bobot") },
                    singleLine = true,
                    modifier = Modifier.width(88.dp)
                )
            }

            OutlinedTextField(
                value = stimulus,
                onValueChange = { stimulus = it },
                label = { Text("Wacana / Studi Kasus (Opsional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().testTag("bank_stimulus_input")
            )
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Teks Pertanyaan") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().testTag("bank_prompt_input")
            )

            if (qType == "MULTIPLE_CHOICE") {
                OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("Opsi A") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("bank_opt_a"))
                OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("Opsi B") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("bank_opt_b"))
                OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("Opsi C") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("bank_opt_c"))
                OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("Opsi D") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("bank_opt_d"))
                OutlinedTextField(value = optE, onValueChange = { optE = it }, label = { Text("Opsi E") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("bank_opt_e"))
                Text("Kunci Jawaban Benar:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("A", "B", "C", "D", "E").forEach { l ->
                        FilterChip(
                            selected = correct == l,
                            onClick = { correct = l },
                            label = { Text(l) },
                            modifier = Modifier.height(48.dp).testTag("bank_key_$l")
                        )
                    }
                }
            }

            OutlinedTextField(
                value = expl,
                onValueChange = { expl = it },
                label = { Text(if (qType == "ESSAY") "Pedoman Penilaian / Pembahasan Uraian" else "Pembahasan Soal") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().testTag("bank_explanation_input")
            )

            ConfigSwitchRow("Buka Soal Ini untuk Siswa (Nonaktif = Draf)", qPublished, "bank_question_published_switch") {
                qPublished = it
            }

            val canSubmit = !busy && prompt.isNotBlank() && (qType == "ESSAY" || (optA.isNotBlank() && optB.isNotBlank()))
            Button(
                onClick = {
                    val base = editingQuestion
                    val orderIdx = base?.orderIndex ?: (existingCount + 1).coerceAtMost(100)
                    onSave(
                        (base ?: Question()).copy(
                            quizId = quiz.id,
                            orderIndex = orderIdx,
                            type = qType,
                            prompt = prompt,
                            stimulusText = stimulus.takeIf { it.isNotBlank() },
                            optionA = if (qType == "MULTIPLE_CHOICE") optA else null,
                            optionB = if (qType == "MULTIPLE_CHOICE") optB else null,
                            optionC = if (qType == "MULTIPLE_CHOICE") optC else null,
                            optionD = if (qType == "MULTIPLE_CHOICE") optD else null,
                            optionE = if (qType == "MULTIPLE_CHOICE") optE else null,
                            hotsCategory = hotsTag.ifBlank { if (stimulus.isNotBlank()) "Soal Kasus" else "HOTS" },
                            maxPoints = pointsText.toIntOrNull()?.coerceIn(1, 100) ?: if (qType == "ESSAY") 50 else 20,
                            published = qPublished
                        ),
                        if (qType == "MULTIPLE_CHOICE") correct else null,
                        expl.takeIf { it.isNotBlank() }
                    )
                    if (editingQuestion == null) {
                        prompt = ""; stimulus = ""; optA = ""; optB = ""; optC = ""; optD = ""; optE = ""; expl = ""
                    }
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("teacher_save_bank_question_button")
            ) {
                Icon(if (editingQuestion == null) Icons.Default.Add else Icons.Default.Save, contentDescription = "Simpan Soal")
                Spacer(Modifier.width(6.dp))
                Text(if (editingQuestion == null) "Tambahkan ke Bank Soal" else "Perbarui Butir Soal")
            }
        }
    }
}

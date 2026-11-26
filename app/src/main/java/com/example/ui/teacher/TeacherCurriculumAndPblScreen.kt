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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.example.data.model.Assignment
import com.example.data.model.ClassRoom
import com.example.data.model.Enrollment
import com.example.data.model.GroupDiscussionMessage
import com.example.data.model.LearningModule
import com.example.data.model.Lesson
import com.example.data.model.PblGroup
import com.example.data.model.Question
import com.example.data.model.Quiz
import com.example.data.model.QuizAnswerKey
import com.example.ui.components.StatusPill

@Composable
fun TeacherCurriculumTab(
    classes: List<ClassRoom>,
    enrollments: List<Enrollment> = emptyList(),
    modules: List<LearningModule>,
    lessons: List<Lesson>,
    groups: List<PblGroup>,
    groupDiscussions: List<GroupDiscussionMessage> = emptyList(),
    assignments: List<Assignment>,
    quizzes: List<Quiz>,
    questions: List<Question>,
    answerKeys: Map<String, QuizAnswerKey> = emptyMap(),
    busy: Boolean,
    onSaveModule: (LearningModule) -> Unit,
    onSaveLesson: (Lesson) -> Unit,
    onDeleteLesson: (String) -> Unit = {},
    onCreateGroup: (String, String, String) -> Unit,
    onObserveGroupDiscussion: (String?, String?) -> Unit = { _, _ -> },
    onAssignGroupMembers: (PblGroup, String, List<String>, List<String>, Map<String, String>) -> Unit = { _, _, _, _, _ -> },
    onSaveGroupComment: (String, String) -> Unit = { _, _ -> },
    onSendGroupDiscussion: (PblGroup, Int, String) -> Unit = { _, _, _ -> },
    onCreateAssignment: (String, String, Int, String, String, String, Int) -> Unit,
    onCreateQuestion: (Quiz, Int, String, String, String?, String?, String?, String?, String?, String?, String?, String?) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onToggleQuizEvaluation: (Quiz, Boolean, Boolean) -> Unit = { _, _, _ -> },
    onEnsureAnswerKeys: () -> Unit = {},
    onSaveQuizConfig: (Quiz) -> Unit = {},
    onSaveBankQuestion: (Quiz, Question, String?, String?) -> Unit = { _, _, _, _ -> },
    onDeleteBankQuestion: (String) -> Unit = {}
) {
    var section by remember { mutableIntStateOf(0) }
    var editingLesson by remember { mutableStateOf<Lesson?>(null) }
    var pendingDeleteLesson by remember { mutableStateOf<Lesson?>(null) }
    var lessonFilterModId by remember(modules) { mutableStateOf(modules.firstOrNull()?.id ?: "modul_1_etnosains") }
    var selectedQuizId by remember(quizzes) { mutableStateOf(quizzes.firstOrNull()?.id ?: "quiz_modul_1") }
    var editingQuestion by remember { mutableStateOf<Question?>(null) }
    var pendingDeleteQuestion by remember { mutableStateOf<Question?>(null) }

    LaunchedEffect(section) {
        if (section == 2) onEnsureAnswerKeys()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("teacher_curriculum_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(selected = section == 0, onClick = { section = 0 }, label = { Text("Materi", maxLines = 1) }, modifier = Modifier.height(48.dp))
                }
                item {
                    FilterChip(selected = section == 1, onClick = { section = 1 }, label = { Text("PBL & LKM", maxLines = 1) }, modifier = Modifier.height(48.dp))
                }
                item {
                    FilterChip(selected = section == 2, onClick = { section = 2 }, label = { Text("Kuis & Soal", maxLines = 1) }, modifier = Modifier.height(48.dp))
                }
            }
        }

        when (section) {
            0 -> {
                item {
                    val firstMod = modules.firstOrNull()
                    var editModId by remember(modules) { mutableStateOf(firstMod?.id ?: "modul_1_etnosains") }
                    val activeMod = modules.firstOrNull { it.id == editModId } ?: firstMod
                    var meetingLabel by remember(activeMod?.id) { mutableStateOf(activeMod?.meetingLabel ?: "Pertemuan 1") }
                    var durationText by remember(activeMod?.id) { mutableStateOf((activeMod?.durationMinutes ?: 90).toString()) }
                    var modTitle by remember(activeMod?.id) { mutableStateOf(activeMod?.title ?: "") }
                    var modSubtitle by remember(activeMod?.id) { mutableStateOf(activeMod?.subtitle ?: "") }
                    var modDesc by remember(activeMod?.id) { mutableStateOf(activeMod?.description.orEmpty()) }
                    var modPublished by remember(activeMod?.id) { mutableStateOf(activeMod?.published ?: true) }

                    if (activeMod != null) {
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Kelola Pertemuan Pembelajaran", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    modules.forEach { m ->
                                        FilterChip(
                                            selected = editModId == m.id,
                                            onClick = { editModId = m.id },
                                            label = { Text("Pertemuan ${m.moduleNumber}") },
                                            modifier = Modifier.height(48.dp)
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = meetingLabel,
                                        onValueChange = { meetingLabel = it },
                                        label = { Text("Label Pertemuan") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("teacher_meeting_label_input")
                                    )
                                    OutlinedTextField(
                                        value = durationText,
                                        onValueChange = { durationText = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Durasi (Menit)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(0.7f)
                                    )
                                }
                                OutlinedTextField(value = modTitle, onValueChange = { modTitle = it }, label = { Text("Judul Pertemuan / Modul") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = modSubtitle, onValueChange = { modSubtitle = it }, label = { Text("Fokus Materi / Subjudul") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = modDesc, onValueChange = { modDesc = it }, label = { Text("Deskripsi / Agenda Pertemuan") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Tampilkan Pertemuan ke Siswa", style = MaterialTheme.typography.bodySmall)
                                    Switch(checked = modPublished, onCheckedChange = { modPublished = it })
                                }
                                Button(
                                    onClick = {
                                        onSaveModule(
                                            activeMod.copy(
                                                meetingLabel = meetingLabel,
                                                durationMinutes = durationText.toIntOrNull()?.coerceIn(10, 360) ?: 90,
                                                title = modTitle,
                                                subtitle = modSubtitle,
                                                description = modDesc,
                                                published = modPublished
                                            )
                                        )
                                    },
                                    enabled = !busy && modTitle.trim().length >= 2 && meetingLabel.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("teacher_save_module_button")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Simpan Pertemuan")
                                    Spacer(Modifier.width(6.dp))
                                    Text("Simpan Pengaturan Pertemuan")
                                }
                            }
                        }
                    }
                }
                item {
                    val editTarget = editingLesson
                    var selectedModId by remember(editTarget?.id) {
                        mutableStateOf(editTarget?.moduleId ?: lessonFilterModId)
                    }
                    var newTitle by remember(editTarget?.id) { mutableStateOf(editTarget?.title.orEmpty()) }
                    var newObj by remember(editTarget?.id) { mutableStateOf(editTarget?.learningObjective.orEmpty()) }
                    var newExp by remember(editTarget?.id) { mutableStateOf(editTarget?.explanation.orEmpty()) }
                    var newCase by remember(editTarget?.id) { mutableStateOf(editTarget?.dailyExample.orEmpty()) }
                    var newSpark by remember(editTarget?.id) { mutableStateOf(editTarget?.sparkQuestion.orEmpty()) }
                    var newSum by remember(editTarget?.id) { mutableStateOf(editTarget?.summary.orEmpty()) }
                    var newPublished by remember(editTarget?.id) { mutableStateOf(editTarget?.published ?: true) }

                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (editTarget == null) "Tambah Materi Baru" else "Edit Subbab: ${editTarget.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (editTarget != null) {
                                    TextButton(onClick = { editingLesson = null }) {
                                        Text("Batal Edit")
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                modules.forEach { m ->
                                    FilterChip(
                                        selected = selectedModId == m.id,
                                        onClick = { selectedModId = m.id },
                                        label = { Text("Modul ${m.moduleNumber}") },
                                        modifier = Modifier.height(48.dp)
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = newTitle,
                                onValueChange = { newTitle = it },
                                label = { Text("Judul Subtopik") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("teacher_lesson_title_input")
                            )
                            OutlinedTextField(
                                value = newObj,
                                onValueChange = { newObj = it },
                                label = { Text("Tujuan Pembelajaran") },
                                modifier = Modifier.fillMaxWidth().testTag("teacher_lesson_obj_input")
                            )
                            OutlinedTextField(
                                value = newExp,
                                onValueChange = { newExp = it },
                                label = { Text("Penjelasan Ilmiah Lengkap") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth().testTag("teacher_lesson_exp_input")
                            )
                            OutlinedTextField(
                                value = newCase,
                                onValueChange = { newCase = it },
                                label = { Text("Studi Kasus / Contoh Kontekstual (Opsional)") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newSpark,
                                onValueChange = { newSpark = it },
                                label = { Text("Pertanyaan Pemantik (Opsional)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newSum,
                                onValueChange = { newSum = it },
                                label = { Text("Rangkuman Subbab") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Publikasikan Subbab ke Siswa", style = MaterialTheme.typography.bodySmall)
                                Switch(
                                    checked = newPublished,
                                    onCheckedChange = { newPublished = it },
                                    modifier = Modifier.testTag("teacher_lesson_published_switch")
                                )
                            }
                            Button(
                                onClick = {
                                    val base = editTarget
                                    val nextOrder = base?.orderIndex ?: (lessons.count { it.moduleId == selectedModId } + 1).coerceAtMost(100)
                                    onSaveLesson(
                                        (base ?: Lesson()).copy(
                                            moduleId = selectedModId,
                                            orderIndex = nextOrder,
                                            title = newTitle,
                                            learningObjective = newObj.ifBlank { "Memahami $newTitle" },
                                            explanation = newExp,
                                            dailyExample = newCase.takeIf { it.isNotBlank() },
                                            sparkQuestion = newSpark.takeIf { it.isNotBlank() },
                                            summary = newSum.ifBlank { newTitle },
                                            citation = base?.citation ?: "Materi Guru (Parijoto Learning)",
                                            published = newPublished
                                        )
                                    )
                                    editingLesson = null
                                    newTitle = ""; newObj = ""; newExp = ""; newCase = ""; newSpark = ""; newSum = ""
                                },
                                enabled = !busy && newTitle.isNotBlank() && newExp.isNotBlank(),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("teacher_save_lesson_button")
                            ) {
                                Icon(if (editTarget == null) Icons.Default.Add else Icons.Default.Save, contentDescription = "Simpan Materi")
                                Spacer(Modifier.width(6.dp))
                                Text(if (editTarget == null) "Simpan & Publikasikan Materi" else "Perbarui Subbab Materi")
                            }
                        }
                    }
                }
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(modules, key = { it.id }) { m ->
                            val count = lessons.count { it.moduleId == m.id }
                            FilterChip(
                                selected = lessonFilterModId == m.id,
                                onClick = { lessonFilterModId = m.id },
                                label = { Text("Subbab Modul ${m.moduleNumber} ($count)", maxLines = 1) },
                                modifier = Modifier.height(48.dp).testTag("teacher_filter_mod_${m.moduleNumber}")
                            )
                        }
                    }
                }
                val filteredTeacherLessons = lessons.filter { it.moduleId == lessonFilterModId }.sortedBy { it.orderIndex }
                items(filteredTeacherLessons, key = { it.id }) { les ->
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Materi", tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(les.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    if (!les.published) {
                                        StatusPill("Draf", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                                Text(les.learningObjective, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            }
                            IconButton(
                                onClick = { editingLesson = les },
                                modifier = Modifier.size(48.dp).testTag("teacher_edit_lesson_${les.id}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Subbab")
                            }
                            IconButton(
                                onClick = { pendingDeleteLesson = les },
                                modifier = Modifier.size(48.dp).testTag("teacher_delete_lesson_${les.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus Subbab", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            1 -> {
                item {
                    TeacherCreateGroupAndAssignmentCards(
                        classes = classes,
                        groupsCount = groups.size,
                        assignments = assignments,
                        busy = busy,
                        onCreateGroup = onCreateGroup,
                        onCreateAssignment = onCreateAssignment
                    )
                }

                if (groups.isNotEmpty()) {
                    item {
                        Text("Pemantauan Kelompok PBL (${groups.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    items(groups, key = { it.id }) { grp ->
                        val clsEnrollments = enrollments.filter { it.classId == grp.classId && it.status == "ACTIVE" }
                        TeacherGroupMonitorCard(
                            group = grp,
                            classEnrollments = clsEnrollments,
                            discussions = groupDiscussions.filter { it.groupId == grp.id },
                            busy = busy,
                            onObserveDiscussions = onObserveGroupDiscussion,
                            onAssignMembers = onAssignGroupMembers,
                            onSaveComment = onSaveGroupComment,
                            onSendDiscussion = onSendGroupDiscussion
                        )
                    }
                }
            }

            else -> {
                teacherQuizAndBankItems(
                    quizzes = quizzes,
                    questions = questions,
                    answerKeys = answerKeys,
                    selectedQuizId = selectedQuizId,
                    editingQuestion = editingQuestion,
                    busy = busy,
                    onSelectQuiz = { selectedQuizId = it },
                    onEditQuestion = { editingQuestion = it },
                    onRequestDeleteQuestion = { pendingDeleteQuestion = it },
                    onSaveQuizConfig = onSaveQuizConfig,
                    onSaveBankQuestion = onSaveBankQuestion
                )
            }
        }
    }

    DeleteBankQuestionDialog(
        target = pendingDeleteQuestion,
        onDismiss = { pendingDeleteQuestion = null },
        onConfirm = { qId ->
            onDeleteBankQuestion(qId)
            if (editingQuestion?.id == qId) editingQuestion = null
            pendingDeleteQuestion = null
        }
    )

    pendingDeleteLesson?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteLesson = null },
            title = { Text("Hapus Subbab Materi?") },
            text = { Text("Subbab '${target.title}' akan dihapus dari daftar materi Anda.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteLesson(target.id)
                        if (editingLesson?.id == target.id) editingLesson = null
                        pendingDeleteLesson = null
                    },
                    modifier = Modifier.testTag("dialog_confirm_button")
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteLesson = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

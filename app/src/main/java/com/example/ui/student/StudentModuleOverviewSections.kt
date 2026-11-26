package com.example.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.curriculum.ModuleOverviewData
import com.example.data.curriculum.ParijotoAssessmentsAndGlossary
import com.example.data.curriculum.ParijotoCurriculumPart1
import com.example.data.model.LearningModule
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.Reflection
import com.example.ui.components.LessonIllustrationCard
import com.example.ui.components.StatusPill

@Composable
fun ModuleIdentityAndConceptSection(
    module: LearningModule,
    overview: ModuleOverviewData
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Identitas Modul
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "Identitas Modul", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Identitas Modul", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Text("Judul: ${module.title}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text("Penyusun: ${overview.authorAttribution}", style = MaterialTheme.typography.bodySmall)
                Text("Jenjang/Fase: ${overview.phaseAndGrade}", style = MaterialTheme.typography.bodySmall)
                Text("Model & Pendekatan: ${overview.learningModel}", style = MaterialTheme.typography.bodySmall)
                Text("Alokasi Waktu: ${overview.allocationJp} (${module.meetingLabel})", style = MaterialTheme.typography.bodySmall)
            }
        }

        // 2. Tujuan Pembelajaran
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Tujuan Pembelajaran", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Tujuan Pembelajaran Modul", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                overview.objectives.forEachIndexed { idx, obj ->
                    Text("${idx + 1}. $obj", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // 3. Peta Konsep Modul
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountTree, contentDescription = "Peta Konsep", tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text("Peta Konsep Modul ${module.moduleNumber}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                overview.conceptMap.forEach { branch ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(branch.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            branch.points.forEach { pt ->
                                Text("• $pt", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        // 4. Pertanyaan Pemantik
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = "Pertanyaan Pemantik", tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(8.dp))
                    Text("Pertanyaan Pemantik", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                overview.sparkQuestions.forEachIndexed { idx, q ->
                    Text("${idx + 1}. $q", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ModuleCaseStudyAndSummarySection(
    overview: ModuleOverviewData
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 5. Studi Kasus & Penjelasan Ilmiah
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Science, contentDescription = "Studi Kasus Ilmiah", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(overview.caseStudy.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Text(overview.caseStudy.scenario, style = MaterialTheme.typography.bodySmall)
                LessonIllustrationCard(illustrationType = overview.caseStudy.illustrationType)
                Text("Analisis & Penjelasan Ilmiah Kritis:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                overview.caseStudy.criticalNotes.forEach { note ->
                    Text("• $note", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // 6. Rangkuman Modul
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Summarize, contentDescription = "Rangkuman Modul", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Rangkuman Terpadu Modul", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                overview.summaryPoints.forEach { pt ->
                    Text("• $pt", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ModuleQuizAndReflectionSection(
    moduleId: String,
    moduleQuiz: Quiz?,
    questionCount: Int,
    attempt: QuizAttempt?,
    existingReflection: Reflection?,
    busy: Boolean,
    onStartQuiz: (Quiz) -> Unit,
    onSubmitReflection: (String, List<Boolean>, String?, String?) -> Unit
) {
    val checklist = if (moduleId == ParijotoCurriculumPart1.modul1.id) {
        ParijotoAssessmentsAndGlossary.modul1SelfChecklist
    } else {
        ParijotoAssessmentsAndGlossary.modul2SelfChecklist
    }
    val checklistState = remember(moduleId, existingReflection?.id) {
        mutableStateMapOf<Int, Boolean>().apply {
            checklist.indices.forEach { i ->
                this[i] = existingReflection?.answers?.getOrNull(i) ?: false
            }
        }
    }
    var reflectionNotes by remember(moduleId, existingReflection?.id) {
        mutableStateOf(existingReflection?.notes.orEmpty())
    }
    var pblReflection by remember(moduleId, existingReflection?.id) {
        mutableStateOf(existingReflection?.pblReflection.orEmpty())
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (moduleQuiz != null) {
            val isPublished = moduleQuiz.resultsPublished || (attempt?.resultsPublished == true)
            val statusBadge = when {
                attempt == null -> "$questionCount Soal"
                attempt.status == "IN_PROGRESS" -> "Draf #${attempt.attemptNumber}"
                isPublished -> "Skor: ${attempt.totalScore.toInt()}"
                else -> "Terkirim"
            }
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Quiz, contentDescription = "Kuis Modul", tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(moduleQuiz.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        StatusPill(
                            text = statusBadge,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        "Durasi ${moduleQuiz.durationMinutes} menit • Maks ${moduleQuiz.maxAttempts} percobaan • ${moduleQuiz.scheduleLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { onStartQuiz(moduleQuiz) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("module_start_quiz_button")
                    ) {
                        Text(
                            when {
                                attempt == null -> "Kerjakan Kuis Modul"
                                attempt.status == "IN_PROGRESS" -> "Lanjutkan Draf Kuis"
                                isPublished && moduleQuiz.closed && moduleQuiz.showDiscussion -> "Lihat Hasil & Pembahasan"
                                else -> "Lihat Status & Jawaban"
                            }
                        )
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Refleksi & Penilaian Diri Modul",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                checklist.forEachIndexed { idx, statement ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
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
                    label = { Text("Refleksi Pembelajaran Modul Ini") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val answers = checklist.indices.map { checklistState[it] == true }
                        onSubmitReflection(moduleId, answers, reflectionNotes, pblReflection)
                    },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("module_submit_reflection_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Simpan Refleksi")
                    Spacer(Modifier.width(6.dp))
                    Text("Simpan Refleksi Modul")
                }
            }
        }
    }
}

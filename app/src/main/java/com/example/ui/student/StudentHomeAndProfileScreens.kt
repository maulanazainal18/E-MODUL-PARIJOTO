package com.example.ui.student

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.model.Announcement
import com.example.data.model.Assignment
import com.example.data.model.ClassRoom
import com.example.data.model.Enrollment
import com.example.data.model.GradeRecord
import com.example.data.model.LearningModule
import com.example.data.model.QuizAttempt
import com.example.data.model.Submission
import com.example.data.model.SystemSetting
import com.example.data.model.UserAccount
import com.example.ui.components.MetricStatCard
import com.example.ui.components.ParijotoBotanicalBanner
import com.example.ui.components.StatusPill

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentHomeTab(
    user: UserAccount,
    systemSetting: SystemSetting,
    enrollments: List<Enrollment>,
    activeEnrollment: Enrollment?,
    enrolledClassesMap: Map<String, ClassRoom>,
    modules: List<LearningModule>,
    totalLessons: Int,
    assignments: List<Assignment>,
    submissions: List<Submission>,
    attempts: List<QuizAttempt>,
    publishedGrades: List<GradeRecord>,
    announcements: List<Announcement>,
    busy: Boolean,
    onSelectEnrollment: (Enrollment) -> Unit,
    onJoinClass: (String) -> Unit,
    onOpenModules: () -> Unit,
    onOpenPbl: () -> Unit,
    onOpenEvaluation: () -> Unit,
    onOpenGlossary: () -> Unit
) {
    var classCodeInput by remember { mutableStateOf("") }
    var showJoinAdditional by remember { mutableStateOf(false) }
    val completedCount = activeEnrollment?.completedLessons?.size ?: 0
    val progressPct = activeEnrollment?.progressPercent ?: 0
    val activeClassRoom = activeEnrollment?.let { enrolledClassesMap[it.classId] }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("student_home_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ParijotoBotanicalBanner(compact = true)
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Halo, ${user.nama}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${systemSetting.schoolName} • TA ${systemSetting.academicYear}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(
                            text = activeEnrollment?.className ?: "Belum Gabung Kelas",
                            containerColor = if (activeEnrollment != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Progres Pembelajaran: $progressPct% ($completedCount/$totalLessons topik selesai)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (progressPct / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                    )
                }
            }
        }

        if (enrollments.size > 1) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Kelas yang Diikuti (${enrollments.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(enrollments, key = { it.id }) { enr ->
                            FilterChip(
                                selected = enr.id == activeEnrollment?.id,
                                onClick = { onSelectEnrollment(enr) },
                                label = { Text("${enr.className} (${enr.classCode})") },
                                modifier = Modifier.height(48.dp)
                            )
                        }
                    }
                }
            }
        }

        if (activeEnrollment != null) {
            item {
                JoinedClassAndTeacherCard(
                    enrollment = activeEnrollment,
                    classRoom = activeClassRoom,
                    systemSetting = systemSetting,
                    showJoinToggle = showJoinAdditional,
                    onToggleJoinForm = { showJoinAdditional = !showJoinAdditional }
                )
            }
        }

        if (activeEnrollment == null || showJoinAdditional) {
            item {
                JoinClassCodeCard(
                    codeInput = classCodeInput,
                    onCodeChange = { classCodeInput = it.uppercase() },
                    busy = busy,
                    isAdditional = activeEnrollment != null,
                    onSubmit = {
                        onJoinClass(classCodeInput)
                        classCodeInput = ""
                        showJoinAdditional = false
                    }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    label = "Tugas Terkirim",
                    value = "${submissions.count { it.status != "DRAFT" }}/${assignments.size}",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    label = "Kuis Dikerjakan",
                    value = "${attempts.size}/3",
                    icon = Icons.Default.Quiz,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                "Informasi Pembelajaran & Alur 4 JP (2 Pertemuan PBL)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(modules, key = { it.id }) { mod ->
            Card(
                onClick = onOpenModules,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            text = mod.meetingLabel,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "${mod.durationMinutes} menit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(mod.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(mod.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    mod.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenPbl,
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.height(48.dp).testTag("quick_open_pbl_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "LKM")
                    Spacer(Modifier.width(6.dp))
                    Text("Isi E-LKM", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onOpenEvaluation,
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.height(48.dp).testTag("quick_open_eval_button")
                ) {
                    Icon(Icons.Default.Quiz, contentDescription = "Evaluasi")
                    Spacer(Modifier.width(6.dp))
                    Text("Kuis & Nilai", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onOpenGlossary,
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.height(48.dp).testTag("quick_open_glossary_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Glosarium")
                    Spacer(Modifier.width(6.dp))
                    Text("Glosarium", maxLines = 1)
                }
            }
        }

        if (publishedGrades.isNotEmpty()) {
            item {
                Text("Nilai Terbaru yang Dipublikasikan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            items(publishedGrades.take(4), key = { it.id }) { grade ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(grade.sourceTitle, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${grade.assessmentType} • ${grade.feedback ?: "Dinilai oleh guru"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                "${grade.finalScore.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        if (announcements.isNotEmpty()) {
            item {
                Text("Pengumuman Guru", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            items(announcements, key = { it.id }) { ann ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Campaign, contentDescription = "Pengumuman", tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(6.dp))
                            Text(ann.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        Text(ann.message, style = MaterialTheme.typography.bodySmall)
                        Text("Oleh ${ann.teacherName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun JoinedClassAndTeacherCard(
    enrollment: Enrollment,
    classRoom: ClassRoom?,
    systemSetting: SystemSetting,
    showJoinToggle: Boolean,
    onToggleJoinForm: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Class, contentDescription = "Kelas", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        classRoom?.name ?: enrollment.className,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                StatusPill(
                    text = "Kode: ${enrollment.classCode}",
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = "Profil Guru", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Guru Pengampu: ${classRoom?.teacherName ?: "Guru Biologi Pengampu"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, contentDescription = "Info Pembelajaran", tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "${classRoom?.gradeLevel ?: "SMA/MA Kelas X"} • Fase ${classRoom?.phase ?: "E"} • TA ${classRoom?.academicYear ?: systemSetting.academicYear}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Materi: Keanekaragaman Hayati Etnosains Parijoto (Problem Based Learning • 4 JP)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    classRoom?.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            OutlinedButton(
                onClick = onToggleJoinForm,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("toggle_join_another_class_button")
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = "Gabung Kelas Lain")
                Spacer(Modifier.width(6.dp))
                Text(if (showJoinToggle) "Tutup Form Gabung Kelas" else "Gabung Kelas Lain dengan Kode")
            }
        }
    }
}

@Composable
private fun JoinClassCodeCard(
    codeInput: String,
    onCodeChange: (String) -> Unit,
    busy: Boolean,
    isAdditional: Boolean,
    onSubmit: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GroupAdd, contentDescription = "Gabung Kelas", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isAdditional) "Gabung ke Kelas Biologi Lain" else "Gabung ke Kelas Biologi Guru",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "Masukkan kode kelas dari guru Anda agar profil guru, informasi pembelajaran, LKM, dan nilai tersinkronisasi.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = onCodeChange,
                    label = { Text("Kode Kelas (mis. PRJ...)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("join_class_code_input")
                )
                Button(
                    onClick = onSubmit,
                    enabled = !busy && codeInput.trim().length >= 4,
                    modifier = Modifier.height(54.dp).testTag("join_class_button")
                ) {
                    Text("Gabung")
                }
            }
        }
    }
}

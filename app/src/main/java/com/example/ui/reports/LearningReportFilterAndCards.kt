package com.example.ui.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.data.curriculum.ClassReportSummary
import com.example.data.curriculum.QuizEngineUtils
import com.example.data.curriculum.ReportFilter
import com.example.data.curriculum.ReportRoleMode
import com.example.data.curriculum.StudentReportSummary
import com.example.data.model.ClassRoom
import com.example.data.model.Enrollment
import com.example.data.model.LearningModule
import com.example.ui.components.StatusPill
import com.example.ui.student.submissionStatusLabel

@Composable
fun ReportFilterCard(
    roleMode: ReportRoleMode,
    filter: ReportFilter,
    classes: List<ClassRoom>,
    modules: List<LearningModule>,
    enrollments: List<Enrollment>,
    studentSearch: String,
    onStudentSearchChange: (String) -> Unit,
    onFilterChange: (ReportFilter) -> Unit
) {
    val studentCandidates = remember(enrollments, filter.classId, studentSearch) {
        val scoped = enrollments
            .filter { it.status == "ACTIVE" }
            .filter { filter.classId.isNullOrBlank() || it.classId == filter.classId }
            .distinctBy { it.studentUid }
            .sortedBy { it.studentName.lowercase() }
        val q = studentSearch.trim().lowercase()
        if (q.isEmpty()) scoped else scoped.filter { it.studentName.lowercase().contains(q) }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.testTag("report_filter_card")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter Laporan", tint = MaterialTheme.colorScheme.primary)
                Text("Filter Laporan Hasil Belajar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }

            if (classes.isNotEmpty()) {
                Text("Kelas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = filter.classId == null,
                            onClick = { onFilterChange(filter.copy(classId = null, studentUid = if (roleMode == ReportRoleMode.SISWA) filter.studentUid else null)) },
                            label = { Text("Semua Kelas (${classes.size})") },
                            modifier = Modifier.height(48.dp).testTag("report_filter_class_ALL")
                        )
                    }
                    items(classes, key = { it.id }) { cls ->
                        FilterChip(
                            selected = filter.classId == cls.id,
                            onClick = { onFilterChange(filter.copy(classId = cls.id, studentUid = if (roleMode == ReportRoleMode.SISWA) filter.studentUid else null)) },
                            label = { Text(cls.name) },
                            modifier = Modifier.height(48.dp).testTag("report_filter_class_${cls.id}")
                        )
                    }
                }
            }

            Text("Pertemuan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(null to "Semua", 1 to "Pertemuan 1", 2 to "Pertemuan 2").forEach { (mtg, label) ->
                    FilterChip(
                        selected = filter.meetingNumber == mtg,
                        onClick = { onFilterChange(filter.copy(meetingNumber = mtg)) },
                        label = { Text(label) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("report_filter_meeting_${mtg ?: "ALL"}")
                    )
                }
            }

            Text("Modul Pembelajaran", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = filter.moduleId == null,
                        onClick = { onFilterChange(filter.copy(moduleId = null)) },
                        label = { Text("Semua Modul") },
                        modifier = Modifier.height(48.dp).testTag("report_filter_module_ALL")
                    )
                }
                items(modules, key = { it.id }) { mod ->
                    FilterChip(
                        selected = filter.moduleId == mod.id,
                        onClick = { onFilterChange(filter.copy(moduleId = mod.id)) },
                        label = { Text("Modul ${mod.moduleNumber}") },
                        modifier = Modifier.height(48.dp).testTag("report_filter_module_${mod.id}")
                    )
                }
            }

            if (roleMode != ReportRoleMode.SISWA) {
                Text("Siswa", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = studentSearch,
                    onValueChange = onStudentSearchChange,
                    label = { Text("Cari Nama Siswa") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari Siswa") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_filter_student_search")
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = filter.studentUid == null,
                            onClick = { onFilterChange(filter.copy(studentUid = null)) },
                            label = { Text("Semua Siswa (${studentCandidates.size})") },
                            modifier = Modifier.height(48.dp).testTag("report_filter_student_ALL")
                        )
                    }
                    items(studentCandidates, key = { it.studentUid }) { st ->
                        FilterChip(
                            selected = filter.studentUid == st.studentUid,
                            onClick = { onFilterChange(filter.copy(studentUid = st.studentUid)) },
                            label = { Text(st.studentName) },
                            modifier = Modifier.height(48.dp).testTag("report_filter_student_${st.studentUid}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClassReportCard(summary: ClassReportSummary) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.testTag("class_report_card_${summary.classId}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(summary.className, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Kode: ${summary.classCode} • Pengampu: ${summary.teacherName} • ${summary.activeStudentCount} siswa",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(
                    text = "Rata-rata ${QuizEngineUtils.roundScore(summary.avgFinalScore)}",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Text(
                "Kuis: ${fmtScore(summary.avgQuizScore)} • Post-Test: ${fmtScore(summary.avgPostTestScore)} • " +
                    "Keterampilan: ${fmtScore(summary.avgSkillScore)} • Proyek: ${fmtScore(summary.avgProjectScore)} • Sikap: ${fmtScore(summary.avgAttitudeScore)}",
                style = MaterialTheme.typography.bodySmall
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Materi Selesai: ${QuizEngineUtils.roundScore(summary.avgMaterialCompletionPercent)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { (summary.avgMaterialCompletionPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Pengumpulan Tugas: ${QuizEngineUtils.roundScore(summary.avgAssignmentCompletionPercent)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { (summary.avgAssignmentCompletionPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
fun StudentReportCard(
    summary: StudentReportSummary,
    isStudentMode: Boolean
) {
    var expanded by remember { mutableStateOf(isStudentMode) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.testTag("student_report_card_${summary.studentUid}_${summary.classId}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(summary.studentName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${summary.className} • ${summary.predicate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(
                    text = if (summary.activeWeightPercent > 0) {
                        "Rapor: ${QuizEngineUtils.roundScore(summary.finalReportScore)}"
                    } else {
                        "Menunggu Nilai"
                    },
                    containerColor = if (summary.activeWeightPercent > 0) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = if (summary.activeWeightPercent > 0) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            }

            if (isStudentMode && summary.unpublishedSuppressedCount > 0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                ) {
                    Text(
                        "${summary.unpublishedSuppressedCount} komponen nilai masih dalam proses verifikasi guru dan belum dipublikasikan.",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // 5-Component Score Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniScoreChip("Kuis", summary.quizScore, Modifier.weight(1f))
                MiniScoreChip("Post-Test", summary.postTestScore, Modifier.weight(1f))
                MiniScoreChip("Keterampilan", summary.skillScore, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniScoreChip("Proyek CER", summary.projectScore, Modifier.weight(1f))
                MiniScoreChip("Sikap", summary.attitudeScore, Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Bobot Aktif", style = MaterialTheme.typography.labelSmall)
                        Text("${summary.activeWeightPercent}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Material completion & Task submission percentages
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Materi: ${QuizEngineUtils.roundScore(summary.materialCompletionPercent)}% (${summary.completedLessonsCount}/${summary.totalLessonsCount})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { (summary.materialCompletionPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Tugas: ${QuizEngineUtils.roundScore(summary.assignmentCompletionPercent)}% (${summary.submittedAssignmentsCount}/${summary.totalAssignmentsCount})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { (summary.assignmentCompletionPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .align(Alignment.End)
                    .height(48.dp)
                    .testTag("toggle_trace_${summary.studentUid}_${summary.classId}")
            ) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Telusur Komponen"
                )
                Spacer(Modifier.width(4.dp))
                Text(if (expanded) "Tutup Telusur Komponen" else "Telusur Komponen & Tugas")
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider()
                    Text(
                        "Penelusuran Komponen Nilai (Kuis 20% • Post-Test 25% • Keterampilan 20% • Proyek 20% • Sikap 15%)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (summary.traces.isEmpty()) {
                        Text(
                            if (isStudentMode) "Belum ada komponen nilai resmi yang dipublikasikan."
                            else "Belum ada nilai tercatat untuk siswa ini pada filter aktif.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        summary.traces.forEach { tr ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(
                                            "[${tr.componentType.label} • ${tr.weightPercent}%] ${tr.sourceTitle}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text("${tr.score}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text(
                                        "${tr.formulaDetail} • ${if (tr.published) "Dipublikasikan" else "Draf"}" +
                                            if (tr.includedInAverage) "" else " • (Riwayat percobaan lain)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        "Rekap Pengumpulan Tugas (${summary.submittedAssignmentsCount}/${summary.totalAssignmentsCount} terkumpul • ${summary.gradedAssignmentsCount} dinilai)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    summary.assignmentDetails.forEach { asg ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "• P${asg.meetingNumber}: ${asg.title}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            val lbl = if (asg.status == "BELUM") "Belum" else submissionStatusLabel(asg.status)
                            Text(
                                lbl,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniScoreChip(label: String, score: Double?, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                fmtScore(score),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (score != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun fmtScore(value: Double?): String = value?.let { "${QuizEngineUtils.roundScore(it)}" } ?: "-"

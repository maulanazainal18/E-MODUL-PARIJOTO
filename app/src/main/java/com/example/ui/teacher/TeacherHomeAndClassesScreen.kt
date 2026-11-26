package com.example.ui.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ClassRoom
import com.example.data.model.Enrollment
import com.example.data.model.GradeRecord
import com.example.data.model.QuizAttempt
import com.example.data.model.Submission
import com.example.data.model.SystemSetting
import com.example.data.model.UserAccount
import com.example.ui.components.MetricStatCard
import com.example.ui.components.ParijotoConfirmDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.shareTextIntent

@Composable
fun TeacherHomeTab(
    user: UserAccount,
    classes: List<ClassRoom>,
    enrollments: List<Enrollment>,
    submissions: List<Submission>,
    attempts: List<QuizAttempt>,
    grades: List<GradeRecord>,
    busy: Boolean,
    onPostAnnouncement: (String, String, String, Boolean) -> Unit,
    onExportCsv: (String?) -> String
) {
    val context = LocalContext.current
    var selectedClassId by remember { mutableStateOf<String?>(null) }
    var annTitle by remember { mutableStateOf("") }
    var annMessage by remember { mutableStateOf("") }
    var showAnnForm by remember { mutableStateOf(false) }

    val scopedEnrollments = enrollments.filter { selectedClassId == null || it.classId == selectedClassId }
    val activeEnrollments = scopedEnrollments.filter { it.status == "ACTIVE" }
    val scopedSubs = submissions.filter { selectedClassId == null || it.classId == selectedClassId }
    val scopedAttempts = attempts.filter { selectedClassId == null || it.classId == selectedClassId }
    val scopedGrades = grades.filter { selectedClassId == null || it.classId == selectedClassId }

    val pendingCount = scopedSubs.count { it.status == "SUBMITTED" } + scopedAttempts.count { it.status == "SUBMITTED" }
    val gradedCount = scopedGrades.count { it.published }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Dashboard Guru • ${user.nama}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Pantau progres siswa, kelola pembelajaran PBL Parijoto, nilai LKM/Uraian, dan publikasikan rapor biologi.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (classes.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedClassId == null,
                            onClick = { selectedClassId = null },
                            label = { Text("Semua Kelas (${classes.size})") }
                        )
                    }
                    items(classes, key = { it.id }) { cls ->
                        FilterChip(
                            selected = selectedClassId == cls.id,
                            onClick = { selectedClassId = cls.id },
                            label = { Text(cls.name) }
                        )
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricStatCard("Kelas Aktif", "${classes.count { !it.archived }}", Icons.Default.Class, modifier = Modifier.weight(1f))
                MetricStatCard("Siswa Aktif", "${activeEnrollments.size}", Icons.Default.Group, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricStatCard("Tugas Terkumpul", "${scopedSubs.count { it.status != "DRAFT" }}", Icons.AutoMirrored.Filled.Assignment, modifier = Modifier.weight(1f))
                MetricStatCard("Perlu Dinilai", "$pendingCount", Icons.Default.PendingActions, accentColor = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricStatCard("Nilai Dipublikasi", "$gradedCount", Icons.Default.CheckCircle, modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = {
                        val csv = onExportCsv(selectedClassId)
                        shareTextIntent(context, "Laporan Nilai Parijoto Learning (CSV)", csv)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .testTag("teacher_export_csv_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Download, contentDescription = "Ekspor CSV")
                        Text("Unduh / Bagikan CSV", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Progres Belajar Siswa (${scopedEnrollments.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { showAnnForm = !showAnnForm },
                    modifier = Modifier.height(48.dp).testTag("toggle_announcement_form")
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = "Pengumuman")
                    Spacer(Modifier.width(6.dp))
                    Text("Pengumuman", maxLines = 1)
                }
            }
        }

        if (showAnnForm) {
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = annTitle,
                            onValueChange = { annTitle = it },
                            label = { Text("Judul Pengumuman") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = annMessage,
                            onValueChange = { annMessage = it },
                            label = { Text("Isi Pesan untuk Siswa") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                onPostAnnouncement(selectedClassId ?: "ALL", annTitle, annMessage, true)
                                annTitle = ""
                                annMessage = ""
                                showAnnForm = false
                            },
                            enabled = !busy && annTitle.isNotBlank() && annMessage.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("publish_announcement_button")
                        ) {
                            Text("Kirim Pengumuman")
                        }
                    }
                }
            }
        }

        if (scopedEnrollments.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(
                        "Belum ada siswa bergabung di kelas terpilih. Buat kelas di tab Kelas lalu bagikan kode kelas kepada siswa.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(scopedEnrollments, key = { it.id }) { enr ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(enr.studentName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${enr.progressPercent}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("${enr.className} • ${enr.completedLessons.size} topik selesai", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LinearProgressIndicator(
                            progress = { (enr.progressPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherClassesTab(
    user: UserAccount,
    systemSetting: SystemSetting,
    classes: List<ClassRoom>,
    enrollments: List<Enrollment>,
    busy: Boolean,
    onCreateClass: (String, String, String, String, String?) -> Unit,
    onToggleArchive: (ClassRoom) -> Unit,
    onToggleStudentStatus: (Enrollment) -> Unit,
    onRemoveStudent: (String) -> Unit
) {
    val context = LocalContext.current
    var className by remember { mutableStateOf("") }
    var gradeLevel by remember { mutableStateOf("X SMA/MA") }
    var description by remember { mutableStateOf("") }
    var expandedClassId by remember { mutableStateOf<String?>(null) }
    var memberToDelete by remember { mutableStateOf<Enrollment?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Buat Kelas Biologi Baru", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = className,
                        onValueChange = { className = it },
                        label = { Text("Nama Kelas (mis. X IPA 1)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("teacher_class_name_input")
                    )
                    OutlinedTextField(
                        value = gradeLevel,
                        onValueChange = { gradeLevel = it },
                        label = { Text("Jenjang / Fase") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Deskripsi Singkat (opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onCreateClass(user.nama, className, gradeLevel, systemSetting.academicYear, description)
                            className = ""
                            description = ""
                        },
                        enabled = !busy && className.trim().length >= 2,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("teacher_create_class_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Buat")
                        Spacer(Modifier.width(8.dp))
                        Text("Buat Kelas & Hasilkan Kode")
                    }
                }
            }
        }

        item {
            Text("Daftar Kelas Saya (${classes.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }

        items(classes, key = { it.id }) { cls ->
            val members = enrollments.filter { it.classId == cls.id }
            val isExpanded = expandedClassId == cls.id
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val activeCount = members.count { it.status == "ACTIVE" }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cls.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${cls.gradeLevel} • TA ${cls.academicYear} • $activeCount aktif (${members.size} total)", style = MaterialTheme.typography.bodySmall)
                        }
                        StatusPill(
                            text = if (cls.archived) "Diarsipkan" else "Kode: ${cls.classCode}",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                shareTextIntent(
                                    context,
                                    "Kode Kelas Parijoto Learning",
                                    "Gabung ke kelas ${cls.name} (${cls.gradeLevel}) di aplikasi PARIJOTO LEARNING menggunakan kode kelas: ${cls.classCode}"
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Bagikan Kode")
                            Spacer(Modifier.width(6.dp))
                            Text("Bagikan Kode", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { expandedClassId = if (isExpanded) null else cls.id },
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(if (isExpanded) "Tutup Anggota" else "Anggota (${members.size})", maxLines = 1)
                        }
                        IconButton(
                            onClick = { onToggleArchive(cls) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = "Arsipkan Kelas")
                        }
                    }

                    if (isExpanded) {
                        if (members.isEmpty()) {
                            Text("Belum ada siswa bergabung.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            members.forEach { m ->
                                val isActiveMember = m.status == "ACTIVE"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${m.studentName} (${m.progressPercent}%)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = if (isActiveMember) "Status: Aktif" else "Status: Dinonaktifkan",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isActiveMember) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onToggleStudentStatus(m) },
                                        enabled = !busy,
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("toggle_member_status_${m.studentUid}")
                                    ) {
                                        Text(
                                            if (isActiveMember) "Nonaktifkan" else "Aktifkan",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { memberToDelete = m },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("remove_member_${m.studentUid}")
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Siswa Permanen", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    memberToDelete?.let { target ->
        ParijotoConfirmDialog(
            title = "Hapus Anggota Kelas?",
            message = "Apakah Anda yakin ingin menghapus permanen ${target.studentName} dari kelas ${target.className}? Anda juga dapat memilih 'Nonaktifkan' untuk menyimpan riwayat progres.",
            confirmText = "Hapus Permanen",
            onConfirm = {
                onRemoveStudent(target.id)
                memberToDelete = null
            },
            onDismiss = { memberToDelete = null }
        )
    }
}

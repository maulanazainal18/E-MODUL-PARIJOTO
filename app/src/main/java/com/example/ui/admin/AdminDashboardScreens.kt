package com.example.ui.admin

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
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountStatus
import com.example.data.model.ClassRoom
import com.example.data.model.Enrollment
import com.example.data.model.GradeRecord
import com.example.data.model.QuizAttempt
import com.example.data.model.Reflection
import com.example.data.model.Submission
import com.example.data.model.UserAccount
import com.example.data.model.UserRole
import com.example.ui.components.MetricStatCard
import com.example.ui.components.StatusPill
import com.example.ui.components.shareTextIntent

@Composable
fun AdminOverviewTab(
    users: List<UserAccount>,
    classes: List<ClassRoom>,
    enrollments: List<Enrollment>,
    submissions: List<Submission>,
    attempts: List<QuizAttempt>,
    grades: List<GradeRecord>,
    reflections: List<Reflection>,
    onExportCsv: () -> String
) {
    val context = LocalContext.current
    val teachersCount = users.count { it.roleEnum() == UserRole.GURU }
    val studentsCount = users.count { it.roleEnum() == UserRole.SISWA }
    val activeEnrollments = enrollments.count { it.status == "ACTIVE" }
    val avgGrade = if (grades.isEmpty()) 0 else grades.map { it.finalScore }.average().toInt()

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
                    Text("Statistik & Monitoring PARIJOTO LEARNING", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Ringkasan menyeluruh akun guru & siswa, kelas aktif, aktivitas belajar, dan rekap nilai.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricStatCard("Akun Guru", "$teachersCount", Icons.Default.School, modifier = Modifier.weight(1f))
                MetricStatCard("Akun Siswa", "$studentsCount", Icons.Default.Group, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricStatCard("Kelas (${classes.count { !it.archived }} Aktif)", "${classes.size}", Icons.Default.Class, modifier = Modifier.weight(1f))
                MetricStatCard("Keanggotaan Aktif", "$activeEnrollments", Icons.Default.Grade, accentColor = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricStatCard("Tugas LKM/CER", "${submissions.size}", Icons.AutoMirrored.Filled.Assignment, modifier = Modifier.weight(1f))
                MetricStatCard("Kuis / Rata-rata", "${attempts.size} • $avgGrade", Icons.Default.Quiz, modifier = Modifier.weight(1f))
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    shareTextIntent(context, "Rekap Seluruh Nilai Parijoto Learning (CSV)", onExportCsv())
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_export_csv_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = "Unduh Rekap")
                Spacer(Modifier.width(8.dp))
                Text("Ekspor / Bagikan Rekap Seluruh Nilai (${grades.size} entri)")
            }
        }

        item {
            Text("Monitoring Aktivitas Pembelajaran Terbaru (${enrollments.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }

        if (enrollments.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(
                        "Belum ada keanggotaan kelas yang tercatat. Buat kelas di tab Kurikulum atau minta guru membagikan kode kelas.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(enrollments.take(15), key = { it.id }) { enr ->
                val isActive = enr.status == "ACTIVE"
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(enr.studentName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text("${enr.className} (${enr.classCode}) • ${enr.completedLessons.size} topik selesai", style = MaterialTheme.typography.bodySmall)
                        }
                        StatusPill(
                            text = if (isActive) "${enr.progressPercent}%" else "Nonaktif",
                            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        if (grades.isNotEmpty() || reflections.isNotEmpty()) {
            item {
                Text("Rekap Nilai & Refleksi (${grades.size} Nilai • ${reflections.size} Refleksi)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            items(grades.take(20), key = { it.id }) { g ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${g.studentName} • ${g.assessmentType}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text(g.sourceTitle, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("${g.finalScore.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminUsersTab(
    users: List<UserAccount>,
    busy: Boolean,
    onUpdateUser: (UserAccount, UserRole, String) -> Unit
) {
    var roleFilter by remember { mutableStateOf<UserRole?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val filtered = users.filter { acct ->
        val matchesRole = roleFilter == null || acct.roleEnum() == roleFilter
        val q = searchQuery.trim().lowercase()
        val matchesQuery = q.isEmpty() ||
            acct.nama.lowercase().contains(q) ||
            acct.email.lowercase().contains(q)
        matchesRole && matchesQuery
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_users_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Cari Nama / Email Pengguna") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_user_search_input")
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = roleFilter == null,
                        onClick = { roleFilter = null },
                        label = { Text("Semua (${users.size})") },
                        modifier = Modifier.height(48.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = roleFilter == UserRole.GURU,
                        onClick = { roleFilter = UserRole.GURU },
                        label = { Text("Guru (${users.count { it.roleEnum() == UserRole.GURU }})") },
                        modifier = Modifier.height(48.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = roleFilter == UserRole.SISWA,
                        onClick = { roleFilter = UserRole.SISWA },
                        label = { Text("Siswa (${users.count { it.roleEnum() == UserRole.SISWA }})") },
                        modifier = Modifier.height(48.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = roleFilter == UserRole.ADMIN,
                        onClick = { roleFilter = UserRole.ADMIN },
                        label = { Text("Admin (${users.count { it.roleEnum() == UserRole.ADMIN }})") },
                        modifier = Modifier.height(48.dp)
                    )
                }
            }
        }

        items(filtered, key = { it.uid }) { acct ->
            val currentRole = acct.roleEnum()
            val isActive = acct.statusEnum() == AccountStatus.ACTIVE
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(acct.nama, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(acct.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            acct.className?.takeIf { it.isNotBlank() }?.let {
                                Text("Kelas terakhir: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        StatusPill(
                            text = "${currentRole.label} • ${acct.statusEnum().label}",
                            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(UserRole.SISWA, UserRole.GURU, UserRole.ADMIN).forEach { r ->
                            FilterChip(
                                selected = currentRole == r,
                                onClick = { onUpdateUser(acct, r, acct.accountStatus) },
                                enabled = !busy,
                                label = { Text(r.label) },
                                modifier = Modifier.height(48.dp)
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val nextStatus = if (isActive) AccountStatus.SUSPENDED.name else AccountStatus.ACTIVE.name
                            onUpdateUser(acct, currentRole, nextStatus)
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_toggle_user_${acct.uid}")
                    ) {
                        Text(if (isActive) "Nonaktifkan" else "Aktifkan")
                    }
                }
            }
        }
    }
}

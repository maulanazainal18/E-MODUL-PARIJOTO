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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ClassRoom
import com.example.data.model.Enrollment
import com.example.data.model.GradeRecord
import com.example.data.model.UserAccount
import com.example.ui.components.StatusPill

@Composable
fun StudentProfileTab(
    user: UserAccount,
    enrollments: List<Enrollment>,
    enrolledClassesMap: Map<String, ClassRoom>,
    downloadedModulesCount: Int,
    publishedGrades: List<GradeRecord>,
    busy: Boolean,
    onUpdateName: (String) -> Unit,
    onJoinClass: (String) -> Unit,
    onOpenGlossary: () -> Unit,
    onSignOut: () -> Unit
) {
    var nameInput by remember(user.nama) { mutableStateOf(user.nama) }
    var codeInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Profil Siswa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(
                            text = "Role: ${user.roleEnum().label}",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        StatusPill(
                            text = "Offline: $downloadedModulesCount modul",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nama Lengkap") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                    )
                    Button(
                        onClick = { onUpdateName(nameInput) },
                        enabled = !busy && nameInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_profile_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Simpan")
                        Spacer(Modifier.width(8.dp))
                        Text("Simpan Nama Profil")
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Class, contentDescription = "Kelas", tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Kelas yang Diikuti & Profil Guru (${enrollments.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    if (enrollments.isEmpty()) {
                        Text("Anda belum bergabung dengan kelas mana pun.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        enrollments.forEach { enr ->
                            val cls = enrolledClassesMap[enr.classId]
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Aktif", tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(6.dp))
                                        Text("${cls?.name ?: enr.className} (Kode: ${enr.classCode})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        "Guru Pengampu: ${cls?.teacherName ?: "Guru Biologi Pengampu"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "${cls?.gradeLevel ?: "SMA/MA Kelas X"} • Fase ${cls?.phase ?: "E"} • TA ${cls?.academicYear ?: "2026/2027"} • Progres ${enr.progressPercent}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { codeInput = it.uppercase() },
                            label = { Text("Gabung Kelas via Kode") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("profile_join_code_input")
                        )
                        Button(
                            onClick = {
                                onJoinClass(codeInput)
                                codeInput = ""
                            },
                            enabled = !busy && codeInput.trim().length >= 4,
                            modifier = Modifier.height(54.dp).testTag("profile_join_code_button")
                        ) {
                            Text("Gabung")
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Grade, contentDescription = "Riwayat Nilai", tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(8.dp))
                        Text("Riwayat Penilaian (${publishedGrades.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    if (publishedGrades.isEmpty()) {
                        Text("Belum ada nilai yang dipublikasikan guru.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        publishedGrades.forEach { g ->
                            Text("• ${g.sourceTitle} (${g.assessmentType}): ${g.finalScore.toInt()}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(onClick = onOpenGlossary, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Glosarium")
                Spacer(Modifier.width(8.dp))
                Text("Buka Glosarium & Peta Konsep")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button")) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Keluar")
                Spacer(Modifier.width(8.dp))
                Text("Keluar dari Akun")
            }
        }
    }
}

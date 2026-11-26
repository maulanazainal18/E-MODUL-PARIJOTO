package com.example.ui.admin

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.example.data.model.LearningModule
import com.example.data.model.Lesson
import com.example.data.model.Subject
import com.example.data.model.SystemSetting
import com.example.data.model.UserAccount
import com.example.data.model.UserRole
import com.example.ui.components.StatusPill

@Composable
fun AdminClassesAndCurriculumTab(
    currentAdmin: UserAccount,
    users: List<UserAccount>,
    classes: List<ClassRoom>,
    enrollments: List<Enrollment>,
    subjects: List<Subject>,
    modules: List<LearningModule>,
    lessons: List<Lesson>,
    systemSetting: SystemSetting,
    busy: Boolean,
    onCreateClass: (String, String, String, String, String, String?) -> Unit,
    onToggleArchiveClass: (ClassRoom) -> Unit,
    onSaveSubject: (Subject) -> Unit
) {
    val primarySubject = subjects.firstOrNull() ?: Subject()
    var subjTitle by remember(primarySubject.id) { mutableStateOf(primarySubject.title) }
    var subjGrade by remember(primarySubject.id) { mutableStateOf(primarySubject.grade) }
    var subjYear by remember(primarySubject.id) { mutableStateOf(primarySubject.academicYear) }

    val teacherCandidates = remember(users, currentAdmin) {
        val list = users.filter { it.roleEnum() == UserRole.GURU || it.roleEnum() == UserRole.ADMIN }
        if (list.any { it.uid == currentAdmin.uid }) list else listOf(currentAdmin) + list
    }
    var selectedTeacherUid by remember(teacherCandidates) {
        mutableStateOf(
            teacherCandidates.firstOrNull { it.roleEnum() == UserRole.GURU }?.uid
                ?: currentAdmin.uid
        )
    }
    val selectedTeacher = teacherCandidates.firstOrNull { it.uid == selectedTeacherUid } ?: currentAdmin

    var newClassName by remember { mutableStateOf("") }
    var newClassGrade by remember { mutableStateOf("X SMA/MA") }
    var newClassYear by remember(systemSetting.academicYear) { mutableStateOf(systemSetting.academicYear) }
    var newClassDesc by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Buat Kelas Baru (Admin)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Pilih Guru Pengampu:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(teacherCandidates, key = { it.uid }) { t ->
                            FilterChip(
                                selected = selectedTeacher.uid == t.uid,
                                onClick = { selectedTeacherUid = t.uid },
                                label = { Text("${t.nama} (${t.roleEnum().label})") },
                                modifier = Modifier.height(48.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = newClassName,
                        onValueChange = { newClassName = it },
                        label = { Text("Nama Kelas (mis. X IPA 2)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_class_name_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newClassGrade,
                            onValueChange = { newClassGrade = it },
                            label = { Text("Jenjang") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newClassYear,
                            onValueChange = { newClassYear = it },
                            label = { Text("Tahun Ajaran") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = newClassDesc,
                        onValueChange = { newClassDesc = it },
                        label = { Text("Deskripsi Kelas (opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onCreateClass(
                                selectedTeacher.uid,
                                selectedTeacher.nama,
                                newClassName,
                                newClassGrade,
                                newClassYear,
                                newClassDesc
                            )
                            newClassName = ""
                            newClassDesc = ""
                        },
                        enabled = !busy && newClassName.trim().length >= 2,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_create_class_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Buat Kelas")
                        Spacer(Modifier.width(6.dp))
                        Text("Buat Kelas & Kode Unik")
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pengelolaan Mata Pelajaran Biologi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = subjTitle,
                        onValueChange = { subjTitle = it },
                        label = { Text("Judul Mata Pelajaran") },
                        modifier = Modifier.fillMaxWidth().testTag("admin_subject_title_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = subjGrade,
                            onValueChange = { subjGrade = it },
                            label = { Text("Tingkat") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = subjYear,
                            onValueChange = { subjYear = it },
                            label = { Text("Tahun Ajaran") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            onSaveSubject(primarySubject.copy(title = subjTitle, grade = subjGrade, academicYear = subjYear))
                        },
                        enabled = !busy && subjTitle.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_save_subject_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Simpan")
                        Spacer(Modifier.width(6.dp))
                        Text("Simpan Mata Pelajaran")
                    }
                }
            }
        }

        item {
            Text("Manajemen Seluruh Kelas (${classes.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        items(classes, key = { it.id }) { cls ->
            val memberCount = enrollments.count { it.classId == cls.id && it.status == "ACTIVE" }
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${cls.name} (Kode: ${cls.classCode})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Guru: ${cls.teacherName} • $memberCount siswa aktif", style = MaterialTheme.typography.bodySmall)
                        StatusPill(
                            text = if (cls.archived) "Diarsipkan" else "${cls.gradeLevel} • TA ${cls.academicYear}",
                            containerColor = if (cls.archived) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (cls.archived) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = { onToggleArchiveClass(cls) }) {
                        Icon(Icons.Default.Archive, contentDescription = "Arsipkan/Aktifkan Kelas")
                    }
                }
            }
        }

        item {
            Text("Ringkasan Modul & Materi (${modules.size} Modul, ${lessons.size} Topik)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        items(modules, key = { it.id }) { mod ->
            val modLessons = lessons.count { it.moduleId == mod.id }
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(mod.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text("${mod.meetingLabel} (${mod.durationMinutes} menit) • $modLessons topik aktif", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun AdminSystemSettingsTab(
    user: UserAccount,
    setting: SystemSetting,
    busy: Boolean,
    onUpdateName: (String) -> Unit,
    onSaveSetting: (SystemSetting) -> Unit,
    onOpenGlossary: () -> Unit,
    onSignOut: () -> Unit
) {
    var adminName by remember(user.nama) { mutableStateOf(user.nama) }
    var academicYear by remember(setting.academicYear) { mutableStateOf(setting.academicYear) }
    var semester by remember(setting.semester) { mutableStateOf(setting.semester) }
    var schoolName by remember(setting.schoolName) { mutableStateOf(setting.schoolName) }
    var allowRegistration by remember(setting.allowRegistration) { mutableStateOf(setting.allowRegistration) }
    var maintenanceMode by remember(setting.maintenanceMode) { mutableStateOf(setting.maintenanceMode) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Profil Administrator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Email terverifikasi: ${user.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = adminName,
                        onValueChange = { adminName = it },
                        label = { Text("Nama Tampilan Admin") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_profile_name_input")
                    )
                    Button(
                        onClick = { onUpdateName(adminName) },
                        enabled = !busy && adminName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_save_profile_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Simpan Profil")
                        Spacer(Modifier.width(8.dp))
                        Text("Simpan Profil Admin")
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tahun Ajaran & Pengaturan Sistem", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = schoolName, onValueChange = { schoolName = it }, label = { Text("Nama Sekolah / Satuan Pendidikan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = academicYear, onValueChange = { academicYear = it }, label = { Text("Tahun Ajaran (mis. 2026/2027)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Ganjil", "Genap").forEach { sem ->
                            FilterChip(selected = semester == sem, onClick = { semester = sem }, label = { Text("Semester $sem") }, modifier = Modifier.weight(1f).height(48.dp))
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Izinkan Pendaftaran Akun Baru", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = allowRegistration, onCheckedChange = { allowRegistration = it })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Mode Pemeliharaan Sistem", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = maintenanceMode, onCheckedChange = { maintenanceMode = it })
                    }
                    Button(
                        onClick = {
                            onSaveSetting(
                                setting.copy(
                                    schoolName = schoolName,
                                    academicYear = academicYear,
                                    semester = semester,
                                    allowRegistration = allowRegistration,
                                    maintenanceMode = maintenanceMode
                                )
                            )
                        },
                        enabled = !busy && academicYear.isNotBlank() && schoolName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("admin_save_settings_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Simpan Pengaturan")
                        Spacer(Modifier.width(8.dp))
                        Text("Simpan Pengaturan Sistem")
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

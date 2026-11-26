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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.data.model.PblGroup
import com.example.ui.components.StatusPill

@Composable
fun TeacherGroupMonitorCard(
    group: PblGroup,
    classEnrollments: List<Enrollment>,
    discussions: List<GroupDiscussionMessage>,
    busy: Boolean,
    onObserveDiscussions: (String?, String?) -> Unit,
    onAssignMembers: (PblGroup, String, List<String>, List<String>, Map<String, String>) -> Unit,
    onSaveComment: (String, String) -> Unit,
    onSendDiscussion: (PblGroup, Int, String) -> Unit
) {
    var expanded by remember(group.id) { mutableStateOf(false) }

    if (expanded) {
        DisposableEffect(group.id, group.classId) {
            onObserveDiscussions(group.id, group.classId)
            onDispose { onObserveDiscussions(null, null) }
        }
    }

    val selectedUids = remember(group.id, group.memberUids) {
        mutableStateListOf<String>().apply { addAll(group.memberUids) }
    }
    var leaderUid by remember(group.id, group.leaderUid) { mutableStateOf(group.leaderUid) }
    val roleMap = remember(group.id, group.memberRoles) {
        mutableStateMapOf<String, String>().apply { putAll(group.memberRoles) }
    }
    var teacherComment by remember(group.id, group.teacherComment) { mutableStateOf(group.teacherComment.orEmpty()) }
    var chatInput by remember(group.id) { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.testTag("teacher_group_card_${group.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Group, contentDescription = "Kelompok", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(group.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${group.memberUids.size} anggota • ${group.topic ?: "PBL Parijoto"}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                StatusPill("Fase ${group.pblPhase}/5", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
            }

            OutlinedButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("teacher_expand_group_${group.id}")
            ) {
                Icon(Icons.Default.Forum, contentDescription = "Pantau Kelompok")
                Spacer(Modifier.width(6.dp))
                Text(if (expanded) "Tutup Pemantauan Kelompok" else "Kelola Anggota, Jawaban & Diskusi")
            }

            if (expanded) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Jawaban 5 Fase Kelompok", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("F1 Masalah: ${group.rootProblem.orEmpty().ifBlank { "(Belum diisi)" }}", style = MaterialTheme.typography.bodySmall)
                        Text("F2 Hipotesis: ${group.hypothesis.orEmpty().ifBlank { "(Belum diisi)" }}", style = MaterialTheme.typography.bodySmall)
                        Text("F3 Penyelidikan: ${group.investigationNotes.orEmpty().ifBlank { "(Belum diisi)" }}", style = MaterialTheme.typography.bodySmall)
                        Text("F4 Solusi: ${group.conservationStrategy.orEmpty().ifBlank { "(Belum diisi)" }}", style = MaterialTheme.typography.bodySmall)
                        Text("Hasil Diskusi: ${group.discussionSummary.orEmpty().ifBlank { "(Belum diisi)" }}", style = MaterialTheme.typography.bodySmall)
                        Text("F5 Kampanye CER: ${group.campaignCaption.orEmpty().ifBlank { "(Belum diisi)" }}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (classEnrollments.isNotEmpty()) {
                    Text("Bagi Anggota & Tentukan Ketua Kelompok", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    classEnrollments.forEach { enr ->
                        val isMember = enr.studentUid in selectedUids
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Checkbox(
                                    checked = isMember,
                                    onCheckedChange = { checked ->
                                        if (checked && enr.studentUid !in selectedUids) {
                                            selectedUids.add(enr.studentUid)
                                            if (leaderUid.isBlank() || leaderUid == group.teacherUid) leaderUid = enr.studentUid
                                        } else if (!checked) {
                                            selectedUids.remove(enr.studentUid)
                                        }
                                    }
                                )
                                Text(enr.studentName, style = MaterialTheme.typography.bodySmall)
                            }
                            if (isMember) {
                                FilterChip(
                                    selected = leaderUid == enr.studentUid,
                                    onClick = {
                                        leaderUid = enr.studentUid
                                        roleMap[enr.studentUid] = "Ketua Kelompok"
                                    },
                                    label = { Text(if (leaderUid == enr.studentUid) "Ketua" else "Jadikan Ketua") },
                                    modifier = Modifier.height(48.dp)
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            val names = selectedUids.map { uid ->
                                classEnrollments.firstOrNull { it.studentUid == uid }?.studentName
                                    ?: group.memberNames.getOrNull(group.memberUids.indexOf(uid))
                                    ?: "Siswa"
                            }
                            val finalLeader = leaderUid.takeIf { it in selectedUids } ?: selectedUids.firstOrNull() ?: group.teacherUid
                            onAssignMembers(group, finalLeader, selectedUids.toList(), names, roleMap.toMap())
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Simpan Anggota")
                        Spacer(Modifier.width(6.dp))
                        Text("Simpan Anggota & Ketua Kelompok")
                    }
                }

                OutlinedTextField(
                    value = teacherComment,
                    onValueChange = { teacherComment = it },
                    label = { Text("Komentar & Arahan Guru untuk Kelompok") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("teacher_group_comment_input_${group.id}")
                )
                Button(
                    onClick = { onSaveComment(group.id, teacherComment) },
                    enabled = !busy && teacherComment.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("teacher_save_group_comment_${group.id}")
                ) {
                    Text("Kirim Catatan Bimbingan Kelompok")
                }

                Text("Ruang Diskusi Kelompok (${discussions.size} pesan)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                discussions.takeLast(5).forEach { msg ->
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            Text("${msg.senderName} (${msg.senderRole} • F${msg.pblPhase})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(msg.message, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        label = { Text("Balas di Ruang Diskusi...") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (chatInput.isNotBlank()) {
                                onSendDiscussion(group, group.pblPhase, chatInput)
                                chatInput = ""
                            }
                        },
                        enabled = !busy && chatInput.isNotBlank()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim Balasan", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherCreateGroupAndAssignmentCards(
    classes: List<ClassRoom>,
    groupsCount: Int,
    assignments: List<Assignment>,
    busy: Boolean,
    onCreateGroup: (String, String, String) -> Unit,
    onCreateAssignment: (String, String, Int, String, String, String, Int) -> Unit
) {
    val activeClass = classes.firstOrNull()
    val hasClass = activeClass != null
    var grpName by remember { mutableStateOf("") }
    var grpTopic by remember { mutableStateOf("Konservasi Parijoto Gunung Muria") }

    if (!hasClass) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).testTag("teacher_no_class_pbl_warning")
        ) {
            Text(
                "Buat kelas terlebih dahulu di tab Kelas sebelum membuat kelompok atau tugas agar siswa dapat bergabung dan tersinkronisasi.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp)
            )
        }
    }

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Buat Kelompok PBL Kelas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = grpName, onValueChange = { grpName = it }, label = { Text("Nama Kelompok") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = grpTopic, onValueChange = { grpTopic = it }, label = { Text("Topik Fokus PBL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    val targetClassId = activeClass?.id ?: return@Button
                    onCreateGroup(targetClassId, grpName, grpTopic)
                    grpName = ""
                },
                enabled = !busy && hasClass && grpName.trim().length >= 2,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("teacher_create_group_button")
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = "Buat Kelompok")
                Spacer(Modifier.width(6.dp))
                Text("Tambah Kelompok PBL ($groupsCount)")
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    var asgTitle by remember { mutableStateOf("") }
    var asgInstr by remember { mutableStateOf("") }
    var asgType by remember { mutableStateOf("E_LKM_2") }

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Buat Tugas / E-LKM Tambahan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("E_LKM_1", "E_LKM_2", "PROYEK_CER").forEach { t ->
                    FilterChip(selected = asgType == t, onClick = { asgType = t }, label = { Text(t) }, modifier = Modifier.height(48.dp))
                }
            }
            OutlinedTextField(value = asgTitle, onValueChange = { asgTitle = it }, label = { Text("Judul Tugas / E-LKM") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = asgInstr, onValueChange = { asgInstr = it }, label = { Text("Instruksi Langkah Kerja PBL") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    val targetClassId = activeClass?.id ?: return@Button
                    onCreateAssignment(targetClassId, "modul_2_konservasi", 3, asgTitle, asgInstr, asgType, 2)
                    asgTitle = ""; asgInstr = ""
                },
                enabled = !busy && hasClass && asgTitle.isNotBlank() && asgInstr.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Publikasikan Tugas (${assignments.size})")
            }
        }
    }
}

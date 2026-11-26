package com.example.ui.student

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ai.AiEvidenceAnalysis
import com.example.data.curriculum.ParijotoAssessmentsAndGlossary
import com.example.data.model.Assignment
import com.example.data.model.Enrollment
import com.example.data.model.GroupDiscussionMessage
import com.example.data.model.PblGroup
import com.example.data.model.PeerReview
import com.example.data.model.Reflection
import com.example.data.model.Submission
import com.example.data.model.UserAccount
import com.example.ui.components.StatusPill

@Composable
fun StudentPblTab(
    user: UserAccount,
    enrollment: Enrollment?,
    myGroups: List<PblGroup>,
    openGroups: List<PblGroup>,
    groupDiscussions: List<GroupDiscussionMessage>,
    assignments: List<Assignment>,
    submissions: List<Submission>,
    classPresentations: List<Submission>,
    peerReviews: List<PeerReview>,
    reflections: List<Reflection>,
    aiEvidence: AiEvidenceAnalysis?,
    aiLoading: Boolean,
    busy: Boolean,
    onCreateGroup: (String, String) -> Unit,
    onJoinGroup: (PblGroup) -> Unit,
    onObserveGroupDiscussion: (String?, String?) -> Unit,
    onUpdateGroupWorkspace: (String, String?, Int, String?, String?, String?, String?, String?, String?, Map<String, String>?) -> Unit,
    onUpdateGroupLeaderAndRoles: (String, String, Map<String, String>, Boolean) -> Unit,
    onSendGroupDiscussion: (PblGroup, String, Int, String) -> Unit,
    onSaveSubmission: (Submission, Boolean) -> Unit,
    onSubmitPeerReview: (Submission, Int, Int, Int, String) -> Unit,
    onSubmitPblReflection: (List<Boolean>, String, String) -> Unit,
    onAnalyzeEvidenceAi: (String) -> Unit
) {
    var activeAssignment by remember { mutableStateOf<Assignment?>(null) }
    var activeGroup by remember { mutableStateOf<PblGroup?>(null) }
    var showPresentations by remember { mutableStateOf(false) }

    var selectedPblPhase by remember { mutableIntStateOf(myGroups.firstOrNull()?.pblPhase ?: 1) }
    var showCreateGroupForm by remember { mutableStateOf(false) }
    var groupNameInput by remember { mutableStateOf("") }
    var groupTopicInput by remember { mutableStateOf("Konservasi Parijoto Lereng Muria") }

    val existingPblReflection = reflections.firstOrNull { it.moduleId == "pbl_phase_5" }
    val pblChecklistItems = remember {
        listOf(
            "Saya aktif berdiskusi dan membagi tugas secara adil di kelompok PBL.",
            "Saya mampu membedakan bukti praklinis, komputasi, dan klinis pada etnosains Parijoto.",
            "Saya menganalisis data mikroklimat (2019–2023) & flavonoid untuk merancang solusi konservasi.",
            "Saya mempresentasikan argumen CER dan menghargai umpan balik teman serta guru."
        )
    }
    val checklistState = remember(existingPblReflection?.id) {
        mutableStateListOf<Boolean>().apply {
            val saved = existingPblReflection?.answers ?: emptyList()
            pblChecklistItems.indices.forEach { idx -> add(saved.getOrNull(idx) ?: false) }
        }
    }
    var pblReflectionNote by remember(existingPblReflection?.id) {
        mutableStateOf(existingPblReflection?.pblReflection.orEmpty())
    }
    var pblNeedReviewNote by remember(existingPblReflection?.id) {
        mutableStateOf(existingPblReflection?.notes.orEmpty())
    }

    if (activeAssignment != null) {
        val currentAsg = activeAssignment!!
        val existingSub = submissions.firstOrNull { it.assignmentId == currentAsg.id }
        BackHandler { activeAssignment = null }
        AssignmentEditorScreen(
            user = user,
            enrollment = enrollment,
            group = myGroups.firstOrNull(),
            assignment = currentAsg,
            existingSubmission = existingSub,
            aiEvidence = aiEvidence,
            aiLoading = aiLoading,
            busy = busy,
            onBack = { activeAssignment = null },
            onSaveSubmission = onSaveSubmission,
            onAnalyzeEvidenceAi = onAnalyzeEvidenceAi
        )
        return
    }

    if (activeGroup != null) {
        val currentGroup = myGroups.firstOrNull { it.id == activeGroup?.id } ?: activeGroup!!
        BackHandler { activeGroup = null }
        GroupWorkspaceScreen(
            user = user,
            group = currentGroup,
            discussions = groupDiscussions,
            busy = busy,
            onBack = { activeGroup = null },
            onObserveDiscussions = onObserveGroupDiscussion,
            onSaveWorkspace = onUpdateGroupWorkspace,
            onUpdateLeaderAndRoles = onUpdateGroupLeaderAndRoles,
            onSendDiscussionMessage = onSendGroupDiscussion
        )
        return
    }

    if (showPresentations) {
        BackHandler { showPresentations = false }
        ProjectPresentationAndPeerReviewScreen(
            user = user,
            assignments = assignments,
            presentations = classPresentations,
            peerReviews = peerReviews,
            busy = busy,
            onBack = { showPresentations = false },
            onSubmitPeerReview = onSubmitPeerReview
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("student_pbl_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Alur Problem Based Learning (PBL) 5 Fase", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        items((1..5).toList(), key = { it }) { phase ->
                            FilterChip(
                                selected = selectedPblPhase == phase,
                                onClick = { selectedPblPhase = phase },
                                label = { Text("Fase $phase", maxLines = 1) },
                                modifier = Modifier.height(48.dp).testTag("pbl_stepper_phase_$phase")
                            )
                        }
                    }
                    Text(phaseTitle(selectedPblPhase), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(phaseDescription(selectedPblPhase), style = MaterialTheme.typography.bodySmall)
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
                    "Kelompok PBL Saya (${myGroups.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { showCreateGroupForm = !showCreateGroupForm },
                    modifier = Modifier.height(48.dp).testTag("toggle_create_group_button")
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = "Buat Kelompok")
                    Spacer(Modifier.width(6.dp))
                    Text("Buat Kelompok")
                }
            }
        }

        if (showCreateGroupForm) {
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = groupNameInput,
                            onValueChange = { groupNameInput = it },
                            label = { Text("Nama Kelompok (mis. Kelompok Epifit 1)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("create_group_name_input")
                        )
                        OutlinedTextField(
                            value = groupTopicInput,
                            onValueChange = { groupTopicInput = it },
                            label = { Text("Fokus Topik Investigasi") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                onCreateGroup(groupNameInput, groupTopicInput)
                                groupNameInput = ""
                                showCreateGroupForm = false
                            },
                            enabled = !busy && groupNameInput.trim().length >= 2,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("create_group_submit_button")
                        ) {
                            Text("Simpan Kelompok PBL")
                        }
                    }
                }
            }
        }

        items(myGroups, key = { it.id }) { grp ->
            val leaderIdx = grp.memberUids.indexOf(grp.leaderUid)
            val leaderName = if (leaderIdx >= 0) grp.memberNames.getOrNull(leaderIdx) ?: "-" else "-"
            Card(
                onClick = { activeGroup = grp },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.testTag("my_group_card_${grp.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Group, contentDescription = "Kelompok", tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(grp.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        StatusPill(
                            text = "Fase ${grp.pblPhase}/5",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text("Ketua: $leaderName • Anggota: ${grp.memberNames.joinToString(", ").ifBlank { "1 anggota" }}", style = MaterialTheme.typography.bodySmall)
                    if (!grp.teacherComment.isNullOrBlank()) {
                        Text("Catatan Guru: ${grp.teacherComment}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, maxLines = 2)
                    }
                    Text("Ketuk untuk membuka Ruang Kerja 5 Fase, Pembagian Peran & Ruang Diskusi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        val joinableGroups = openGroups.filter { grp ->
            (enrollment == null || grp.classId == enrollment.classId) && !grp.memberUids.contains(user.uid)
        }
        if (joinableGroups.isNotEmpty()) {
            item {
                Text("Kelompok Terbuka di Kelas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            items(joinableGroups, key = { "open_${it.id}" }) { grp ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(grp.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${grp.memberUids.size}/12 anggota • ${grp.topic ?: "Etnosains & Konservasi"}", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { onJoinGroup(grp) }, enabled = !busy, modifier = Modifier.height(48.dp)) {
                            Text("Gabung")
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("Lembar Kerja Digital (E-LKM) & Proyek Kampanye CER", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }

        items(assignments, key = { it.id }) { asg ->
            val sub = submissions.firstOrNull { it.assignmentId == asg.id }
            Card(
                onClick = { activeAssignment = asg },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.testTag("assignment_card_${asg.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Tugas", tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(8.dp))
                            Text(asg.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        StatusPill(
                            text = submissionStatusLabel(sub?.status),
                            containerColor = when (sub?.status) {
                                "REVISION_REQUIRED" -> MaterialTheme.colorScheme.errorContainer
                                "GRADED", "SUBMITTED" -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            },
                            contentColor = when (sub?.status) {
                                "REVISION_REQUIRED" -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    }
                    Text(asg.instructions, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                    val teacherFb = sub?.teacherFeedback
                    if (!teacherFb.isNullOrBlank()) {
                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)) {
                            Text(
                                "Umpan Balik Guru: $teacherFb",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Slideshow, contentDescription = "Presentasi", tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Fase 5: Presentasi Hasil Proyek & Penilaian Antarteman", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    val myReviewCount = peerReviews.count { it.reviewerUid == user.uid }
                    Text(
                        "${classPresentations.size} karya kelompok/siswa siap ditinjau di kelas • Anda telah memberi $myReviewCount ulasan teman.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = { showPresentations = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_presentations_button")
                    ) {
                        Icon(Icons.Default.RateReview, contentDescription = "Buka Galeri")
                        Spacer(Modifier.width(8.dp))
                        Text("Buka Galeri Presentasi & Nilai Teman")
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SelfImprovement, contentDescription = "Refleksi Individu", tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Refleksi Individu Pembelajaran PBL", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        if (existingPblReflection != null) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Tersimpan", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    pblChecklistItems.forEachIndexed { idx, prompt ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(
                                checked = checklistState[idx],
                                onCheckedChange = { checklistState[idx] = it }
                            )
                            Text(prompt, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        }
                    }
                    OutlinedTextField(
                        value = pblReflectionNote,
                        onValueChange = { pblReflectionNote = it },
                        label = { Text("Refleksi pribadi: konsep baru & kontribusi di kelompok") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("pbl_individual_reflection_input")
                    )
                    OutlinedTextField(
                        value = pblNeedReviewNote,
                        onValueChange = { pblNeedReviewNote = it },
                        label = { Text("Hal yang masih perlu didalami (opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onSubmitPblReflection(checklistState.toList(), pblNeedReviewNote, pblReflectionNote) },
                        enabled = !busy && pblReflectionNote.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit_pbl_reflection_button")
                    ) {
                        Text(if (existingPblReflection == null) "Simpan Refleksi Individu PBL" else "Perbarui Refleksi Individu PBL")
                    }
                }
            }
        }
    }
}

private fun phaseTitle(phase: Int): String = when (phase) {
    1 -> "1. Orientasi terhadap Masalah"
    2 -> "2. Mengorganisasikan Peserta Didik"
    3 -> "3. Penyelidikan Mandiri & Kelompok"
    4 -> "4. Mengembangkan & Menyajikan Solusi"
    else -> "5. Evaluasi, Presentasi & Refleksi"
}

private fun phaseDescription(phase: Int): String = when (phase) {
    1 -> "Mengamati fenomena nyata tradisi mitoni, bioprospeksi Parijoto, & Paradoks Konservasi di Lereng Muria."
    2 -> "Membentuk kelompok PBL, menentukan ketua & peran anggota, serta merumuskan hipotesis di E-LKM 1."
    3 -> "Mengumpulkan bukti ilmiah (praklinis/komputasi/klinis), analisis data mikroklimat 2019–2023 & flavonoid Vifta 2021."
    4 -> "Merumuskan solusi konservasi in-situ (panen 30% + ornitokori), ex-situ (kultur jaringan totipotensi), & IG."
    else -> "Mempresentasikan kampanye digital CER, melakukan penilaian antarteman, menindaklanjuti koreksi guru, & refleksi individu."
}
